"""
test_chapter_content_source.py
职责说明：验证逐章接口的取数策略——优先从整本全文切分，失败时退回逐章读取。
执行流程：以离线样本桩掉上游 -> 断言走全文路径时不产生逐章调用，且返回正文格式与库实现一致。

背景：整本导入需连续读取全部章节，逐章请求会被适配层 5 秒/次的节流拖成十几分钟，
且极易触发 Cloudflare 拦截。改为从整本全文切分后，单本导入的上游调用降到 1 次。
本用例守护三件事：优化路径确实被走到、缓存存的是章节正文而非整段响应、降级路径不退化。

注意：不在此处比对「全文切分」与「真实逐章接口」的输出——那需要真实上游。
两者的格式契约由 samples/ 样本与 test_全文切分保留原始格式 共同钉住。

运行：.venv/bin/python tests/test_chapter_content_source.py
"""
from __future__ import annotations

import asyncio
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

import app.main as main
from wenku8.utils import get_chapter_content

SAMPLE = pathlib.Path(__file__).resolve().parents[1] / "samples" / "full-content-sample.json"


def load_sample() -> dict:
    return json.loads(SAMPLE.read_text(encoding="utf-8"))


class _Chapter:
    def __init__(self, cid: int, title: str) -> None:
        self.cid, self.title = cid, title


class _Volume:
    def __init__(self, vid: int, title: str, chapters: list) -> None:
        self.vid, self.title, self.chapters = vid, title, chapters


class FakeIndex:
    def __init__(self, volumes: list) -> None:
        self.volumes = volumes


def build_index(chapters: list[dict]) -> FakeIndex:
    grouped: dict[str, list[dict]] = {}
    for chapter in chapters:
        grouped.setdefault(chapter.get("volumeTitle") or "", []).append(chapter)
    return FakeIndex([
        _Volume(
            vid=index,
            title=volume_title,
            chapters=[_Chapter(int(c["chapterId"]), c["title"]) for c in items],
        )
        for index, (volume_title, items) in enumerate(grouped.items())
    ])


class patched:
    """临时替换 app.main 的上游调用，退出时还原，避免用例之间互相污染。"""

    def __init__(self, **kwargs):
        self.kwargs = kwargs
        self.saved: dict = {}

    def __enter__(self):
        for name, value in self.kwargs.items():
            self.saved[name] = getattr(main, name)
            setattr(main, name, value)
        main._cache.clear()  # 缓存会跨用例保留，必须清空
        return self

    def __exit__(self, *exc):
        for name, value in self.saved.items():
            setattr(main, name, value)
        main._cache.clear()
        return False


async def test_优先走全文切分且不产生逐章调用() -> None:
    sample = load_sample()
    index = build_index(sample["chapters"])
    cid = int(sample["chapters"][3]["chapterId"])
    expected = get_chapter_content(sample["content"], index, cid)

    per_chapter_calls: list[int] = []

    async def fake_full(novel_id: int) -> dict:
        return sample

    async def fake_index(novel_id: int) -> FakeIndex:
        return index

    async def fake_content(novel_id: int, chapter_id: int) -> str:
        per_chapter_calls.append(chapter_id)
        return "不应被调用的逐章结果"

    with patched(_full_content=fake_full, _novel_index=fake_index, _content=fake_content):
        page = await main._content_frame(1, cid)

    assert page == expected, "全文路径返回的正文与库切分结果不一致"
    assert per_chapter_calls == [], f"走全文路径时不应触发逐章请求，实际={per_chapter_calls}"
    print(f"  章节 {cid} 走全文切分，逐章调用 0 次")


async def test_重复请求命中缓存而非重复切分() -> None:
    """_cached 存的是章节正文本身；若误存整段响应，这里会取到 dict 而非 str。"""
    sample = load_sample()
    index = build_index(sample["chapters"])
    cid = int(sample["chapters"][1]["chapterId"])
    full_calls: list[int] = []

    async def fake_full(novel_id: int) -> dict:
        full_calls.append(novel_id)
        return sample

    async def fake_index(novel_id: int) -> FakeIndex:
        return index

    async def fake_content(novel_id: int, chapter_id: int) -> str:
        raise AssertionError("不应走到逐章路径")

    with patched(_full_content=fake_full, _novel_index=fake_index, _content=fake_content):
        first = await main._content_frame(1, cid)
        second = await main._content_frame(1, cid)

    assert isinstance(first, str) and isinstance(second, str), "缓存值应为字符串正文"
    assert first == second
    assert len(full_calls) == 1, f"第二次请求应命中缓存，实际全文调用 {len(full_calls)} 次"
    print("  第二次请求命中缓存，全文调用 1 次")


async def test_全文不可用时退回逐章路径() -> None:
    """版权受限等情况下全文不可用，必须仍能逐章读到内容，不得整体失败。"""
    calls: list[int] = []

    async def failing_full(novel_id: int):
        raise main.UpstreamError("CONTENT_UNAVAILABLE", "该小说暂不可连续阅读")

    async def fake_index(novel_id: int) -> FakeIndex:
        raise AssertionError("全文不可用时不应继续读取目录")

    async def fake_content(novel_id: int, chapter_id: int) -> str:
        calls.append(chapter_id)
        return "逐章兜底正文"

    with patched(_full_content=failing_full, _novel_index=fake_index, _content=fake_content):
        page = await main._content_frame(1, 999)

    assert page == "逐章兜底正文"
    assert calls == [999], "应恰好退回一次逐章读取"
    print("  全文不可用时正确退回逐章路径")


async def test_全文切分保留原始格式() -> None:
    """回归基线：正文使用 CRLF 与普通空格缩进，与 pywenku8api 的输出保持一致。"""
    sample = load_sample()
    index = build_index(sample["chapters"])
    cid = int(sample["chapters"][0]["chapterId"])
    text = get_chapter_content(sample["content"], index, cid)

    assert "\r\n" in text, "正文应保留 CRLF 换行"
    assert "\xa0" not in text, "正文不应含 NBSP 缩进（那是下游渲染层的转换，不属于适配层输出）"
    assert text.strip(), "正文不应为空"
    print(f"  章节 {cid} 长度={len(text)}，格式符合基线")


async def main_async() -> None:
    print("test_chapter_content_source.py")
    await test_优先走全文切分且不产生逐章调用()
    await test_重复请求命中缓存而非重复切分()
    await test_全文不可用时退回逐章路径()
    await test_全文切分保留原始格式()
    print("全部通过")


if __name__ == "__main__":
    asyncio.run(main_async())

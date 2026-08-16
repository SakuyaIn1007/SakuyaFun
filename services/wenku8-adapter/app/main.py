"""
main.py
职责说明：以真实 pywenku8api 异步接口提供受控的内部 HTTP 适配层。
执行流程：启动时创建 API 客户端并读取仅驻留内存的凭据 -> 请求进入缓存/并发控制 -> 调用上游 -> 输出稳定 DTO 或错误码。
"""
from __future__ import annotations

import asyncio
import logging
import os
import time
from collections.abc import Awaitable, Callable
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException, Query, Response
import httpx
import zendriver

from wenku8 import Wenku8API
from wenku8.consts import NovelSortMethod
from wenku8.exceptions import CloudflareChallengeException, NotLoggedInException, PageParseError, RateLimitException

app = FastAPI(title="Sakuya Wenku8 Adapter", docs_url=None, redoc_url=None)
logger = logging.getLogger("wenku8_adapter")
_enabled = os.getenv("WENKU8_ENABLED", "false").lower() == "true"
_username, _password = os.getenv("WENKU8_USERNAME"), os.getenv("WENKU8_PASSWORD")
_macos_chrome = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
_browser_path = os.getenv("WENKU8_BROWSER_PATH") or (_macos_chrome if Path(_macos_chrome).is_file() else None)


class ConfiguredWenku8API(Wenku8API):
    """为部署环境显式指定 Chrome，避免 zendriver 在 macOS 上自动发现浏览器失败。"""

    async def _ensure_browser(self):
        if self._browser is None:
            async with self._browser_lock:
                if self._browser is None:
                    browser_args = None
                    if self.proxy:
                        # Chromium 的 SOCKS5 配置使用代理端 DNS，和 socks5h 语义一致。
                        browser_args = [f"--proxy-server={self.proxy.replace('socks5h://', 'socks5://')}"]
                    self._browser = await zendriver.start(config=zendriver.Config(
                        headless=os.getenv("WENKU8_BROWSER_HEADLESS", "true").lower() == "true",
                        sandbox=False,
                        browser_executable_path=_browser_path,
                        browser_args=browser_args,
                        # macOS 首次拉起 Chrome 有时超过 zendriver 的默认 2.5 秒连接窗口。
                        browser_connection_timeout=1.0,
                        browser_connection_max_tries=30,
                    ))
        return self._browser


_api = ConfiguredWenku8API(proxy=os.getenv("WENKU8_PROXY") or None)
_login_lock = asyncio.Lock()
_session_verified = False
_semaphore = asyncio.Semaphore(int(os.getenv("WENKU8_MAX_CONCURRENCY", "2")))
_cache: dict[str, tuple[float, Any]] = {}
_cache_ttl = int(os.getenv("WENKU8_CACHE_SECONDS", "120"))
# 全文是大响应且 pywenku8api 自身会在内存保留 30 分钟；适配层使用同样的默认时长，
# 避免同一本小说在用户连续选择章节时重复请求 CDN。该缓存不会落盘。
_full_content_cache_ttl = int(os.getenv("WENKU8_FULL_CONTENT_CACHE_SECONDS", "1800"))
_full_content_timeout = max(75, int(os.getenv("WENKU8_FULL_CONTENT_TIMEOUT_SECONDS", "180")))


class UpstreamError(Exception):
    def __init__(self, code: str, message: str): self.code, self.message = code, message; super().__init__(message)


async def _require_login() -> Wenku8API:
    """目录/详情/正文均被库标记为 login_required；凭据只来自部署环境且不返回给调用方。"""
    if not _enabled: raise UpstreamError("ADAPTER_DISABLED", "Wenku8 内部验证服务尚未启用")
    global _session_verified
    if _api.is_logged_in and _session_verified: return _api
    if not _username or not _password: raise UpstreamError("UPSTREAM_AUTH_REQUIRED", "适配服务未配置 Wenku8 内部验证账号")
    async with _login_lock:
        if not _session_verified:
            try:
                if not _api.is_logged_in: await _api.login(_username, _password)
                # pywenku8api 的 login 只检查是否获得 PHPSESSID，不会校验密码是否真正登录成功。
                # 读取书架可验证会话已认证；空书架仍是合法响应。
                await _api.get_bookshelf()
                _session_verified = True
            except Exception as error: raise _map_error(error) from error
    return _api


def _map_error(error: Exception) -> UpstreamError:
    # 详细网页片段只写入受控服务端日志，绝不返回客户端，避免泄露登录态或上游 HTML。
    logger.exception("Wenku8 上游调用失败：%s", error)
    if "Failed to connect to browser" in str(error):
        return UpstreamError("BROWSER_UNAVAILABLE", "Wenku8 登录浏览器启动失败，请检查 Chrome 与 WENKU8_BROWSER_PATH")
    if isinstance(error, (CloudflareChallengeException, RateLimitException)): return UpstreamError("UPSTREAM_BLOCKED", "Wenku8 被 Cloudflare 或限流拦截")
    if isinstance(error, NotLoggedInException): return UpstreamError("UPSTREAM_AUTH_REQUIRED", "Wenku8 登录已失效")
    if isinstance(error, PageParseError):
        if "login.php" in error.html.lower() or "登录" in error.html:
            return UpstreamError("UPSTREAM_AUTH_REQUIRED", "Wenku8 登录未成功，请检查账号、密码或网站登录流程")
        return UpstreamError("CONTENT_UNAVAILABLE", "该内容不可阅读或上游页面结构已变化")
    if isinstance(error, httpx.HTTPStatusError):
        # 全文 TXT 来自 CDN；403/429 表示上游明确拒绝或限流，其他 4xx 通常是版权/资源不存在。
        if error.response.status_code in (403, 429):
            return UpstreamError("UPSTREAM_BLOCKED", "Wenku8 被 Cloudflare 或限流拦截")
        if 400 <= error.response.status_code < 500:
            return UpstreamError("CONTENT_UNAVAILABLE", "该内容不可阅读或不存在")
    if isinstance(error, TimeoutError): return UpstreamError("UPSTREAM_TIMEOUT", "Wenku8 响应超时")
    return UpstreamError("UPSTREAM_UNAVAILABLE", "Wenku8 上游服务异常")


async def _cached(
    key: str,
    work: Callable[[], Awaitable[Any]],
    *,
    cache_seconds: int | None = None,
    timeout_seconds: int = 75,
) -> Any:
    """统一执行上游读取；调用方可为大响应单独指定内存缓存与超时，不改变普通接口默认值。"""
    ttl = _cache_ttl if cache_seconds is None else cache_seconds
    cached = _cache.get(key)
    if cached and time.monotonic() - cached[0] < ttl: return cached[1]
    async with _semaphore:
        try: result = await asyncio.wait_for(work(), timeout=timeout_seconds)
        except UpstreamError: raise
        except Exception as error: raise _map_error(error) from error
    _cache[key] = (time.monotonic(), result)
    return result


def _raise(error: UpstreamError) -> None:
    status = 504 if error.code == "UPSTREAM_TIMEOUT" else 503
    raise HTTPException(status_code=status, detail={"code": error.code, "message": error.message})


def _novel_dto(item: Any) -> dict[str, Any]:
    return {"id": str(item.aid), "title": item.title, "author": item.author, "description": item.intro_preview, "status": item.status, "tags": item.tags, "copyright": item.copyright}


@app.on_event("shutdown")
async def close_api() -> None: await _api.close()

@app.get("/health")
async def health(verify: bool = Query(default=False)) -> dict[str, Any]:
    """verify=true 会真实校验登录态，便于部署排障；默认不启动浏览器，保持健康检查轻量。"""
    if verify:
        try: await _require_login()
        except UpstreamError as error: _raise(error)
    return {"status": "ok", "enabled": _enabled, "credentialsConfigured": bool(_username and _password), "sessionVerified": _session_verified}

@app.get("/novels/search")
async def search(keyword: str = Query(min_length=1, max_length=80), page: int = Query(default=0, ge=0)) -> dict[str, Any]:
    try:
        # pywenku8api 使用从 1 开始的页码，应用网关继续维持从 0 开始的分页契约。
        result = await _cached(f"search:{keyword}:{page}", lambda: _search(keyword, page))
        return {"items": [_novel_dto(item) for item in result.results], "nextPage": page + 1 if result.page_control.now < result.page_control.end else None}
    except UpstreamError as error: _raise(error)

async def _search(keyword: str, page: int): return await (await _require_login()).search_novel_by_name(keyword, page=page + 1)

@app.get("/novels/{novel_id}")
async def novel(novel_id: int) -> dict[str, Any]:
    try:
        info = await _cached(f"novel:{novel_id}", lambda: _info(novel_id))
        return {"id": str(info.aid), "title": info.title, "author": info.author, "description": info.intro, "status": info.status, "tags": info.tags, "copyright": info.copyright}
    except UpstreamError as error: _raise(error)

async def _info(novel_id: int): return await (await _require_login()).get_novel_info(novel_id)

@app.get("/novels/{novel_id}/chapters")
async def chapters(novel_id: int) -> dict[str, Any]:
    try:
        index = await _cached(f"index:{novel_id}", lambda: _index(novel_id))
        return {"volumes": [{"id": str(volume.vid), "title": volume.title, "chapters": [{"id": str(chapter.cid), "title": chapter.title, "order": order} for order, chapter in enumerate(volume.chapters)]} for volume in index.volumes]}
    except UpstreamError as error: _raise(error)

async def _index(novel_id: int): return await (await _require_login()).get_novel_index(novel_id)


def _chapter_anchors(full_text: str, index: Any) -> list[dict[str, Any]]:
    """将目录中的卷名和章节名映射为全文字符偏移量。

    pywenku8api 的整本 TXT 以“卷名 + 空格 + 章节名”作为章节边界。按目录顺序从
    上一次命中位置继续检索，能处理同名章节；若上游 TXT 与目录不一致，则返回 -1，
    让后续客户端安全降级而不是跳到错误位置。
    """
    anchors: list[dict[str, Any]] = []
    search_from = 0
    for volume in index.volumes:
        for chapter in volume.chapters:
            header = f"{volume.title} {chapter.title}"
            offset = full_text.find(header, search_from)
            if offset >= 0:
                search_from = offset + len(header)
            anchors.append({
                "chapterId": str(chapter.cid),
                "title": chapter.title,
                "volumeTitle": volume.title,
                "offset": offset,
            })
    return anchors


@app.get("/novels/{novel_id}/full-content")
async def full_content(novel_id: int) -> dict[str, Any]:
    """读取整本可访问 TXT，并返回目录章节在文本中的锚点；正文仅驻留在服务内存缓存。"""
    try:
        document = await _cached(
            f"full-content:{novel_id}",
            lambda: _full_content(novel_id),
            cache_seconds=_full_content_cache_ttl,
            timeout_seconds=_full_content_timeout,
        )
        if not document["content"].strip():
            raise UpstreamError("CONTENT_UNAVAILABLE", "该小说暂不可连续阅读")
        return document
    except UpstreamError as error: _raise(error)


async def _full_content(novel_id: int) -> dict[str, Any]:
    """先下载整本 TXT，再读取同一小说目录计算锚点；全程不写入数据库、文件或日志。"""
    api = await _require_login()
    content = await api.get_full_novel_content(novel_id)
    index = await api.get_novel_index(novel_id)
    return {
        "novelId": str(novel_id),
        "title": index.title,
        "content": content,
        "chapters": _chapter_anchors(content, index),
    }

@app.get("/catalog/novels")
async def novel_list(sort: str = Query(default="lastupdate"), page: int = Query(default=0, ge=0)) -> dict[str, Any]:
    """公开列表仅接受库内定义的排序值，避免将未校验参数拼入上游 URL。"""
    try:
        method = NovelSortMethod(sort)
        result = await _cached(f"list:{sort}:{page}", lambda: _list(method, page))
        return {"items": [_novel_dto(item) for item in result.results], "nextPage": page + 1 if result.page_control.now < result.page_control.end else None}
    except ValueError: raise HTTPException(status_code=400, detail={"code": "INVALID_SORT", "message": "不支持的小说排序方式"})
    except UpstreamError as error: _raise(error)

async def _list(sort: NovelSortMethod, page: int): return await (await _require_login()).get_novel_list(sort, page=page + 1)

@app.get("/novels/{novel_id}/cover")
async def cover(novel_id: int) -> Response:
    try:
        image = await _cached(f"cover:{novel_id}", lambda: _cover(novel_id))
        return Response(content=image, media_type="image/jpeg")
    except UpstreamError as error: _raise(error)

async def _cover(novel_id: int):
    if not _enabled: raise UpstreamError("ADAPTER_DISABLED", "Wenku8 内部验证服务尚未启用")
    return await _api.get_novel_cover(novel_id)

@app.get("/chapters/{chapter_id}/content")
async def content(chapter_id: int, novel_id: int = Query(alias="novelId")) -> dict[str, Any]:
    try:
        text = await _cached(f"content:{novel_id}:{chapter_id}", lambda: _content(novel_id, chapter_id))
        if not text.strip(): raise UpstreamError("CONTENT_UNAVAILABLE", "该章节暂不可阅读")
        return {"novelId": str(novel_id), "chapterId": str(chapter_id), "content": text}
    except UpstreamError as error: _raise(error)

async def _content(novel_id: int, chapter_id: int): return await (await _require_login()).get_novel_content(novel_id, chapter_id)

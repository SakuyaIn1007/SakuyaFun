"""
test_singleflight.py
职责说明：验证同一 key 的并发请求被合并成一次上游调用，且失败/取消的传播语义正确。
执行流程：直接以 .venv/bin/python tests/test_singleflight.py 运行（不依赖 pytest）；
          用 asyncio.Event 卡住 work，在不真实等待的前提下断言调用次数与结果共享。

与 test_throttle.py 同风格：pytest 在本仓库是**刻意的非依赖**，新测试也自带运行器。
"""
from __future__ import annotations

import asyncio
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

from app.singleflight import SingleFlight


async def _drain() -> None:
    """让已排入事件循环的回调（含 add_done_callback）有机会执行。"""
    for _ in range(5):
        await asyncio.sleep(0)


def test_concurrent_same_key_runs_work_once() -> None:
    """同一 key 的 N 个并发调用只应真正执行一次 work——这是本模块存在的全部理由。"""
    async def scenario() -> int:
        flight = SingleFlight()
        started = asyncio.Event()
        release = asyncio.Event()
        calls = 0

        async def work() -> str:
            nonlocal calls
            calls += 1
            started.set()
            await release.wait()
            return "value"

        tasks = [asyncio.create_task(flight.run("k", work)) for _ in range(5)]
        await started.wait()
        await _drain()
        release.set()
        results = await asyncio.gather(*tasks)
        return calls, results

    calls, results = asyncio.run(scenario())
    assert calls == 1, f"work 应只执行 1 次，实际 {calls}"
    assert set(results) == {"value"}, f"全部等待者应拿到同一结果，实际 {results}"


def test_waiters_share_identical_result_object() -> None:
    """等待者拿到的是同一个对象，而非各自重新计算出的等值副本。"""
    async def scenario() -> list:
        flight = SingleFlight()
        release = asyncio.Event()

        async def work() -> dict:
            await release.wait()
            return {"payload": [1, 2, 3]}

        tasks = [asyncio.create_task(flight.run("k", work)) for _ in range(3)]
        await _drain()
        release.set()
        results = await asyncio.gather(*tasks)
        return results

    results = asyncio.run(scenario())
    assert all(item is results[0] for item in results), "等待者应共享同一个结果对象"


def test_different_keys_are_not_merged() -> None:
    """不同 key 必须各自执行，合并不得跨 key 生效。"""
    async def scenario() -> int:
        flight = SingleFlight()
        release = asyncio.Event()
        calls = 0

        async def work() -> str:
            nonlocal calls
            calls += 1
            await release.wait()
            return "v"

        tasks = [asyncio.create_task(flight.run(key, work)) for key in ("a", "b", "c")]
        await _drain()
        release.set()
        await asyncio.gather(*tasks)
        return calls

    assert asyncio.run(scenario()) == 3, "不同 key 不应被合并"


def test_failure_propagates_to_all_waiters_and_releases_marker() -> None:
    """失败必须送达全部等待者，且完成后在途表清空，使下次调用能重新执行 work。"""
    async def scenario() -> tuple:
        flight = SingleFlight()
        started = asyncio.Event()
        release = asyncio.Event()
        calls = 0

        async def failing() -> str:
            nonlocal calls
            calls += 1
            started.set()
            await release.wait()
            raise ValueError("boom")

        tasks = [asyncio.create_task(flight.run("k", failing)) for _ in range(3)]
        await started.wait()
        await _drain()
        release.set()
        errors = []
        for task in tasks:
            try:
                await task
            except ValueError as error:
                errors.append(str(error))
        await _drain()
        return len(errors), calls, flight.inflight_count()

    error_count, calls, inflight = asyncio.run(scenario())
    assert error_count == 3, f"异常应送达全部 3 个等待者，实际 {error_count}"
    assert calls == 1, f"失败路径同样只应执行 1 次 work，实际 {calls}"
    assert inflight == 0, f"完成后不应残留在途登记，实际 {inflight}"


def test_failure_is_not_cached_and_next_call_reruns() -> None:
    """单飞不是缓存：失败后再次调用必须重新执行 work。"""
    async def scenario() -> tuple:
        flight = SingleFlight()
        calls = 0

        async def failing() -> str:
            nonlocal calls
            calls += 1
            raise ValueError("boom")

        for _ in range(2):
            try:
                await flight.run("k", failing)
            except ValueError:
                pass
        return calls

    assert asyncio.run(scenario()) == 2, "失败后再次调用应重新执行 work"


def test_creator_cancellation_does_not_cancel_waiters() -> None:
    """创建者被取消（客户端断开）时，其余等待者仍应拿到结果——shield 的意义所在。"""
    async def scenario() -> tuple:
        flight = SingleFlight()
        started = asyncio.Event()
        release = asyncio.Event()

        async def work() -> str:
            started.set()
            await release.wait()
            return "survived"

        creator = asyncio.create_task(flight.run("k", work))
        await started.wait()
        waiter = asyncio.create_task(flight.run("k", work))
        await _drain()

        creator.cancel()
        release.set()
        cancelled = False
        try:
            await creator
        except asyncio.CancelledError:
            cancelled = True
        value = await waiter
        return cancelled, value

    cancelled, value = asyncio.run(scenario())
    assert cancelled is True, "创建者应确实被取消"
    assert value == "survived", f"等待者应仍拿到结果，实际 {value}"


def test_no_leak_after_success() -> None:
    """成功后不应在在途表留下任何条目。"""
    async def scenario() -> int:
        flight = SingleFlight()

        async def work() -> str:
            return "v"

        await flight.run("k", work)
        await _drain()
        return flight.inflight_count()

    assert asyncio.run(scenario()) == 0, "完成后在途表应为空"


def test_second_call_after_completion_reruns_work() -> None:
    """串行调用两次（非并发）不受单飞影响，work 各执行一次。"""
    async def scenario() -> int:
        flight = SingleFlight()
        calls = 0

        async def work() -> str:
            nonlocal calls
            calls += 1
            return "v"

        await flight.run("k", work)
        await flight.run("k", work)
        return calls

    assert asyncio.run(scenario()) == 2, "串行两次调用应各执行一次 work"


def _run_all() -> int:
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    failed = 0
    for test in tests:
        try:
            test()
            print(f"  PASS  {test.__name__}")
        except Exception as error:  # noqa: BLE001 - 测试运行器需要汇总所有失败
            failed += 1
            print(f"  FAIL  {test.__name__}: {type(error).__name__}: {error}")
    print(f"\n{len(tests) - failed}/{len(tests)} 通过")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(_run_all())

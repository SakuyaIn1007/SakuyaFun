"""
singleflight.py
职责说明：把同一 key 的并发上游请求合并成一次，其余调用方共享同一个结果或同一个异常。
执行流程：run() 先看在途表 -> 已有任务则共享它 -> 没有则创建任务并登记 -> 任务完成时由回调注销。

为什么需要：_cached 的缓存命中检查位于信号量之外且没有在途表，两个并发请求命中同一 key 时
都会 miss，随后各自进信号量、各自 await _throttle.acquire()、各自执行一次 work()。
在 5 秒/次节流的约束下这是实打实的重复上游请求——表现为「等完 5 秒再等 5 秒做同一件事」，
同时把 Cloudflare 风控的暴露面翻倍。

按 throttle.py 的先例抽成独立模块：可独立测试、不依赖 FastAPI。
"""
from __future__ import annotations

import asyncio
from collections.abc import Awaitable, Callable
from typing import Any


class SingleFlight:
    """同 key 在途任务只保留一个；等待者共享同一个结果与同一个异常。"""

    def __init__(self) -> None:
        self._inflight: dict[str, asyncio.Task] = {}

    def inflight_count(self) -> int:
        """当前在途任务数；仅用于测试与排查泄漏。"""
        return len(self._inflight)

    async def run(self, key: str, work: Callable[[], Awaitable[Any]]) -> Any:
        existing = self._inflight.get(key)
        if existing is not None:
            # shield：等待者被取消时不得连带取消那个别人也在等的任务。
            return await asyncio.shield(existing)

        # create_task 本身不挂起，因此「查表 → 建任务 → 登记」在单线程事件循环内是原子的，
        # 不会出现两个创建者为同一个 key 各建一个任务。后续修改必须保持这一性质。
        task = asyncio.create_task(work())
        self._inflight[key] = task
        # 注销必须绑定【任务完成】，不能绑定创建者的 finally：
        # 创建者被取消时 finally 会立即摘除标记，而任务仍在运行——
        # 此时第三个请求进来会为同一 key 再建一个任务，合并直接失效。
        task.add_done_callback(lambda finished: self._release(key, finished))
        return await asyncio.shield(task)

    def _release(self, key: str, task: asyncio.Task) -> None:
        self._inflight.pop(key, None)
        if not task.cancelled():
            # 取回异常，避免「异常从未被 await」在事件循环关闭时产生告警。
            task.exception()

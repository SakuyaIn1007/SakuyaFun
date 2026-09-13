"""
throttle.py
职责说明：约束发往 Wenku8 上游的请求频率，并在被 Cloudflare 限流后进入冷却期。
执行流程：调用方在发起上游请求前 await acquire() -> 冷却中则抛 CooldownActive，
          否则补足距上次请求的剩余间隔 -> 命中限流时由调用方 note_rate_limited() 开启冷却。

为什么需要：pywenku8api 除了等待页面导航的 sleep 外没有任何限流，而 _semaphore 只限制
并发数、不限制单位时间内的请求数——串行导入会背靠背发送请求，实测连续 5 次成功后即被
Cloudflare 拒绝。固定间隔无法根除拦截（判定是多维的），但能显著降低触发概率；冷却期则
避免在已被拒绝时继续猛敲，从而恶化 IP 信誉。
"""
from __future__ import annotations

import asyncio
import time
from collections.abc import Awaitable, Callable


class CooldownActive(Exception):
    """上游处于限流冷却期，本次请求未被发出。"""

    def __init__(self, remaining: float) -> None:
        self.remaining = remaining
        super().__init__(f"上游冷却中，剩余 {remaining:.0f} 秒")


class UpstreamThrottle:
    """保证两次上游请求的最小间隔，并在限流后按冷却时长拒绝后续请求。

    时钟与休眠均可注入，使行为可在不真实等待的前提下被测试。
    """

    def __init__(
        self,
        min_interval_seconds: float = 5.0,
        cooldown_seconds: float = 900.0,
        *,
        clock: Callable[[], float] = time.monotonic,
        sleep: Callable[[float], Awaitable[None]] = asyncio.sleep,
    ) -> None:
        self._min_interval = min_interval_seconds
        self._cooldown_seconds = cooldown_seconds
        self._clock = clock
        self._sleep = sleep
        self._last_started: float | None = None
        self._cooldown_until: float | None = None

    def cooling_down(self) -> bool:
        """是否仍处于限流冷却期。"""
        return self._cooldown_until is not None and self._clock() < self._cooldown_until

    def remaining_cooldown(self) -> float:
        """冷却剩余秒数；未处于冷却时为 0。"""
        if self._cooldown_until is None:
            return 0.0
        return max(0.0, self._cooldown_until - self._clock())

    def note_rate_limited(self) -> None:
        """记录一次限流拦截，从当前时刻起重新计算冷却窗口。"""
        self._cooldown_until = self._clock() + self._cooldown_seconds

    async def acquire(self) -> None:
        """在发起上游请求前调用：必要时等待，冷却期内则直接拒绝。"""
        if self.cooling_down():
            raise CooldownActive(self.remaining_cooldown())
        if self._last_started is not None:
            remaining = self._min_interval - (self._clock() - self._last_started)
            if remaining > 0:
                await self._sleep(remaining)
        # 必须记录休眠之后的时间：等待期间流逝的秒数若不计入，相邻请求的实际间隔会被压缩。
        self._last_started = self._clock()

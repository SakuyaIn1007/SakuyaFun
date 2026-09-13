"""
test_throttle.py
职责说明：验证上游请求节流与限流冷却的行为，全程使用假时钟，不产生真实等待或网络访问。
执行流程：直接以 .venv/bin/python tests/test_throttle.py 运行（不依赖 pytest）；
          若后续安装了 pytest，同名用例可被自动收集。
"""
from __future__ import annotations

import asyncio
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

from app.throttle import CooldownActive, UpstreamThrottle


class FakeClock:
    """可手动推进的单调时钟，让测试在不真实等待的前提下观察间隔与冷却。"""

    def __init__(self, start: float = 1000.0) -> None:
        self.now = start
        self.slept: list[float] = []

    def monotonic(self) -> float:
        return self.now

    async def sleep(self, seconds: float) -> None:
        self.slept.append(seconds)
        self.now += seconds


def make(min_interval: float = 5.0, cooldown: float = 900.0):
    clock = FakeClock()
    throttle = UpstreamThrottle(
        min_interval, cooldown, clock=clock.monotonic, sleep=clock.sleep
    )
    return throttle, clock


def test_first_call_does_not_sleep() -> None:
    """首次请求没有前序请求可参照，不应产生任何等待。"""
    throttle, clock = make()
    asyncio.run(throttle.acquire())
    assert clock.slept == [], f"首次请求不应等待，实际 slept={clock.slept}"


def test_second_call_waits_full_interval() -> None:
    """紧接的第二次请求必须补满整个最小间隔。"""
    throttle, clock = make(min_interval=5.0)
    asyncio.run(throttle.acquire())
    asyncio.run(throttle.acquire())
    assert clock.slept == [5.0], f"应等待 5.0 秒，实际 slept={clock.slept}"


def test_second_call_waits_only_remaining_time() -> None:
    """已经过去的时间要抵扣，不能每次都固定等满间隔。"""
    throttle, clock = make(min_interval=5.0)
    asyncio.run(throttle.acquire())
    clock.now += 3.0  # 调用方自己先消耗了 3 秒
    asyncio.run(throttle.acquire())
    assert clock.slept == [2.0], f"应只等待剩余的 2.0 秒，实际 slept={clock.slept}"


def test_interval_elapsed_does_not_sleep() -> None:
    """间隔已经自然满足时不应再等待。"""
    throttle, clock = make(min_interval=5.0)
    asyncio.run(throttle.acquire())
    clock.now += 10.0
    asyncio.run(throttle.acquire())
    assert clock.slept == [], f"间隔已满足不应等待，实际 slept={clock.slept}"


def test_cooldown_not_active_before_rate_limit() -> None:
    """未触发限流时不应处于冷却态。"""
    throttle, _ = make()
    assert throttle.cooling_down() is False


def test_rate_limit_enters_cooldown_and_blocks() -> None:
    """命中限流后，冷却期内的请求必须被拒绝且不再打上游。"""
    throttle, _ = make(cooldown=900.0)
    throttle.note_rate_limited()
    assert throttle.cooling_down() is True
    try:
        asyncio.run(throttle.acquire())
    except CooldownActive as error:
        assert error.remaining == 900.0, f"剩余冷却应为 900.0，实际 {error.remaining}"
    else:
        raise AssertionError("冷却期内 acquire() 应当抛出 CooldownActive")


def test_cooldown_expires_after_elapsed() -> None:
    """冷却时间走完后应恢复放行，且不再等待最小间隔之外的时间。"""
    throttle, clock = make(min_interval=5.0, cooldown=900.0)
    throttle.note_rate_limited()
    clock.now += 900.0
    assert throttle.cooling_down() is False
    asyncio.run(throttle.acquire())
    assert clock.slept == [], f"冷却结束后首次请求不应等待，实际 slept={clock.slept}"


def test_zero_interval_disables_throttling() -> None:
    """间隔配为 0 时退化为不限速，便于需要时临时关闭。"""
    throttle, clock = make(min_interval=0.0)
    asyncio.run(throttle.acquire())
    asyncio.run(throttle.acquire())
    assert clock.slept == [], f"间隔为 0 时不应等待，实际 slept={clock.slept}"


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

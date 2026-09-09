package com.sakuya.ui.motion

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MotionSpecTest.kt
 * 职责说明：保证统一动效常量保持短时、非弹跳的交互节奏，防止后续调整意外引入过长动画。
 */
class MotionSpecTest {
    @Test
    fun motionDurations_remainShortAndOrdered() {
        assertTrue(MotionSpec.QUICK_DURATION_MILLIS in 1 until MotionSpec.STANDARD_DURATION_MILLIS)
        assertTrue(MotionSpec.STANDARD_DURATION_MILLIS in 1..MotionSpec.EMPHASIZED_DURATION_MILLIS)
        assertTrue(MotionSpec.EMPHASIZED_DURATION_MILLIS <= 300)
    }

    @Test
    fun interactionScales_remainSubtle() {
        assertTrue(MotionSpec.PRESS_SCALE in 0.9f..1f)
        assertTrue(MotionSpec.INTERACTIVE_SCALE in 1f..1.08f)
    }
}

package com.sakuya.common.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
        private const val DEFAULT_PATTERN = "yyyy-MM-dd"

        /**
         * Date -> String
         */
        fun format(
            date: Date,
            pattern: String = DEFAULT_PATTERN
        ): String {
            return SimpleDateFormat(
                pattern,
                Locale.getDefault()
            ).format(date)
        }

        /**
         * timestamp -> String
         */
        fun format(
            timestamp: Long,
            pattern: String = DEFAULT_PATTERN
        ): String {
            return format(Date(timestamp), pattern)
        }

        /**
         * String -> Date
         */
        fun parse(
            value: String,
            pattern: String = DEFAULT_PATTERN
        ): Date? {
            return runCatching {
                SimpleDateFormat(
                    pattern,
                    Locale.getDefault()
                ).parse(value)
            }.getOrNull()
        }

        /**
         * 当前时间
         */
        fun now(): Long {
            return System.currentTimeMillis()
        }

        /**
         * 今天 yyyy-MM-dd
         */
        fun today(
            pattern: String = DEFAULT_PATTERN
        ): String {
            return format(Date(), pattern)
        }

        /**
         * 是否今天
         */
        fun isToday(timestamp: Long): Boolean {
            val today = Calendar.getInstance()

            val target = Calendar.getInstance().apply {
                timeInMillis = timestamp
            }

            return today.get(Calendar.YEAR) ==
                    target.get(Calendar.YEAR)
                    &&
                    today.get(Calendar.DAY_OF_YEAR) ==
                    target.get(Calendar.DAY_OF_YEAR)
        }

        /**
         * 是否昨天
         */
        fun isYesterday(timestamp: Long): Boolean {
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }

            val target = Calendar.getInstance().apply {
                timeInMillis = timestamp
            }

            return yesterday.get(Calendar.YEAR) ==
                    target.get(Calendar.YEAR)
                    &&
                    yesterday.get(Calendar.DAY_OF_YEAR) ==
                    target.get(Calendar.DAY_OF_YEAR)
        }

        /**
         * 生日算年龄
         *
         * birthday: yyyy-MM-dd
         */
        fun getAge(
            birthday: String,
            pattern: String = DEFAULT_PATTERN
        ): Int {
            val birthDate = parse(
                birthday,
                pattern
            ) ?: return 0

            val today = Calendar.getInstance()

            val birth = Calendar.getInstance().apply {
                time = birthDate
            }

            var age =
                today.get(Calendar.YEAR) -
                        birth.get(Calendar.YEAR)

            if (
                today.get(Calendar.DAY_OF_YEAR)
                <
                birth.get(Calendar.DAY_OF_YEAR)
            ) {
                age--
            }

            return age
        }

}
package com.example.util

import java.util.Calendar
import java.util.Date

object JalaliCalendar {
    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    fun g2j(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0)) {
            gDaysInMonth[2] = 29
        }
        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1
        var gDayNo = 365 * gy2 + gy2 / 4 - gy2 / 100 + gy2 / 400
        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i + 1]
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (jDayNo < 186) {
            jm = 1 + jDayNo / 31
            jd = 1 + jDayNo % 31
        } else {
            val t = jDayNo - 186
            jm = 7 + t / 30
            jd = 1 + t % 30
        }
        return JalaliDate(jy, jm, jd)
    }

    fun getPersianDayOfWeekName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یک‌شنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }

    fun getPersianMonthName(month: Int): String {
        return when (month) {
            1 -> "فروردین"
            2 -> "اردیبهشت"
            3 -> "خرداد"
            4 -> "تیر"
            5 -> "مرداد"
            6 -> "شهریور"
            7 -> "مهر"
            8 -> "آبان"
            9 -> "آذر"
            10 -> "دی"
            11 -> "بهمن"
            12 -> "اسفند"
            else -> ""
        }
    }

    fun formatJalaliDate(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1 // 1-indexed for algorithm
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val jDate = g2j(gy, gm, gd)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayName = getPersianDayOfWeekName(dayOfWeek)
        val monthName = getPersianMonthName(jDate.month)
        
        // Return e.g., شنبه، ۴ تیر ۱۴۰۵
        return "$dayName، ${formatPersianNumber(jDate.day)} $monthName ${formatPersianNumber(jDate.year)}"
    }

    fun formatPersianNumber(number: Int): String {
        val numStr = number.toString()
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val builder = StringBuilder()
        for (element in numStr) {
            if (element in '0'..'9') {
                builder.append(persianDigits[element - '0'])
            } else {
                builder.append(element)
            }
        }
        return builder.toString()
    }

    fun formatPersianNumber(numberStr: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val builder = StringBuilder()
        for (element in numberStr) {
            if (element in '0'..'9') {
                builder.append(persianDigits[element - '0'])
            } else {
                builder.append(element)
            }
        }
        return builder.toString()
    }
}

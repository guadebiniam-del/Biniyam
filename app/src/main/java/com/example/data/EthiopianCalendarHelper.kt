package com.example.data

import java.util.Calendar
import java.util.Locale

object EthiopianCalendarHelper {

    val ETHIOPIAN_MONTHS = listOf(
        "መስከረም", "ጥቅምት", "ህዳር", "ታህሳስ", "ጥር", "የካቲት",
        "መጋቢት", "ሚያዚያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜ"
    )

    val AMHARIC_WEEKDAYS = listOf(
        "እሁድ", "ሰኞ", "ማክሰኞ", "ረቡዕ", "ሐሙስ", "አርብ", "ቅዳሜ"
    )

    // Check if Ethiopian year is leap
    fun isEthiopianLeapYear(year: Int): Boolean {
        return year % 4 == 3
    }

    // Convert Gregorian date to JDN
    fun gregorianToJdn(year: Int, month: Int, day: Int): Int {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = a / 4
        val c = 2 - a + b
        val d = (365.25 * (y + 4716)).toInt()
        val e = (30.6001 * (m + 1)).toInt()
        return c + d + e + day - 1524
    }

    // Convert JDN to Gregorian Date (Year, Month, Day)
    fun jdnToGregorian(jdn: Int): Triple<Int, Int, Int> {
        val j = jdn + 32044
        val g = j / 146097
        val dg = j % 146097
        val c = (dg / 36524 + 1) * 3 / 4
        val dc = dg - c * 36524
        val b = dc / 1461
        val db = dc % 1461
        val a = (db / 365 + 1) * 3 / 4
        val da = db - a * 365
        val y = g * 400 + c * 100 + b * 4 + a
        val m = (da * 5 + 308) / 153 - 2
        val d = da - (m + 4) * 153 / 5 + 122
        val gYear = y - 4800 + (m + 2) / 12
        val gMonth = (m + 2) % 12 + 1
        val gDay = d + 1
        return Triple(gYear, gMonth, gDay)
    }

    // Convert Gregorian date to Ethiopian date (100% robust JDN version)
    fun gregorianToEthiopian(gYear: Int, gMonth: Int, gDay: Int): Triple<Int, Int, Int> {
        val newYearDayOfCurrentYear = if ((gYear + 1) % 4 == 0) 12 else 11
        val isAfterNewYear = gMonth > 9 || (gMonth == 9 && gDay >= newYearDayOfCurrentYear)
        
        val ethYear = if (isAfterNewYear) gYear - 7 else gYear - 8

        val startGYear = if (isAfterNewYear) gYear else gYear - 1
        val startGMonth = 9
        val startGDay = if (isAfterNewYear) newYearDayOfCurrentYear else {
            if (gYear % 4 == 0) 12 else 11
        }

        val targetJdn = gregorianToJdn(gYear, gMonth, gDay)
        val startJdn = gregorianToJdn(startGYear, startGMonth, startGDay)
        val diffDays = targetJdn - startJdn

        val ethMonth = (diffDays / 30) + 1
        val ethDay = (diffDays % 30) + 1

        return Triple(ethYear, ethMonth, ethDay)
    }

    // Convert Ethiopian date to Gregorian date (100% robust JDN version)
    fun ethiopianToGregorian(ethYear: Int, ethMonth: Int, ethDay: Int): Triple<Int, Int, Int> {
        val gYearStart = ethYear + 7
        val newYearDay = if ((gYearStart + 1) % 4 == 0) 12 else 11
        
        val startJdn = gregorianToJdn(gYearStart, 9, newYearDay)
        val daysPassed = (ethMonth - 1) * 30 + (ethDay - 1)
        val targetJdn = startJdn + daysPassed

        return jdnToGregorian(targetJdn)
    }

    // Convert JDN to Ethiopian Date (Year, Month, Day)
    fun jdnToEthiopian(jdn: Int): Triple<Int, Int, Int> {
        val greg = jdnToGregorian(jdn)
        return gregorianToEthiopian(greg.first, greg.second, greg.third)
    }

    // Convert Ethiopian date to JDN
    fun ethiopianToJdn(ethYear: Int, ethMonth: Int, ethDay: Int): Int {
        val greg = ethiopianToGregorian(ethYear, ethMonth, ethDay)
        return gregorianToJdn(greg.first, greg.second, greg.third)
    }

    // Get today's Ethiopian date as "yyyy-MM-dd"
    fun getTodayEthiopianString(): String {
        val cal = Calendar.getInstance()
        val triple = gregorianToEthiopian(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        return String.format(Locale.US, "%04d-%02d-%02d", triple.first, triple.second, triple.third)
    }

    // Shift an Ethiopian date formatted as "yyyy-MM-dd" by offsetDays
    fun shiftEthiopianDate(dateStr: String, offsetDays: Int): String {
        val parts = dateStr.split("-")
        val ethYear = parts.getOrNull(0)?.toIntOrNull() ?: 2018
        val ethMonth = parts.getOrNull(1)?.toIntOrNull() ?: 9
        val ethDay = parts.getOrNull(2)?.toIntOrNull() ?: 22
        val jdn = ethiopianToJdn(ethYear, ethMonth, ethDay)
        val newJdn = jdn + offsetDays
        val triple = jdnToEthiopian(newJdn)
        return String.format(Locale.US, "%04d-%02d-%02d", triple.first, triple.second, triple.third)
    }

    fun getAmharicWeekday(ethYear: Int, ethMonth: Int, ethDay: Int): String {
        val jdn = ethiopianToJdn(ethYear, ethMonth, ethDay)
        val dayOfWeek = (jdn + 1) % 7
        return AMHARIC_WEEKDAYS.getOrElse(dayOfWeek) { "" }
    }

    // Format an Ethiopian date "yyyy-MM-dd" to friendly format
    fun formatEthiopianDateFriendly(dateStr: String): String {
        val parts = dateStr.split("-")
        val ethYear = parts.getOrNull(0)?.toIntOrNull() ?: return dateStr
        val ethMonth = parts.getOrNull(1)?.toIntOrNull() ?: return dateStr
        val ethDay = parts.getOrNull(2)?.toIntOrNull() ?: return dateStr

        val monthName = ETHIOPIAN_MONTHS.getOrNull(ethMonth - 1) ?: "Unknown"
        val weekday = getAmharicWeekday(ethYear, ethMonth, ethDay)
        return "$weekday፣ $monthName $ethDay ቀን $ethYear ዓ.ም."
    }

    // Calculate start and end date for Ethiopian periods (Daily, Weekly, Monthly, Yearly)
    fun getRangeForEthiopianPeriod(referenceDateStr: String, periodType: String): Pair<String, String> {
        val parts = referenceDateStr.split("-")
        val ethYear = parts.getOrNull(0)?.toIntOrNull() ?: 2018
        val ethMonth = parts.getOrNull(1)?.toIntOrNull() ?: 9
        val ethDay = parts.getOrNull(2)?.toIntOrNull() ?: 22

        return when (periodType.uppercase()) {
            "DAILY" -> {
                Pair(referenceDateStr, referenceDateStr)
            }
            "WEEKLY" -> {
                val jdn = ethiopianToJdn(ethYear, ethMonth, ethDay)
                val dayOfWeek = (jdn + 1) % 7 // 0 is Sunday, 1 is Monday ... 6 is Saturday
                val offsetToMonday = if (dayOfWeek == 0) 6 else dayOfWeek - 1
                val mondayJdn = jdn - offsetToMonday
                val sundayJdn = mondayJdn + 6

                val monTriple = jdnToEthiopian(mondayJdn)
                val sunTriple = jdnToEthiopian(sundayJdn)

                val start = String.format(Locale.US, "%04d-%02d-%02d", monTriple.first, monTriple.second, monTriple.third)
                val end = String.format(Locale.US, "%04d-%02d-%02d", sunTriple.first, sunTriple.second, sunTriple.third)
                Pair(start, end)
            }
            "MONTHLY" -> {
                val start = String.format(Locale.US, "%04d-%02d-01", ethYear, ethMonth)
                val maxDay = if (ethMonth == 13) {
                    if (isEthiopianLeapYear(ethYear)) 6 else 5
                } else {
                    30
                }
                val end = String.format(Locale.US, "%04d-%02d-%02d", ethYear, ethMonth, maxDay)
                Pair(start, end)
            }
            "YEARLY" -> {
                val start = String.format(Locale.US, "%04d-01-01", ethYear)
                val maxDay = if (isEthiopianLeapYear(ethYear)) 6 else 5
                val end = String.format(Locale.US, "%04d-13-%02d", ethYear, maxDay)
                Pair(start, end)
            }
            else -> Pair(referenceDateStr, referenceDateStr)
        }
    }
}

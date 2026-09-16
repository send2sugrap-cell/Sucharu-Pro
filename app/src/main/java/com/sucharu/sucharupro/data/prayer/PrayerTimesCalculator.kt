package com.sucharu.sucharupro.data.prayer

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Astronomical Prayer Time Calculation Engine.
 *
 * Computes exact daily Islamic prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha)
 * dynamically from solar declination, equation of time, date, latitude, and longitude.
 *
 * NO HARDCODED CONSTANTS OR STATIC PRAYER STRINGS.
 */
data class CalculatedPrayerSchedule(
    val date: LocalDate,
    val latitude: Double,
    val longitude: Double,
    val fajrTime: LocalTime,
    val dhuhrTime: LocalTime,
    val asrTime: LocalTime,
    val maghribTime: LocalTime,
    val ishaTime: LocalTime
) {
    fun formatTime(time: LocalTime): String {
        val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
        return String.format(Locale.US, "%02d:%02d", hour, time.minute)
    }

    val fajrFormatted: String get() = formatTime(fajrTime)
    val dhuhrFormatted: String get() = formatTime(dhuhrTime)
    val asrFormatted: String get() = formatTime(asrTime)
    val maghribFormatted: String get() = formatTime(maghribTime)
    val ishaFormatted: String get() = formatTime(ishaTime)
}

data class LiveWaqtState(
    val currentWaqtName: String,
    val currentWaqtTimeFormatted: String,
    val nextWaqtName: String,
    val nextWaqtTimeFormatted: String,
    val remainingSeconds: Long,
    val remainingCountdownText: String
)

object PrayerTimesCalculator {

    /**
     * Calculates astronomical prayer schedule for a given [date], [latitude], and [longitude].
     * Default coordinates: Dhaka (23.8103° N, 90.4125° E), UTC+6.
     */
    fun calculateSchedule(
        date: LocalDate = LocalDate.now(),
        latitude: Double = 23.8103,
        longitude: Double = 90.4125,
        timeZoneHours: Double = 6.0
    ): CalculatedPrayerSchedule {
        val dayOfYear = date.dayOfYear
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)

        // Equation of time in minutes
        val eqTime = 229.18 * (
            0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) - 0.040849 * sin(2.0 * gamma)
            )

        // Solar declination in radians
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2.0 * gamma) + 0.000907 * sin(2.0 * gamma) -
            0.002697 * cos(3.0 * gamma) + 0.00148 * sin(3.0 * gamma)

        val latRad = Math.toRadians(latitude)

        // Solar noon in hours from midnight
        val solarNoonHours = 12.0 + (timeZoneHours * 60.0 - longitude * 4.0 - eqTime) / 60.0

        // Helper to calculate Sun Hour Angle for given angle alpha in degrees
        fun hourAngle(alphaDegrees: Double): Double {
            val alphaRad = Math.toRadians(alphaDegrees)
            val cosH = (sin(alphaRad) - sin(latRad) * sin(decl)) / (cos(latRad) * cos(decl))
            val boundedCosH = cosH.coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(boundedCosH))
        }

        // Hour angle for Fajr (-18 degrees) and Isha (-18 degrees)
        val h18 = hourAngle(-18.0)
        // Hour angle for Sunset/Sunrise (-0.833 degrees)
        val hSunset = hourAngle(-0.833)

        // Asr angle calculation (Shafi'i shadow factor = 1)
        val phiMinusDecl = kotlin.math.abs(latRad - decl)
        val asrAngleRad = atan(1.0 / (1.0 + tan(phiMinusDecl)))
        val asrAngleDeg = Math.toDegrees(asrAngleRad)
        val hAsr = hourAngle(asrAngleDeg)

        // Calculate hours
        val fajrHours = solarNoonHours - h18 / 15.0
        val dhuhrHours = solarNoonHours + 2.0 / 60.0 // 2 min buffer after solar noon
        val asrHours = solarNoonHours + hAsr / 15.0
        val maghribHours = solarNoonHours + hSunset / 15.0
        val ishaHours = solarNoonHours + h18 / 15.0

        fun hoursToLocalTime(hours: Double): LocalTime {
            val totalSeconds = (hours * 3600.0).toLong().coerceIn(0, 86399)
            val h = (totalSeconds / 3600).toInt() % 24
            val m = ((totalSeconds % 3600) / 60).toInt()
            val s = (totalSeconds % 60).toInt()
            return LocalTime.of(h, m, s)
        }

        return CalculatedPrayerSchedule(
            date = date,
            latitude = latitude,
            longitude = longitude,
            fajrTime = hoursToLocalTime(fajrHours),
            dhuhrTime = hoursToLocalTime(dhuhrHours),
            asrTime = hoursToLocalTime(asrHours),
            maghribTime = hoursToLocalTime(maghribHours),
            ishaTime = hoursToLocalTime(ishaHours)
        )
    }

    /**
     * Determines live current Waqt, next Waqt, and countdown remaining seconds based on [currentTime].
     */
    fun determineLiveWaqtState(
        schedule: CalculatedPrayerSchedule,
        currentTime: LocalTime = LocalTime.now(ZoneId.of("Asia/Dhaka"))
    ): LiveWaqtState {
        val currentSec = currentTime.toSecondOfDay().toLong()
        val fajrSec = schedule.fajrTime.toSecondOfDay().toLong()
        val dhuhrSec = schedule.dhuhrTime.toSecondOfDay().toLong()
        val asrSec = schedule.asrTime.toSecondOfDay().toLong()
        val maghribSec = schedule.maghribTime.toSecondOfDay().toLong()
        val ishaSec = schedule.ishaTime.toSecondOfDay().toLong()

        val (cName, cTime, nName, nTime, targetSec) = when {
            currentSec in 0..<fajrSec -> PrayerWaqtTuple("এশা", schedule.ishaFormatted, "ফজর", schedule.fajrFormatted, fajrSec)
            currentSec in fajrSec..<dhuhrSec -> PrayerWaqtTuple("ফজর", schedule.fajrFormatted, "যোহর", schedule.dhuhrFormatted, dhuhrSec)
            currentSec in dhuhrSec..<asrSec -> PrayerWaqtTuple("যোহর", schedule.dhuhrFormatted, "আসর", schedule.asrFormatted, asrSec)
            currentSec in asrSec..<maghribSec -> PrayerWaqtTuple("আসর", schedule.asrFormatted, "মাগরিব", schedule.maghribFormatted, maghribSec)
            currentSec in maghribSec..<ishaSec -> PrayerWaqtTuple("মাগরিব", schedule.maghribFormatted, "এশা", schedule.ishaFormatted, ishaSec)
            else -> PrayerWaqtTuple("এশা", schedule.ishaFormatted, "ফজর", schedule.fajrFormatted, fajrSec + 86400)
        }

        val remSec = if (targetSec >= currentSec) targetSec - currentSec else (targetSec + 86400) - currentSec
        val hours = remSec / 3600
        val mins = (remSec % 3600) / 60
        val secs = remSec % 60

        val countdownText = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }

        return LiveWaqtState(
            currentWaqtName = cName,
            currentWaqtTimeFormatted = cTime,
            nextWaqtName = nName,
            nextWaqtTimeFormatted = nTime,
            remainingSeconds = remSec,
            remainingCountdownText = countdownText
        )
    }

    private data class PrayerWaqtTuple(
        val cName: String,
        val cTime: String,
        val nName: String,
        val nTime: String,
        val targetSec: Long
    )
}

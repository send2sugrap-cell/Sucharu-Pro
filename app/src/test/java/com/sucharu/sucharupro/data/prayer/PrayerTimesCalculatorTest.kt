package com.sucharu.sucharupro.data.prayer

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimesCalculatorTest {

    @Test
    fun testCalculateSchedule_calculatesValidAstronomicalPrayerTimesForDhaka() {
        val date = LocalDate.of(2026, 9, 16)
        val schedule = PrayerTimesCalculator.calculateSchedule(date = date)

        assertNotNull(schedule)
        assertEquals(date, schedule.date)

        // Verify prayer order: Fajr < Dhuhr < Asr < Maghrib < Isha
        assertTrue("Fajr must be before Dhuhr", schedule.fajrTime.isBefore(schedule.dhuhrTime))
        assertTrue("Dhuhr must be before Asr", schedule.dhuhrTime.isBefore(schedule.asrTime))
        assertTrue("Asr must be before Maghrib", schedule.asrTime.isBefore(schedule.maghribTime))
        assertTrue("Maghrib must be before Isha", schedule.maghribTime.isBefore(schedule.ishaTime))
    }

    @Test
    fun testDetermineLiveWaqtState_correctlyIdentifiesCurrentWaqtAndCountdown() {
        val date = LocalDate.of(2026, 9, 16)
        val schedule = PrayerTimesCalculator.calculateSchedule(date = date)

        // Test time during Dhuhr interval
        val testTimeDhuhr = schedule.dhuhrTime.plusMinutes(10)
        val stateDhuhr = PrayerTimesCalculator.determineLiveWaqtState(schedule, testTimeDhuhr)

        assertEquals("যোহর", stateDhuhr.currentWaqtName)
        assertEquals("আসর", stateDhuhr.nextWaqtName)

        // Test time during Asr interval
        val testTimeAsr = schedule.asrTime.plusMinutes(10)
        val stateAsr = PrayerTimesCalculator.determineLiveWaqtState(schedule, testTimeAsr)

        assertEquals("আসর", stateAsr.currentWaqtName)
        assertEquals("মাগরিব", stateAsr.nextWaqtName)
    }
}

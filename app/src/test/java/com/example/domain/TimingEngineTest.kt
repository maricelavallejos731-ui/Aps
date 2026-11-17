package com.example.domain

import com.example.data.model.DriverStatus
import com.example.data.model.SectorColorState
import com.example.data.model.SessionEntry
import com.example.data.model.SessionType
import com.example.data.model.TimingSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimingEngineTest {

    @Test
    fun formatLapTime_formatsMillisecondsAccurately() {
        assertEquals("1:23.456", TimingEngine.formatLapTime(83_456L))
        assertEquals("59.007", TimingEngine.formatLapTime(59_007L))
        assertEquals("--:--.---", TimingEngine.formatLapTime(0L))
    }

    @Test
    fun classifySectorColor_distinguishesPurpleGreenYellow() {
        assertEquals(SectorColorState.PURPLE, TimingEngine.classifySectorColor(24_100L, 24_300L, 24_100L))
        assertEquals(SectorColorState.GREEN, TimingEngine.classifySectorColor(24_200L, 24_200L, 24_000L))
        assertEquals(SectorColorState.YELLOW, TimingEngine.classifySectorColor(24_500L, 24_200L, 24_000L))
        assertEquals(SectorColorState.NONE, TimingEngine.classifySectorColor(0L, 24_200L, 24_000L))
    }

    @Test
    fun validateLapTime_rejectsImpossibleTimes() {
        assertFalse(TimingEngine.validateLapTime(2_500L).first)
        assertFalse(TimingEngine.validateLapTime(0L).first)
        assertTrue(TimingEngine.validateLapTime(78_432L).first)
    }

    @Test
    fun buildLiveTimingRows_computesQualifyingGapsAndIntervals() {
        val session = TimingSession(
            id = "s1",
            sessionType = SessionType.QUALIFYING.name,
            bestSessionLapMs = 80_000L,
            bestSector1Ms = 25_000L,
            bestSector2Ms = 30_000L,
            bestSector3Ms = 25_000L
        )
        val e1 = SessionEntry(id = "d1", driverName = "Piloto A", carNumber = 1, lapsCompleted = 5, bestLapMs = 80_000L)
        val e2 = SessionEntry(id = "d2", driverName = "Piloto B", carNumber = 2, lapsCompleted = 5, bestLapMs = 80_250L)
        val e3 = SessionEntry(id = "d3", driverName = "Piloto C", carNumber = 3, lapsCompleted = 5, bestLapMs = 80_600L)

        val rows = TimingEngine.buildLiveTimingRows(session, listOf(e3, e1, e2))
        assertEquals("d1", rows[0].entry.id)
        assertEquals("LÍDER", rows[0].gapToLeader)
        assertEquals("+0.250", rows[1].gapToLeader)
        assertEquals("+0.250", rows[1].intervalToAhead)
        assertEquals("+0.600", rows[2].gapToLeader)
        assertEquals("+0.350", rows[2].intervalToAhead)
    }

    @Test
    fun computeChampionshipStandings_sumsPointsAndFastestLapBonus() {
        val s1 = TimingSession(id = "r1", sessionType = SessionType.RACE.name)
        val e1 = SessionEntry(
            id = "e1", sessionId = "r1", driverName = "A. Rossi", driverCode = "ROS",
            teamName = "Apex Racing", teamColorHex = "#FF1E38", lapsCompleted = 20,
            totalTimeMs = 1_600_000L, hasFastestLap = true, driverStatus = DriverStatus.ON_TRACK.name
        )
        val e2 = SessionEntry(
            id = "e2", sessionId = "r1", driverName = "M. López", driverCode = "LOP",
            teamName = "Apex Racing", teamColorHex = "#FF1E38", lapsCompleted = 20,
            totalTimeMs = 1_605_000L, hasFastestLap = false, driverStatus = DriverStatus.ON_TRACK.name
        )
        val (drivers, constructors) = TimingEngine.computeChampionshipStandings(listOf(s1), listOf(e1, e2))
        assertEquals(26, drivers[0].points) // 25 + 1 fastest lap
        assertEquals(18, drivers[1].points)
        assertEquals(44, constructors[0].points)
    }
}

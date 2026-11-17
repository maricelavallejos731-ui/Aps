package com.example.domain

import androidx.compose.ui.graphics.Color
import com.example.data.model.ConstructorStanding
import com.example.data.model.DriverStanding
import com.example.data.model.DriverStatus
import com.example.data.model.LiveTimingRow
import com.example.data.model.SectorColorState
import com.example.data.model.SessionEntry
import com.example.data.model.SessionType
import com.example.data.model.TimingSession
import java.util.Locale

object TimingEngine {

    private val F1_POINTS = listOf(25, 18, 15, 12, 10, 8, 6, 4, 2, 1)

    fun formatLapTime(ms: Long): String {
        if (ms <= 0L) return "--:--.---"
        val minutes = ms / 60_000L
        val seconds = (ms % 60_000L) / 1_000L
        val millis = ms % 1_000L
        return if (minutes > 0L) {
            String.format(Locale.US, "%d:%02d.%03d", minutes, seconds, millis)
        } else {
            String.format(Locale.US, "%02d.%03d", seconds, millis)
        }
    }

    fun formatSectorTime(ms: Long): String {
        if (ms <= 0L) return "--.---"
        val seconds = ms / 1_000L
        val millis = ms % 1_000L
        return String.format(Locale.US, "%02d.%03d", seconds, millis)
    }

    fun formatDelta(deltaMs: Long): String {
        if (deltaMs <= 0L) return "0.000"
        val seconds = deltaMs / 1_000L
        val millis = deltaMs % 1_000L
        return String.format(Locale.US, "+%d.%03d", seconds, millis)
    }

    fun formatSessionClock(elapsedMs: Long): String {
        val safe = elapsedMs.coerceAtLeast(0L)
        val hours = safe / 3_600_000L
        val minutes = (safe % 3_600_000L) / 60_000L
        val seconds = (safe % 60_000L) / 1_000L
        val tenths = (safe % 1_000L) / 100L
        return if (hours > 0L) {
            String.format(Locale.US, "%02d:%02d:%02d.%d", hours, minutes, seconds, tenths)
        } else {
            String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
        }
    }

    fun validateLapTime(lapTimeMs: Long, minReasonableLapMs: Long = 5_000L): Pair<Boolean, String> {
        return when {
            lapTimeMs <= 0L -> false to "Tiempo nulo o negativo"
            lapTimeMs < minReasonableLapMs -> false to "Tiempo inferior al mínimo físico (<5.000s)"
            lapTimeMs > 3_600_000L -> false to "Tiempo fuera de rango de sesión (>60m)"
            else -> true to ""
        }
    }

    fun classifySectorColor(
        sectorMs: Long,
        personalBestMs: Long,
        sessionBestMs: Long
    ): SectorColorState {
        if (sectorMs <= 0L) return SectorColorState.NONE
        if (sessionBestMs > 0L && sectorMs <= sessionBestMs) return SectorColorState.PURPLE
        if (personalBestMs > 0L && sectorMs <= personalBestMs) return SectorColorState.GREEN
        return SectorColorState.YELLOW
    }

    fun sortEntriesForSession(
        entries: List<SessionEntry>,
        sessionType: SessionType
    ): List<SessionEntry> {
        return when (sessionType) {
            SessionType.RACE -> entries.sortedWith(
                compareBy<SessionEntry> {
                    val status = DriverStatus.fromString(it.driverStatus)
                    if (status == DriverStatus.RETIRED || status == DriverStatus.OUT_OF_SESSION) 1 else 0
                }
                    .thenByDescending { it.lapsCompleted }
                    .thenBy {
                        val effectiveTotal = it.totalTimeMs + (it.penaltySeconds * 1_000L)
                        if (effectiveTotal <= 0L) Long.MAX_VALUE else effectiveTotal
                    }
                    .thenBy { it.position }
                    .thenBy { it.carNumber }
            )
            SessionType.PRACTICE, SessionType.QUALIFYING -> entries.sortedWith(
                compareBy<SessionEntry> { if (it.bestLapMs <= 0L) Long.MAX_VALUE else it.bestLapMs }
                    .thenByDescending { it.lapsCompleted }
                    .thenBy { it.position }
                    .thenBy { it.carNumber }
            )
        }
    }

    fun buildLiveTimingRows(
        session: TimingSession?,
        entries: List<SessionEntry>
    ): List<LiveTimingRow> {
        if (entries.isEmpty()) return emptyList()
        val sessionType = SessionType.fromString(session?.sessionType ?: SessionType.PRACTICE.name)
        val sorted = sortEntriesForSession(entries, sessionType)
        val leader = sorted.first()
        val sessionS1 = session?.bestSector1Ms ?: 0L
        val sessionS2 = session?.bestSector2Ms ?: 0L
        val sessionS3 = session?.bestSector3Ms ?: 0L

        return sorted.mapIndexed { index, entry ->
            val pos = index + 1
            val ahead = if (index > 0) sorted[index - 1] else null

            val gapToLeader = computeGap(leader, entry, sessionType, isLeader = index == 0)
            val interval = if (ahead == null) {
                "INTERVAL"
            } else {
                computeGap(ahead, entry, sessionType, isLeader = false)
            }

            LiveTimingRow(
                entry = entry,
                computedPosition = pos,
                gapToLeader = gapToLeader,
                intervalToAhead = interval,
                sector1Color = classifySectorColor(entry.currentSector1Ms, entry.bestSector1Ms, sessionS1),
                sector2Color = classifySectorColor(entry.currentSector2Ms, entry.bestSector2Ms, sessionS2),
                sector3Color = classifySectorColor(entry.currentSector3Ms, entry.bestSector3Ms, sessionS3)
            )
        }
    }

    private fun computeGap(
        reference: SessionEntry,
        target: SessionEntry,
        sessionType: SessionType,
        isLeader: Boolean
    ): String {
        if (isLeader) return "LÍDER"
        val status = DriverStatus.fromString(target.driverStatus)
        if (status == DriverStatus.RETIRED) return "RET"
        if (status == DriverStatus.OUT_OF_SESSION) return "OUT"

        return when (sessionType) {
            SessionType.RACE -> {
                val lapDiff = reference.lapsCompleted - target.lapsCompleted
                when {
                    lapDiff > 1L -> "+$lapDiff VUELTAS"
                    lapDiff == 1L -> "+1 VUELTA"
                    reference.totalTimeMs <= 0L || target.totalTimeMs <= 0L -> "--.---"
                    else -> {
                        val refTotal = reference.totalTimeMs + (reference.penaltySeconds * 1_000L)
                        val tgtTotal = target.totalTimeMs + (target.penaltySeconds * 1_000L)
                        val diff = (tgtTotal - refTotal).coerceAtLeast(0L)
                        formatDelta(diff)
                    }
                }
            }
            SessionType.PRACTICE, SessionType.QUALIFYING -> {
                if (reference.bestLapMs <= 0L || target.bestLapMs <= 0L) {
                    "SIN TIEMPO"
                } else {
                    formatDelta((target.bestLapMs - reference.bestLapMs).coerceAtLeast(0L))
                }
            }
        }
    }

    fun calculatePointsForPosition(position: Int, hasFastestLap: Boolean, lapsCompleted: Long): Int {
        if (lapsCompleted <= 0L || position < 1 || position > F1_POINTS.size) return 0
        val base = F1_POINTS[position - 1]
        val bonus = if (hasFastestLap) 1 else 0
        return base + bonus
    }

    fun computeChampionshipStandings(
        finishedRaceSessions: List<TimingSession>,
        allEntries: List<SessionEntry>
    ): Pair<List<DriverStanding>, List<ConstructorStanding>> {
        val finishedSessionIds = finishedRaceSessions
            .filter { SessionType.fromString(it.sessionType) == SessionType.RACE }
            .map { it.id }
            .toSet()

        if (finishedSessionIds.isEmpty()) return emptyList<DriverStanding>() to emptyList()

        val driverAcc = mutableMapOf<String, MutableDriverAcc>()
        val teamAcc = mutableMapOf<String, MutableTeamAcc>()

        finishedSessionIds.forEach { sessionId ->
            val sessionEntries = sortEntriesForSession(
                allEntries.filter { it.sessionId == sessionId },
                SessionType.RACE
            )
            sessionEntries.forEachIndexed { idx, entry ->
                val pos = idx + 1
                val status = DriverStatus.fromString(entry.driverStatus)
                val pts = if (entry.pointsAwarded > 0L) {
                    entry.pointsAwarded.toInt()
                } else if (status != DriverStatus.RETIRED && status != DriverStatus.OUT_OF_SESSION) {
                    calculatePointsForPosition(pos, entry.hasFastestLap, entry.lapsCompleted)
                } else {
                    0
                }
                val isWin = pos == 1 && entry.lapsCompleted > 0L
                val isPodium = pos in 1..3 && entry.lapsCompleted > 0L

                val dKey = entry.driverName.trim()
                val d = driverAcc.getOrPut(dKey) {
                    MutableDriverAcc(
                        driverName = entry.driverName,
                        driverCode = entry.driverCode,
                        countryCode = entry.countryCode,
                        teamName = entry.teamName,
                        teamColorHex = entry.teamColorHex
                    )
                }
                d.points += pts
                if (isWin) d.wins++
                if (isPodium) d.podiums++
                if (entry.hasFastestLap) d.fastestLaps++

                val tKey = entry.teamName.trim()
                val t = teamAcc.getOrPut(tKey) {
                    MutableTeamAcc(
                        teamName = entry.teamName,
                        teamColorHex = entry.teamColorHex
                    )
                }
                t.points += pts
                if (isWin) t.wins++
            }
        }

        val drivers = driverAcc.values
            .sortedWith(compareByDescending<MutableDriverAcc> { it.points }.thenByDescending { it.wins }.thenBy { it.driverName })
            .mapIndexed { i, acc ->
                DriverStanding(
                    position = i + 1,
                    driverName = acc.driverName,
                    driverCode = acc.driverCode,
                    countryCode = acc.countryCode,
                    teamName = acc.teamName,
                    teamColorHex = acc.teamColorHex,
                    points = acc.points,
                    wins = acc.wins,
                    podiums = acc.podiums,
                    fastestLaps = acc.fastestLaps
                )
            }

        val constructors = teamAcc.values
            .sortedWith(compareByDescending<MutableTeamAcc> { it.points }.thenByDescending { it.wins }.thenBy { it.teamName })
            .mapIndexed { i, acc ->
                ConstructorStanding(
                    position = i + 1,
                    teamName = acc.teamName,
                    teamColorHex = acc.teamColorHex,
                    points = acc.points,
                    wins = acc.wins
                )
            }

        return drivers to constructors
    }

    fun countryCodeToFlag(countryCode: String): String {
        val code = countryCode.trim().uppercase(Locale.US)
        if (code.length != 2 || !code.all { it in 'A'..'Z' }) return "🏁"
        val first = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val second = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    fun parseColorSafe(hex: String, fallback: Color = Color(0xFFFF1E38)): Color {
        return try {
            val cleaned = hex.trim().removePrefix("#")
            val parsed = when (cleaned.length) {
                6 -> ("FF$cleaned").toLong(16)
                8 -> cleaned.toLong(16)
                else -> return fallback
            }
            Color(parsed)
        } catch (_: Exception) {
            fallback
        }
    }

    private data class MutableDriverAcc(
        val driverName: String,
        val driverCode: String,
        val countryCode: String,
        val teamName: String,
        val teamColorHex: String,
        var points: Int = 0,
        var wins: Int = 0,
        var podiums: Int = 0,
        var fastestLaps: Int = 0
    )

    private data class MutableTeamAcc(
        val teamName: String,
        val teamColorHex: String,
        var points: Int = 0,
        var wins: Int = 0
    )
}

package com.example.data.model

enum class UserRole(val labelEs: String) {
    SUPER_ADMIN("Superadministrador"),
    RACE_DIRECTOR("Director de Carrera"),
    TIMEKEEPER("Cronometrador"),
    SPECTATOR("Espectador");

    val canControlSessions: Boolean
        get() = this == SUPER_ADMIN || this == RACE_DIRECTOR

    val canRecordTiming: Boolean
        get() = this == SUPER_ADMIN || this == RACE_DIRECTOR || this == TIMEKEEPER

    val canManageChampionshipsAndUsers: Boolean
        get() = this == SUPER_ADMIN

    companion object {
        fun fromString(raw: String): UserRole =
            entries.firstOrNull { it.name == raw } ?: SPECTATOR
    }
}

enum class SessionType(val labelEs: String) {
    PRACTICE("Entrenamientos"),
    QUALIFYING("Clasificación"),
    RACE("Carrera");

    companion object {
        fun fromString(raw: String): SessionType =
            entries.firstOrNull { it.name == raw } ?: PRACTICE
    }
}

enum class SessionStatus(val labelEs: String) {
    SCHEDULED("Programada"),
    ACTIVE("En Curso"),
    PAUSED("Pausada"),
    FINISHED("Finalizada");

    companion object {
        fun fromString(raw: String): SessionStatus =
            entries.firstOrNull { it.name == raw } ?: SCHEDULED
    }
}

enum class FlagStatus(val labelEs: String) {
    GREEN("Bandera Verde"),
    YELLOW("Bandera Amarilla"),
    RED("Bandera Roja"),
    CHECKERED("Bandera a Cuadros");

    companion object {
        fun fromString(raw: String): FlagStatus =
            entries.firstOrNull { it.name == raw } ?: GREEN
    }
}

enum class TimingMode(val labelEs: String) {
    MANUAL("Manual"),
    GPS("GPS Dispositivo"),
    EXTERNAL_BLE_API("Externo BLE / Wi-Fi API");

    companion object {
        fun fromString(raw: String): TimingMode =
            entries.firstOrNull { it.name == raw } ?: MANUAL
    }
}

enum class DriverStatus(val labelEs: String, val shortLabel: String) {
    ON_TRACK("En pista", "PISTA"),
    IN_PITS("En boxes", "PIT"),
    RETIRED("Retirado", "RET"),
    OUT_OF_SESSION("Fuera de sesión", "OUT");

    companion object {
        fun fromString(raw: String): DriverStatus =
            entries.firstOrNull { it.name == raw } ?: ON_TRACK
    }
}

enum class EventStatus(val labelEs: String) {
    UPCOMING("Próximo"),
    LIVE("En Curso"),
    FINISHED("Finalizado");

    companion object {
        fun fromString(raw: String): EventStatus =
            entries.firstOrNull { it.name == raw } ?: UPCOMING
    }
}

enum class PenaltyType(val labelEs: String, val defaultSeconds: Int) {
    TIME_5S("+5s Tiempo", 5),
    TIME_10S("+10s Tiempo", 10),
    DRIVE_THROUGH("Drive-Through", 20),
    GRID_DROP("Sanción Parrilla", 0),
    WARNING("Amonestación", 0),
    DISQUALIFIED("Descalificado", 0);

    companion object {
        fun fromString(raw: String): PenaltyType =
            entries.firstOrNull { it.name == raw } ?: WARNING
    }
}

enum class TimingSource(val labelEs: String) {
    MANUAL("Cronometrador Manual"),
    GPS("GPS Dispositivo"),
    BLUETOOTH("Transponder BLE"),
    WIFI_API("Feed Wi-Fi / API");

    companion object {
        fun fromString(raw: String): TimingSource =
            entries.firstOrNull { it.name == raw } ?: MANUAL
    }
}

enum class SectorColorState {
    NONE,
    YELLOW,
    GREEN,
    PURPLE
}

data class Championship(
    val id: String = "",
    val name: String = "",
    val seasonYear: Long = 2026L,
    val category: String = "",
    val status: String = "ACTIVE",
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class Circuit(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val countryCode: String = "",
    val lengthKm: Double = 0.0,
    val turnsCount: Long = 0L,
    val sectorCount: Long = 3L,
    val startFinishLabel: String = "Meta",
    val sectorSplits: List<Double> = listOf(0.33, 0.67, 1.0),
    val measurementPoints: List<String> = emptyList(),
    val trackPoints: List<String> = emptyList(),
    val customImageUri: String = "",
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class RaceEvent(
    val id: String = "",
    val championshipId: String = "",
    val circuitId: String = "",
    val circuitName: String = "",
    val grandPrixName: String = "",
    val country: String = "",
    val countryCode: String = "",
    val eventDateMillis: Long = 0L,
    val practiceSchedule: String = "",
    val qualifyingSchedule: String = "",
    val raceSchedule: String = "",
    val raceDurationLabel: String = "",
    val status: String = EventStatus.UPCOMING.name,
    val roundNumber: Long = 1L,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class TimingSession(
    val id: String = "",
    val eventId: String = "",
    val championshipId: String = "",
    val circuitId: String = "",
    val circuitName: String = "",
    val sessionName: String = "",
    val sessionType: String = SessionType.PRACTICE.name,
    val status: String = SessionStatus.SCHEDULED.name,
    val flagStatus: String = FlagStatus.GREEN.name,
    val timingMode: String = TimingMode.MANUAL.name,
    val totalLapsTarget: Long = 0L,
    val durationMinutes: Long = 60L,
    val startedAtEpochMs: Long = 0L,
    val elapsedPausedMs: Long = 0L,
    val bestSessionLapMs: Long = 0L,
    val bestSessionLapDriverId: String = "",
    val bestSector1Ms: Long = 0L,
    val bestSector2Ms: Long = 0L,
    val bestSector3Ms: Long = 0L,
    val sectorCount: Long = 3L,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class SessionEntry(
    val id: String = "",
    val sessionId: String = "",
    val eventId: String = "",
    val championshipId: String = "",
    val driverName: String = "",
    val driverCode: String = "",
    val nationality: String = "",
    val countryCode: String = "",
    val teamName: String = "",
    val teamColorHex: String = "#FF1E38",
    val carModel: String = "",
    val carNumber: Long = 0L,
    val driverStatus: String = DriverStatus.ON_TRACK.name,
    val position: Long = 1L,
    val lapsCompleted: Long = 0L,
    val lastLapMs: Long = 0L,
    val bestLapMs: Long = 0L,
    val totalTimeMs: Long = 0L,
    val currentSector1Ms: Long = 0L,
    val currentSector2Ms: Long = 0L,
    val currentSector3Ms: Long = 0L,
    val bestSector1Ms: Long = 0L,
    val bestSector2Ms: Long = 0L,
    val bestSector3Ms: Long = 0L,
    val penaltySeconds: Long = 0L,
    val pointsAwarded: Long = 0L,
    val hasFastestLap: Boolean = false,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class LapRecord(
    val id: String = "",
    val sessionId: String = "",
    val entryId: String = "",
    val driverName: String = "",
    val carNumber: Long = 0L,
    val lapNumber: Long = 1L,
    val lapTimeMs: Long = 0L,
    val sectorTimesMs: List<Long> = emptyList(),
    val isValid: Boolean = true,
    val invalidReason: String = "",
    val isPersonalBest: Boolean = false,
    val isSessionBest: Boolean = false,
    val timingSource: String = TimingSource.MANUAL.name,
    val gpsAccuracyMeters: Double = 0.0,
    val recordedAtEpochMs: Long = 0L
)

data class PenaltyRecord(
    val id: String = "",
    val sessionId: String = "",
    val entryId: String = "",
    val driverName: String = "",
    val carNumber: Long = 0L,
    val penaltyType: String = PenaltyType.WARNING.name,
    val secondsAdded: Long = 0L,
    val reason: String = "",
    val issuedAtEpochMs: Long = 0L
)

data class LiveTimingRow(
    val entry: SessionEntry,
    val computedPosition: Int,
    val gapToLeader: String,
    val intervalToAhead: String,
    val sector1Color: SectorColorState,
    val sector2Color: SectorColorState,
    val sector3Color: SectorColorState
)

data class DriverStanding(
    val position: Int,
    val driverName: String,
    val driverCode: String,
    val countryCode: String,
    val teamName: String,
    val teamColorHex: String,
    val points: Int,
    val wins: Int,
    val podiums: Int,
    val fastestLaps: Int
)

data class ConstructorStanding(
    val position: Int,
    val teamName: String,
    val teamColorHex: String,
    val points: Int,
    val wins: Int
)

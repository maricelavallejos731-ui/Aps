package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.Championship
import com.example.data.model.Circuit
import com.example.data.model.LapRecord
import com.example.data.model.PenaltyRecord
import com.example.data.model.RaceEvent
import com.example.data.model.SessionEntry
import com.example.data.model.TimingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.apexPrefsDataStore by preferencesDataStore(name = "apex_timing_prefs")

object MasterPasswordHasher {
    const val INITIAL_SALT = "apex_v1_salt_731"
    const val INITIAL_HASH = "6a429810419d1b7e8860f5ebfc210fadee6de3471f2023fcb742a34fde8bb9aa"

    fun hashPassword(salt: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$password".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verify(input: String, salt: String, expectedHash: String): Boolean {
        if (input.isEmpty() || expectedHash.isEmpty()) return false
        val actual = hashPassword(salt, input).toByteArray(Charsets.UTF_8)
        val expected = expectedHash.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(actual, expected)
    }

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

class LocalPreferencesDataStore(private val context: Context) {
    private val localModeKey = booleanPreferencesKey("local_offline_mode")
    private val wifiEndpointKey = stringPreferencesKey("wifi_api_endpoint")
    private val selectedChampionshipIdKey = stringPreferencesKey("selected_championship_id")
    private val selectedSessionIdKey = stringPreferencesKey("selected_session_id")
    private val masterSaltKey = stringPreferencesKey("master_password_salt")
    private val masterHashKey = stringPreferencesKey("master_password_hash")
    private val customPasswordSetKey = booleanPreferencesKey("custom_password_configured")

    val isLocalOfflineMode: Flow<Boolean> = context.apexPrefsDataStore.data.map { it[localModeKey] ?: false }
    val wifiEndpointUrl: Flow<String> = context.apexPrefsDataStore.data.map { it[wifiEndpointKey] ?: "" }
    val selectedChampionshipId: Flow<String> = context.apexPrefsDataStore.data.map { it[selectedChampionshipIdKey] ?: "" }
    val selectedSessionId: Flow<String> = context.apexPrefsDataStore.data.map { it[selectedSessionIdKey] ?: "" }
    val hasCustomMasterPassword: Flow<Boolean> = context.apexPrefsDataStore.data.map { it[customPasswordSetKey] ?: false }

    suspend fun setLocalOfflineMode(enabled: Boolean) {
        context.apexPrefsDataStore.edit { it[localModeKey] = enabled }
    }

    suspend fun setWifiEndpointUrl(url: String) {
        context.apexPrefsDataStore.edit { it[wifiEndpointKey] = url.trim() }
    }

    suspend fun setSelectedChampionshipId(id: String) {
        context.apexPrefsDataStore.edit { it[selectedChampionshipIdKey] = id }
    }

    suspend fun setSelectedSessionId(id: String) {
        context.apexPrefsDataStore.edit { it[selectedSessionIdKey] = id }
    }

    suspend fun verifyMasterPassword(candidate: String): Boolean {
        val prefs = context.apexPrefsDataStore.data.first()
        val salt = prefs[masterSaltKey] ?: MasterPasswordHasher.INITIAL_SALT
        val expectedHash = prefs[masterHashKey] ?: MasterPasswordHasher.INITIAL_HASH
        return MasterPasswordHasher.verify(candidate.trim(), salt, expectedHash)
    }

    suspend fun updateMasterPassword(currentPassword: String, newPassword: String): Result<Unit> {
        val trimmedNew = newPassword.trim()
        if (trimmedNew.length < 4) {
            return Result.failure(IllegalArgumentException("La nueva contraseña debe tener al menos 4 caracteres."))
        }
        if (!verifyMasterPassword(currentPassword)) {
            return Result.failure(IllegalArgumentException("La contraseña actual no es correcta."))
        }
        val newSalt = MasterPasswordHasher.generateSalt()
        val newHash = MasterPasswordHasher.hashPassword(newSalt, trimmedNew)
        context.apexPrefsDataStore.edit { prefs ->
            prefs[masterSaltKey] = newSalt
            prefs[masterHashKey] = newHash
            prefs[customPasswordSetKey] = true
        }
        return Result.success(Unit)
    }
}

class ApexRoomConverters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString("||")

    @TypeConverter
    fun toStringList(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split("||")

    @TypeConverter
    fun fromDoubleList(list: List<Double>): String = list.joinToString(",")

    @TypeConverter
    fun toDoubleList(raw: String): List<Double> =
        if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.toDoubleOrNull() }

    @TypeConverter
    fun fromLongList(list: List<Long>): String = list.joinToString(",")

    @TypeConverter
    fun toLongList(raw: String): List<Long> =
        if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.toLongOrNull() }
}

@Entity(tableName = "championships")
data class ChampionshipEntity(
    @PrimaryKey val id: String,
    val name: String,
    val seasonYear: Long,
    val category: String,
    val status: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toModel(): Championship = Championship(
        id = id,
        name = name,
        seasonYear = seasonYear,
        category = category,
        status = status,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "circuits")
data class CircuitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val country: String,
    val countryCode: String,
    val lengthKm: Double,
    val turnsCount: Long,
    val sectorCount: Long,
    val startFinishLabel: String,
    val sectorSplits: List<Double>,
    val measurementPoints: List<String>,
    val trackPoints: List<String>,
    val customImageUri: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toModel(): Circuit = Circuit(
        id = id,
        name = name,
        country = country,
        countryCode = countryCode,
        lengthKm = lengthKm,
        turnsCount = turnsCount,
        sectorCount = sectorCount,
        startFinishLabel = startFinishLabel,
        sectorSplits = sectorSplits,
        measurementPoints = measurementPoints,
        trackPoints = trackPoints,
        customImageUri = customImageUri,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "race_events")
data class RaceEventEntity(
    @PrimaryKey val id: String,
    val championshipId: String,
    val circuitId: String,
    val circuitName: String,
    val grandPrixName: String,
    val country: String,
    val countryCode: String,
    val eventDateMillis: Long,
    val practiceSchedule: String,
    val qualifyingSchedule: String,
    val raceSchedule: String,
    val raceDurationLabel: String,
    val status: String,
    val roundNumber: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toModel(): RaceEvent = RaceEvent(
        id = id,
        championshipId = championshipId,
        circuitId = circuitId,
        circuitName = circuitName,
        grandPrixName = grandPrixName,
        country = country,
        countryCode = countryCode,
        eventDateMillis = eventDateMillis,
        practiceSchedule = practiceSchedule,
        qualifyingSchedule = qualifyingSchedule,
        raceSchedule = raceSchedule,
        raceDurationLabel = raceDurationLabel,
        status = status,
        roundNumber = roundNumber,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "timing_sessions")
data class TimingSessionEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val championshipId: String,
    val circuitId: String,
    val circuitName: String,
    val sessionName: String,
    val sessionType: String,
    val status: String,
    val flagStatus: String,
    val timingMode: String,
    val totalLapsTarget: Long,
    val durationMinutes: Long,
    val startedAtEpochMs: Long,
    val elapsedPausedMs: Long,
    val bestSessionLapMs: Long,
    val bestSessionLapDriverId: String,
    val bestSector1Ms: Long,
    val bestSector2Ms: Long,
    val bestSector3Ms: Long,
    val sectorCount: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toModel(): TimingSession = TimingSession(
        id = id,
        eventId = eventId,
        championshipId = championshipId,
        circuitId = circuitId,
        circuitName = circuitName,
        sessionName = sessionName,
        sessionType = sessionType,
        status = status,
        flagStatus = flagStatus,
        timingMode = timingMode,
        totalLapsTarget = totalLapsTarget,
        durationMinutes = durationMinutes,
        startedAtEpochMs = startedAtEpochMs,
        elapsedPausedMs = elapsedPausedMs,
        bestSessionLapMs = bestSessionLapMs,
        bestSessionLapDriverId = bestSessionLapDriverId,
        bestSector1Ms = bestSector1Ms,
        bestSector2Ms = bestSector2Ms,
        bestSector3Ms = bestSector3Ms,
        sectorCount = sectorCount,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "session_entries")
data class SessionEntryEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val eventId: String,
    val championshipId: String,
    val driverName: String,
    val driverCode: String,
    val nationality: String,
    val countryCode: String,
    val teamName: String,
    val teamColorHex: String,
    val carModel: String,
    val carNumber: Long,
    val driverStatus: String,
    val position: Long,
    val lapsCompleted: Long,
    val lastLapMs: Long,
    val bestLapMs: Long,
    val totalTimeMs: Long,
    val currentSector1Ms: Long,
    val currentSector2Ms: Long,
    val currentSector3Ms: Long,
    val bestSector1Ms: Long,
    val bestSector2Ms: Long,
    val bestSector3Ms: Long,
    val penaltySeconds: Long,
    val pointsAwarded: Long,
    val hasFastestLap: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toModel(): SessionEntry = SessionEntry(
        id = id,
        sessionId = sessionId,
        eventId = eventId,
        championshipId = championshipId,
        driverName = driverName,
        driverCode = driverCode,
        nationality = nationality,
        countryCode = countryCode,
        teamName = teamName,
        teamColorHex = teamColorHex,
        carModel = carModel,
        carNumber = carNumber,
        driverStatus = driverStatus,
        position = position,
        lapsCompleted = lapsCompleted,
        lastLapMs = lastLapMs,
        bestLapMs = bestLapMs,
        totalTimeMs = totalTimeMs,
        currentSector1Ms = currentSector1Ms,
        currentSector2Ms = currentSector2Ms,
        currentSector3Ms = currentSector3Ms,
        bestSector1Ms = bestSector1Ms,
        bestSector2Ms = bestSector2Ms,
        bestSector3Ms = bestSector3Ms,
        penaltySeconds = penaltySeconds,
        pointsAwarded = pointsAwarded,
        hasFastestLap = hasFastestLap,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "laps")
data class LapRecordEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val entryId: String,
    val driverName: String,
    val carNumber: Long,
    val lapNumber: Long,
    val lapTimeMs: Long,
    val sectorTimesMs: List<Long>,
    val isValid: Boolean,
    val invalidReason: String,
    val isPersonalBest: Boolean,
    val isSessionBest: Boolean,
    val timingSource: String,
    val gpsAccuracyMeters: Double,
    val recordedAtEpochMs: Long
) {
    fun toModel(): LapRecord = LapRecord(
        id = id,
        sessionId = sessionId,
        entryId = entryId,
        driverName = driverName,
        carNumber = carNumber,
        lapNumber = lapNumber,
        lapTimeMs = lapTimeMs,
        sectorTimesMs = sectorTimesMs,
        isValid = isValid,
        invalidReason = invalidReason,
        isPersonalBest = isPersonalBest,
        isSessionBest = isSessionBest,
        timingSource = timingSource,
        gpsAccuracyMeters = gpsAccuracyMeters,
        recordedAtEpochMs = recordedAtEpochMs
    )
}

@Entity(tableName = "penalties")
data class PenaltyRecordEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val entryId: String,
    val driverName: String,
    val carNumber: Long,
    val penaltyType: String,
    val secondsAdded: Long,
    val reason: String,
    val issuedAtEpochMs: Long
) {
    fun toModel(): PenaltyRecord = PenaltyRecord(
        id = id,
        sessionId = sessionId,
        entryId = entryId,
        driverName = driverName,
        carNumber = carNumber,
        penaltyType = penaltyType,
        secondsAdded = secondsAdded,
        reason = reason,
        issuedAtEpochMs = issuedAtEpochMs
    )
}

@Entity(tableName = "pending_laps")
data class PendingLapEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val entryId: String,
    val driverName: String,
    val carNumber: Long,
    val lapNumber: Long,
    val lapTimeMs: Long,
    val sector1Ms: Long,
    val sector2Ms: Long,
    val sector3Ms: Long,
    val isValid: Boolean,
    val invalidReason: String,
    val timingSource: String,
    val gpsAccuracyMeters: Double,
    val recordedAtEpochMs: Long
)

@Dao
interface PendingLapDao {
    @Query("SELECT * FROM pending_laps ORDER BY recordedAtEpochMs ASC")
    fun observeAll(): Flow<List<PendingLapEntity>>

    @Query("SELECT * FROM pending_laps ORDER BY recordedAtEpochMs ASC")
    suspend fun getAllOnce(): List<PendingLapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lap: PendingLapEntity)

    @Query("DELETE FROM pending_laps WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_laps")
    suspend fun clearAll()
}

@Dao
interface ApexTimingDao {
    @Query("SELECT * FROM championships ORDER BY seasonYear DESC, createdAtEpochMs DESC")
    fun observeChampionships(): Flow<List<ChampionshipEntity>>

    @Query("SELECT COUNT(*) FROM championships")
    suspend fun countChampionships(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChampionship(item: ChampionshipEntity)

    @Query("DELETE FROM championships WHERE id = :id")
    suspend fun deleteChampionship(id: String)

    @Query("SELECT * FROM circuits ORDER BY name ASC")
    fun observeCircuits(): Flow<List<CircuitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCircuit(item: CircuitEntity)

    @Query("SELECT * FROM race_events ORDER BY roundNumber ASC")
    fun observeEvents(): Flow<List<RaceEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(item: RaceEventEntity)

    @Query("UPDATE race_events SET status = :status, updatedAtEpochMs = :updatedAt WHERE id = :eventId")
    suspend fun updateEventStatus(eventId: String, status: String, updatedAt: Long)

    @Query("SELECT * FROM timing_sessions ORDER BY createdAtEpochMs DESC")
    fun observeSessions(): Flow<List<TimingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(item: TimingSessionEntity)

    @Query("SELECT * FROM timing_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): TimingSessionEntity?

    @Query("SELECT * FROM session_entries ORDER BY position ASC")
    fun observeAllSessionEntries(): Flow<List<SessionEntryEntity>>

    @Query("SELECT * FROM session_entries WHERE sessionId = :sessionId")
    suspend fun getEntriesForSessionOnce(sessionId: String): List<SessionEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(item: SessionEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(items: List<SessionEntryEntity>)

    @Query("UPDATE session_entries SET driverStatus = :status, updatedAtEpochMs = :updatedAt WHERE id = :entryId")
    suspend fun updateDriverStatus(entryId: String, status: String, updatedAt: Long)

    @Query("SELECT * FROM laps WHERE sessionId = :sessionId ORDER BY recordedAtEpochMs DESC")
    fun observeLapsForSession(sessionId: String): Flow<List<LapRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLap(item: LapRecordEntity)

    @Query("SELECT * FROM laps WHERE id = :lapId LIMIT 1")
    suspend fun getLapById(lapId: String): LapRecordEntity?

    @Query("UPDATE laps SET isValid = :valid, invalidReason = :reason WHERE id = :lapId")
    suspend fun updateLapValidity(lapId: String, valid: Boolean, reason: String)

    @Query("SELECT * FROM penalties WHERE sessionId = :sessionId ORDER BY issuedAtEpochMs DESC")
    fun observePenaltiesForSession(sessionId: String): Flow<List<PenaltyRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPenalty(item: PenaltyRecordEntity)
}

@Database(
    entities = [
        ChampionshipEntity::class,
        CircuitEntity::class,
        RaceEventEntity::class,
        TimingSessionEntity::class,
        SessionEntryEntity::class,
        LapRecordEntity::class,
        PenaltyRecordEntity::class,
        PendingLapEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(ApexRoomConverters::class)
abstract class ApexLocalDatabase : RoomDatabase() {
    abstract fun pendingLapDao(): PendingLapDao
    abstract fun timingDao(): ApexTimingDao

    companion object {
        @Volatile
        private var INSTANCE: ApexLocalDatabase? = null

        fun getInstance(context: Context): ApexLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ApexLocalDatabase::class.java,
                    "apex_local_timing.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

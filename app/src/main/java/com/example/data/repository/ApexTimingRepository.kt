package com.example.data.repository

import com.example.data.local.ApexTimingDao
import com.example.data.local.ChampionshipEntity
import com.example.data.local.CircuitEntity
import com.example.data.local.LapRecordEntity
import com.example.data.local.PenaltyRecordEntity
import com.example.data.local.PendingLapDao
import com.example.data.local.PendingLapEntity
import com.example.data.local.RaceEventEntity
import com.example.data.local.SessionEntryEntity
import com.example.data.local.TimingSessionEntity
import com.example.data.model.Championship
import com.example.data.model.Circuit
import com.example.data.model.DriverStatus
import com.example.data.model.EventStatus
import com.example.data.model.FlagStatus
import com.example.data.model.LapRecord
import com.example.data.model.PenaltyRecord
import com.example.data.model.PenaltyType
import com.example.data.model.RaceEvent
import com.example.data.model.SessionEntry
import com.example.data.model.SessionStatus
import com.example.data.model.SessionType
import com.example.data.model.TimingMode
import com.example.data.model.TimingSession
import com.example.domain.TimingEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ApexTimingRepository(
    private val dao: ApexTimingDao,
    private val pendingLapDao: PendingLapDao
) {
    fun observeChampionships(): Flow<List<Championship>> =
        dao.observeChampionships().map { list -> list.map { it.toModel() } }

    fun observeCircuits(): Flow<List<Circuit>> =
        dao.observeCircuits().map { list -> list.map { it.toModel() } }

    fun observeEvents(): Flow<List<RaceEvent>> =
        dao.observeEvents().map { list -> list.map { it.toModel() } }

    fun observeSessions(): Flow<List<TimingSession>> =
        dao.observeSessions().map { list -> list.map { it.toModel() } }

    fun observeAllSessionEntries(): Flow<List<SessionEntry>> =
        dao.observeAllSessionEntries().map { list -> list.map { it.toModel() } }

    fun observeLapsForSession(sessionId: String): Flow<List<LapRecord>> =
        dao.observeLapsForSession(sessionId).map { list -> list.map { it.toModel() } }

    fun observePenaltiesForSession(sessionId: String): Flow<List<PenaltyRecord>> =
        dao.observePenaltiesForSession(sessionId).map { list -> list.map { it.toModel() } }

    suspend fun ensureInitialStructureIfEmpty() {
        if (dao.countChampionships() == 0) {
            initializeTemplateStructure()
        }
    }

    suspend fun createChampionship(name: String, seasonYear: Long, category: String): Result<String> {
        return try {
            val id = "champ_${UUID.randomUUID().toString().take(8)}"
            val now = System.currentTimeMillis()
            dao.upsertChampionship(
                ChampionshipEntity(
                    id = id,
                    name = name.trim().take(120),
                    seasonYear = seasonYear,
                    category = category.trim().take(80),
                    status = "ACTIVE",
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChampionship(championshipId: String): Result<Unit> {
        return try {
            dao.deleteChampionship(championshipId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCircuit(
        circuitId: String? = null,
        name: String,
        country: String,
        countryCode: String,
        lengthKm: Double,
        turnsCount: Long,
        sectorCount: Long,
        startFinishLabel: String,
        sectorSplits: List<Double>,
        measurementPoints: List<String>,
        trackPoints: List<String>,
        customImageUri: String? = null
    ): Result<String> {
        return try {
            val id = circuitId ?: "circ_${UUID.randomUUID().toString().take(8)}"
            val now = System.currentTimeMillis()
            dao.upsertCircuit(
                CircuitEntity(
                    id = id,
                    name = name.trim().take(120),
                    country = country.trim().take(80),
                    countryCode = countryCode.trim().uppercase().take(4),
                    lengthKm = lengthKm,
                    turnsCount = turnsCount,
                    sectorCount = sectorCount.coerceIn(1L, 6L),
                    startFinishLabel = startFinishLabel.trim().take(100),
                    sectorSplits = sectorSplits,
                    measurementPoints = measurementPoints.take(16),
                    trackPoints = trackPoints.take(64),
                    customImageUri = customImageUri.orEmpty().take(500),
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRaceEvent(
        championshipId: String,
        circuit: Circuit,
        grandPrixName: String,
        eventDateMillis: Long,
        practiceSchedule: String,
        qualifyingSchedule: String,
        raceSchedule: String,
        raceDurationLabel: String,
        roundNumber: Long,
        status: EventStatus = EventStatus.UPCOMING
    ): Result<String> {
        return try {
            val id = "evt_${UUID.randomUUID().toString().take(8)}"
            val now = System.currentTimeMillis()
            dao.upsertEvent(
                RaceEventEntity(
                    id = id,
                    championshipId = championshipId,
                    circuitId = circuit.id,
                    circuitName = circuit.name,
                    grandPrixName = grandPrixName.trim().take(140),
                    country = circuit.country,
                    countryCode = circuit.countryCode,
                    eventDateMillis = eventDateMillis,
                    practiceSchedule = practiceSchedule.trim().take(100),
                    qualifyingSchedule = qualifyingSchedule.trim().take(100),
                    raceSchedule = raceSchedule.trim().take(100),
                    raceDurationLabel = raceDurationLabel.trim().take(80),
                    status = status.name,
                    roundNumber = roundNumber,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEventStatus(eventId: String, status: EventStatus): Result<Unit> {
        return try {
            dao.updateEventStatus(eventId, status.name, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSession(
        event: RaceEvent,
        sessionName: String,
        sessionType: SessionType,
        totalLapsTarget: Long,
        durationMinutes: Long,
        sectorCount: Long
    ): Result<String> {
        return try {
            val id = "sess_${UUID.randomUUID().toString().take(8)}"
            val now = System.currentTimeMillis()
            dao.upsertSession(
                TimingSessionEntity(
                    id = id,
                    eventId = event.id,
                    championshipId = event.championshipId,
                    circuitId = event.circuitId,
                    circuitName = event.circuitName,
                    sessionName = sessionName.trim().take(100),
                    sessionType = sessionType.name,
                    status = SessionStatus.SCHEDULED.name,
                    flagStatus = FlagStatus.GREEN.name,
                    timingMode = TimingMode.MANUAL.name,
                    totalLapsTarget = totalLapsTarget,
                    durationMinutes = durationMinutes,
                    startedAtEpochMs = 0L,
                    elapsedPausedMs = 0L,
                    bestSessionLapMs = 0L,
                    bestSessionLapDriverId = "",
                    bestSector1Ms = 0L,
                    bestSector2Ms = 0L,
                    bestSector3Ms = 0L,
                    sectorCount = sectorCount.coerceIn(1L, 6L),
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSessionControl(
        session: TimingSession,
        status: SessionStatus? = null,
        flagStatus: FlagStatus? = null,
        timingMode: TimingMode? = null,
        startedAtEpochMs: Long? = null,
        elapsedPausedMs: Long? = null
    ): Result<Unit> {
        return try {
            val existing = dao.getSessionById(session.id)
                ?: return Result.failure(IllegalStateException("Sesión no encontrada"))
            dao.upsertSession(
                existing.copy(
                    status = status?.name ?: existing.status,
                    flagStatus = flagStatus?.name ?: existing.flagStatus,
                    timingMode = timingMode?.name ?: existing.timingMode,
                    startedAtEpochMs = startedAtEpochMs ?: existing.startedAtEpochMs,
                    elapsedPausedMs = elapsedPausedMs ?: existing.elapsedPausedMs,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSessionEntry(
        session: TimingSession,
        driverName: String,
        driverCode: String,
        nationality: String,
        countryCode: String,
        teamName: String,
        teamColorHex: String,
        carModel: String,
        carNumber: Long,
        initialPosition: Long
    ): Result<String> {
        return try {
            val id = "ent_${UUID.randomUUID().toString().take(8)}"
            val now = System.currentTimeMillis()
            dao.upsertEntry(
                SessionEntryEntity(
                    id = id,
                    sessionId = session.id,
                    eventId = session.eventId,
                    championshipId = session.championshipId,
                    driverName = driverName.trim().take(100),
                    driverCode = driverCode.trim().uppercase().take(5),
                    nationality = nationality.trim().take(80),
                    countryCode = countryCode.trim().uppercase().take(4),
                    teamName = teamName.trim().take(100),
                    teamColorHex = teamColorHex.trim().take(12),
                    carModel = carModel.trim().take(80),
                    carNumber = carNumber,
                    driverStatus = DriverStatus.ON_TRACK.name,
                    position = initialPosition,
                    lapsCompleted = 0L,
                    lastLapMs = 0L,
                    bestLapMs = 0L,
                    totalTimeMs = 0L,
                    currentSector1Ms = 0L,
                    currentSector2Ms = 0L,
                    currentSector3Ms = 0L,
                    bestSector1Ms = 0L,
                    bestSector2Ms = 0L,
                    bestSector3Ms = 0L,
                    penaltySeconds = 0L,
                    pointsAwarded = 0L,
                    hasFastestLap = false,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDriverStatus(entryId: String, status: DriverStatus): Result<Unit> {
        return try {
            dao.updateDriverStatus(entryId, status.name, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun issuePenalty(
        sessionId: String,
        entry: SessionEntry,
        penaltyType: PenaltyType,
        reason: String
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val penaltyId = "pen_${UUID.randomUUID().toString().take(8)}"
            dao.upsertPenalty(
                PenaltyRecordEntity(
                    id = penaltyId,
                    sessionId = sessionId,
                    entryId = entry.id,
                    driverName = entry.driverName,
                    carNumber = entry.carNumber,
                    penaltyType = penaltyType.name,
                    secondsAdded = penaltyType.defaultSeconds.toLong(),
                    reason = reason.trim().ifBlank { penaltyType.labelEs }.take(240),
                    issuedAtEpochMs = now
                )
            )
            val entries = dao.getEntriesForSessionOnce(sessionId)
            entries.firstOrNull { it.id == entry.id }?.let { existingEntry ->
                dao.upsertEntry(
                    existingEntry.copy(
                        penaltySeconds = existingEntry.penaltySeconds + penaltyType.defaultSeconds,
                        driverStatus = if (penaltyType == PenaltyType.DISQUALIFIED) {
                            DriverStatus.OUT_OF_SESSION.name
                        } else {
                            existingEntry.driverStatus
                        },
                        updatedAtEpochMs = now
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordLap(
        session: TimingSession,
        entry: SessionEntry,
        allSessionEntries: List<SessionEntry>,
        lapTimeMs: Long,
        sectorTimesMs: List<Long>,
        timingSource: String,
        gpsAccuracyMeters: Double,
        forceInvalidReason: String? = null,
        localOfflineOnly: Boolean = false
    ): Result<Unit> {
        val (autoValid, autoReason) = TimingEngine.validateLapTime(lapTimeMs)
        val isValid = autoValid && forceInvalidReason.isNullOrBlank()
        val invalidReason = forceInvalidReason?.take(160) ?: autoReason

        val s1 = sectorTimesMs.getOrElse(0) { lapTimeMs / 3 }
        val s2 = sectorTimesMs.getOrElse(1) { lapTimeMs / 3 }
        val s3 = sectorTimesMs.getOrElse(2) { lapTimeMs - s1 - s2 }
        val cleanSectors = if (sectorTimesMs.isNotEmpty()) sectorTimesMs else listOf(s1, s2, s3)
        val newLapNum = entry.lapsCompleted + 1L
        val lapId = "lap_${UUID.randomUUID().toString().take(10)}"
        val nowMs = System.currentTimeMillis()

        if (localOfflineOnly) {
            pendingLapDao.insert(
                PendingLapEntity(
                    id = lapId,
                    sessionId = session.id,
                    entryId = entry.id,
                    driverName = entry.driverName,
                    carNumber = entry.carNumber,
                    lapNumber = newLapNum,
                    lapTimeMs = lapTimeMs,
                    sector1Ms = s1,
                    sector2Ms = s2,
                    sector3Ms = s3,
                    isValid = isValid,
                    invalidReason = invalidReason,
                    timingSource = timingSource,
                    gpsAccuracyMeters = gpsAccuracyMeters,
                    recordedAtEpochMs = nowMs
                )
            )
            return Result.success(Unit)
        }

        return commitLapToDatabase(
            lapId = lapId,
            session = session,
            entry = entry,
            allSessionEntries = allSessionEntries,
            lapNumber = newLapNum,
            lapTimeMs = lapTimeMs,
            sectorTimesMs = cleanSectors,
            s1 = s1,
            s2 = s2,
            s3 = s3,
            isValid = isValid,
            invalidReason = invalidReason,
            timingSource = timingSource,
            gpsAccuracyMeters = gpsAccuracyMeters,
            recordedAtEpochMs = nowMs
        )
    }

    private suspend fun commitLapToDatabase(
        lapId: String,
        session: TimingSession,
        entry: SessionEntry,
        allSessionEntries: List<SessionEntry>,
        lapNumber: Long,
        lapTimeMs: Long,
        sectorTimesMs: List<Long>,
        s1: Long,
        s2: Long,
        s3: Long,
        isValid: Boolean,
        invalidReason: String,
        timingSource: String,
        gpsAccuracyMeters: Double,
        recordedAtEpochMs: Long
    ): Result<Unit> {
        return try {
            val isPersonalBest = isValid && (entry.bestLapMs == 0L || lapTimeMs < entry.bestLapMs)
            val isSessionBest = isValid && (session.bestSessionLapMs == 0L || lapTimeMs < session.bestSessionLapMs)

            val newBestLap = if (isPersonalBest) lapTimeMs else entry.bestLapMs
            val newBestS1 = if (isValid && s1 > 0 && (entry.bestSector1Ms == 0L || s1 < entry.bestSector1Ms)) s1 else entry.bestSector1Ms
            val newBestS2 = if (isValid && s2 > 0 && (entry.bestSector2Ms == 0L || s2 < entry.bestSector2Ms)) s2 else entry.bestSector2Ms
            val newBestS3 = if (isValid && s3 > 0 && (entry.bestSector3Ms == 0L || s3 < entry.bestSector3Ms)) s3 else entry.bestSector3Ms

            val newSessBestLap = if (isSessionBest) lapTimeMs else session.bestSessionLapMs
            val newSessBestDriverId = if (isSessionBest) entry.id else session.bestSessionLapDriverId
            val newSessS1 = if (isValid && s1 > 0 && (session.bestSector1Ms == 0L || s1 < session.bestSector1Ms)) s1 else session.bestSector1Ms
            val newSessS2 = if (isValid && s2 > 0 && (session.bestSector2Ms == 0L || s2 < session.bestSector2Ms)) s2 else session.bestSector2Ms
            val newSessS3 = if (isValid && s3 > 0 && (session.bestSector3Ms == 0L || s3 < session.bestSector3Ms)) s3 else session.bestSector3Ms

            val updatedEntry = entry.copy(
                lapsCompleted = lapNumber,
                lastLapMs = lapTimeMs,
                bestLapMs = newBestLap,
                totalTimeMs = entry.totalTimeMs + lapTimeMs,
                currentSector1Ms = s1,
                currentSector2Ms = s2,
                currentSector3Ms = s3,
                bestSector1Ms = newBestS1,
                bestSector2Ms = newBestS2,
                bestSector3Ms = newBestS3,
                hasFastestLap = if (isSessionBest) true else entry.hasFastestLap,
                updatedAtEpochMs = recordedAtEpochMs
            )

            val sessionType = SessionType.fromString(session.sessionType)
            val updatedList = allSessionEntries.map {
                if (it.id == entry.id) updatedEntry
                else if (isSessionBest) it.copy(hasFastestLap = false, updatedAtEpochMs = recordedAtEpochMs)
                else it
            }
            val resorted = TimingEngine.sortEntriesForSession(updatedList, sessionType)

            dao.upsertLap(
                LapRecordEntity(
                    id = lapId,
                    sessionId = session.id,
                    entryId = entry.id,
                    driverName = entry.driverName,
                    carNumber = entry.carNumber,
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
            )

            val entityUpdates = resorted.mapIndexed { idx, item ->
                SessionEntryEntity(
                    id = item.id,
                    sessionId = item.sessionId,
                    eventId = item.eventId,
                    championshipId = item.championshipId,
                    driverName = item.driverName,
                    driverCode = item.driverCode,
                    nationality = item.nationality,
                    countryCode = item.countryCode,
                    teamName = item.teamName,
                    teamColorHex = item.teamColorHex,
                    carModel = item.carModel,
                    carNumber = item.carNumber,
                    driverStatus = item.driverStatus,
                    position = (idx + 1).toLong(),
                    lapsCompleted = item.lapsCompleted,
                    lastLapMs = item.lastLapMs,
                    bestLapMs = item.bestLapMs,
                    totalTimeMs = item.totalTimeMs,
                    currentSector1Ms = item.currentSector1Ms,
                    currentSector2Ms = item.currentSector2Ms,
                    currentSector3Ms = item.currentSector3Ms,
                    bestSector1Ms = item.bestSector1Ms,
                    bestSector2Ms = item.bestSector2Ms,
                    bestSector3Ms = item.bestSector3Ms,
                    penaltySeconds = item.penaltySeconds,
                    pointsAwarded = item.pointsAwarded,
                    hasFastestLap = item.hasFastestLap,
                    createdAtEpochMs = item.createdAtEpochMs,
                    updatedAtEpochMs = recordedAtEpochMs
                )
            }
            dao.upsertEntries(entityUpdates)

            if (isValid) {
                dao.getSessionById(session.id)?.let { currentSess ->
                    dao.upsertSession(
                        currentSess.copy(
                            bestSessionLapMs = newSessBestLap,
                            bestSessionLapDriverId = newSessBestDriverId,
                            bestSector1Ms = newSessS1,
                            bestSector2Ms = newSessS2,
                            bestSector3Ms = newSessS3,
                            updatedAtEpochMs = recordedAtEpochMs
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLapValidity(lap: LapRecord, valid: Boolean, reason: String): Result<Unit> {
        return try {
            dao.updateLapValidity(
                lapId = lap.id,
                valid = valid,
                reason = if (valid) "" else reason.ifBlank { "Invalidada por dirección de carrera" }.take(160)
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPendingOfflineLaps(
        sessions: List<TimingSession>,
        entries: List<SessionEntry>
    ): Result<Int> {
        val pending = pendingLapDao.getAllOnce()
        if (pending.isEmpty()) return Result.success(0)

        var syncedCount = 0
        for (item in pending) {
            val sess = sessions.firstOrNull { it.id == item.sessionId } ?: continue
            val ent = entries.firstOrNull { it.id == item.entryId } ?: continue
            val sessEntries = entries.filter { it.sessionId == sess.id }
            val result = commitLapToDatabase(
                lapId = item.id,
                session = sess,
                entry = ent,
                allSessionEntries = sessEntries,
                lapNumber = item.lapNumber,
                lapTimeMs = item.lapTimeMs,
                sectorTimesMs = listOf(item.sector1Ms, item.sector2Ms, item.sector3Ms),
                s1 = item.sector1Ms,
                s2 = item.sector2Ms,
                s3 = item.sector3Ms,
                isValid = item.isValid,
                invalidReason = item.invalidReason,
                timingSource = item.timingSource,
                gpsAccuracyMeters = item.gpsAccuracyMeters,
                recordedAtEpochMs = item.recordedAtEpochMs
            )
            if (result.isSuccess) {
                pendingLapDao.deleteById(item.id)
                syncedCount++
            }
        }
        return Result.success(syncedCount)
    }

    suspend fun finalizeRaceAndAwardPoints(
        session: TimingSession,
        entries: List<SessionEntry>
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val sorted = TimingEngine.sortEntriesForSession(entries, SessionType.fromString(session.sessionType))
            val updated = sorted.mapIndexed { index, entry ->
                val pos = index + 1
                val pts = if (SessionType.fromString(session.sessionType) == SessionType.RACE) {
                    TimingEngine.calculatePointsForPosition(pos, entry.hasFastestLap, entry.lapsCompleted)
                } else {
                    0
                }
                SessionEntryEntity(
                    id = entry.id,
                    sessionId = entry.sessionId,
                    eventId = entry.eventId,
                    championshipId = entry.championshipId,
                    driverName = entry.driverName,
                    driverCode = entry.driverCode,
                    nationality = entry.nationality,
                    countryCode = entry.countryCode,
                    teamName = entry.teamName,
                    teamColorHex = entry.teamColorHex,
                    carModel = entry.carModel,
                    carNumber = entry.carNumber,
                    driverStatus = entry.driverStatus,
                    position = pos.toLong(),
                    lapsCompleted = entry.lapsCompleted,
                    lastLapMs = entry.lastLapMs,
                    bestLapMs = entry.bestLapMs,
                    totalTimeMs = entry.totalTimeMs,
                    currentSector1Ms = entry.currentSector1Ms,
                    currentSector2Ms = entry.currentSector2Ms,
                    currentSector3Ms = entry.currentSector3Ms,
                    bestSector1Ms = entry.bestSector1Ms,
                    bestSector2Ms = entry.bestSector2Ms,
                    bestSector3Ms = entry.bestSector3Ms,
                    penaltySeconds = entry.penaltySeconds,
                    pointsAwarded = pts.toLong(),
                    hasFastestLap = entry.hasFastestLap,
                    createdAtEpochMs = entry.createdAtEpochMs,
                    updatedAtEpochMs = now
                )
            }
            dao.upsertEntries(updated)
            dao.getSessionById(session.id)?.let { existingSess ->
                dao.upsertSession(
                    existingSess.copy(
                        status = SessionStatus.FINISHED.name,
                        flagStatus = FlagStatus.CHECKERED.name,
                        updatedAtEpochMs = now
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun initializeTemplateStructure(): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val champId = "champ_apex_2026"
            val circId = "circ_apex_gp"
            val evtId = "evt_apex_r1"
            val sessId = "sess_apex_r1_race"

            dao.upsertChampionship(
                ChampionshipEntity(
                    id = champId,
                    name = "Campeonato Mundial Apex GP",
                    seasonYear = 2026L,
                    category = "Fórmula Apex",
                    status = "ACTIVE",
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            dao.upsertCircuit(
                CircuitEntity(
                    id = circId,
                    name = "Circuito Internacional Apex",
                    country = "España",
                    countryCode = "ES",
                    lengthKm = 4.657,
                    turnsCount = 14L,
                    sectorCount = 3L,
                    startFinishLabel = "Recta Principal · Meta",
                    sectorSplits = listOf(0.32, 0.68, 1.0),
                    measurementPoints = listOf("Meta", "Fin S1 (Curva 4)", "Fin S2 (Curva 10)"),
                    trackPoints = listOf(
                        "0.18,0.72", "0.52,0.72", "0.78,0.68", "0.86,0.52",
                        "0.80,0.32", "0.62,0.22", "0.50,0.36", "0.38,0.20",
                        "0.18,0.28", "0.14,0.48", "0.26,0.56", "0.18,0.72"
                    ),
                    customImageUri = "",
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            dao.upsertEvent(
                RaceEventEntity(
                    id = evtId,
                    championshipId = champId,
                    circuitId = circId,
                    circuitName = "Circuito Internacional Apex",
                    grandPrixName = "Gran Premio Inaugural Apex",
                    country = "España",
                    countryCode = "ES",
                    eventDateMillis = now,
                    practiceSchedule = "Vie 11:00 - 12:00",
                    qualifyingSchedule = "Sáb 16:00 - 17:00",
                    raceSchedule = "Dom 14:00",
                    raceDurationLabel = "45 vueltas / 90 min",
                    status = EventStatus.LIVE.name,
                    roundNumber = 1L,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            dao.upsertSession(
                TimingSessionEntity(
                    id = sessId,
                    eventId = evtId,
                    championshipId = champId,
                    circuitId = circId,
                    circuitName = "Circuito Internacional Apex",
                    sessionName = "Clasificación Q1",
                    sessionType = SessionType.QUALIFYING.name,
                    status = SessionStatus.SCHEDULED.name,
                    flagStatus = FlagStatus.GREEN.name,
                    timingMode = TimingMode.MANUAL.name,
                    totalLapsTarget = 25L,
                    durationMinutes = 60L,
                    startedAtEpochMs = 0L,
                    elapsedPausedMs = 0L,
                    bestSessionLapMs = 0L,
                    bestSessionLapDriverId = "",
                    bestSector1Ms = 0L,
                    bestSector2Ms = 0L,
                    bestSector3Ms = 0L,
                    sectorCount = 3L,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

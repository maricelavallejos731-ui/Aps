package com.example.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.hardware.BleTransponderDevice
import com.example.data.hardware.GpsTelemetryState
import com.example.data.hardware.TimingHardwareManager
import com.example.data.hardware.WifiFeedState
import com.example.data.local.ApexLocalDatabase
import com.example.data.local.LocalPreferencesDataStore
import com.example.data.local.PendingLapEntity
import com.example.data.model.Championship
import com.example.data.model.Circuit
import com.example.data.model.ConstructorStanding
import com.example.data.model.DriverStanding
import com.example.data.model.DriverStatus
import com.example.data.model.EventStatus
import com.example.data.model.FlagStatus
import com.example.data.model.LapRecord
import com.example.data.model.LiveTimingRow
import com.example.data.model.PenaltyRecord
import com.example.data.model.PenaltyType
import com.example.data.model.RaceEvent
import com.example.data.model.SessionEntry
import com.example.data.model.SessionStatus
import com.example.data.model.SessionType
import com.example.data.model.TimingMode
import com.example.data.model.TimingSession
import com.example.data.model.TimingSource
import com.example.data.model.UserRole
import com.example.data.repository.ApexTimingRepository
import com.example.domain.TimingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManualChronoState(
    val selectedEntryId: String = "",
    val isRunning: Boolean = false,
    val lapStartMonoNs: Long = 0L,
    val lastSectorMonoNs: Long = 0L,
    val currentElapsedMs: Long = 0L,
    val capturedSectorsMs: List<Long> = emptyList(),
    val markNextInvalid: Boolean = false,
    val invalidReason: String = ""
)

class ApexTimingViewModel(
    private val repository: ApexTimingRepository,
    private val localDb: ApexLocalDatabase,
    private val prefs: LocalPreferencesDataStore,
    private val hardwareManager: TimingHardwareManager
) : ViewModel() {

    private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

    private val _isMasterUnlocked = MutableStateFlow(false)
    val isMasterUnlocked: StateFlow<Boolean> = _isMasterUnlocked.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _unlockedRole = MutableStateFlow(UserRole.SUPER_ADMIN)

    val currentUserRole: StateFlow<UserRole> = combine(_isMasterUnlocked, _unlockedRole) { unlocked, role ->
        if (unlocked) role else UserRole.SPECTATOR
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserRole.SPECTATOR)

    val hasCustomMasterPassword: StateFlow<Boolean> = prefs.hasCustomMasterPassword
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val _selectedSessionId = MutableStateFlow("")
    val selectedSessionId: StateFlow<String> = _selectedSessionId.asStateFlow()

    private val _selectedCircuitId = MutableStateFlow("")
    val selectedCircuitId: StateFlow<String> = _selectedCircuitId.asStateFlow()

    private val _manualChrono = MutableStateFlow(ManualChronoState())
    val manualChrono: StateFlow<ManualChronoState> = _manualChrono.asStateFlow()

    private val _sessionClockMs = MutableStateFlow(0L)
    val sessionClockMs: StateFlow<Long> = _sessionClockMs.asStateFlow()

    private val _isSyncingOffline = MutableStateFlow(false)
    val isSyncingOffline: StateFlow<Boolean> = _isSyncingOffline.asStateFlow()

    val championships: StateFlow<List<Championship>> = repository.observeChampionships()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val circuits: StateFlow<List<Circuit>> = repository.observeCircuits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val events: StateFlow<List<RaceEvent>> = repository.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val sessions: StateFlow<List<TimingSession>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allEntries: StateFlow<List<SessionEntry>> = repository.observeAllSessionEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pendingOfflineLaps: StateFlow<List<PendingLapEntity>> = localDb.pendingLapDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val isLocalOfflineMode: StateFlow<Boolean> = prefs.isLocalOfflineMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val wifiEndpointUrl: StateFlow<String> = prefs.wifiEndpointUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    val activeSession: StateFlow<TimingSession?> = combine(sessions, _selectedSessionId) { list, selId ->
        list.firstOrNull { it.id == selId }
            ?: list.firstOrNull { it.status == SessionStatus.ACTIVE.name }
            ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeCircuit: StateFlow<Circuit?> = combine(circuits, activeSession, _selectedCircuitId) { circs, sess, selCirc ->
        circs.firstOrNull { it.id == selCirc }
            ?: circs.firstOrNull { it.id == sess?.circuitId }
            ?: circs.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeSessionEntries: StateFlow<List<SessionEntry>> = combine(allEntries, activeSession) { entries, sess ->
        if (sess == null) emptyList() else entries.filter { it.sessionId == sess.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val liveTimingRows: StateFlow<List<LiveTimingRow>> = combine(activeSession, activeSessionEntries) { sess, entries ->
        TimingEngine.buildLiveTimingRows(sess, entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSessionLaps: StateFlow<List<LapRecord>> = activeSession
        .flatMapLatest { sess ->
            if (sess == null) flowOf(emptyList())
            else repository.observeLapsForSession(sess.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSessionPenalties: StateFlow<List<PenaltyRecord>> = activeSession
        .flatMapLatest { sess ->
            if (sess == null) flowOf(emptyList())
            else repository.observePenaltiesForSession(sess.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val standings: StateFlow<Pair<List<DriverStanding>, List<ConstructorStanding>>> =
        combine(sessions, allEntries) { sessList, entries ->
            val finishedRaces = sessList.filter {
                it.status == SessionStatus.FINISHED.name && it.sessionType == SessionType.RACE.name
            }
            TimingEngine.computeChampionshipStandings(finishedRaces, entries)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList<DriverStanding>() to emptyList())

    val gpsState: StateFlow<GpsTelemetryState> = hardwareManager.gpsState
    val bleDevices: StateFlow<List<BleTransponderDevice>> = hardwareManager.bleDevices
    val isScanningBle: StateFlow<Boolean> = hardwareManager.isScanningBle
    val connectedBleAddress: StateFlow<String?> = hardwareManager.connectedBleAddress
    val wifiFeedState: StateFlow<WifiFeedState> = hardwareManager.wifiFeedState

    init {
        viewModelScope.launch {
            repository.ensureInitialStructureIfEmpty()
        }

        hardwareManager.setAutomaticLapListener { trigger ->
            val sess = activeSession.value ?: return@setAutomaticLapListener
            val entries = activeSessionEntries.value
            val targetId = _manualChrono.value.selectedEntryId.ifBlank { entries.firstOrNull()?.id.orEmpty() }
            val entry = entries.firstOrNull { it.id == targetId } ?: return@setAutomaticLapListener
            if (!currentUserRole.value.canRecordTiming) return@setAutomaticLapListener

            viewModelScope.launch {
                val s1 = trigger.lapTimeMs / 3
                val s2 = trigger.lapTimeMs / 3
                val s3 = trigger.lapTimeMs - s1 - s2
                val res = repository.recordLap(
                    session = sess,
                    entry = entry,
                    allSessionEntries = entries,
                    lapTimeMs = trigger.lapTimeMs,
                    sectorTimesMs = listOf(s1, s2, s3),
                    timingSource = trigger.source,
                    gpsAccuracyMeters = trigger.accuracyMeters,
                    localOfflineOnly = isLocalOfflineMode.value
                )
                if (res.isSuccess) {
                    _uiMessages.tryEmit("Paso automático (${trigger.source}) registrado: ${TimingEngine.formatLapTime(trigger.lapTimeMs)}")
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                val sess = activeSession.value
                _sessionClockMs.value = when {
                    sess == null -> 0L
                    sess.status == SessionStatus.ACTIVE.name && sess.startedAtEpochMs > 0L -> {
                        sess.elapsedPausedMs + (System.currentTimeMillis() - sess.startedAtEpochMs).coerceAtLeast(0L)
                    }
                    else -> sess.elapsedPausedMs
                }

                val chrono = _manualChrono.value
                if (chrono.isRunning && chrono.lapStartMonoNs > 0L) {
                    val elapsed = (SystemClock.elapsedRealtimeNanos() - chrono.lapStartMonoNs) / 1_000_000L
                    _manualChrono.value = chrono.copy(currentElapsedMs = elapsed.coerceAtLeast(0L))
                }
                delay(100L)
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun unlockWithPassword(password: String) {
        viewModelScope.launch {
            val ok = prefs.verifyMasterPassword(password)
            if (ok) {
                _authError.value = null
                _unlockedRole.value = UserRole.SUPER_ADMIN
                _isMasterUnlocked.value = true
                _uiMessages.tryEmit("Apartado privado desbloqueado")
            } else {
                _authError.value = "Contraseña incorrecta. Acceso denegado."
            }
        }
    }

    fun lockMasterAccess() {
        _isMasterUnlocked.value = false
        _authError.value = null
        _uiMessages.tryEmit("Acceso privado bloqueado (Modo Espectador)")
    }

    fun switchOperatorRole(role: UserRole) {
        if (!_isMasterUnlocked.value) return
        _unlockedRole.value = role
        _uiMessages.tryEmit("Modo operativo cambiado a ${role.labelEs}")
    }

    fun changeMasterPassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (!_isMasterUnlocked.value) return
        if (newPassword.trim() != confirmPassword.trim()) {
            _uiMessages.tryEmit("Las contraseñas nuevas no coinciden")
            return
        }
        viewModelScope.launch {
            val res = prefs.updateMasterPassword(currentPassword, newPassword)
            if (res.isSuccess) {
                _uiMessages.tryEmit("Contraseña privada actualizada correctamente")
            } else {
                _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "No se pudo cambiar la contraseña")
            }
        }
    }

    fun selectSession(sessionId: String) {
        _selectedSessionId.value = sessionId
    }

    fun selectCircuit(circuitId: String) {
        _selectedCircuitId.value = circuitId
    }

    fun selectDriverForChrono(entryId: String) {
        _manualChrono.value = _manualChrono.value.copy(selectedEntryId = entryId)
    }

    fun startManualChrono() {
        if (!currentUserRole.value.canRecordTiming) return
        val entries = activeSessionEntries.value
        val targetId = _manualChrono.value.selectedEntryId.ifBlank { entries.firstOrNull()?.id.orEmpty() }
        if (targetId.isBlank()) {
            _uiMessages.tryEmit("Agrega o selecciona un piloto primero")
            return
        }
        val nowNs = SystemClock.elapsedRealtimeNanos()
        _manualChrono.value = ManualChronoState(
            selectedEntryId = targetId,
            isRunning = true,
            lapStartMonoNs = nowNs,
            lastSectorMonoNs = nowNs,
            currentElapsedMs = 0L,
            capturedSectorsMs = emptyList()
        )
    }

    fun captureSectorSplit() {
        val chrono = _manualChrono.value
        if (!chrono.isRunning) return
        val maxSectors = (activeSession.value?.sectorCount ?: 3L).toInt().coerceIn(2, 6)
        if (chrono.capturedSectorsMs.size >= maxSectors - 1) {
            _uiMessages.tryEmit("Último sector en curso: pulsa 'Meta / Vuelta' para cerrar vuelta")
            return
        }
        val nowNs = SystemClock.elapsedRealtimeNanos()
        val splitMs = ((nowNs - chrono.lastSectorMonoNs) / 1_000_000L).coerceAtLeast(1L)
        _manualChrono.value = chrono.copy(
            lastSectorMonoNs = nowNs,
            capturedSectorsMs = chrono.capturedSectorsMs + splitMs
        )
    }

    fun toggleMarkNextInvalid(invalid: Boolean, reason: String = "Límites de pista") {
        _manualChrono.value = _manualChrono.value.copy(
            markNextInvalid = invalid,
            invalidReason = if (invalid) reason else ""
        )
    }

    fun finishManualLapAndContinue(continueRunning: Boolean = true) {
        if (!currentUserRole.value.canRecordTiming) return
        val sess = activeSession.value ?: return
        val entries = activeSessionEntries.value
        val chrono = _manualChrono.value
        val entry = entries.firstOrNull { it.id == chrono.selectedEntryId } ?: return
        if (!chrono.isRunning) return

        val nowNs = SystemClock.elapsedRealtimeNanos()
        val totalLapMs = ((nowNs - chrono.lapStartMonoNs) / 1_000_000L).coerceAtLeast(1L)
        val lastSectorMs = ((nowNs - chrono.lastSectorMonoNs) / 1_000_000L).coerceAtLeast(1L)
        val sectors = (chrono.capturedSectorsMs + lastSectorMs).ifEmpty {
            listOf(totalLapMs / 3, totalLapMs / 3, totalLapMs - (2 * (totalLapMs / 3)))
        }

        viewModelScope.launch {
            val res = repository.recordLap(
                session = sess,
                entry = entry,
                allSessionEntries = entries,
                lapTimeMs = totalLapMs,
                sectorTimesMs = sectors,
                timingSource = TimingSource.MANUAL.name,
                gpsAccuracyMeters = 0.0,
                forceInvalidReason = if (chrono.markNextInvalid) chrono.invalidReason else null,
                localOfflineOnly = isLocalOfflineMode.value
            )
            if (res.isSuccess) {
                _uiMessages.tryEmit(
                    if (isLocalOfflineMode.value) "Vuelta guardada en cola local (${TimingEngine.formatLapTime(totalLapMs)})"
                    else "Vuelta registrada: #${entry.carNumber} ${TimingEngine.formatLapTime(totalLapMs)}"
                )
            } else {
                _uiMessages.tryEmit("Error registrando vuelta: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }

        _manualChrono.value = if (continueRunning) {
            ManualChronoState(
                selectedEntryId = chrono.selectedEntryId,
                isRunning = true,
                lapStartMonoNs = nowNs,
                lastSectorMonoNs = nowNs,
                currentElapsedMs = 0L,
                capturedSectorsMs = emptyList()
            )
        } else {
            ManualChronoState(selectedEntryId = chrono.selectedEntryId)
        }
    }

    fun recordManualExactLap(entryId: String, lapTimeMs: Long, s1Ms: Long, s2Ms: Long, s3Ms: Long, invalidReason: String?) {
        if (!currentUserRole.value.canRecordTiming) return
        val sess = activeSession.value ?: return
        val entries = activeSessionEntries.value
        val entry = entries.firstOrNull { it.id == entryId } ?: return
        viewModelScope.launch {
            val sectors = listOf(s1Ms, s2Ms, s3Ms).filter { it > 0L }
            val res = repository.recordLap(
                session = sess,
                entry = entry,
                allSessionEntries = entries,
                lapTimeMs = lapTimeMs,
                sectorTimesMs = sectors,
                timingSource = TimingSource.MANUAL.name,
                gpsAccuracyMeters = 0.0,
                forceInvalidReason = invalidReason,
                localOfflineOnly = isLocalOfflineMode.value
            )
            if (res.isSuccess) {
                _uiMessages.tryEmit("Tiempo #${entry.carNumber} guardado: ${TimingEngine.formatLapTime(lapTimeMs)}")
            } else {
                _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "No autorizado")
            }
        }
    }

    fun toggleLapValidity(lap: LapRecord) {
        if (!currentUserRole.value.canRecordTiming) return
        viewModelScope.launch {
            val res = repository.toggleLapValidity(lap, !lap.isValid, "Invalidada manualmente")
            if (res.isFailure) _uiMessages.tryEmit("Error al corregir vuelta")
        }
    }

    fun controlSessionStatus(newStatus: SessionStatus) {
        if (!currentUserRole.value.canControlSessions) return
        val sess = activeSession.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val res = when (newStatus) {
                SessionStatus.ACTIVE -> repository.updateSessionControl(
                    session = sess,
                    status = SessionStatus.ACTIVE,
                    startedAtEpochMs = now
                )
                SessionStatus.PAUSED -> {
                    val runningDelta = if (sess.status == SessionStatus.ACTIVE.name && sess.startedAtEpochMs > 0L) {
                        (now - sess.startedAtEpochMs).coerceAtLeast(0L)
                    } else 0L
                    repository.updateSessionControl(
                        session = sess,
                        status = SessionStatus.PAUSED,
                        startedAtEpochMs = 0L,
                        elapsedPausedMs = sess.elapsedPausedMs + runningDelta
                    )
                }
                SessionStatus.FINISHED -> {
                    repository.finalizeRaceAndAwardPoints(sess, activeSessionEntries.value)
                }
                SessionStatus.SCHEDULED -> repository.updateSessionControl(sess, status = SessionStatus.SCHEDULED)
            }
            if (res.isFailure) _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Error de sesión")
        }
    }

    fun setFlagStatus(flag: FlagStatus) {
        if (!currentUserRole.value.canControlSessions) return
        val sess = activeSession.value ?: return
        viewModelScope.launch {
            val res = repository.updateSessionControl(sess, flagStatus = flag)
            if (res.isFailure) _uiMessages.tryEmit("No se pudo cambiar bandera")
        }
    }

    fun setTimingMode(mode: TimingMode) {
        if (!currentUserRole.value.canControlSessions) return
        val sess = activeSession.value ?: return
        viewModelScope.launch {
            val res = repository.updateSessionControl(sess, timingMode = mode)
            if (res.isFailure) _uiMessages.tryEmit("Error cambiando modalidad")
        }
    }

    fun updateDriverStatus(entryId: String, status: DriverStatus) {
        if (!currentUserRole.value.canControlSessions) return
        viewModelScope.launch {
            val res = repository.updateDriverStatus(entryId, status)
            if (res.isFailure) _uiMessages.tryEmit("No se pudo cambiar estado")
        }
    }

    fun addDriverToSession(
        driverName: String,
        driverCode: String,
        nationality: String,
        countryCode: String,
        teamName: String,
        teamColorHex: String,
        carModel: String,
        carNumber: Long
    ) {
        if (!currentUserRole.value.canControlSessions) return
        val sess = activeSession.value ?: return
        viewModelScope.launch {
            val pos = (activeSessionEntries.value.size + 1).toLong()
            val res = repository.addSessionEntry(
                session = sess,
                driverName = driverName,
                driverCode = driverCode,
                nationality = nationality,
                countryCode = countryCode,
                teamName = teamName,
                teamColorHex = teamColorHex,
                carModel = carModel,
                carNumber = carNumber,
                initialPosition = pos
            )
            if (res.isSuccess) _uiMessages.tryEmit("Piloto #$carNumber inscrito en sesión")
            else _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Error inscribiendo piloto")
        }
    }

    fun issuePenalty(entry: SessionEntry, type: PenaltyType, reason: String) {
        if (!currentUserRole.value.canControlSessions) return
        val sess = activeSession.value ?: return
        viewModelScope.launch {
            val res = repository.issuePenalty(sess.id, entry, type, reason)
            if (res.isSuccess) _uiMessages.tryEmit("Penalización aplicada a ${entry.driverCode}")
            else _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Error aplicando penalización")
        }
    }

    fun createChampionship(name: String, year: Long, category: String) {
        if (!currentUserRole.value.canManageChampionshipsAndUsers) return
        viewModelScope.launch {
            val res = repository.createChampionship(name, year, category)
            if (res.isSuccess) _uiMessages.tryEmit("Campeonato creado")
            else _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Error creando campeonato")
        }
    }

    fun deleteChampionship(id: String) {
        if (!currentUserRole.value.canManageChampionshipsAndUsers) return
        viewModelScope.launch {
            val res = repository.deleteChampionship(id)
            if (res.isSuccess) _uiMessages.tryEmit("Campeonato eliminado")
            else _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Error eliminando campeonato")
        }
    }

    fun saveCircuit(
        name: String,
        country: String,
        countryCode: String,
        lengthKm: Double,
        turnsCount: Long,
        sectorCount: Long,
        startFinishLabel: String,
        measurementPoints: List<String>,
        trackPoints: List<String>,
        customImageUri: String?
    ) {
        if (!currentUserRole.value.canManageChampionshipsAndUsers) return
        viewModelScope.launch {
            val count = sectorCount.toInt().coerceIn(1, 6)
            val splits = (1..count).map { idx -> (idx.toDouble() / count.toDouble()) }
            val res = repository.saveCircuit(
                name = name,
                country = country,
                countryCode = countryCode,
                lengthKm = lengthKm,
                turnsCount = turnsCount,
                sectorCount = sectorCount,
                startFinishLabel = startFinishLabel,
                sectorSplits = splits,
                measurementPoints = measurementPoints,
                trackPoints = trackPoints,
                customImageUri = customImageUri
            )
            if (res.isSuccess) _uiMessages.tryEmit("Circuito guardado en biblioteca")
            else _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Error guardando circuito")
        }
    }

    fun createRaceEvent(
        championshipId: String,
        circuit: Circuit,
        grandPrixName: String,
        practiceSchedule: String,
        qualifyingSchedule: String,
        raceSchedule: String,
        raceDurationLabel: String,
        roundNumber: Long
    ) {
        if (!currentUserRole.value.canManageChampionshipsAndUsers) return
        viewModelScope.launch {
            val res = repository.createRaceEvent(
                championshipId = championshipId,
                circuit = circuit,
                grandPrixName = grandPrixName,
                eventDateMillis = System.currentTimeMillis(),
                practiceSchedule = practiceSchedule,
                qualifyingSchedule = qualifyingSchedule,
                raceSchedule = raceSchedule,
                raceDurationLabel = raceDurationLabel,
                roundNumber = roundNumber
            )
            if (res.isSuccess) _uiMessages.tryEmit("Gran Premio añadido al calendario")
            else _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Error añadiendo evento")
        }
    }

    fun updateEventStatus(eventId: String, status: EventStatus) {
        if (!currentUserRole.value.canManageChampionshipsAndUsers) return
        viewModelScope.launch {
            val res = repository.updateEventStatus(eventId, status)
            if (res.isFailure) _uiMessages.tryEmit("Error al modificar calendario")
        }
    }

    fun createSession(
        event: RaceEvent,
        sessionName: String,
        sessionType: SessionType,
        totalLapsTarget: Long,
        durationMinutes: Long
    ) {
        if (!currentUserRole.value.canControlSessions) return
        val circ = circuits.value.firstOrNull { it.id == event.circuitId }
        val sectors = circ?.sectorCount ?: 3L
        viewModelScope.launch {
            val res = repository.createSession(event, sessionName, sessionType, totalLapsTarget, durationMinutes, sectors)
            if (res.isSuccess) {
                _selectedSessionId.value = res.getOrNull().orEmpty()
                _uiMessages.tryEmit("Sesión '$sessionName' creada")
            } else {
                _uiMessages.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Error creando sesión")
            }
        }
    }

    fun initializeBaseStructure() {
        if (!currentUserRole.value.canManageChampionshipsAndUsers) return
        viewModelScope.launch {
            val res = repository.initializeTemplateStructure()
            if (res.isSuccess) _uiMessages.tryEmit("Estructura base de campeonato y circuito creada")
            else _uiMessages.tryEmit("Error al inicializar estructura base")
        }
    }

    fun setLocalOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setLocalOfflineMode(enabled)
            if (!enabled && pendingOfflineLaps.value.isNotEmpty()) {
                syncOfflineQueue()
            }
        }
    }

    fun syncOfflineQueue() {
        if (_isSyncingOffline.value) return
        viewModelScope.launch {
            _isSyncingOffline.value = true
            val res = repository.syncPendingOfflineLaps(sessions.value, allEntries.value)
            _isSyncingOffline.value = false
            if (res.isSuccess) {
                val count = res.getOrDefault(0)
                _uiMessages.tryEmit("Registros de cola aplicados ($count vueltas)")
            } else {
                _uiMessages.tryEmit("No se pudo procesar la cola local")
            }
        }
    }

    // Hardware Controls
    fun startGps() = hardwareManager.startGpsTracking()
    fun stopGps() = hardwareManager.stopGpsTracking()
    fun markGpsFinishLine() = hardwareManager.setStartFinishLineHere()
    fun startBleScan() = hardwareManager.startBleScan()
    fun stopBleScan() = hardwareManager.stopBleScan()
    fun selectBleDevice(address: String) = hardwareManager.selectBleDevice(address)
    fun disconnectBle() = hardwareManager.disconnectBleDevice()
    fun connectWifiWebSocket(url: String) {
        viewModelScope.launch { prefs.setWifiEndpointUrl(url) }
        hardwareManager.connectWifiWebSocket(url)
    }
    fun pollHttpApi(url: String) {
        viewModelScope.launch { prefs.setWifiEndpointUrl(url) }
        hardwareManager.pollHttpTimingEndpoint(url)
    }
    fun disconnectWifi() = hardwareManager.disconnectWifiWebSocket()

    override fun onCleared() {
        super.onCleared()
        hardwareManager.release()
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as Application
                val localDb = ApexLocalDatabase.getInstance(app)
                val prefs = LocalPreferencesDataStore(app)
                val repo = ApexTimingRepository(localDb.timingDao(), localDb.pendingLapDao())
                ApexTimingViewModel(
                    repository = repo,
                    localDb = localDb,
                    prefs = prefs,
                    hardwareManager = TimingHardwareManager(app, kotlinx.coroutines.CoroutineScope(Dispatchers.Main))
                )
            }
        }
    }
}

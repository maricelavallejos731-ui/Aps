package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DriverStatus
import com.example.data.model.FlagStatus
import com.example.data.model.LapRecord
import com.example.data.model.LiveTimingRow
import com.example.data.model.PenaltyType
import com.example.data.model.SectorColorState
import com.example.data.model.SessionEntry
import com.example.data.model.SessionStatus
import com.example.data.model.SessionType
import com.example.data.model.TimingMode
import com.example.data.model.TimingSession
import com.example.data.model.UserRole
import com.example.domain.TimingEngine
import com.example.ui.theme.ApexCrimson
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.GridBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutRed
import com.example.ui.theme.PitAmber
import com.example.ui.theme.RetiredSlate
import com.example.ui.theme.SectorGreen
import com.example.ui.theme.SectorPurple
import com.example.ui.theme.SectorYellow
import com.example.ui.theme.TelemetryCard
import com.example.ui.theme.TelemetryCardElevated
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TelemetryMonoStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ApexTimingViewModel

@Composable
fun LiveTimingScreen(
    viewModel: ApexTimingViewModel,
    isWideScreen: Boolean
) {
    val role by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val rows by viewModel.liveTimingRows.collectAsStateWithLifecycle()
    val laps by viewModel.activeSessionLaps.collectAsStateWithLifecycle()
    val sessionClockMs by viewModel.sessionClockMs.collectAsStateWithLifecycle()
    val manualChrono by viewModel.manualChrono.collectAsStateWithLifecycle()
    val pendingOffline by viewModel.pendingOfflineLaps.collectAsStateWithLifecycle()
    val isLocalOffline by viewModel.isLocalOfflineMode.collectAsStateWithLifecycle()
    val gpsState by viewModel.gpsState.collectAsStateWithLifecycle()
    val bleDevices by viewModel.bleDevices.collectAsStateWithLifecycle()
    val isScanningBle by viewModel.isScanningBle.collectAsStateWithLifecycle()
    val connectedBle by viewModel.connectedBleAddress.collectAsStateWithLifecycle()
    val wifiFeed by viewModel.wifiFeedState.collectAsStateWithLifecycle()
    val savedWifiUrl by viewModel.wifiEndpointUrl.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()

    var showAddDriverDialog by remember { mutableStateOf(false) }
    var showCreateSessionDialog by remember { mutableStateOf(false) }
    var showExactLapDialog by remember { mutableStateOf(false) }
    var penaltyTargetEntry by remember { mutableStateOf<SessionEntry?>(null) }
    var statusTargetEntry by remember { mutableStateOf<SessionEntry?>(null) }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.startGps()
        }
    }

    val blePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            viewModel.startBleScan()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Telemetry Top Banner: Session Selector, Synchronized Clock, Flag & Sync State
        item {
            SessionTelemetryHeader(
                sessions = sessions,
                activeSession = activeSession,
                sessionClockMs = sessionClockMs,
                isLocalOffline = isLocalOffline,
                pendingOfflineCount = pendingOffline.size,
                role = role,
                onSelectSession = viewModel::selectSession,
                onSyncNow = viewModel::syncOfflineQueue,
                onCreateSessionClick = { showCreateSessionDialog = true }
            )
        }

        // Empty state if no session exists yet
        if (activeSession == null) {
            item {
                EmptyTimingBoardCard(
                    role = role,
                    hasEvents = events.isNotEmpty(),
                    onCreateSession = { showCreateSessionDialog = true },
                    onInitBaseStructure = viewModel::initializeBaseStructure
                )
            }
        } else {
            val sess = activeSession!!
            val timingMode = TimingMode.fromString(sess.timingMode)

            // 2. Race Director & SuperAdmin Control Strip
            if (role.canControlSessions) {
                item {
                    RaceDirectorControlPanel(
                        session = sess,
                        onStatusChange = viewModel::controlSessionStatus,
                        onFlagChange = viewModel::setFlagStatus,
                        onModeChange = viewModel::setTimingMode,
                        onAddDriver = { showAddDriverDialog = true }
                    )
                }
            }

            // 3. Timekeeper / Director / SuperAdmin Timing Console (Manual OR Automatic GPS/BLE/Wi-Fi)
            if (role.canRecordTiming) {
                item {
                    ChronometerConsoleCard(
                        session = sess,
                        entries = rows.map { it.entry },
                        timingMode = timingMode,
                        chronoState = manualChrono,
                        gpsState = gpsState,
                        bleDevices = bleDevices,
                        isScanningBle = isScanningBle,
                        connectedBleAddress = connectedBle,
                        wifiFeed = wifiFeed,
                        savedWifiUrl = savedWifiUrl,
                        onSelectDriver = viewModel::selectDriverForChrono,
                        onStartChrono = viewModel::startManualChrono,
                        onCaptureSector = viewModel::captureSectorSplit,
                        onFinishLap = { viewModel.finishManualLapAndContinue(true) },
                        onStopChrono = { viewModel.finishManualLapAndContinue(false) },
                        onToggleInvalid = viewModel::toggleMarkNextInvalid,
                        onOpenExactLapDialog = { showExactLapDialog = true },
                        onRequestGps = {
                            locationPermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onStopGps = viewModel::stopGps,
                        onMarkGpsLine = viewModel::markGpsFinishLine,
                        onStartBle = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                blePermLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_SCAN,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    )
                                )
                            } else {
                                viewModel.startBleScan()
                            }
                        },
                        onStopBle = viewModel::stopBleScan,
                        onSelectBle = viewModel::selectBleDevice,
                        onConnectWifi = viewModel::connectWifiWebSocket,
                        onPollHttp = viewModel::pollHttpApi,
                        onDisconnectWifi = viewModel::disconnectWifi
                    )
                }
            }

            // 4. Session Fastest Lap & Sector Purple Banner
            item {
                SessionBestSummaryBar(session = sess, entries = rows.map { it.entry })
            }

            // 5. F1 Live Timing Compact Table
            item {
                LiveTimingBoardCard(
                    rows = rows,
                    isWideScreen = isWideScreen,
                    canModifyStatus = role.canControlSessions,
                    onDriverStatusClick = { statusTargetEntry = it },
                    onPenaltyClick = { penaltyTargetEntry = it }
                )
            }

            // 6. Lap Time Evolution Telemetry Graph + Lap Audit History
            if (laps.isNotEmpty()) {
                item {
                    LapEvolutionChartCard(laps = laps, entries = rows.map { it.entry })
                }
                item {
                    Text(
                        text = "HISTORIAL DE VUELTAS (${laps.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(laps.take(30), key = { it.id }) { lap ->
                    LapHistoryRowCard(
                        lap = lap,
                        canToggleValidity = role.canRecordTiming,
                        onToggleValid = { viewModel.toggleLapValidity(lap) }
                    )
                }
            }
        }
    }

    if (showAddDriverDialog) {
        AddDriverDialog(
            onDismiss = { showAddDriverDialog = false },
            onConfirm = { name, code, nat, cc, team, color, car, num ->
                viewModel.addDriverToSession(name, code, nat, cc, team, color, car, num)
                showAddDriverDialog = false
            }
        )
    }

    if (showCreateSessionDialog) {
        CreateSessionDialog(
            events = events,
            onDismiss = { showCreateSessionDialog = false },
            onConfirm = { evt, name, type, lapsTarget, durMin ->
                viewModel.createSession(evt, name, type, lapsTarget, durMin)
                showCreateSessionDialog = false
            }
        )
    }

    if (showExactLapDialog && activeSession != null) {
        ExactLapEntryDialog(
            entries = rows.map { it.entry },
            initialEntryId = manualChrono.selectedEntryId,
            onDismiss = { showExactLapDialog = false },
            onConfirm = { entryId, lapMs, s1, s2, s3, inv ->
                viewModel.recordManualExactLap(entryId, lapMs, s1, s2, s3, inv)
                showExactLapDialog = false
            }
        )
    }

    statusTargetEntry?.let { target ->
        DriverStatusDialog(
            entry = target,
            onDismiss = { statusTargetEntry = null },
            onSelectStatus = { st ->
                viewModel.updateDriverStatus(target.id, st)
                statusTargetEntry = null
            },
            onOpenPenalty = {
                statusTargetEntry = null
                penaltyTargetEntry = target
            }
        )
    }

    penaltyTargetEntry?.let { target ->
        PenaltyDialog(
            entry = target,
            onDismiss = { penaltyTargetEntry = null },
            onApply = { pType, reason ->
                viewModel.issuePenalty(target, pType, reason)
                penaltyTargetEntry = null
            }
        )
    }
}

@Composable
private fun SessionTelemetryHeader(
    sessions: List<TimingSession>,
    activeSession: TimingSession?,
    sessionClockMs: Long,
    isLocalOffline: Boolean,
    pendingOfflineCount: Int,
    role: UserRole,
    onSelectSession: (String) -> Unit,
    onSyncNow: () -> Unit,
    onCreateSessionClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val flag = FlagStatus.fromString(activeSession?.flagStatus ?: FlagStatus.GREEN.name)
    val flagColor = when (flag) {
        FlagStatus.GREEN -> SectorGreen
        FlagStatus.YELLOW -> SectorYellow
        FlagStatus.RED -> OutRed
        FlagStatus.CHECKERED -> TextPrimary
    }

    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(34.dp)
                            .background(flagColor, RoundedCornerShape(2.dp))
                    )
                    Box {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = sessions.isNotEmpty()) { menuExpanded = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = activeSession?.sessionName ?: "SIN SESIÓN ACTIVA",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeSession?.let {
                                    "${it.circuitName} · ${SessionType.fromString(it.sessionType).labelEs}"
                                } ?: "Selecciona o crea una sesión",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            sessions.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.sessionName} (${SessionStatus.fromString(s.status).labelEs})") },
                                    onClick = {
                                        onSelectSession(s.id)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Synchronized Session Clock
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = TimingEngine.formatSessionClock(sessionClockMs),
                        style = TelemetryMonoStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = if (activeSession?.status == SessionStatus.ACTIVE.name) SectorGreen else TextPrimary
                    )
                    Text(
                        text = flag.labelEs.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = flagColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isLocalOffline || pendingOfflineCount > 0) Icons.Default.CloudOff else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (isLocalOffline || pendingOfflineCount > 0) PitAmber else SectorGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when {
                            pendingOfflineCount > 0 -> "MODO LOCAL · $pendingOfflineCount EN COLA"
                            isLocalOffline -> "MODO LOCAL SIN RED"
                            else -> "CRONOMETRAJE EN VIVO"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLocalOffline || pendingOfflineCount > 0) PitAmber else SectorGreen
                    )
                    if (pendingOfflineCount > 0) {
                        TextButton(onClick = onSyncNow, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text("SINCRONIZAR", style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
                        }
                    }
                }

                if (role.canControlSessions) {
                    OutlinedButton(
                        onClick = onCreateSessionClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).testTag("new_session_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nueva Sesión", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTimingBoardCard(
    role: UserRole,
    hasEvents: Boolean,
    onCreateSession: () -> Unit,
    onInitBaseStructure: () -> Unit
) {
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = ApexCrimson, modifier = Modifier.size(44.dp))
            Text(
                text = "PANTALLA DE CRONOMETRAJE EN ESPERA",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "No hay sesiones activas en este momento. Los tiempos mostrados siempre provienen de registros reales.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (role.canControlSessions && hasEvents) {
                Button(
                    onClick = onCreateSession,
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson)
                ) {
                    Text("Crear Sesión Oficial")
                }
            } else if (role == UserRole.SUPER_ADMIN && !hasEvents) {
                Button(
                    onClick = onInitBaseStructure,
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson),
                    modifier = Modifier.testTag("init_base_structure_button")
                ) {
                    Text("Inicializar Campeonato y Circuito Base")
                }
            }
        }
    }
}

@Composable
private fun RaceDirectorControlPanel(
    session: TimingSession,
    onStatusChange: (SessionStatus) -> Unit,
    onFlagChange: (FlagStatus) -> Unit,
    onModeChange: (TimingMode) -> Unit,
    onAddDriver: () -> Unit
) {
    Surface(
        color = TelemetryCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DIRECCIÓN DE CARRERA", style = MaterialTheme.typography.labelLarge, color = SectorYellow)
                OutlinedButton(
                    onClick = onAddDriver,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp).testTag("add_driver_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Piloto", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Start / Pause / Finish
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isActive = session.status == SessionStatus.ACTIVE.name
                Button(
                    onClick = { onStatusChange(SessionStatus.ACTIVE) },
                    enabled = !isActive && session.status != SessionStatus.FINISHED.name,
                    colors = ButtonDefaults.buttonColors(containerColor = SectorGreen, contentColor = ObsidianBg),
                    modifier = Modifier.weight(1f).height(44.dp).testTag("start_session_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Iniciar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onStatusChange(SessionStatus.PAUSED) },
                    enabled = isActive,
                    modifier = Modifier.weight(1f).height(44.dp).testTag("pause_session_button")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pausar", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = { onStatusChange(SessionStatus.FINISHED) },
                    enabled = session.status != SessionStatus.FINISHED.name,
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson, contentColor = TextPrimary),
                    modifier = Modifier.weight(1f).height(44.dp).testTag("finish_session_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Finalizar", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Flag Chips & Timing Mode Chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FlagStatus.entries.forEach { flag ->
                    val selected = session.flagStatus == flag.name
                    FilterChip(
                        selected = selected,
                        onClick = { onFlagChange(flag) },
                        label = { Text(flag.labelEs, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (flag) {
                                FlagStatus.GREEN -> SectorGreen.copy(alpha = 0.25f)
                                FlagStatus.YELLOW -> SectorYellow.copy(alpha = 0.25f)
                                FlagStatus.RED -> OutRed.copy(alpha = 0.25f)
                                FlagStatus.CHECKERED -> TextPrimary.copy(alpha = 0.2f)
                            }
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimingMode.entries.forEach { mode ->
                    FilterChip(
                        selected = session.timingMode == mode.name,
                        onClick = { onModeChange(mode) },
                        label = { Text("Modo: ${mode.labelEs}", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChronometerConsoleCard(
    session: TimingSession,
    entries: List<SessionEntry>,
    timingMode: TimingMode,
    chronoState: com.example.ui.viewmodel.ManualChronoState,
    gpsState: com.example.data.hardware.GpsTelemetryState,
    bleDevices: List<com.example.data.hardware.BleTransponderDevice>,
    isScanningBle: Boolean,
    connectedBleAddress: String?,
    wifiFeed: com.example.data.hardware.WifiFeedState,
    savedWifiUrl: String,
    onSelectDriver: (String) -> Unit,
    onStartChrono: () -> Unit,
    onCaptureSector: () -> Unit,
    onFinishLap: () -> Unit,
    onStopChrono: () -> Unit,
    onToggleInvalid: (Boolean, String) -> Unit,
    onOpenExactLapDialog: () -> Unit,
    onRequestGps: () -> Unit,
    onStopGps: () -> Unit,
    onMarkGpsLine: () -> Unit,
    onStartBle: () -> Unit,
    onStopBle: () -> Unit,
    onSelectBle: (String) -> Unit,
    onConnectWifi: (String) -> Unit,
    onPollHttp: (String) -> Unit,
    onDisconnectWifi: () -> Unit
) {
    var wifiInput by remember(savedWifiUrl) { mutableStateOf(savedWifiUrl) }

    Surface(
        color = TelemetryCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TelemetryCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONSOLA CRONOMETRADOR (${timingMode.labelEs.uppercase()})",
                    style = MaterialTheme.typography.labelLarge,
                    color = TelemetryCyan
                )
                TextButton(onClick = onOpenExactLapDialog, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Ingreso Directo", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                }
            }

            if (entries.isEmpty()) {
                Text(
                    text = "Añade al menos un piloto en Dirección de Carrera para comenzar a registrar vueltas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                return@Column
            }

            // Driver selection strip
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val selectedId = chronoState.selectedEntryId.ifBlank { entries.first().id }
                entries.forEach { e ->
                    FilterChip(
                        selected = e.id == selectedId,
                        onClick = { onSelectDriver(e.id) },
                        label = {
                            Text(
                                "#${e.carNumber} ${e.driverCode}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            when (timingMode) {
                TimingMode.MANUAL -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = TimingEngine.formatLapTime(chronoState.currentElapsedMs),
                                style = TelemetryMonoStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                                color = if (chronoState.isRunning) SectorGreen else TextPrimary
                            )
                            val sectorsText = if (chronoState.capturedSectorsMs.isEmpty()) {
                                "Sectores: -- | -- | --"
                            } else {
                                chronoState.capturedSectorsMs.mapIndexed { idx, ms ->
                                    "S${idx + 1}: ${TimingEngine.formatSectorTime(ms)}"
                                }.joinToString("  ·  ")
                            }
                            Text(sectorsText, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Inválida", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = chronoState.markNextInvalid,
                                onCheckedChange = { onToggleInvalid(it, "Límites de pista") }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!chronoState.isRunning) {
                            Button(
                                onClick = onStartChrono,
                                colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("manual_start_chrono_btn")
                            ) {
                                Text("INICIAR CRONO PILOTO", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onCaptureSector,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("manual_sector_btn")
                            ) {
                                Text("SECTOR ${chronoState.capturedSectorsMs.size + 1}", style = MaterialTheme.typography.labelMedium)
                            }
                            Button(
                                onClick = onFinishLap,
                                colors = ButtonDefaults.buttonColors(containerColor = SectorGreen, contentColor = ObsidianBg),
                                modifier = Modifier.weight(1.3f).height(48.dp).testTag("manual_finish_lap_btn")
                            ) {
                                Text("META / VUELTA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = onStopChrono, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Stop, contentDescription = "Detener y guardar", tint = OutRed)
                            }
                        }
                    }
                }

                TimingMode.GPS -> {
                    Surface(
                        color = PitAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PitAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = PitAmber, modifier = Modifier.size(18.dp))
                            Text(
                                text = "GPS del dispositivo: Margen de error ±${"%.1f".format(gpsState.accuracyMeters)} m. Alternativa auxiliar; no sustituye transponder profesional.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }

                    Text(
                        text = "${gpsState.statusMessage} · Vel: ${"%.1f".format(gpsState.speedKmh)} km/h" +
                            (gpsState.distanceToLineMeters?.let { " · Meta a ${"%.1f".format(it)} m" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = if (gpsState.isTracking) onStopGps else onRequestGps,
                            colors = ButtonDefaults.buttonColors(containerColor = if (gpsState.isTracking) OutRed else TelemetryCyan),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (gpsState.isTracking) "Detener GPS" else "Activar GPS")
                        }
                        OutlinedButton(
                            onClick = onMarkGpsLine,
                            enabled = gpsState.isTracking,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("Fijar Línea Meta")
                        }
                    }
                }

                TimingMode.EXTERNAL_BLE_API -> {
                    Text(
                        text = "Transponder Bluetooth BLE o Feed Wi-Fi/WebSocket/REST",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = if (isScanningBle) onStopBle else onStartBle,
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isScanningBle) "Detener BLE" else "Buscar Transponder BLE")
                        }
                        if (connectedBleAddress != null) {
                            Surface(color = SectorGreen.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "BLE: $connectedBleAddress",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SectorGreen,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    bleDevices.take(3).forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CarbonSurface)
                                .clickable { onSelectBle(dev.address) }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${dev.name} (${dev.address})", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text("${dev.rssi} dBm", style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
                        }
                    }

                    OutlinedTextField(
                        value = wifiInput,
                        onValueChange = { wifiInput = it },
                        label = { Text("Endpoint Wi-Fi WebSocket o HTTP JSON") },
                        placeholder = { Text("ws://192.168.1.50:8080/timing") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (wifiFeed.isConnected) onDisconnectWifi() else onConnectWifi(wifiInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = ObsidianBg),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (wifiFeed.isConnected) "Desconectar WS" else "Conectar WS")
                        }
                        OutlinedButton(
                            onClick = { onPollHttp(wifiInput) },
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Consultar API REST")
                        }
                    }
                    Text(
                        text = wifiFeed.error ?: wifiFeed.lastPacketSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (wifiFeed.error != null) OutRed else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionBestSummaryBar(session: TimingSession, entries: List<SessionEntry>) {
    val bestDriver = entries.firstOrNull { it.id == session.bestSessionLapDriverId }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BestMetricPill(
            label = "VUELTA RÁPIDA",
            value = TimingEngine.formatLapTime(session.bestSessionLapMs),
            sub = bestDriver?.driverCode ?: "--",
            accent = SectorPurple
        )
        BestMetricPill("MEJOR S1", TimingEngine.formatSectorTime(session.bestSector1Ms), "PÚRPURA", SectorPurple)
        BestMetricPill("MEJOR S2", TimingEngine.formatSectorTime(session.bestSector2Ms), "PÚRPURA", SectorPurple)
        BestMetricPill("MEJOR S3", TimingEngine.formatSectorTime(session.bestSector3Ms), "PÚRPURA", SectorPurple)
    }
}

@Composable
private fun BestMetricPill(label: String, value: String, sub: String, accent: Color) {
    Surface(
        color = TelemetryCard,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
            Text(value, style = TelemetryMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = TextPrimary)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun LiveTimingBoardCard(
    rows: List<LiveTimingRow>,
    isWideScreen: Boolean,
    canModifyStatus: Boolean,
    onDriverStatusClick: (SessionEntry) -> Unit,
    onPenaltyClick: (SessionEntry) -> Unit
) {
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridBorder, RoundedCornerShape(12.dp))
    ) {
        if (rows.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Sin pilotos inscritos en esta sesión.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        } else {
            val hScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isWideScreen) Modifier else Modifier.horizontalScroll(hScroll))
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .background(TelemetryCardElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderCell("POS", 40.dp)
                    HeaderCell("PILOTO / EQUIPO", 168.dp)
                    HeaderCell("EST", 54.dp)
                    HeaderCell("ÚLT. VUELTA", 96.dp)
                    HeaderCell("MEJOR", 96.dp)
                    HeaderCell("GAP", 78.dp)
                    HeaderCell("INT", 74.dp)
                    HeaderCell("VTS", 42.dp)
                    HeaderCell("S1", 66.dp)
                    HeaderCell("S2", 66.dp)
                    HeaderCell("S3", 66.dp)
                }

                rows.forEachIndexed { idx, row ->
                    val e = row.entry
                    val teamColor = TimingEngine.parseColorSafe(e.teamColorHex)
                    val status = DriverStatus.fromString(e.driverStatus)
                    val rowBg = if (idx % 2 == 0) CarbonSurface else TelemetryCard.copy(alpha = 0.55f)

                    Row(
                        modifier = Modifier
                            .background(rowBg)
                            .clickable(enabled = canModifyStatus) { onDriverStatusClick(e) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Position + Team Color Strip
                        Row(modifier = Modifier.width(40.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(26.dp)
                                    .background(teamColor, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "P${row.computedPosition}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (row.computedPosition == 1) SectorPurple else TextPrimary
                            )
                        }

                        // Driver + Flag + Car# + Team
                        Column(modifier = Modifier.width(168.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(TimingEngine.countryCodeToFlag(e.countryCode), fontSize = 13.sp)
                                Text(
                                    text = "#${e.carNumber} ${e.driverCode}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                if (e.hasFastestLap) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SectorPurple)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("VR", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                    }
                                }
                                if (e.penaltySeconds > 0L) {
                                    Text("+${e.penaltySeconds}s", style = MaterialTheme.typography.labelSmall, color = PitAmber)
                                }
                            }
                            Text(
                                text = "${e.driverName} · ${e.teamName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Status Chip
                        Box(modifier = Modifier.width(54.dp)) {
                            val stColor = when (status) {
                                DriverStatus.ON_TRACK -> SectorGreen
                                DriverStatus.IN_PITS -> PitAmber
                                DriverStatus.RETIRED -> RetiredSlate
                                DriverStatus.OUT_OF_SESSION -> OutRed
                            }
                            Text(
                                text = status.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = stColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(stColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        MonoCell(TimingEngine.formatLapTime(e.lastLapMs), 96.dp, TextPrimary)
                        MonoCell(
                            TimingEngine.formatLapTime(e.bestLapMs),
                            96.dp,
                            if (e.hasFastestLap) SectorPurple else SectorGreen
                        )
                        MonoCell(row.gapToLeader, 78.dp, if (row.computedPosition == 1) SectorYellow else TextPrimary)
                        MonoCell(row.intervalToAhead, 74.dp, TextSecondary)
                        MonoCell(e.lapsCompleted.toString(), 42.dp, TextPrimary)

                        SectorCell(e.currentSector1Ms, row.sector1Color, 66.dp)
                        SectorCell(e.currentSector2Ms, row.sector2Color, 66.dp)
                        SectorCell(e.currentSector3Ms, row.sector3Color, 66.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun MonoCell(text: String, width: androidx.compose.ui.unit.Dp, color: Color) {
    Text(
        text = text,
        style = TelemetryMonoStyle.copy(fontSize = 12.sp),
        color = color,
        maxLines = 1,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun SectorCell(ms: Long, colorState: SectorColorState, width: androidx.compose.ui.unit.Dp) {
    val bg = when (colorState) {
        SectorColorState.PURPLE -> SectorPurple.copy(alpha = 0.25f)
        SectorColorState.GREEN -> SectorGreen.copy(alpha = 0.20f)
        SectorColorState.YELLOW -> SectorYellow.copy(alpha = 0.18f)
        SectorColorState.NONE -> Color.Transparent
    }
    val fg = when (colorState) {
        SectorColorState.PURPLE -> SectorPurple
        SectorColorState.GREEN -> SectorGreen
        SectorColorState.YELLOW -> SectorYellow
        SectorColorState.NONE -> TextMuted
    }
    Box(
        modifier = Modifier
            .width(width)
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Text(
            text = TimingEngine.formatSectorTime(ms),
            style = TelemetryMonoStyle.copy(fontSize = 11.sp),
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
private fun LapEvolutionChartCard(laps: List<LapRecord>, entries: List<SessionEntry>) {
    val validLaps = laps.filter { it.isValid && it.lapTimeMs > 0L }.sortedBy { it.lapNumber }
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("EVOLUCIÓN DE TIEMPOS POR VUELTA", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            if (validLaps.size < 2) {
                Text(
                    text = "Se requieren al menos 2 vueltas válidas para trazar la curva de ritmo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            } else {
                val minMs = validLaps.minOf { it.lapTimeMs }
                val maxMs = validLaps.maxOf { it.lapTimeMs }.coerceAtLeast(minMs + 1000L)
                val maxLapNum = validLaps.maxOf { it.lapNumber }.coerceAtLeast(2L)
                val entryColors = entries.associate { it.id to TimingEngine.parseColorSafe(it.teamColorHex) }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(ObsidianBg, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    val grouped = validLaps.groupBy { it.entryId }
                    grouped.forEach { (entryId, driverLaps) ->
                        val color = entryColors[entryId] ?: TelemetryCyan
                        val ordered = driverLaps.sortedBy { it.lapNumber }
                        val path = Path()
                        ordered.forEachIndexed { i, lap ->
                            val x = ((lap.lapNumber - 1).toFloat() / (maxLapNum - 1).coerceAtLeast(1L)) * size.width
                            val normY = (lap.lapTimeMs - minMs).toFloat() / (maxMs - minMs).toFloat()
                            val y = size.height - (normY * size.height * 0.85f + size.height * 0.08f)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(color = if (lap.isSessionBest) SectorPurple else color, radius = 5f, center = Offset(x, y))
                        }
                        if (ordered.size >= 2) {
                            drawPath(path = path, color = color, style = Stroke(width = 3f, cap = StrokeCap.Round))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LapHistoryRowCard(
    lap: LapRecord,
    canToggleValidity: Boolean,
    onToggleValid: () -> Unit
) {
    Surface(
        color = TelemetryCard,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when {
                    !lap.isValid -> OutRed.copy(alpha = 0.5f)
                    lap.isSessionBest -> SectorPurple.copy(alpha = 0.7f)
                    else -> GridBorder
                },
                RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("#${lap.carNumber} ${lap.driverName}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("V${lap.lapNumber}", style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
                    if (!lap.isValid) {
                        Text("INVÁLIDA (${lap.invalidReason})", style = MaterialTheme.typography.labelSmall, color = OutRed)
                    }
                }
                val secText = lap.sectorTimesMs.mapIndexed { i, ms -> "S${i + 1} ${TimingEngine.formatSectorTime(ms)}" }.joinToString(" · ")
                Text(
                    text = "$secText · ${lap.timingSource}" + if (lap.gpsAccuracyMeters > 0.0) " (±${"%.1f".format(lap.gpsAccuracyMeters)}m)" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = TimingEngine.formatLapTime(lap.lapTimeMs),
                    style = TelemetryMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = when {
                        !lap.isValid -> OutRed
                        lap.isSessionBest -> SectorPurple
                        lap.isPersonalBest -> SectorGreen
                        else -> TextPrimary
                    }
                )
                if (canToggleValidity) {
                    IconButton(onClick = onToggleValid, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (lap.isValid) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = "Cambiar validez de vuelta",
                            tint = if (lap.isValid) SectorGreen else OutRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDriverDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("España") }
    var countryCode by remember { mutableStateOf("ES") }
    var team by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#FF1E38") }
    var carModel by remember { mutableStateOf("AX-26") }
    var carNumText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inscribir Piloto en Sesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre completo") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it.take(3).uppercase() }, label = { Text("Código (3)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = carNumText, onValueChange = { carNumText = it.filter(Char::isDigit) }, label = { Text("N° Auto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nationality, onValueChange = { nationality = it }, label = { Text("País") }, singleLine = true, modifier = Modifier.weight(1.4f))
                    OutlinedTextField(value = countryCode, onValueChange = { countryCode = it.take(2).uppercase() }, label = { Text("ISO (ES)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = team, onValueChange = { team = it }, label = { Text("Escudería / Equipo") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = carModel, onValueChange = { carModel = it }, label = { Text("Monoplaza") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = colorHex, onValueChange = { colorHex = it }, label = { Text("Color #HEX") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = carNumText.toLongOrNull() ?: 1L
                    onConfirm(
                        name.ifBlank { "Piloto #$num" },
                        code.ifBlank { "PIL" },
                        nationality,
                        countryCode.ifBlank { "ES" },
                        team.ifBlank { "Apex Motorsport" },
                        colorHex.ifBlank { "#FF1E38" },
                        carModel.ifBlank { "AX-26" },
                        num
                    )
                }
            ) { Text("Inscribir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CreateSessionDialog(
    events: List<com.example.data.model.RaceEvent>,
    onDismiss: () -> Unit,
    onConfirm: (com.example.data.model.RaceEvent, String, SessionType, Long, Long) -> Unit
) {
    var selectedEvent by remember(events) { mutableStateOf(events.firstOrNull()) }
    var name by remember { mutableStateOf("Carrera Oficial") }
    var type by remember { mutableStateOf(SessionType.RACE) }
    var lapsText by remember { mutableStateOf("45") }
    var minsText by remember { mutableStateOf("90") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Sesión de Cronometraje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (events.isEmpty()) {
                    Text("Primero crea un Gran Premio en el Calendario o inicializa la estructura base.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        events.forEach { evt ->
                            FilterChip(
                                selected = selectedEvent?.id == evt.id,
                                onClick = { selectedEvent = evt },
                                label = { Text("R${evt.roundNumber}: ${evt.grandPrixName}") }
                            )
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de sesión") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SessionType.entries.forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.labelEs, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = lapsText, onValueChange = { lapsText = it.filter(Char::isDigit) }, label = { Text("Vueltas") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = minsText, onValueChange = { minsText = it.filter(Char::isDigit) }, label = { Text("Minutos") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedEvent != null,
                onClick = {
                    selectedEvent?.let {
                        onConfirm(it, name.ifBlank { type.labelEs }, type, lapsText.toLongOrNull() ?: 25L, minsText.toLongOrNull() ?: 60L)
                    }
                }
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ExactLapEntryDialog(
    entries: List<SessionEntry>,
    initialEntryId: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, Long, Long, String?) -> Unit
) {
    var selectedId by remember { mutableStateOf(initialEntryId.ifBlank { entries.firstOrNull()?.id.orEmpty() }) }
    var s1Text by remember { mutableStateOf("26450") }
    var s2Text by remember { mutableStateOf("28120") }
    var s3Text by remember { mutableStateOf("27310") }
    var invalidReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ingreso Exacto (Milésimas)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entries.forEach { e ->
                        FilterChip(selected = e.id == selectedId, onClick = { selectedId = e.id }, label = { Text("#${e.carNumber} ${e.driverCode}") })
                    }
                }
                OutlinedTextField(value = s1Text, onValueChange = { s1Text = it.filter(Char::isDigit) }, label = { Text("Sector 1 (ms)") }, singleLine = true)
                OutlinedTextField(value = s2Text, onValueChange = { s2Text = it.filter(Char::isDigit) }, label = { Text("Sector 2 (ms)") }, singleLine = true)
                OutlinedTextField(value = s3Text, onValueChange = { s3Text = it.filter(Char::isDigit) }, label = { Text("Sector 3 (ms)") }, singleLine = true)
                val total = (s1Text.toLongOrNull() ?: 0L) + (s2Text.toLongOrNull() ?: 0L) + (s3Text.toLongOrNull() ?: 0L)
                Text("Total calculado: ${TimingEngine.formatLapTime(total)}", style = MaterialTheme.typography.titleSmall, color = TelemetryCyan)
                OutlinedTextField(value = invalidReason, onValueChange = { invalidReason = it }, label = { Text("Motivo invalidez (opcional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s1 = s1Text.toLongOrNull() ?: 0L
                    val s2 = s2Text.toLongOrNull() ?: 0L
                    val s3 = s3Text.toLongOrNull() ?: 0L
                    onConfirm(selectedId, s1 + s2 + s3, s1, s2, s3, invalidReason.takeIf { it.isNotBlank() })
                }
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DriverStatusDialog(
    entry: SessionEntry,
    onDismiss: () -> Unit,
    onSelectStatus: (DriverStatus) -> Unit,
    onOpenPenalty: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("#${entry.carNumber} ${entry.driverName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estado en pista:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                DriverStatus.entries.forEach { st ->
                    OutlinedButton(onClick = { onSelectStatus(st) }, modifier = Modifier.fillMaxWidth()) {
                        Text(st.labelEs)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onOpenPenalty,
                    colors = ButtonDefaults.buttonColors(containerColor = PitAmber, contentColor = ObsidianBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aplicar Penalización")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun PenaltyDialog(
    entry: SessionEntry,
    onDismiss: () -> Unit,
    onApply: (PenaltyType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(PenaltyType.TIME_5S) }
    var reason by remember { mutableStateOf("Límites de pista reiterados") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Penalizar a #${entry.carNumber} ${entry.driverCode}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PenaltyType.entries.forEach { pt ->
                    FilterChip(
                        selected = selectedType == pt,
                        onClick = { selectedType = pt },
                        label = { Text(pt.labelEs) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Motivo oficial") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedType, reason) },
                colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson)
            ) { Text("Aplicar Sanción") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

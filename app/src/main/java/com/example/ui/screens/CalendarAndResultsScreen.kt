package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Championship
import com.example.data.model.Circuit
import com.example.data.model.ConstructorStanding
import com.example.data.model.DriverStanding
import com.example.data.model.EventStatus
import com.example.data.model.LiveTimingRow
import com.example.data.model.RaceEvent
import com.example.data.model.SessionType
import com.example.data.model.TimingSession
import com.example.domain.TimingEngine
import com.example.ui.theme.ApexCrimson
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.GridBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PitAmber
import com.example.ui.theme.SectorGreen
import com.example.ui.theme.SectorPurple
import com.example.ui.theme.SectorYellow
import com.example.ui.theme.TelemetryCard
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TelemetryMonoStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ApexTimingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalendarAndResultsScreen(
    viewModel: ApexTimingViewModel
) {
    val role by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val championships by viewModel.championships.collectAsStateWithLifecycle()
    val circuits by viewModel.circuits.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val rows by viewModel.liveTimingRows.collectAsStateWithLifecycle()
    val penalties by viewModel.activeSessionPenalties.collectAsStateWithLifecycle()
    val standings by viewModel.standings.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateEventDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CarbonSurface,
            contentColor = ApexCrimson
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Calendario GP") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Resultados") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Campeonato") })
        }

        when (selectedTab) {
            0 -> CalendarTabContent(
                events = events,
                canEditCalendar = role.canManageChampionshipsAndUsers,
                onAddEvent = { showCreateEventDialog = true },
                onChangeStatus = viewModel::updateEventStatus
            )
            1 -> SessionResultsTabContent(
                session = activeSession,
                rows = rows,
                penalties = penalties
            )
            2 -> ChampionshipStandingsTabContent(
                drivers = standings.first,
                constructors = standings.second
            )
        }
    }

    if (showCreateEventDialog) {
        CreateEventDialog(
            championships = championships,
            circuits = circuits,
            nextRound = (events.size + 1).toLong(),
            onDismiss = { showCreateEventDialog = false },
            onConfirm = { champId, circuit, gpName, prac, qual, race, dur, round ->
                viewModel.createRaceEvent(champId, circuit, gpName, prac, qual, race, dur, round)
                showCreateEventDialog = false
            }
        )
    }
}

@Composable
private fun CalendarTabContent(
    events: List<RaceEvent>,
    canEditCalendar: Boolean,
    onAddEvent: () -> Unit,
    onChangeStatus: (String, EventStatus) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CALENDARIO DEL CAMPEONATO", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    if (!canEditCalendar) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                            Text("Vista de solo lectura (desbloquea en Privado para editar)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
                if (canEditCalendar) {
                    Button(
                        onClick = onAddEvent,
                        colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson),
                        modifier = Modifier.testTag("add_event_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir GP", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (events.isEmpty()) {
            item {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, GridBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No hay Grandes Premios programados en el calendario.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        } else {
            items(events, key = { it.id }) { evt ->
                val status = EventStatus.fromString(evt.status)
                val statusColor = when (status) {
                    EventStatus.LIVE -> SectorGreen
                    EventStatus.UPCOMING -> TelemetryCyan
                    EventStatus.FINISHED -> TextMuted
                }
                val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("es-ES")) }

                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GridBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("R${evt.roundNumber}", style = MaterialTheme.typography.labelLarge, color = ApexCrimson)
                                Text(TimingEngine.countryCodeToFlag(evt.countryCode), fontSize = 18.sp)
                                Column {
                                    Text(evt.grandPrixName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("${evt.circuitName} · ${dateFmt.format(Date(evt.eventDateMillis))}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                            Surface(color = statusColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = status.labelEs.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScheduleChip("ENTRENOS", evt.practiceSchedule, Modifier.weight(1f))
                            ScheduleChip("CLASIF.", evt.qualifyingSchedule, Modifier.weight(1f))
                            ScheduleChip("CARRERA", evt.raceSchedule, Modifier.weight(1f))
                        }

                        Text("Duración: ${evt.raceDurationLabel}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                        if (canEditCalendar) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EventStatus.entries.forEach { st ->
                                    FilterChip(
                                        selected = status == st,
                                        onClick = { onChangeStatus(evt.id, st) },
                                        label = { Text(st.labelEs, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleChip(label: String, time: String, modifier: Modifier = Modifier) {
    Surface(color = TelemetryCard, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(time.ifBlank { "--:--" }, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SessionResultsTabContent(
    session: TimingSession?,
    rows: List<LiveTimingRow>,
    penalties: List<com.example.data.model.PenaltyRecord>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text(
                    text = session?.let { "CLASIFICACIÓN: ${it.sessionName.uppercase()}" } ?: "TABLA DE RESULTADOS",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = session?.let { SessionType.fromString(it.sessionType).labelEs } ?: "Selecciona una sesión",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        if (rows.isEmpty()) {
            item {
                Text("Sin resultados registrados para esta sesión.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        } else {
            items(rows, key = { it.entry.id }) { row ->
                val e = row.entry
                val pts = if (e.pointsAwarded > 0L) e.pointsAwarded.toInt()
                else if (session?.sessionType == SessionType.RACE.name) {
                    TimingEngine.calculatePointsForPosition(row.computedPosition, e.hasFastestLap, e.lapsCompleted)
                } else 0

                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, GridBorder, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Text("P${row.computedPosition}", style = MaterialTheme.typography.labelLarge, color = if (row.computedPosition <= 3) SectorYellow else TextPrimary)
                            Box(modifier = Modifier.width(3.dp).height(26.dp).background(TimingEngine.parseColorSafe(e.teamColorHex)))
                            Column {
                                Text("${TimingEngine.countryCodeToFlag(e.countryCode)} #${e.carNumber} ${e.driverName}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${e.teamName} · ${e.lapsCompleted} vts · Mejor: ${TimingEngine.formatLapTime(e.bestLapMs)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                if (e.penaltySeconds > 0L) {
                                    Text("Penalización acumulada: +${e.penaltySeconds}s", style = MaterialTheme.typography.labelSmall, color = PitAmber)
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (row.computedPosition == 1) TimingEngine.formatLapTime(e.totalTimeMs.takeIf { it > 0 } ?: e.bestLapMs) else row.gapToLeader,
                                style = TelemetryMonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            if (pts > 0) {
                                Text("+$pts PTS", style = MaterialTheme.typography.labelSmall, color = SectorGreen)
                            }
                        }
                    }
                }
            }
        }

        if (penalties.isNotEmpty()) {
            item {
                Text("ACTA DE PENALIZACIONES (${penalties.size})", style = MaterialTheme.typography.labelLarge, color = PitAmber, modifier = Modifier.padding(top = 6.dp))
            }
            items(penalties, key = { it.id }) { pen ->
                Surface(
                    color = PitAmber.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, PitAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("#${pen.carNumber} ${pen.driverName} · ${pen.reason}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text(if (pen.secondsAdded > 0) "+${pen.secondsAdded}s" else pen.penaltyType, style = MaterialTheme.typography.labelSmall, color = PitAmber)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChampionshipStandingsTabContent(
    drivers: List<DriverStanding>,
    constructors: List<ConstructorStanding>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("CAMPEONATO DE PILOTOS", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Text("Actualizado automáticamente al finalizar carreras oficiales (25-18-15-12-10-8-6-4-2-1 + 1 VR)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        if (drivers.isEmpty()) {
            item {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, GridBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SectorYellow, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Finaliza una sesión de tipo Carrera para publicar puntos del campeonato.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        } else {
            items(drivers, key = { it.driverName }) { d ->
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, GridBorder, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("P${d.position}", style = MaterialTheme.typography.labelLarge, color = if (d.position == 1) SectorYellow else TextPrimary)
                            Box(modifier = Modifier.width(3.dp).height(26.dp).background(TimingEngine.parseColorSafe(d.teamColorHex)))
                            Column {
                                Text("${TimingEngine.countryCodeToFlag(d.countryCode)} ${d.driverName}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${d.teamName} · ${d.wins}V · ${d.podiums} Podios · ${d.fastestLaps} VR", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Text("${d.points} PTS", style = TelemetryMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = SectorGreen)
                    }
                }
            }

            item {
                Text("CAMPEONATO DE CONSTRUCTORES", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
            }

            items(constructors, key = { it.teamName }) { c ->
                Surface(
                    color = TelemetryCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, GridBorder, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("P${c.position}", style = MaterialTheme.typography.labelLarge, color = TelemetryCyan)
                            Box(modifier = Modifier.width(4.dp).height(24.dp).background(TimingEngine.parseColorSafe(c.teamColorHex)))
                            Column {
                                Text(c.teamName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${c.wins} victorias", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Text("${c.points} PTS", style = TelemetryMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = SectorPurple)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateEventDialog(
    championships: List<Championship>,
    circuits: List<Circuit>,
    nextRound: Long,
    onDismiss: () -> Unit,
    onConfirm: (String, Circuit, String, String, String, String, String, Long) -> Unit
) {
    var selectedChamp by remember(championships) { mutableStateOf(championships.firstOrNull()) }
    var selectedCircuit by remember(circuits) { mutableStateOf(circuits.firstOrNull()) }
    var gpName by remember { mutableStateOf("Gran Premio de Ronda $nextRound") }
    var practice by remember { mutableStateOf("Vie 11:00 - 12:00") }
    var qualifying by remember { mutableStateOf("Sáb 16:00 - 17:00") }
    var race by remember { mutableStateOf("Dom 14:00") }
    var duration by remember { mutableStateOf("53 vueltas / 90 min") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programar Gran Premio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (championships.isEmpty() || circuits.isEmpty()) {
                    Text("Se requiere al menos un Campeonato (Admin) y un Circuito para programar un evento.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Campeonato:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        championships.forEach { ch ->
                            FilterChip(selected = selectedChamp?.id == ch.id, onClick = { selectedChamp = ch }, label = { Text(ch.name) })
                        }
                    }
                    Text("Circuito:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        circuits.forEach { ci ->
                            FilterChip(selected = selectedCircuit?.id == ci.id, onClick = { selectedCircuit = ci }, label = { Text(ci.name) })
                        }
                    }
                    OutlinedTextField(value = gpName, onValueChange = { gpName = it }, label = { Text("Nombre del Gran Premio") }, singleLine = true)
                    OutlinedTextField(value = practice, onValueChange = { practice = it }, label = { Text("Horario entrenamientos") }, singleLine = true)
                    OutlinedTextField(value = qualifying, onValueChange = { qualifying = it }, label = { Text("Horario clasificación") }, singleLine = true)
                    OutlinedTextField(value = race, onValueChange = { race = it }, label = { Text("Horario carrera") }, singleLine = true)
                    OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duración de la prueba") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedChamp != null && selectedCircuit != null,
                onClick = {
                    if (selectedChamp != null && selectedCircuit != null) {
                        onConfirm(selectedChamp!!.id, selectedCircuit!!, gpName, practice, qualifying, race, duration, nextRound)
                    }
                }
            ) { Text("Guardar GP") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

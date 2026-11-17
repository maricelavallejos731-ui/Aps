package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Circuit
import com.example.data.model.LiveTimingRow
import com.example.data.model.SectorColorState
import com.example.data.model.TimingSession
import com.example.data.model.UserRole
import com.example.domain.TimingEngine
import com.example.ui.theme.ApexCrimson
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.GridBorder
import com.example.ui.theme.ObsidianBg
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

@Composable
fun SectorsAndCircuitsScreen(
    viewModel: ApexTimingViewModel,
    isWideScreen: Boolean
) {
    val role by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val circuits by viewModel.circuits.collectAsStateWithLifecycle()
    val activeCircuit by viewModel.activeCircuit.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val rows by viewModel.liveTimingRows.collectAsStateWithLifecycle()

    var showCreateCircuitDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SECTORES Y CIRCUITOS", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text(
                        text = "Mapa del trazado, tiempos por sector y biblioteca oficial",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (role.canManageChampionshipsAndUsers) {
                    Button(
                        onClick = { showCreateCircuitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson),
                        modifier = Modifier.testTag("create_circuit_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importar Circuito", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (activeCircuit != null) {
            item {
                TrackSectorMapCard(
                    circuit = activeCircuit!!,
                    session = activeSession,
                    isWideScreen = isWideScreen
                )
            }
        }

        if (rows.isNotEmpty()) {
            item {
                Text("DESGLOSE DE SECTORES EN DIRECTO", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
            items(rows, key = { it.entry.id }) { row ->
                DriverSectorCard(row = row)
            }
        }

        item {
            Text(
                text = "BIBLIOTECA DE CIRCUITOS (${circuits.size})",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (circuits.isEmpty()) {
            item {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, GridBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No hay circuitos registrados todavía.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        } else {
            items(circuits, key = { it.id }) { circuit ->
                CircuitLibraryCard(
                    circuit = circuit,
                    isSelected = circuit.id == activeCircuit?.id,
                    onClick = { viewModel.selectCircuit(circuit.id) }
                )
            }
        }
    }

    if (showCreateCircuitDialog) {
        CreateCircuitDialog(
            onDismiss = { showCreateCircuitDialog = false },
            onConfirm = { name, country, cc, len, turns, sectors, startLabel, points, trackPts, imgUri ->
                viewModel.saveCircuit(name, country, cc, len, turns, sectors, startLabel, points, trackPts, imgUri)
                showCreateCircuitDialog = false
            }
        )
    }
}

@Composable
private fun TrackSectorMapCard(
    circuit: Circuit,
    session: TimingSession?,
    isWideScreen: Boolean
) {
    val sectorColors = listOf(ApexCrimson, TelemetryCyan, SectorYellow, SectorGreen, SectorPurple, Color(0xFFFB923C))
    val sectorCount = circuit.sectorCount.toInt().coerceIn(1, 6)

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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(TimingEngine.countryCodeToFlag(circuit.countryCode), fontSize = 20.sp)
                    Column {
                        Text(circuit.name, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Text(
                            text = "${circuit.country} · ${circuit.lengthKm} km · ${circuit.turnsCount} curvas · $sectorCount sectores",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Track layout visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isWideScreen) 240.dp else 190.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GridBorder, RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_circuit_backdrop),
                    contentDescription = "Fondo técnico de circuito",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (circuit.customImageUri.isNotBlank()) {
                    AsyncImage(
                        model = circuit.customImageUri,
                        contentDescription = "Imagen del trazado ${circuit.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        alpha = 0.45f
                    )
                }

                // Interactive Sector-colored Track Canvas
                val parsedPoints = remember(circuit.trackPoints) {
                    circuit.trackPoints.mapNotNull { raw ->
                        val parts = raw.split(",")
                        val x = parts.getOrNull(0)?.trim()?.toFloatOrNull()
                        val y = parts.getOrNull(1)?.trim()?.toFloatOrNull()
                        if (x != null && y != null) Offset(x.coerceIn(0.05f, 0.95f), y.coerceIn(0.05f, 0.95f)) else null
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                    if (parsedPoints.size >= 3) {
                        val totalSegments = parsedPoints.size - 1
                        val fullPath = Path().apply {
                            parsedPoints.forEachIndexed { idx, pt ->
                                val px = Offset(pt.x * size.width, pt.y * size.height)
                                if (idx == 0) moveTo(px.x, px.y) else lineTo(px.x, px.y)
                            }
                        }
                        drawPath(
                            path = fullPath,
                            color = Color.Black.copy(alpha = 0.65f),
                            style = Stroke(width = 18f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        for (i in 0 until totalSegments) {
                            val fraction = i.toFloat() / totalSegments.toFloat()
                            val sectorIdx = (fraction * sectorCount).toInt().coerceIn(0, sectorCount - 1)
                            val p1 = Offset(parsedPoints[i].x * size.width, parsedPoints[i].y * size.height)
                            val p2 = Offset(parsedPoints[i + 1].x * size.width, parsedPoints[i + 1].y * size.height)
                            drawLine(
                                color = sectorColors[sectorIdx % sectorColors.size],
                                start = p1,
                                end = p2,
                                strokeWidth = 9f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Start/Finish marker
                        val sf = Offset(parsedPoints.first().x * size.width, parsedPoints.first().y * size.height)
                        drawCircle(color = Color.White, radius = 11f, center = sf)
                        drawCircle(color = ApexCrimson, radius = 6f, center = sf)

                        // Sector boundary markers
                        for (s in 1 until sectorCount) {
                            val ptIdx = ((s.toFloat() / sectorCount.toFloat()) * totalSegments).toInt().coerceIn(1, totalSegments - 1)
                            val pt = Offset(parsedPoints[ptIdx].x * size.width, parsedPoints[ptIdx].y * size.height)
                            drawCircle(color = Color.White, radius = 8f, center = pt)
                            drawCircle(color = sectorColors[s % sectorColors.size], radius = 5f, center = pt)
                        }
                    }
                }
            }

            // Sector Legend + Best Session Sector Times
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (s in 1..sectorCount) {
                    val col = sectorColors[(s - 1) % sectorColors.size]
                    val bestMs = when (s) {
                        1 -> session?.bestSector1Ms ?: 0L
                        2 -> session?.bestSector2Ms ?: 0L
                        3 -> session?.bestSector3Ms ?: 0L
                        else -> 0L
                    }
                    Surface(
                        color = TelemetryCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, col.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(col, RoundedCornerShape(2.dp)))
                            Text("S$s", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            Text(
                                text = TimingEngine.formatSectorTime(bestMs),
                                style = TelemetryMonoStyle.copy(fontSize = 12.sp),
                                color = if (bestMs > 0L) SectorPurple else TextMuted
                            )
                        }
                    }
                }
            }

            if (circuit.measurementPoints.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(15.dp))
                    Text(
                        text = "Puntos de medición: ${circuit.measurementPoints.joinToString(" · ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverSectorCard(row: LiveTimingRow) {
    val e = row.entry
    Surface(
        color = TelemetryCard,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridBorder, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("P${row.computedPosition}", style = MaterialTheme.typography.labelLarge, color = TelemetryCyan)
                    Text("#${e.carNumber} ${e.driverName}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                }
                Text(
                    text = "Mejor: ${TimingEngine.formatLapTime(e.bestLapMs)}",
                    style = TelemetryMonoStyle.copy(fontSize = 12.sp),
                    color = SectorGreen
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectorSplitBadge("S1", e.currentSector1Ms, e.bestSector1Ms, row.sector1Color, Modifier.weight(1f))
                SectorSplitBadge("S2", e.currentSector2Ms, e.bestSector2Ms, row.sector2Color, Modifier.weight(1f))
                SectorSplitBadge("S3", e.currentSector3Ms, e.bestSector3Ms, row.sector3Color, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectorSplitBadge(
    label: String,
    currentMs: Long,
    bestMs: Long,
    colorState: SectorColorState,
    modifier: Modifier = Modifier
) {
    val color = when (colorState) {
        SectorColorState.PURPLE -> SectorPurple
        SectorColorState.GREEN -> SectorGreen
        SectorColorState.YELLOW -> SectorYellow
        SectorColorState.NONE -> TextMuted
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                text = TimingEngine.formatSectorTime(currentMs),
                style = TelemetryMonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "PB ${TimingEngine.formatSectorTime(bestMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CircuitLibraryCard(
    circuit: Circuit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) TelemetryCard else CarbonSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isSelected) ApexCrimson else GridBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(TimingEngine.countryCodeToFlag(circuit.countryCode), fontSize = 16.sp)
                    Text(circuit.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Text(
                    text = "${circuit.country} · ${circuit.lengthKm} km · ${circuit.turnsCount} curvas · ${circuit.sectorCount} sectores",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Meta: ${circuit.startFinishLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            if (isSelected) {
                Text("ACTIVO", style = MaterialTheme.typography.labelSmall, color = ApexCrimson)
            }
        }
    }
}

@Composable
private fun CreateCircuitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Long, Long, String, List<String>, List<String>, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("España") }
    var cc by remember { mutableStateOf("ES") }
    var lengthText by remember { mutableStateOf("4.657") }
    var turnsText by remember { mutableStateOf("14") }
    var sectorCount by remember { mutableStateOf(3L) }
    var startFinish by remember { mutableStateOf("Recta Principal") }
    var pointsText by remember { mutableStateOf("Meta, Fin S1, Fin S2") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar / Configurar Circuito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del circuito") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("País") }, singleLine = true, modifier = Modifier.weight(1.4f))
                    OutlinedTextField(value = cc, onValueChange = { cc = it.take(2).uppercase() }, label = { Text("ISO") }, singleLine = true, modifier = Modifier.weight(0.8f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lengthText, onValueChange = { lengthText = it }, label = { Text("Longitud (km)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = turnsText, onValueChange = { turnsText = it.filter(Char::isDigit) }, label = { Text("Curvas") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Cantidad de sectores:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2L, 3L, 4L, 5L).forEach { count ->
                        FilterChip(
                            selected = sectorCount == count,
                            onClick = { sectorCount = count },
                            label = { Text("$count") }
                        )
                    }
                }
                OutlinedTextField(value = startFinish, onValueChange = { startFinish = it }, label = { Text("Línea de salida/meta") }, singleLine = true)
                OutlinedTextField(value = pointsText, onValueChange = { pointsText = it }, label = { Text("Puntos de medición (separados por coma)") }, singleLine = true)
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (selectedImageUri != null) "Imagen del trazado seleccionada" else "Importar Imagen del Trazado (Photo Picker)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val defaultLoop = listOf(
                        "0.18,0.72", "0.52,0.72", "0.80,0.65", "0.84,0.40",
                        "0.64,0.24", "0.42,0.34", "0.24,0.22", "0.14,0.48", "0.18,0.72"
                    )
                    onConfirm(
                        name.ifBlank { "Nuevo Circuito GP" },
                        country.ifBlank { "España" },
                        cc.ifBlank { "ES" },
                        lengthText.toDoubleOrNull() ?: 4.5,
                        turnsText.toLongOrNull() ?: 12L,
                        sectorCount,
                        startFinish.ifBlank { "Meta" },
                        pointsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        defaultLoop,
                        selectedImageUri?.toString()
                    )
                }
            ) { Text("Guardar Circuito") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRole
import com.example.ui.theme.ApexCrimson
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.GridBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutRed
import com.example.ui.theme.PitAmber
import com.example.ui.theme.RoleRaceDirector
import com.example.ui.theme.RoleSpectator
import com.example.ui.theme.RoleSuperAdmin
import com.example.ui.theme.RoleTimekeeper
import com.example.ui.theme.SectorGreen
import com.example.ui.theme.TelemetryCard
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ApexTimingViewModel

@Composable
fun AdminUsersScreen(
    viewModel: ApexTimingViewModel
) {
    val isUnlocked by viewModel.isMasterUnlocked.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val role by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val championships by viewModel.championships.collectAsStateWithLifecycle()
    val isLocalOffline by viewModel.isLocalOfflineMode.collectAsStateWithLifecycle()
    val pendingOffline by viewModel.pendingOfflineLaps.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingOffline.collectAsStateWithLifecycle()
    val hasCustomPassword by viewModel.hasCustomMasterPassword.collectAsStateWithLifecycle()

    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showCreateChampDialog by remember { mutableStateOf(false) }

    var currentPwInput by remember { mutableStateOf("") }
    var newPwInput by remember { mutableStateOf("") }
    var confirmPwInput by remember { mutableStateOf("") }
    var showChangePwFields by remember { mutableStateOf(false) }

    val roleColor = when (role) {
        UserRole.SUPER_ADMIN -> RoleSuperAdmin
        UserRole.RACE_DIRECTOR -> RoleRaceDirector
        UserRole.TIMEKEEPER -> RoleTimekeeper
        UserRole.SPECTATOR -> RoleSpectator
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isUnlocked) {
            item {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GridBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ApexCrimson.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Acceso privado bloqueado",
                                    tint = ApexCrimson,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "APARTADO PRIVADO",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Modo actual: Espectador (solo lectura)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = "Introduce tu contraseña privada para habilitar el control de sesiones, cronometraje, circuitos, calendario y campeonatos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                if (authError != null) viewModel.clearAuthError()
                            },
                            label = { Text("Contraseña privada") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.unlockWithPassword(passwordInput)
                                    passwordInput = ""
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("master_password_input")
                        )

                        if (authError != null) {
                            Surface(
                                color = OutRed.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, OutRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = authError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.unlockWithPassword(passwordInput)
                                passwordInput = ""
                            },
                            enabled = passwordInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ApexCrimson,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("unlock_master_button")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DESBLOQUEAR CONTROL",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Unlocked Master Control Card
            item {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SectorGreen.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = SectorGreen)
                                Column {
                                    Text(
                                        text = "ACCESO PRIVADO ACTIVO",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (hasCustomPassword) "Contraseña personalizada protegida" else "Clave privada activa",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Surface(color = roleColor.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = role.labelEs.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = roleColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Text(
                            text = when (role) {
                                UserRole.SUPER_ADMIN -> "Permisos totales: campeonatos, calendarios, circuitos, sesiones, cronómetro y resultados."
                                UserRole.RACE_DIRECTOR -> "Dirección de carrera: iniciar/detener sesiones, cronómetro, banderas, estados de pilotos y penalizaciones."
                                UserRole.TIMEKEEPER -> "Cronometrador: registrar vueltas, sectores S1/S2/S3 y corregir validez de vueltas."
                                UserRole.SPECTATOR -> "Espectador: vista previa en modo solo lectura."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Text("NIVEL OPERATIVO EN PANTALLA", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UserRole.entries.forEach { candidate ->
                                FilterChip(
                                    selected = role == candidate,
                                    onClick = { viewModel.switchOperatorRole(candidate) },
                                    label = { Text(candidate.labelEs, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.lockMasterAccess() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lock_master_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bloquear Apartado Privado")
                        }
                    }
                }
            }

            // Change Private Password Card
            item {
                Surface(
                    color = TelemetryCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GridBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(18.dp))
                            Text("CAMBIAR CONTRASEÑA PRIVADA", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        }
                        Text(
                            text = "Personaliza tu clave para que absolutamente nadie más pueda acceder a este apartado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = currentPwInput,
                            onValueChange = { currentPwInput = it },
                            label = { Text("Contraseña actual") },
                            singleLine = true,
                            visualTransformation = if (showChangePwFields) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPwInput,
                            onValueChange = { newPwInput = it },
                            label = { Text("Nueva contraseña (mín. 4 caracteres)") },
                            singleLine = true,
                            visualTransformation = if (showChangePwFields) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPwInput,
                            onValueChange = { confirmPwInput = it },
                            label = { Text("Confirmar nueva contraseña") },
                            singleLine = true,
                            visualTransformation = if (showChangePwFields) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showChangePwFields = !showChangePwFields }) {
                                Text(if (showChangePwFields) "Ocultar" else "Mostrar")
                            }
                            Button(
                                onClick = {
                                    viewModel.changeMasterPassword(currentPwInput, newPwInput, confirmPwInput)
                                    currentPwInput = ""
                                    newPwInput = ""
                                    confirmPwInput = ""
                                },
                                enabled = currentPwInput.isNotBlank() && newPwInput.isNotBlank() && confirmPwInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = ObsidianBg),
                                modifier = Modifier.testTag("save_new_password_button")
                            ) {
                                Text("Guardar Clave", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Local Queue Engine Card
            item {
                Surface(
                    color = TelemetryCard,
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("COLA DE BÚFER LOCAL", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(
                                    text = "Retiene vueltas en cola antes de volcarlas a la tabla oficial de la sesión.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = isLocalOffline,
                                onCheckedChange = viewModel::setLocalOfflineMode
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pendientes en cola local: ${pendingOffline.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pendingOffline.isNotEmpty()) PitAmber else SectorGreen
                            )
                            Button(
                                onClick = viewModel::syncOfflineQueue,
                                enabled = pendingOffline.isNotEmpty() && !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = ObsidianBg)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSyncing) "Aplicando..." else "Aplicar Cola")
                            }
                        }
                    }
                }
            }

            // Championship Management (available when SUPER_ADMIN active)
            if (role.canManageChampionshipsAndUsers) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CAMPEONATOS (${championships.size})", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = viewModel::initializeBaseStructure) {
                                Text("Base GP", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { showCreateChampDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson),
                                modifier = Modifier.testTag("create_championship_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nuevo", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                items(championships, key = { it.id }) { champ ->
                    Surface(
                        color = CarbonSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GridBorder, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(champ.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${champ.seasonYear} · ${champ.category}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            IconButton(onClick = { viewModel.deleteChampionship(champ.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar campeonato", tint = OutRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateChampDialog) {
        CreateChampionshipDialog(
            onDismiss = { showCreateChampDialog = false },
            onConfirm = { name, year, cat ->
                viewModel.createChampionship(name, year, cat)
                showCreateChampDialog = false
            }
        )
    }
}

@Composable
private fun CreateChampionshipDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("2026") }
    var category by remember { mutableStateOf("Fórmula Apex") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Campeonato") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del campeonato") }, singleLine = true)
                OutlinedTextField(value = yearText, onValueChange = { yearText = it.filter(Char::isDigit) }, label = { Text("Temporada (Año)") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name.ifBlank { "Campeonato Apex" },
                        yearText.toLongOrNull() ?: 2026L,
                        category.ifBlank { "Fórmula Apex" }
                    )
                }
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

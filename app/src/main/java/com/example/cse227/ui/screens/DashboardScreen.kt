package com.example.cse227.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cse227.bluetooth.BleConnectionState
import com.example.cse227.ui.components.BleStatusBadge
import com.example.cse227.ui.components.SyncStatusBadge
import com.example.cse227.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToConnectDevice: () -> Unit,
    onNavigateToPatientSetup: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.startMonitoringService(context)
        viewModel.loadTabletAndPatientInfo()
    }

    val activePatient by viewModel.activePatient.collectAsState()
    val tabletId by viewModel.tabletId.collectAsState()
    val activeDeviceState by viewModel.activeDeviceState.collectAsState()

    val pendingCount by viewModel.pendingCountFlow.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val lastSyncError by viewModel.lastSyncError.collectAsState()

    val latestReadingFromDb by viewModel.latestReadingFromDb.collectAsState()
    val liveMeasurement = activeDeviceState?.latestMeasurement

    val currentSpo2 = liveMeasurement?.spo2 ?: latestReadingFromDb?.spo2
    val currentPulseRate = liveMeasurement?.pulseRate ?: latestReadingFromDb?.pulseRate
    val currentPi = liveMeasurement?.perfusionIndex ?: latestReadingFromDb?.perfusionIndex

    val connectionState = activeDeviceState?.connectionState ?: BleConnectionState.DISCONNECTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Mobile Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Patient Monitor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Station ID: ${tabletId ?: "Not Registered"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.triggerManualSync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Manual Sync")
                        }
                        SyncStatusBadge(syncState = syncState, pendingCount = pendingCount)
                    }
                }
            }
        }

        // Error / Sync Status Banner
        if (!lastSyncError.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sync Alert: $lastSyncError",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.triggerManualSync() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Retry", fontSize = 11.sp)
                    }
                }
            }
        }

        // Patient Card (Full Mobile Width)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Patient", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(onClick = onNavigateToPatientSetup, shape = RoundedCornerShape(10.dp)) {
                        Text(if (activePatient != null) "Change" else "Assign", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (activePatient != null) {
                    Text(
                        text = activePatient?.displayName ?: "Unassigned",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ID: ${activePatient?.displayId}  •  ${activePatient?.age ?: "--"} yrs  •  ${activePatient?.sex ?: "--"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Assigned: ${activePatient?.displayDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = "No active patient assigned on this device.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // AP-20 Oximeter Device Status Card (Full Mobile Width)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AP-20 Pulse Oximeter", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    if (connectionState == BleConnectionState.DISCONNECTED) {
                        Button(onClick = onNavigateToConnectDevice, shape = RoundedCornerShape(10.dp)) {
                            Text("Connect", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = activeDeviceState?.name ?: "No AP-20 Connected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "MAC: ${activeDeviceState?.address ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    BleStatusBadge(connectionState = connectionState)
                }
            }
        }

        // Live Vital Signs Cards (Mobile Responsive Row or Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SpO2 Card
            VitalSignCard(
                modifier = Modifier.weight(1f),
                title = "SpO₂",
                valueText = if (currentSpo2 != null) "$currentSpo2" else "--",
                unitText = "%",
                icon = Icons.Default.WaterDrop,
                accentColor = Color(0xFF0288D1),
                statusLabel = when {
                    currentSpo2 == null -> "Awaiting BLE"
                    currentSpo2 >= 95 -> "Normal"
                    currentSpo2 >= 90 -> "Low"
                    else -> "Critical"
                }
            )

            // Pulse Rate Card
            VitalSignCard(
                modifier = Modifier.weight(1f),
                title = "Pulse Rate",
                valueText = if (currentPulseRate != null) "$currentPulseRate" else "--",
                unitText = "BPM",
                icon = Icons.Default.Favorite,
                accentColor = Color(0xFFD32F2F),
                statusLabel = when {
                    currentPulseRate == null -> "Awaiting BLE"
                    currentPulseRate in 60..100 -> "Normal"
                    currentPulseRate < 60 -> "Bradycardia"
                    else -> "Tachycardia"
                }
            )
        }

        // Perfusion Index & Backend Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Perfusion Index (PI)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = if (currentPi != null) "%.1f".format(currentPi) else "--",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Backend Sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "Pending: $pendingCount records",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (lastSyncTime > 0) "Last: ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(lastSyncTime))}" else "Last: None",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun VitalSignCard(
    modifier: Modifier = Modifier,
    title: String,
    valueText: String,
    unitText: String,
    icon: ImageVector,
    accentColor: Color,
    statusLabel: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = valueText,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unitText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

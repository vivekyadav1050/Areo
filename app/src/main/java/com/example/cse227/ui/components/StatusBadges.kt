package com.example.cse227.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cse227.bluetooth.BleConnectionState
import com.example.cse227.data.sync.SyncStatusState
import com.example.cse227.ui.theme.VitalAmber
import com.example.cse227.ui.theme.VitalAmberBg
import com.example.cse227.ui.theme.VitalGreen
import com.example.cse227.ui.theme.VitalGreenBg
import com.example.cse227.ui.theme.VitalRed
import com.example.cse227.ui.theme.VitalRedBg

@Composable
fun BleStatusBadge(
    connectionState: BleConnectionState,
    modifier: Modifier = Modifier
) {
    val (bgColor, dotColor, label) = when (connectionState) {
        BleConnectionState.CONNECTED -> Triple(VitalGreenBg, VitalGreen, "Live BLE Connected")
        BleConnectionState.CONNECTING -> Triple(VitalAmberBg, VitalAmber, "Connecting...")
        BleConnectionState.DISCONNECTING -> Triple(VitalAmberBg, VitalAmber, "Disconnecting...")
        BleConnectionState.FAILED -> Triple(VitalRedBg, VitalRed, "Connection Failed")
        BleConnectionState.DISCONNECTED -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "Disconnected")
    }

    val animatedBg by animateColorAsState(targetValue = bgColor, label = "BleBgAnim")
    val animatedDot by animateColorAsState(targetValue = dotColor, label = "BleDotAnim")

    Surface(
        color = animatedBg,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(animatedDot, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = animatedDot,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SyncStatusBadge(
    syncState: SyncStatusState,
    pendingCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val (bgColor, dotColor, label) = when {
        syncState == SyncStatusState.SYNCING -> Triple(VitalAmberBg, VitalAmber, "Syncing Batch...")
        syncState == SyncStatusState.SYNC_ERROR -> Triple(VitalRedBg, VitalRed, "Sync Error")
        pendingCount > 0 -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "$pendingCount Pending")
        syncState == SyncStatusState.SYNCED_SUCCESS -> Triple(VitalGreenBg, VitalGreen, "Cloud Synchronized")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "Cloud Idle")
    }

    val animatedBg by animateColorAsState(targetValue = bgColor, label = "SyncBgAnim")
    val animatedDot by animateColorAsState(targetValue = dotColor, label = "SyncDotAnim")

    Surface(
        color = animatedBg,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(animatedDot, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = animatedDot,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

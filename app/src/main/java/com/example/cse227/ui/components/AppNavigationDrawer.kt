package com.example.cse227.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cse227.ui.navigation.Screen

data class DrawerMenuItem(
    val screen: Screen,
    val icon: ImageVector
)

val drawerMenuItems = listOf(
    DrawerMenuItem(Screen.Dashboard, Icons.Default.MonitorHeart),
    DrawerMenuItem(Screen.ConnectDevice, Icons.Default.BluetoothSearching),
    DrawerMenuItem(Screen.ConnectedDevices, Icons.Default.BluetoothConnected),
    DrawerMenuItem(Screen.PatientDetails, Icons.Default.Person),
    DrawerMenuItem(Screen.SyncStatus, Icons.Default.CloudSync),
    DrawerMenuItem(Screen.Settings, Icons.Default.Settings)
)

@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    currentRoute: String,
    username: String,
    tabletId: String?,
    activePatientName: String?,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp)
            ) {
                // Drawer Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Aerio",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Station User: $username",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tablet ID: ${tabletId ?: "Not Registered"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Patient: ${activePatientName ?: "Unassigned"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Drawer Navigation Items
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    drawerMenuItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationDrawerItem(
                            label = { Text(item.screen.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            selected = selected,
                            onClick = { onNavigate(item.screen) },
                            shape = RoundedCornerShape(12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Logout Item
                    NavigationDrawerItem(
                        label = { Text("Logout", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                        icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        content = content
    )
}
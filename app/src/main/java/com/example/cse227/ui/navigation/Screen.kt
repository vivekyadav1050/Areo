package com.example.cse227.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Login")
    object Dashboard : Screen("dashboard", "Live Dashboard")
    object ConnectDevice : Screen("connect_device", "Connect Device")
    object ConnectedDevices : Screen("connected_devices", "Connected Devices")
    object PatientDetails : Screen("patient_details", "Patient Details")
    object SyncStatus : Screen("sync_status", "Sync Status")
    object Settings : Screen("settings", "Settings")
}
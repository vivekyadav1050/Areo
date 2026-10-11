package com.example.cse227.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cse227.Ap20Application
import com.example.cse227.data.sync.SyncCoordinator
import com.example.cse227.service.MonitoringForegroundService
import com.example.cse227.ui.components.AppNavigationDrawer
import com.example.cse227.ui.screens.ConnectDeviceScreen
import com.example.cse227.ui.screens.ConnectedDevicesScreen
import com.example.cse227.ui.screens.DashboardScreen
import com.example.cse227.ui.screens.LoginScreen
import com.example.cse227.ui.screens.PatientDetailsScreen
import com.example.cse227.ui.screens.SettingsScreen
import com.example.cse227.ui.screens.SyncStatusScreen
import com.example.cse227.ui.viewmodel.DashboardViewModel
import com.example.cse227.ui.viewmodel.DeviceViewModel
import com.example.cse227.ui.viewmodel.LoginViewModel
import com.example.cse227.ui.viewmodel.PatientViewModel
import com.example.cse227.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = context.applicationContext as Ap20Application

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: if (app.authRepository.isLoggedIn()) Screen.Dashboard.route else Screen.Login.route

    // Initialize ViewModels
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.Factory(app.authRepository, app.tabletRepository)
    )
    val deviceViewModel: DeviceViewModel = viewModel(
        factory = DeviceViewModel.Factory(app.bleScanner, app.bleManager)
    )
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            tabletRepository = app.tabletRepository,
            monitoringRepository = app.monitoringRepository,
            sessionManager = app.sessionManager,
            bleManager = app.bleManager,
            syncCoordinator = SyncCoordinator(app.monitoringRepository, app.bleManager)
        )
    )
    val patientViewModel: PatientViewModel = viewModel(
        factory = PatientViewModel.Factory(app.patientRepository, app.tabletRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.sessionManager, app.apiClient, app.authRepository, app.monitoringRepository)
    )

    val username = app.sessionManager.getUsername() ?: "Guest"
    val tabletId = app.sessionManager.getTabletId()
    val activePatientName = app.sessionManager.getActivePatientName()

    val isLoginScreen = currentRoute == Screen.Login.route
    val startDestination = if (app.authRepository.isLoggedIn()) Screen.Dashboard.route else Screen.Login.route

    val navigationBuilder: NavGraphBuilder.() -> Unit = {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToConnectDevice = { navController.navigate(Screen.ConnectDevice.route) },
                onNavigateToPatientSetup = { navController.navigate(Screen.PatientDetails.route) }
            )
        }
        composable(Screen.ConnectDevice.route) {
            ConnectDeviceScreen(
                viewModel = deviceViewModel,
                onDeviceConnected = { navController.navigate(Screen.Dashboard.route) }
            )
        }
        composable(Screen.ConnectedDevices.route) {
            ConnectedDevicesScreen(viewModel = deviceViewModel)
        }
        composable(Screen.PatientDetails.route) {
            PatientDetailsScreen(viewModel = patientViewModel)
        }
        composable(Screen.SyncStatus.route) {
            SyncStatusScreen(viewModel = dashboardViewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onLogoutClicked = {
                    app.bleManager.disconnectAll()
                    MonitoringForegroundService.stopMonitoringService(context)
                    app.authRepository.logout()
                    loginViewModel.resetStateOnLogout()
                    scope.launch(Dispatchers.IO) {
                        app.database.readingDao.deleteAllReadings()
                    }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }

    if (isLoginScreen) {
        NavHost(navController = navController, startDestination = startDestination, builder = navigationBuilder)
    } else {
        AppNavigationDrawer(
            drawerState = drawerState,
            currentRoute = currentRoute,
            username = username,
            tabletId = tabletId,
            activePatientName = activePatientName,
            onNavigate = { targetScreen ->
                scope.launch { drawerState.close() }
                navController.navigate(targetScreen.route) {
                    popUpTo(Screen.Dashboard.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onLogout = {
                scope.launch { drawerState.close() }
                app.bleManager.disconnectAll()
                MonitoringForegroundService.stopMonitoringService(context)
                app.authRepository.logout()
                loginViewModel.resetStateOnLogout()
                scope.launch(Dispatchers.IO) {
                    app.database.readingDao.deleteAllReadings()
                }
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentRoute) {
                                    Screen.Dashboard.route -> "Aerio Patient Monitor"
                                    Screen.ConnectDevice.route -> "Scan & Connect AP-20"
                                    Screen.ConnectedDevices.route -> "Connected Pulse Oximeters"
                                    Screen.PatientDetails.route -> "Patient Assignment"
                                    Screen.SyncStatus.route -> "Synchronization Status"
                                    Screen.Settings.route -> "System Settings"
                                    else -> "Aerio Monitor"
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open Drawer Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors()
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding),
                    builder = navigationBuilder
                )
            }
        }
    }
}

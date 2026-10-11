package com.example.cse227.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cse227.bluetooth.Ap20BleManager
import com.example.cse227.bluetooth.Ap20DeviceState
import com.example.cse227.bluetooth.BleDeviceScanner
import com.example.cse227.bluetooth.DiscoveredBleDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceViewModel(
    private val bleScanner: BleDeviceScanner,
    private val bleManager: Ap20BleManager
) : ViewModel() {

    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = bleScanner.discoveredDevices
    val isScanning: StateFlow<Boolean> = bleScanner.isScanning
    val scanError: StateFlow<String?> = bleScanner.scanError

    val deviceStates: StateFlow<Map<String, Ap20DeviceState>> = bleManager.deviceStates

    private val _selectedDeviceAddress = MutableStateFlow<String?>(null)
    val selectedDeviceAddress: StateFlow<String?> = _selectedDeviceAddress.asStateFlow()

    fun startScan() {
        bleScanner.startScan()
    }

    fun stopScan() {
        bleScanner.stopScan()
    }

    fun connectDevice(address: String, name: String) {
        bleScanner.stopScan()
        _selectedDeviceAddress.value = address
        bleManager.connectDevice(address, name)
    }

    fun disconnectDevice(address: String) {
        bleManager.disconnectDevice(address)
        if (_selectedDeviceAddress.value == address) {
            _selectedDeviceAddress.value = null
        }
    }

    fun selectActiveDevice(address: String) {
        _selectedDeviceAddress.value = address
    }

    class Factory(
        private val bleScanner: BleDeviceScanner,
        private val bleManager: Ap20BleManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceViewModel(bleScanner, bleManager) as T
        }
    }
}
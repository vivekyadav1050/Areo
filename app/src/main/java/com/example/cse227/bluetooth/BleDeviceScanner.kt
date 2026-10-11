package com.example.cse227.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BleDeviceScanner(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    private var scanner: BluetoothLeScanner? = null

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val scanRecord = result.scanRecord
                val rawName = device.name ?: scanRecord?.deviceName
                val address = device.address ?: return

                val serviceUuids = scanRecord?.serviceUuids
                val hasAp20Uuid = serviceUuids?.any {
                    it.uuid.toString().contains("FFB0", ignoreCase = true)
                } == true

                val isAp20 = hasAp20Uuid ||
                        (rawName?.contains("AP-20", ignoreCase = true) == true) ||
                        (rawName?.contains("AP20", ignoreCase = true) == true) ||
                        (rawName?.contains("Lepu", ignoreCase = true) == true) ||
                        (rawName?.contains("Oximeter", ignoreCase = true) == true) ||
                        (rawName?.contains("Oxi", ignoreCase = true) == true) ||
                        (rawName?.contains("Pulse", ignoreCase = true) == true) ||
                        (rawName?.contains("AO-", ignoreCase = true) == true) ||
                        (rawName?.contains("PC-", ignoreCase = true) == true)

                // Only include relevant AP-20 pulse oximeter devices
                if (!isAp20) {
                    return
                }

                val deviceName = when {
                    !rawName.isNullOrBlank() -> rawName
                    else -> "AP-20 Oximeter (${address.takeLast(5)})"
                }

                val newDevice = DiscoveredBleDevice(
                    name = deviceName,
                    address = address,
                    rssi = result.rssi,
                    isAp20Candidate = true
                )

                val currentList = _discoveredDevices.value.toMutableList()
                val index = currentList.indexOfFirst { it.address == address }
                if (index >= 0) {
                    currentList[index] = newDevice
                } else {
                    currentList.add(newDevice)
                }
                _discoveredDevices.value = currentList
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _scanError.value = "BLE scan failed with error code: $errorCode"
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(timeoutMillis: Long = 15000L) {
        if (!BlePermissionHelper.hasBlePermissions(context)) {
            _scanError.value = "Bluetooth permissions are required to scan for AP-20 devices."
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _scanError.value = "Bluetooth is disabled or unavailable on this device."
            return
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _scanError.value = "Unable to access BLE scanner."
            return
        }

        _discoveredDevices.value = emptyList()
        _scanError.value = null
        _isScanning.value = true

        try {
            scanner?.startScan(scanCallback)
        } catch (e: Exception) {
            _isScanning.value = false
            _scanError.value = "Failed to start BLE scan: ${e.message}"
            return
        }

        scanJob?.cancel()
        scanJob = scope.launch {
            delay(timeoutMillis)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanJob?.cancel()
        if (_isScanning.value) {
            try {
                scanner?.stopScan(scanCallback)
            } catch (_: Exception) {}
            _isScanning.value = false
        }
    }
}

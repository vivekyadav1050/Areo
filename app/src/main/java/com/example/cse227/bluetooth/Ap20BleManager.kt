package com.example.cse227.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class Ap20BleManager(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000FFB0-0000-1000-8000-00805F9B34FB")
        val CHARACTERISTIC_FFB1: UUID = UUID.fromString("0000FFB1-0000-1000-8000-00805F9B34FB")
        val CHARACTERISTIC_FFB2: UUID = UUID.fromString("0000FFB2-0000-1000-8000-00805F9B34FB")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    private val activeGattMap = mutableMapOf<String, BluetoothGatt>()
    private val decodersMap = mutableMapOf<String, Ap20PacketDecoder>()

    private val _deviceStates = MutableStateFlow<Map<String, Ap20DeviceState>>(emptyMap())
    val deviceStates: StateFlow<Map<String, Ap20DeviceState>> = _deviceStates.asStateFlow()

    private val _measurementsFlow = MutableSharedFlow<Spo2Measurement>(extraBufferCapacity = 64)
    val measurementsFlow: SharedFlow<Spo2Measurement> = _measurementsFlow.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    @SuppressLint("MissingPermission")
    fun connectDevice(address: String, name: String) {
        if (!BlePermissionHelper.hasBlePermissions(context)) {
            updateDeviceState(address, name, BleConnectionState.FAILED, errorMessage = "Bluetooth permission not granted.")
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            updateDeviceState(address, name, BleConnectionState.FAILED, errorMessage = "Bluetooth adapter is disabled.")
            return
        }

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            updateDeviceState(address, name, BleConnectionState.FAILED, errorMessage = "Invalid Bluetooth address: ${e.message}")
            return
        }

        updateDeviceState(address, name, BleConnectionState.CONNECTING)

        val decoder = Ap20PacketDecoder(deviceAddress = address, deviceName = name)
        decodersMap[address] = decoder

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    updateDeviceState(address, name, BleConnectionState.CONNECTED)
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    updateDeviceState(address, name, BleConnectionState.DISCONNECTED)
                    closeGattInternal(address)
                } else {
                    updateDeviceState(address, name, BleConnectionState.FAILED, errorMessage = "GATT connection failed with status code: $status")
                    closeGattInternal(address)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                    val service = gatt.getService(SERVICE_UUID)
                    if (service != null) {
                        val charFfb1 = service.getCharacteristic(CHARACTERISTIC_FFB1)
                        val charFfb2 = service.getCharacteristic(CHARACTERISTIC_FFB2)

                        val targetChar = charFfb2 ?: charFfb1
                        if (targetChar != null) {
                            enableCharacteristicNotification(gatt, targetChar)
                        } else {
                            updateDeviceState(address, name, BleConnectionState.CONNECTED, errorMessage = "AP-20 notification characteristic not found.")
                        }
                    } else {
                        // Inspect all services if standard AP-20 service UUID differs slightly
                        var subscribed = false
                        for (s in gatt.services) {
                            for (c in s.characteristics) {
                                if ((c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                    enableCharacteristicNotification(gatt, c)
                                    subscribed = true
                                    break
                                }
                            }
                            if (subscribed) break
                        }
                    }
                }
            }

            @Deprecated("Deprecated in API 33")
            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                characteristic?.value?.let { bytes ->
                    handleIncomingBytes(address, name, bytes)
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                handleIncomingBytes(address, name, value)
            }
        }

        val gatt = device.connectGatt(context, false, gattCallback)
        if (gatt != null) {
            activeGattMap[address] = gatt
        } else {
            updateDeviceState(address, name, BleConnectionState.FAILED, errorMessage = "Failed to establish GATT connection.")
        }
    }

    private fun handleIncomingBytes(address: String, name: String, bytes: ByteArray) {
        val decoder = decodersMap[address] ?: return
        val measurements = decoder.processData(bytes)
        for (m in measurements) {
            scope.launch {
                _measurementsFlow.emit(m)
            }
            updateLatestMeasurement(address, name, m)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableCharacteristicNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice(address: String) {
        val gatt = activeGattMap[address]
        if (gatt != null) {
            val currentState = _deviceStates.value[address]
            val deviceName = currentState?.name ?: "AP-20"
            updateDeviceState(address, deviceName, BleConnectionState.DISCONNECTING)
            gatt.disconnect()
        } else {
            closeGattInternal(address)
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGattInternal(address: String) {
        val gatt = activeGattMap.remove(address)
        try {
            gatt?.close()
        } catch (_: Exception) {}

        decodersMap.remove(address)?.clear()

        val currentState = _deviceStates.value[address]
        val deviceName = currentState?.name ?: "AP-20"
        updateDeviceState(address, deviceName, BleConnectionState.DISCONNECTED)
    }

    private fun updateDeviceState(
        address: String,
        name: String,
        connectionState: BleConnectionState,
        errorMessage: String? = null
    ) {
        val currentMap = _deviceStates.value.toMutableMap()
        val existing = currentMap[address]
        val updated = Ap20DeviceState(
            address = address,
            name = name,
            connectionState = connectionState,
            latestMeasurement = existing?.latestMeasurement,
            lastUpdateTime = existing?.lastUpdateTime ?: System.currentTimeMillis(),
            rssi = existing?.rssi ?: 0,
            errorMessage = errorMessage
        )
        currentMap[address] = updated
        _deviceStates.value = currentMap
    }

    private fun updateLatestMeasurement(address: String, name: String, measurement: Spo2Measurement) {
        val currentMap = _deviceStates.value.toMutableMap()
        val existing = currentMap[address]
        val updated = Ap20DeviceState(
            address = address,
            name = name,
            connectionState = BleConnectionState.CONNECTED,
            latestMeasurement = measurement,
            lastUpdateTime = System.currentTimeMillis(),
            rssi = existing?.rssi ?: 0,
            errorMessage = null
        )
        currentMap[address] = updated
        _deviceStates.value = currentMap
    }

    fun disconnectAll() {
        val addresses = activeGattMap.keys.toList()
        for (addr in addresses) {
            disconnectDevice(addr)
        }
    }
}
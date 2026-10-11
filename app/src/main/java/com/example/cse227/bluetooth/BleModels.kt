package com.example.cse227.bluetooth

enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    FAILED
}

data class DiscoveredBleDevice(
    val name: String,
    val address: String,
    val rssi: Int = 0,
    val isAp20Candidate: Boolean = false
)

data class Spo2Measurement(
    val deviceAddress: String,
    val deviceName: String,
    val spo2: Int,
    val pulseRate: Int,
    val perfusionIndex: Float? = null,
    val isPiVerified: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class Ap20DeviceState(
    val address: String,
    val name: String,
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val latestMeasurement: Spo2Measurement? = null,
    val lastUpdateTime: Long = 0L,
    val rssi: Int = 0,
    val errorMessage: String? = null
)
package com.example.cse227.bluetooth

import java.io.ByteArrayOutputStream

/**
 * Robust packet decoder for Lepu AP-20 BLE pulse oximeter stream.
 *
 * Protocol framing rules:
 * - Header signature: 0xAA 0x55
 * - Oxygen parameter packet length: 12 bytes
 * - Oxygen packet type ID: 0x08
 * - Waveform packet type ID: 0x07 (ignored for measurement recording)
 * - Byte 5: SpO2 (%)
 * - Byte 6: Pulse Rate (BPM)
 * - Byte 8: Unverified Perfusion Index raw value (kept null/unverified until scaling confirmed)
 */
class Ap20PacketDecoder(
    private val deviceAddress: String,
    private val deviceName: String
) {

    private val buffer = ByteArrayOutputStream()

    companion object {
        const val HEADER_BYTE_1: Byte = 0xAA.toByte()
        const val HEADER_BYTE_2: Byte = 0x55.toByte()
        const val PACKET_TYPE_OXYGEN: Byte = 0x08.toByte()
        const val PACKET_TYPE_WAVEFORM: Byte = 0x07.toByte()
        const val REQUIRED_PACKET_LENGTH = 12
        private const val MAX_BUFFER_SIZE = 1024
    }

    @Synchronized
    fun processData(data: ByteArray): List<Spo2Measurement> {
        val measurements = mutableListOf<Spo2Measurement>()
        if (data.isEmpty()) return measurements

        buffer.write(data)

        var bytes = buffer.toByteArray()

        while (bytes.size >= REQUIRED_PACKET_LENGTH) {
            // Find index of 0xAA 0x55 header
            var headerIndex = -1
            for (i in 0 until bytes.size - 1) {
                if (bytes[i] == HEADER_BYTE_1 && bytes[i + 1] == HEADER_BYTE_2) {
                    headerIndex = i
                    break
                }
            }

            if (headerIndex == -1) {
                // Header not found. Retain last byte in case 0xAA is at the end
                val lastByte = if (bytes.isNotEmpty() && bytes.last() == HEADER_BYTE_1) byteArrayOf(HEADER_BYTE_1) else byteArrayOf()
                buffer.reset()
                buffer.write(lastByte)
                break
            }

            // Discard any garbage bytes before headerIndex
            if (headerIndex > 0) {
                bytes = bytes.copyOfRange(headerIndex, bytes.size)
                buffer.reset()
                buffer.write(bytes)
            }

            if (bytes.size < REQUIRED_PACKET_LENGTH) {
                break
            }

            val packetType = bytes[3]

            if (packetType == PACKET_TYPE_OXYGEN) {
                val rawSpo2 = bytes[5].toInt() and 0xFF
                val rawPulseRate = bytes[6].toInt() and 0xFF
                val rawPi = bytes[8].toInt() and 0xFF

                if (isValidSpo2(rawSpo2) && isValidPulseRate(rawPulseRate)) {
                    val measurement = Spo2Measurement(
                        deviceAddress = deviceAddress,
                        deviceName = deviceName,
                        spo2 = rawSpo2,
                        pulseRate = rawPulseRate,
                        perfusionIndex = if (rawPi in 1..250) rawPi / 10.0f else null,
                        isPiVerified = false,
                        timestamp = System.currentTimeMillis()
                    )
                    measurements.add(measurement)
                }
            }

            // Advance buffer past this 12-byte packet
            val remainingLength = bytes.size - REQUIRED_PACKET_LENGTH
            if (remainingLength > 0) {
                bytes = bytes.copyOfRange(REQUIRED_PACKET_LENGTH, bytes.size)
                buffer.reset()
                buffer.write(bytes)
            } else {
                buffer.reset()
                break
            }
        }

        if (buffer.size() > MAX_BUFFER_SIZE) {
            buffer.reset()
        }

        return measurements
    }

    private fun isValidSpo2(spo2: Int): Boolean {
        return spo2 in 50..100
    }

    private fun isValidPulseRate(pulseRate: Int): Boolean {
        return pulseRate in 30..250
    }

    @Synchronized
    fun clear() {
        buffer.reset()
    }
}
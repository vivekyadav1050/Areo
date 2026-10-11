package com.example.cse227

import com.example.cse227.bluetooth.Ap20PacketDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Ap20PacketDecoderTest {

    private lateinit var decoder: Ap20PacketDecoder

    @Before
    fun setUp() {
        decoder = Ap20PacketDecoder(deviceAddress = "00:11:22:33:44:55", deviceName = "AP-20:00255")
    }

    @Test
    fun testDecodeKnownSamplePacket() {
        // Observed sample packet from Lepu AP-20 pulse oximeter:
        // AA 55 0F 08 01 62 52 00 0F 00 C0 72
        // Byte 5: 0x62 = 98 (SpO2)
        // Byte 6: 0x52 = 82 (Pulse Rate)
        val rawSample = byteArrayOf(
            0xAA.toByte(), 0x55.toByte(), 0x0F.toByte(), 0x08.toByte(),
            0x01.toByte(), 0x62.toByte(), 0x52.toByte(), 0x00.toByte(),
            0x0F.toByte(), 0x00.toByte(), 0xC0.toByte(), 0x72.toByte()
        )

        val measurements = decoder.processData(rawSample)

        assertEquals(1, measurements.size)
        val m = measurements.first()
        assertEquals(98, m.spo2)
        assertEquals(82, m.pulseRate)
        assertEquals("00:11:22:33:44:55", m.deviceAddress)
        assertEquals("AP-20:00255", m.deviceName)
    }

    @Test
    fun testDecodeConcatenatedPackets() {
        // Two consecutive 12-byte parameter packets in one BLE callback payload
        val packet1 = byteArrayOf(
            0xAA.toByte(), 0x55.toByte(), 0x0F.toByte(), 0x08.toByte(),
            0x01.toByte(), 0x62.toByte(), 0x50.toByte(), 0x00.toByte(),
            0x0F.toByte(), 0x00.toByte(), 0xC0.toByte(), 0x72.toByte()
        )
        val packet2 = byteArrayOf(
            0xAA.toByte(), 0x55.toByte(), 0x0F.toByte(), 0x08.toByte(),
            0x01.toByte(), 0x63.toByte(), 0x51.toByte(), 0x00.toByte(),
            0x0F.toByte(), 0x00.toByte(), 0xC0.toByte(), 0x72.toByte()
        )

        val concatenated = packet1 + packet2
        val measurements = decoder.processData(concatenated)

        assertEquals(2, measurements.size)
        assertEquals(98, measurements[0].spo2)
        assertEquals(80, measurements[0].pulseRate)
        assertEquals(99, measurements[1].spo2)
        assertEquals(81, measurements[1].pulseRate)
    }

    @Test
    fun testDecodeSplitFrameAcrossNotifications() {
        val fullPacket = byteArrayOf(
            0xAA.toByte(), 0x55.toByte(), 0x0F.toByte(), 0x08.toByte(),
            0x01.toByte(), 0x61.toByte(), 0x4B.toByte(), 0x00.toByte(),
            0x0F.toByte(), 0x00.toByte(), 0xC0.toByte(), 0x72.toByte()
        )

        val chunk1 = fullPacket.copyOfRange(0, 5)
        val chunk2 = fullPacket.copyOfRange(5, fullPacket.size)

        val res1 = decoder.processData(chunk1)
        assertTrue(res1.isEmpty())

        val res2 = decoder.processData(chunk2)
        assertEquals(1, res2.size)
        assertEquals(97, res2[0].spo2)
        assertEquals(75, res2[0].pulseRate)
    }

    @Test
    fun testWaveformPacketsAreIgnored() {
        // Waveform packet type 0x07
        val waveformPacket = byteArrayOf(
            0xAA.toByte(), 0x55.toByte(), 0x0F.toByte(), 0x07.toByte(),
            0x10.toByte(), 0x20.toByte(), 0x30.toByte(), 0x40.toByte(),
            0x50.toByte(), 0x60.toByte(), 0x70.toByte(), 0x80.toByte()
        )

        val measurements = decoder.processData(waveformPacket)
        assertTrue(measurements.isEmpty())
    }

    @Test
    fun testInvalidValuesAreFiltered() {
        // Packet with implausible SpO2 value (e.g. 10%)
        val invalidPacket = byteArrayOf(
            0xAA.toByte(), 0x55.toByte(), 0x0F.toByte(), 0x08.toByte(),
            0x01.toByte(), 0x0A.toByte(), 0x50.toByte(), 0x00.toByte(),
            0x0F.toByte(), 0x00.toByte(), 0xC0.toByte(), 0x72.toByte()
        )

        val measurements = decoder.processData(invalidPacket)
        assertTrue(measurements.isEmpty())
    }

    @Test
    fun testGarbagePrefixRecovery() {
        val garbagePrefix = byteArrayOf(0x01.toByte(), 0x02.toByte(), 0xFF.toByte())
        val validPacket = byteArrayOf(
            0xAA.toByte(), 0x55.toByte(), 0x0F.toByte(), 0x08.toByte(),
            0x01.toByte(), 0x62.toByte(), 0x52.toByte(), 0x00.toByte(),
            0x0F.toByte(), 0x00.toByte(), 0xC0.toByte(), 0x72.toByte()
        )

        val payload = garbagePrefix + validPacket
        val measurements = decoder.processData(payload)

        assertEquals(1, measurements.size)
        assertEquals(98, measurements[0].spo2)
        assertEquals(82, measurements[0].pulseRate)
    }
}
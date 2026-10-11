package com.example.cse227.data.model

import com.google.gson.annotations.SerializedName

data class ReadingItem(
    @SerializedName("recordId") val recordId: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("spo2") val spo2: Int,
    @SerializedName("pulseRate") val pulseRate: Int,
    @SerializedName("perfusionIndex") val perfusionIndex: Float? = null,
    @SerializedName("batteryLevel") val batteryLevel: Int? = null,
    @SerializedName("probeStatus") val probeStatus: String? = "unknown"
)

data class UploadReadingsRequest(
    @SerializedName("tabletId") val tabletId: String,
    @SerializedName("readings") val readings: List<ReadingItem>
)

data class UploadReadingsResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("inserted") val inserted: Int = 0,
    @SerializedName("duplicates") val duplicates: Int = 0
)
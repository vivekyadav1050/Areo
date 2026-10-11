package com.example.cse227.data.model

import com.google.gson.annotations.SerializedName

data class RegisterTabletRequest(
    @SerializedName("deviceKey") val deviceKey: String
)

data class RegisterTabletResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("tabletId") val tabletId: String? = null,
    @SerializedName("message") val message: String? = null
)

data class ActivePatient(
    @SerializedName("patientId") val patientId: String?,
    @SerializedName("_id") val mongoId: String?,
    @SerializedName("patientName") val patientName: String?,
    @SerializedName("age") val age: Int?,
    @SerializedName("sex") val sex: String?,
    @SerializedName("assignmentDate") val assignmentDate: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("isActive") val isActive: Boolean? = true
) {
    val displayId: String
        get() = patientId ?: mongoId ?: "N/A"

    val displayName: String
        get() = patientName ?: "Unassigned Patient"

    val displayDate: String
        get() = assignmentDate ?: date ?: "N/A"
}

data class TabletInfoResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("tabletId") val tabletId: String? = null,
    @SerializedName("_id") val mongoTabletId: String? = null,
    @SerializedName("deviceKey") val deviceKey: String? = null,
    @SerializedName("activePatient") val activePatient: ActivePatient? = null,
    @SerializedName("patient") val patient: ActivePatient? = null,
    @SerializedName("message") val message: String? = null
) {
    val effectiveTabletId: String?
        get() = tabletId ?: mongoTabletId

    val effectivePatient: ActivePatient?
        get() = activePatient ?: patient
}
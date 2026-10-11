package com.example.cse227.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class UserDto(
    @SerializedName("id") val id: String?,
    @SerializedName("_id") val mongoId: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("role") val role: String?
) {
    val effectiveId: String
        get() = id ?: mongoId ?: "unknown_id"

    val effectiveUsername: String
        get() = username ?: "Unknown"

    val effectiveRole: String
        get() = role ?: "tablet"
}

data class LoginResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: UserDto? = null,
    @SerializedName("message") val message: String? = null
)
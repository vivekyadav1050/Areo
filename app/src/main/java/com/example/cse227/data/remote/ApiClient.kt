package com.example.cse227.data.remote

import com.example.cse227.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient private constructor(sessionManager: SessionManager) {

    private val authInterceptor = AuthInterceptor(sessionManager)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var retrofit: Retrofit = buildRetrofit(sessionManager.getBaseUrl())

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun updateBaseUrl(newBaseUrl: String) {
        retrofit = buildRetrofit(newBaseUrl)
    }

    val authApi: AuthApi get() = retrofit.create(AuthApi::class.java)
    val tabletApi: TabletApi get() = retrofit.create(TabletApi::class.java)
    val patientApi: PatientApi get() = retrofit.create(PatientApi::class.java)
    val monitoringApi: MonitoringApi get() = retrofit.create(MonitoringApi::class.java)

    companion object {
        @Volatile
        private var INSTANCE: ApiClient? = null

        fun getInstance(sessionManager: SessionManager): ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiClient(sessionManager).also { INSTANCE = it }
            }
        }
    }
}
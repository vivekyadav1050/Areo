package com.example.cse227

import android.app.Application
import com.example.cse227.bluetooth.Ap20BleManager
import com.example.cse227.bluetooth.BleDeviceScanner
import com.example.cse227.data.local.AppDatabase
import com.example.cse227.data.local.SessionManager
import com.example.cse227.data.remote.ApiClient
import com.example.cse227.data.repository.AuthRepository
import com.example.cse227.data.repository.MonitoringRepository
import com.example.cse227.data.repository.PatientRepository
import com.example.cse227.data.repository.TabletRepository

class Ap20Application : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var apiClient: ApiClient
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var tabletRepository: TabletRepository
        private set

    lateinit var patientRepository: PatientRepository
        private set

    lateinit var monitoringRepository: MonitoringRepository
        private set

    lateinit var bleManager: Ap20BleManager
        private set

    lateinit var bleScanner: BleDeviceScanner
        private set

    override fun onCreate() {
        super.onCreate()

        sessionManager = SessionManager(this)
        apiClient = ApiClient.getInstance(sessionManager)
        database = AppDatabase.getInstance(this)

        authRepository = AuthRepository(apiClient, sessionManager)
        tabletRepository = TabletRepository(this, apiClient, sessionManager)
        patientRepository = PatientRepository(apiClient, sessionManager)
        monitoringRepository = MonitoringRepository(database.readingDao, apiClient, sessionManager)

        bleManager = Ap20BleManager(this)
        bleScanner = BleDeviceScanner(this)
    }
}
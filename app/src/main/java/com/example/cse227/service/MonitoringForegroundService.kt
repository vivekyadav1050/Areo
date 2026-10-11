package com.example.cse227.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.cse227.Ap20Application
import com.example.cse227.MainActivity
import com.example.cse227.data.sync.SyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MonitoringForegroundService : Service() {

    private var syncCoordinator: SyncCoordinator? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    companion object {
        const val CHANNEL_ID = "ap20_monitoring_service_channel"
        const val NOTIFICATION_ID = 22701

        const val ACTION_START_MONITORING = "com.example.cse227.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.cse227.action.STOP_MONITORING"

        fun startMonitoringService(context: Context) {
            val intent = Intent(context, MonitoringForegroundService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopMonitoringService(context: Context) {
            val intent = Intent(context, MonitoringForegroundService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val app = application as? Ap20Application
        if (app != null) {
            syncCoordinator = SyncCoordinator(
                monitoringRepository = app.monitoringRepository,
                bleManager = app.bleManager
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                stopMonitoringInternal()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_MONITORING, null -> {
                startMonitoringInternal()
            }
        }
        return START_STICKY
    }

    private fun startMonitoringInternal() {
        val initialNotification = buildNotification("Monitoring active", "Awaiting AP-20 live readings...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            try {
                startForeground(NOTIFICATION_ID, initialNotification, type)
            } catch (_: SecurityException) {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        syncCoordinator?.startSyncCoordinator()

        val app = application as? Ap20Application ?: return
        serviceJob?.cancel()
        serviceJob = scope.launch {
            app.bleManager.measurementsFlow.collectLatest { measurement ->
                val title = "AP-20 Live: ${measurement.deviceName}"
                val content = "SpO₂: ${measurement.spo2}%  |  Heart Rate: ${measurement.pulseRate} BPM"
                updateNotification(title, content)
            }
        }
    }

    private fun stopMonitoringInternal() {
        serviceJob?.cancel()
        syncCoordinator?.stopSyncCoordinator()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AP-20 Patient Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live pulse oximeter measurements and backend synchronization status."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    override fun onDestroy() {
        stopMonitoringInternal()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
package com.example.cse227.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cse227.Ap20Application
import com.example.cse227.data.repository.SyncResult
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val app = applicationContext as? Ap20Application ?: return androidx.work.ListenableWorker.Result.failure()
        val repository = app.monitoringRepository

        return when (repository.uploadPendingBatch()) {
            is SyncResult.Success -> androidx.work.ListenableWorker.Result.success()
            is SyncResult.NoData -> androidx.work.ListenableWorker.Result.success()
            is SyncResult.Error -> androidx.work.ListenableWorker.Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "ap20_monitoring_batch_sync_work"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
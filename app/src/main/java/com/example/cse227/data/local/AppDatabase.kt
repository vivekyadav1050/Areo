package com.example.cse227.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    val readingDao: ReadingDao = ReadingDao(this)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS oximeter_readings (
                recordId TEXT PRIMARY KEY,
                tabletId TEXT NOT NULL,
                patientAssignmentId TEXT,
                deviceId TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                spo2 INTEGER NOT NULL,
                pulseRate INTEGER NOT NULL,
                perfusionIndex REAL,
                batteryLevel INTEGER,
                probeStatus TEXT,
                syncStatus TEXT NOT NULL,
                retryCount INTEGER NOT NULL,
                lastError TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS oximeter_readings")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "ap20_patient_monitor.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

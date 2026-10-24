package com.anemoventi.ranbing

import android.app.Application
import android.util.Log
import com.anemoventi.ranbing.data.AppSettings
import com.anemoventi.ranbing.device.DeviceRepository
import java.io.File

class App : Application() {

    lateinit var settings: AppSettings
        private set

    lateinit var device: DeviceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        settings = AppSettings(this)
        device = DeviceRepository(this)
    }

    /**
     * Keeps the last uncaught exception on disk so a crash can be diagnosed after the process
     * restarts. The platform handler still runs afterwards; this only adds the post-mortem trail.
     */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                File(filesDir, "last_crash.txt").writeText(
                    buildString {
                        appendLine("time=${System.currentTimeMillis()}")
                        appendLine("thread=${thread.name}")
                        appendLine(Log.getStackTraceString(error))
                    },
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }
}

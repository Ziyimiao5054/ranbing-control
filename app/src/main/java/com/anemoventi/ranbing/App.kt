package com.anemoventi.ranbing

import android.app.Application
import com.anemoventi.ranbing.data.AppSettings
import com.anemoventi.ranbing.device.DeviceRepository

class App : Application() {

    lateinit var settings: AppSettings
        private set

    lateinit var device: DeviceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings(this)
        device = DeviceRepository(this)
    }
}

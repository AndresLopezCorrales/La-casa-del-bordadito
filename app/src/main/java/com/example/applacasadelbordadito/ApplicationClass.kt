package com.example.applacasadelbordadito

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ApplicationClass : Application() {

    private val ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID

    override fun onCreate() {
        super.onCreate()

        // Quitar VERBOSE en producción
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }
    }
}
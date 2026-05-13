package com.example.applacasadelbordadito

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ApplicationClass : Application() {

    private val ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID
    private var startedActivities = 0
    private var isChangingConfig = false

    override fun onCreate() {
        super.onCreate()

        // Quitar VERBOSE en producción
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                if (startedActivities == 0 && !isChangingConfig) {
                    // App vuelve al primer plano
                    actualizarEstadoOnline(true)
                }
                startedActivities++
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                isChangingConfig = activity.isChangingConfigurations
                startedActivities--
                if (startedActivities == 0 && !isChangingConfig) {
                    // App pasa a segundo plano o se cierra
                    actualizarEstadoOnline(false)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        fun actualizarEstadoOnline(online: Boolean) {
            val uid = FirebaseAuth.getInstance().uid ?: return
            val ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)

            if (online) {
                ref.child("online").setValue(true)
                ref.child("online").onDisconnect().setValue(false)
            } else {
                ref.child("online").setValue(false)
            }
        }
    }
}

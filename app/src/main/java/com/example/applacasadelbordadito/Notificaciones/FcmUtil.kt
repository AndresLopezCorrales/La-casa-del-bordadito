package com.example.applacasadelbordadito.notificaciones

import android.util.Log
import com.example.applacasadelbordadito.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object FcmUtil {

    private val ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID
    private val ONESIGNAL_API_KEY = BuildConfig.ONESIGNAL_API_KEY

    fun enviarNotificacionATodos(titulo: String, mensaje: String) {
        val body = JSONObject().apply {
            put("app_id", ONESIGNAL_APP_ID)
            put("included_segments", JSONArray().put("All"))
            put("headings", JSONObject()
                .put("en", titulo)
                .put("es", titulo))
            put("contents", JSONObject()
                .put("en", mensaje)
                .put("es", mensaje))
            put("large_icon", "logo_app")
            put("small_icon", "ic_taller")
        }

        val request = Request.Builder()
            .url("https://api.onesignal.com/notifications")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Key $ONESIGNAL_API_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OneSignal", "Error: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("OneSignal", "Código: ${response.code} | Body: $responseBody")
                response.close()
            }
        })
    }

    fun enviarNotificacionAUsuario(receptorUid: String, titulo: String, mensaje: String, imagenPerfil: String) {
        val body = JSONObject().apply {
            put("app_id", ONESIGNAL_APP_ID)
            put("include_aliases", JSONObject().put("external_id", JSONArray().put(receptorUid)))
            put("target_channel", "push")

            put("headings", JSONObject()
                .put("en", titulo)
                .put("es", titulo))
            put("contents", JSONObject()
                .put("en", mensaje)
                .put("es", mensaje))

            if (imagenPerfil.isNotEmpty() && imagenPerfil != "null") {
                put("large_icon", imagenPerfil)
            } else {
                put("large_icon", "logo_app")
            }

            put("small_icon", "ic_bell")
            put("android_sound", "default")
            put("priority", 10)
        }

        val request = Request.Builder()
            .url("https://api.onesignal.com/notifications")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Key $ONESIGNAL_API_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OneSignal", "Error PUSH: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("OneSignal", "Código: ${response.code} | Body: $responseBody")
                response.close()
            }
        })
    }
}

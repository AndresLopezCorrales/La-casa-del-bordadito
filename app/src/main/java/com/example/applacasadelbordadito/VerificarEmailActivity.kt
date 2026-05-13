package com.example.applacasadelbordadito

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.databinding.ActivityVerificarEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.util.Log

class VerificarEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificarEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private val EMAILJS_SERVICE_ID = BuildConfig.EMAILJS_SERVICE_ID
    private val EMAILJS_TEMPLATE_ID = BuildConfig.EMAILJS_TEMPLATE_ID
    private val EMAILJS_PUBLIC_KEY = BuildConfig.EMAILJS_PUBLIC_KEY
    private val EMAILJS_PRIVATE_KEY = BuildConfig.EMAILJS_PRIVATE_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificarEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.BtnReenviar.setOnClickListener {
            enviarCorreoVerificacion()
        }

        iniciarPolling()
    }

    private fun enviarCorreoVerificacion() {
        firebaseAuth.currentUser?.sendEmailVerification()
            ?.addOnSuccessListener {
                Toast.makeText(this, "Correo de verificación reenviado", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun iniciarPolling() {
        runnable = Runnable {
            firebaseAuth.currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null && user.isEmailVerified) {
                        Toast.makeText(this, "¡Cuenta verificada!", Toast.LENGTH_SHORT).show()
                        llenarInfoBD()
                    } else {
                        // Seguir intentando cada 3 segundos
                        handler.postDelayed(runnable, 3000)
                    }
                }
            }
        }
        handler.postDelayed(runnable, 3000)
    }

    private fun llenarInfoBD() {
        val tiempo = Constantes.obtenerTiempoDis()
        val user = firebaseAuth.currentUser
        val emailUsuario = user?.email ?: ""
        val uidUsuario = user?.uid
        val nombrePorDefecto = emailUsuario.substringBefore("@")

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = nombrePorDefecto
        hashMap["codigoTelefono"] = ""
        hashMap["telefono"] = ""
        hashMap["urlImagenPerfil"] = ""
        hashMap["proveedor"] = "Email"
        hashMap["escribiendo"] = ""
        hashMap["tiempo"] = tiempo
        hashMap["online"] = true
        hashMap["email"] = emailUsuario
        hashMap["uid"] = "${uidUsuario}"
        hashMap["fecha_nac"] = ""
        hashMap["esAdmin"] = false
        hashMap["esSoporte"] = false

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidUsuario!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                com.onesignal.OneSignal.login(uidUsuario)
                enviarCorreoBienvenida(emailUsuario ?: "")
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al guardar información: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enviarCorreoBienvenida(emailUsuario: String) {
        if (emailUsuario.isEmpty()) return

        val nombreMostrar = emailUsuario.substringBefore("@")
        val client = OkHttpClient()

        val templateParams = JSONObject().apply {
            put("to_email", emailUsuario)
            put("to_name", nombreMostrar)
        }

        val jsonBody = JSONObject().apply {
            put("service_id", EMAILJS_SERVICE_ID)
            put("template_id", EMAILJS_TEMPLATE_ID)
            put("user_id", EMAILJS_PUBLIC_KEY)
            put("accessToken", EMAILJS_PRIVATE_KEY)
            put("template_params", templateParams)
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.emailjs.com/api/v1.0/email/send")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EmailJS", "Error conexión: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseCode = response.code
                val body = response.body?.string()
                if (response.isSuccessful) {
                    Log.d("EmailJS", "Correo enviado con éxito a: $emailUsuario")
                } else {
                    Log.e("EmailJS", "Error de la API ($responseCode): $body")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}
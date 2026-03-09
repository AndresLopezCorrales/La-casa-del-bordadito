package com.example.applacasadelbordadito

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.databinding.ActivityRegistroEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class Registro_email : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private val EMAILJS_SERVICE_ID = BuildConfig.EMAILJS_SERVICE_ID
    private val EMAILJS_TEMPLATE_ID = BuildConfig.EMAILJS_TEMPLATE_ID
    private val EMAILJS_PUBLIC_KEY = BuildConfig.EMAILJS_PUBLIC_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.BtnRegistrar.setOnClickListener {
            validarInfo()
        }
    }

    private var email = ""
    private var password = ""
    private var r_password = ""

    private fun validarInfo() {
        email = binding.EtEmail.text.toString().trim()
        password = binding.EtPassword.text.toString().trim()
        r_password = binding.EtRPassword.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.EtEmail.error = "Email inválido"
            binding.EtEmail.requestFocus()
        } else if (email.isEmpty()) {
            binding.EtEmail.error = "Ingrese un email"
            binding.EtEmail.requestFocus()
        } else if (password.isEmpty()) {
            binding.EtPassword.error = "Ingrese el password"
            binding.EtPassword.requestFocus()
        } else if (r_password.isEmpty()) {
            binding.EtRPassword.error = "Repita el password"
            binding.EtRPassword.requestFocus()
        } else if (password != r_password) {
            binding.EtRPassword.error = "No coinciden"
            binding.EtRPassword.requestFocus()
        } else {
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        progressDialog.setMessage("Creando Cuenta")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                llenarInfoBD()
            }
            .addOnFailureListener { exception ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se registró el usuario debido a ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun llenarInfoBD() {
        progressDialog.setMessage("Guardando información")

        val tiempo = Constantes.obtenerTiempoDis()
        val emailUsuario = firebaseAuth.currentUser!!.email
        val uidUsuario = firebaseAuth.uid

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = ""
        hashMap["codigoTelefono"] = ""
        hashMap["telefono"] = ""
        hashMap["urlImagenPerfil"] = ""
        hashMap["proveedor"] = "Email"
        hashMap["escribiendo"] = ""
        hashMap["tiempo"] = tiempo
        hashMap["online"] = true
        hashMap["email"] = "${emailUsuario}"
        hashMap["uid"] = "${uidUsuario}"
        hashMap["fecha_nac"] = ""

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidUsuario!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                enviarCorreoBienvenida(emailUsuario ?: "")
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { exception ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se registró debido a ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
            put("template_params", templateParams)
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.emailjs.com/api/v1.0/email/send")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("origin", "http://localhost")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Error de conexión: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful) {
                    println("Correo enviado a: $emailUsuario")
                } else {
                    println("Error ${response.code}: $body")
                }
            }
        })
    }
}
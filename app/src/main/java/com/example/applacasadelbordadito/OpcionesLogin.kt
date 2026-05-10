package com.example.applacasadelbordadito

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.Opciones_Login.Login_email
import com.example.applacasadelbordadito.databinding.ActivityOpcionesLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class OpcionesLogin : AppCompatActivity() {

    private lateinit var binding: ActivityOpcionesLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var progressDialog: ProgressDialog

    private val EMAILJS_SERVICE_ID = BuildConfig.EMAILJS_SERVICE_ID
    private val EMAILJS_TEMPLATE_ID = BuildConfig.EMAILJS_TEMPLATE_ID
    private val EMAILJS_PUBLIC_KEY = BuildConfig.EMAILJS_PUBLIC_KEY
    private val EMAILJS_PRIVATE_KEY = BuildConfig.EMAILJS_PRIVATE_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpcionesLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espera por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.IngresarGoogle.setOnClickListener {
            googleLogin()
        }

        binding.IngresarEmail.setOnClickListener {
            startActivity(Intent(this@OpcionesLogin, Login_email::class.java))
        }
    }

    private fun googleLogin() {
        val googleSignInIntent = mGoogleSignInClient.signInIntent
        googleSignInARL.launch(googleSignInIntent)
    }

    private val googleSignInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val data = resultado.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val cuenta = task.getResult(ApiException::class.java)
                autenticacionGoogle(cuenta.idToken)
            } catch (e: Exception) {
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun autenticacionGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { resultadoAuth ->
                if (resultadoAuth.additionalUserInfo!!.isNewUser) {
                    llenarInfoBD()
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun llenarInfoBD() {
        progressDialog.setMessage("Guardando información")

        val tiempo = Constantes.obtenerTiempoDis()
        val emailUsuario = firebaseAuth.currentUser!!.email
        val uidUsuario = firebaseAuth.uid
        val nombreUsuario = firebaseAuth.currentUser?.displayName

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = "${nombreUsuario}"
        hashMap["codigoTelefono"] = ""
        hashMap["telefono"] = ""
        hashMap["urlImagenPerfil"] = ""
        hashMap["proveedor"] = "Google"
        hashMap["escribiendo"] = ""
        hashMap["tiempo"] = tiempo
        hashMap["online"] = true
        hashMap["email"] = "${emailUsuario}"
        hashMap["uid"] = "${uidUsuario}"
        hashMap["fecha_nac"] = ""
        hashMap["esAdmin"] = false

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidUsuario!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                enviarCorreoBienvenida(
                    emailUsuario = emailUsuario ?: "",
                    nombreUsuario = nombreUsuario ?: emailUsuario?.substringBefore("@") ?: ""
                ) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
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

    private fun enviarCorreoBienvenida(
        emailUsuario: String,
        nombreUsuario: String,
        onDone: () -> Unit = {}
    ) {
        if (emailUsuario.isEmpty()) {
            Log.e("EmailJS", "❌ Email vacío, saliendo")
            onDone()
            return
        }

        val client = OkHttpClient()

        val templateParams = JSONObject().apply {
            put("to_email", emailUsuario)
            put("to_name", nombreUsuario)
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
                Log.e("EmailJS", "❌ Error conexión: ${e.message}")
                runOnUiThread { onDone() }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful) {
                    Log.d("EmailJS", "✅ Correo enviado a: $emailUsuario")
                } else {
                    Log.e("EmailJS", "❌ Error ${response.code}: $body")
                }
                runOnUiThread { onDone() }
            }
        })
    }

    private fun comprobarSesion() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            if (user.providerData.any { it.providerId == "password" } && !user.isEmailVerified) {
                firebaseAuth.signOut()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
        }
    }
}
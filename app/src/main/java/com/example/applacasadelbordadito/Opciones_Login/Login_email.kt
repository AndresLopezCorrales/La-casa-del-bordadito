package com.example.applacasadelbordadito.Opciones_Login

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.MainActivity
import com.example.applacasadelbordadito.Registro_email
import com.example.applacasadelbordadito.VerificarEmailActivity
import com.example.applacasadelbordadito.databinding.ActivityLoginEmailBinding
import com.google.firebase.auth.FirebaseAuth

class Login_email : AppCompatActivity() {

    private lateinit var binding: ActivityLoginEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere porfavor")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.BtnIngresar.setOnClickListener {
            validarInfo()
        }

        binding.TxtRegistrarme.setOnClickListener{
            startActivity(Intent(this@Login_email, Registro_email::class.java))
        }
    }

    private var email = ""
    private var password = ""

    private fun validarInfo() {
        email = binding.EtEmail.text.toString().trim()
        password = binding.EtPassword.text.toString().trim()

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.EtEmail.error = "Email inválido"
            binding.EtEmail.requestFocus()
        }
        else if (email.isEmpty()){
            binding.EtEmail.error = "Ingrese un email"
            binding.EtEmail.requestFocus()
        }
        else if (password.isEmpty()){
            binding.EtPassword.error = "Ingrese el password"
            binding.EtPassword.requestFocus()
        }
        else{
            loginUsuario()
        }

    }

    private fun loginUsuario() {
        progressDialog.setMessage("Ingresando")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = firebaseAuth.currentUser
                if (user != null && user.isEmailVerified) {
                    // Verificamos si existe en la base de datos
                    comprobarDatosUsuario(user.uid)
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Debes verificar tu correo antes de iniciar sesión", Toast.LENGTH_LONG).show()
                    // Opcional: Redirigir a la pantalla de verificación
                    val intent = Intent(this, VerificarEmailActivity::class.java)
                    startActivity(intent)
                    firebaseAuth.signOut()
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this,
                    "No se pudo iniciar sesión debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

            }

    }

    private fun comprobarDatosUsuario(uid: String) {
        val ref = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uid).get().addOnSuccessListener { snapshot ->
            progressDialog.dismiss()
            if (snapshot.exists()) {
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
                Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
            } else {
                // Caso borde: está verificado en Auth pero no tiene datos en DB
                // Podriamos mandarlo a llenar datos o crear perfil basico
                llenarInfoBDBasica(uid)
            }
        }.addOnFailureListener {
            progressDialog.dismiss()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }

    private fun llenarInfoBDBasica(uid: String) {
        val tiempo = com.example.applacasadelbordadito.Constantes.obtenerTiempoDis()
        val emailUsuario = firebaseAuth.currentUser?.email

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
        hashMap["uid"] = "${uid}"
        hashMap["fecha_nac"] = ""
        hashMap["esAdmin"] = false

        val ref = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uid).setValue(hashMap).addOnSuccessListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}
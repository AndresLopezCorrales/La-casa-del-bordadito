package com.example.applacasadelbordadito.Perfil

import android.app.ProgressDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.databinding.ActivityTarjetaAgregarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TarjetaAgregarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTarjetaAgregarBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTarjetaAgregarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this).apply {
            setTitle("Por favor espere")
            setCanceledOnTouchOutside(false)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Formato automático MM/AA mientras el usuario escribe
        binding.etExpiry.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) return
                isFormatting = true
                val digits = s.toString().filter { it.isDigit() }
                val formatted = when {
                    digits.length >= 3 -> "${digits.substring(0, 2)}/${digits.substring(2, minOf(4, digits.length))}"
                    digits.length == 2 && before == 0 -> "$digits/"
                    else -> digits
                }
                binding.etExpiry.setText(formatted)
                binding.etExpiry.setSelection(formatted.length)
                isFormatting = false
            }
        })

        binding.btnGuardar.setOnClickListener {
            validarDatos()
        }
    }

    private fun validarDatos() {
        val holder = binding.etCardHolder.text.toString().trim()
        val expiry = binding.etExpiry.text.toString().trim()
        val cvv    = binding.etCVV.text.toString().trim()

        // Validaciones
        when {
            !binding.editCreditView.isCardValid -> {
                Toast.makeText(this, "Número de tarjeta inválido", Toast.LENGTH_SHORT).show()
            }
            holder.isEmpty() -> {
                binding.etCardHolder.error = "Ingresa el nombre del titular"
            }
            !expiry.matches(Regex("^(0[1-9]|1[0-2])/\\d{2}$")) -> {
                binding.etExpiry.error = "Formato inválido (MM/AA)"
            }
            cvv.length != 3 -> {
                binding.etCVV.error = "El CVV debe ser de 3 dígitos"
            }
            else -> {
                val brand  = binding.editCreditView.cardType.toString()
                val rawNum = binding.editCreditView.textWithoutSeparator ?: ""
                val last4  = rawNum.takeLast(4)
                guardarTarjetaBD(brand, last4, holder, expiry)
            }
        }
    }

    private fun guardarTarjetaBD(brand: String, last4: String, holder: String, expiry: String) {
        progressDialog.setMessage("Guardando tarjeta...")
        progressDialog.show()

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
            .child(firebaseAuth.uid!!).child("metodosPago")

        val id = ref.push().key ?: return

        // Consultar cuántas tarjetas tiene el usuario para asignar isDefault
        ref.get().addOnSuccessListener { snapshot ->
            val isDefault = !snapshot.exists() || snapshot.childrenCount == 0L
            val method = PaymentMethod(id, last4, brand, holder, expiry, isDefault)

            ref.child(id).setValue(method)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Tarjeta agregada", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
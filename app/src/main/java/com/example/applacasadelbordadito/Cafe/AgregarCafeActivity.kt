package com.example.applacasadelbordadito.Cafe

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.R
import com.example.applacasadelbordadito.databinding.ActivityAgregarCafeBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AgregarCafeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgregarCafeBinding
    private lateinit var progressDialog: ProgressDialog
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarCafeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Configurar Dropdown de Categorías
        setupCategoriasDropdown()

        binding.btnSeleccionarImagen.setOnClickListener {
            seleccionarImagen()
        }

        binding.btnEliminarImagen.setOnClickListener {
            quitarImagenSeleccionada()
        }

        binding.btnGuardar.setOnClickListener {
            validarDatos()
        }
    }

    private fun setupCategoriasDropdown() {
        val categorias = arrayOf("Calientes", "Fríos", "Especiales")
        val adapter = ArrayAdapter(this, R.layout.list_item_dropdown, categorias)
        binding.etCategoria.setAdapter(adapter)
    }

    private fun seleccionarImagen() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleria.launch(intent)
    }

    private val resultadoGaleria = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            imageUri = resultado.data?.data
            if (imageUri != null) {
                binding.ivFotoCafe.setImageURI(imageUri)
                // Quitamos el tinte y el padding para que la foto se vea bien
                binding.ivFotoCafe.imageTintList = null
                binding.ivFotoCafe.setPadding(0, 0, 0, 0)
                binding.btnEliminarImagen.visibility = View.VISIBLE
            }
        }
    }
    
    private fun quitarImagenSeleccionada() {
        imageUri = null
        binding.ivFotoCafe.setImageResource(R.drawable.ic_cafe)

        // Restauramos el padding original (64dp)
        val paddingPx = (64 * resources.displayMetrics.density).toInt()
        binding.ivFotoCafe.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

        binding.btnEliminarImagen.visibility = View.GONE
    }

    private fun validarDatos() {
        val nombre = binding.etNombre.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()
        val categoria = binding.etCategoria.text.toString().trim()
        val pChico = binding.etPrecioChico.text.toString().toDoubleOrNull() ?: 0.0
        val pMediano = binding.etPrecioMediano.text.toString().toDoubleOrNull() ?: 0.0
        val pGrande = binding.etPrecioGrande.text.toString().toDoubleOrNull() ?: 0.0

        if (nombre.isEmpty()) {
            binding.etNombre.error = "Ingrese nombre"
        } else if (descripcion.isEmpty()) {
            binding.etDescripcion.error = "Ingrese descripción"
        } else if (categoria.isEmpty()) {
            binding.etCategoria.error = "Seleccione categoría"
        } else if (imageUri == null) {
            Toast.makeText(this, "Seleccione una imagen", Toast.LENGTH_SHORT).show()
        } else if (pChico <= 0 && pMediano <= 0 && pGrande <= 0) {
            Toast.makeText(this, "Ingrese al menos un precio", Toast.LENGTH_SHORT).show()
        } else {
            subirImagen(nombre, descripcion, categoria, pChico, pMediano, pGrande)
        }
    }

    private fun subirImagen(nombre: String, desc: String, cat: String, pChico: Double, pMediano: Double, pGrande: Double) {
        progressDialog.setMessage("Subiendo imagen...")
        progressDialog.show()

        val timestamp = System.currentTimeMillis()
        val rutaImagen = "Cafes/foto_cafe_$timestamp"
        val storageRef = FirebaseStorage.getInstance().getReference(rutaImagen)

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val urlImagen = uriTask.result.toString()
                
                guardarEnFirestore(nombre, desc, cat, pChico, pMediano, pGrande, urlImagen)
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarEnFirestore(nombre: String, desc: String, cat: String, pChico: Double, pMediano: Double, pGrande: Double, url: String) {
        progressDialog.setMessage("Guardando café...")

        val precios = mutableMapOf<String, Double>()
        if (pChico > 0) precios["Chico"] = pChico
        if (pMediano > 0) precios["Mediano"] = pMediano
        if (pGrande > 0) precios["Grande"] = pGrande

        val cafe = hashMapOf(
            "nombre" to nombre,
            "descripcion" to desc,
            "categoria" to cat,
            "imagenUrl" to url,
            "tamano" to precios
        )

        FirebaseFirestore.getInstance().collection("cafes")
            .add(cafe)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Café agregado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

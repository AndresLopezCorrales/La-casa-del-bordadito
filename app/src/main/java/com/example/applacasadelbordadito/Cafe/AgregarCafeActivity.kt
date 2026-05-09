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

    private var isEditing = false
    private var cafeId = ""
    private var currentImageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarCafeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isEditing = intent.getBooleanExtra("isEditing", false)
        cafeId = intent.getStringExtra("cafeId") ?: ""

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (isEditing) {
            binding.toolbar.title = "Editar Café"
            binding.btnGuardar.text = "Actualizar"
            cargarDatosCafe()
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

    private fun cargarDatosCafe() {
        progressDialog.setMessage("Cargando datos...")
        progressDialog.show()

        FirebaseFirestore.getInstance().collection("cafes").document(cafeId)
            .get()
            .addOnSuccessListener { document ->
                progressDialog.dismiss()
                if (document.exists()) {
                    val cafe = document.toObject(Cafe::class.java)
                    if (cafe != null) {
                        binding.etNombre.setText(cafe.nombre)
                        binding.etDescripcion.setText(cafe.descripcion)
                        binding.etCategoria.setText(cafe.categoria, false)
                        
                        binding.etPrecioChico.setText(cafe.tamano["Chico"]?.toString() ?: "")
                        binding.etPrecioMediano.setText(cafe.tamano["Mediano"]?.toString() ?: "")
                        binding.etPrecioGrande.setText(cafe.tamano["Grande"]?.toString() ?: "")
                        
                        currentImageUrl = cafe.imagenUrl
                        com.bumptech.glide.Glide.with(this).load(currentImageUrl).into(binding.ivFotoCafe)
                        binding.ivFotoCafe.imageTintList = null
                        binding.ivFotoCafe.setPadding(0, 0, 0, 0)
                        binding.btnEliminarImagen.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al cargar: ${e.message}", Toast.LENGTH_SHORT).show()
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
        currentImageUrl = ""
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
        } else if (imageUri == null && currentImageUrl.isEmpty()) {
            Toast.makeText(this, "Seleccione una imagen", Toast.LENGTH_SHORT).show()
        } else if (pChico <= 0 && pMediano <= 0 && pGrande <= 0) {
            Toast.makeText(this, "Ingrese al menos un precio", Toast.LENGTH_SHORT).show()
        } else {
            if (imageUri != null) {
                subirImagen(nombre, desc = descripcion, cat = categoria, pChico = pChico, pMediano = pMediano, pGrande = pGrande)
            } else {
                guardarEnFirestore(nombre, desc = descripcion, cat = categoria, pChico = pChico, pMediano = pMediano, pGrande = pGrande, url = currentImageUrl)
            }
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
        progressDialog.setMessage(if (isEditing) "Actualizando café..." else "Guardando café...")
        progressDialog.show()

        val precios = mutableMapOf<String, Double>()
        if (pChico > 0) precios["Chico"] = pChico
        if (pMediano > 0) precios["Mediano"] = pMediano
        if (pGrande > 0) precios["Grande"] = pGrande

        val data = hashMapOf(
            "nombre" to nombre,
            "descripcion" to desc,
            "categoria" to cat,
            "imagenUrl" to url,
            "tamano" to precios
        )

        if (!isEditing) {
            data["isActive"] = false
        }

        val db = FirebaseFirestore.getInstance().collection("cafes")
        val task = if (isEditing) {
            db.document(cafeId).update(data as Map<String, Any>)
        } else {
            db.add(data).continueWith { }
        }

        task.addOnSuccessListener {
            progressDialog.dismiss()
            val msg = if (isEditing) "Café actualizado con éxito" else "Café agregado con éxito"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            progressDialog.dismiss()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

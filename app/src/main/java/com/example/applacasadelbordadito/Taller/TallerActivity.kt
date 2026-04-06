package com.example.applacasadelbordadito.Taller

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.applacasadelbordadito.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class TallerActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivFlyerTaller: ImageView
    private lateinit var fabCambiarFlyer: FloatingActionButton
    
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var imageUri: Uri? = null

    private val MY_ADMIN_UID = "P3bMLh6zQcd60w0QX5nHN1hOiHe2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_taller)

        toolbar = findViewById(R.id.toolbar)
        ivFlyerTaller = findViewById(R.id.ivFlyerTaller)
        fabCambiarFlyer = findViewById(R.id.FABCambiarFlyer)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere")
        progressDialog.setCanceledOnTouchOutside(false)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Comprobar si el usuario es admin
        checkUserRole()

        // Cargar el flyer actual desde la base de datos
        cargarFlyer()

        fabCambiarFlyer.setOnClickListener {
            select_imagen_de()
        }
    }

    private fun checkUserRole() {
        val user = firebaseAuth.currentUser
        if (user != null && user.uid == MY_ADMIN_UID) {
            fabCambiarFlyer.visibility = android.view.View.VISIBLE
        } else {
            fabCambiarFlyer.visibility = android.view.View.GONE
        }
    }

    private fun cargarFlyer() {
        val ref = FirebaseDatabase.getInstance().getReference("TallerInfo")
        ref.child("flyerUrl").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val url = snapshot.value.toString()
                if (url.isNotEmpty() && url != "null") {
                    Glide.with(this@TallerActivity)
                        .load(url)
                        .placeholder(R.drawable.aro_bordado)
                        .into(ivFlyerTaller)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun select_imagen_de() {
        val popupMenu = PopupMenu(this, fabCambiarFlyer)
        popupMenu.menu.add(Menu.NONE, 1, 1, "Cámara")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Galería")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if (itemId == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    concederPermisoCamara.launch(arrayOf(android.Manifest.permission.CAMERA))
                } else {
                    concederPermisoCamara.launch(arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ))
                }
            } else if (itemId == 2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    imagenGaleria()
                } else {
                    concederPermisosAlmacenamiento.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            true
        }
    }

    private val concederPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        var concedidoTodos = true
        for (seConcede in resultado.values) {
            concedidoTodos = concedidoTodos && seConcede
        }
        if (concedidoTodos) {
            imagenCamara()
        } else {
            Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
        }
    }

    private val concederPermisosAlmacenamiento = registerForActivityResult(ActivityResultContracts.RequestPermission()) { esConcedido ->
        if (esConcedido) {
            imagenGaleria()
        } else {
            Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleria_ARL.launch(intent)
    }

    private fun imagenCamara() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Flyer_Taller")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Imagen del Taller")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        resultadoCamara_ARL.launch(intent)
    }

    private val resultadoCamara_ARL = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            subirImagenStorage()
        }
    }

    private val resultadoGaleria_ARL = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val data = resultado.data
            imageUri = data?.data
            subirImagenStorage()
        }
    }

    private fun subirImagenStorage() {
        progressDialog.setMessage("Subiendo Imagen del Taller")
        progressDialog.show()

        val rutaImagen = "flyerTaller/flyer_actual"
        val storageReference = FirebaseStorage.getInstance().getReference(rutaImagen)
        
        imageUri?.let { uri ->
            storageReference.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    val uriTask = taskSnapshot.storage.downloadUrl
                    while (!uriTask.isSuccessful);
                    val urlImagenCargada = uriTask.result.toString()
                    if (uriTask.isSuccessful) {
                        actualizarImagenBD(urlImagenCargada)
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun actualizarImagenBD(url: String) {
        progressDialog.setMessage("Actualizando Base de Datos")
        progressDialog.show()

        val ref = FirebaseDatabase.getInstance().getReference("TallerInfo")
        ref.child("flyerUrl").setValue(url)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Flyer actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
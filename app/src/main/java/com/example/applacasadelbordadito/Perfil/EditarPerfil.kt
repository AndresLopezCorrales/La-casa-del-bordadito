package com.example.applacasadelbordadito.Perfil

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.applacasadelbordadito.R
import com.example.applacasadelbordadito.databinding.ActivityEditarPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlin.toString

class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var imageUri: Uri ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor Espere")
        progressDialog.setCanceledOnTouchOutside(false)

        cargarInfo()

        binding.BtnActualizar.setOnClickListener {
            validarInfo()
        }

        binding.FABCambiarImg.setOnClickListener {
            select_imagen_de()
        }

    }

    private var nombres = ""
    private var f_nac = ""
    private var codigo = ""
    private var telefono = ""

    private fun validarInfo(){
        nombres = binding.EtNombres.text.toString().trim()
        f_nac = binding.EtFNac.text.toString().trim()
        codigo = binding.selectorCod.selectedCountryCodeWithPlus
        telefono = binding.EtTelefono.text.toString().trim()

        if (nombres.isEmpty()){
            Toast.makeText(this, "Ingrese sus nombres", Toast.LENGTH_SHORT).show()
        }else if (f_nac.isEmpty()){
            Toast.makeText(this, "Ingrese su fecha de nacimiento", Toast.LENGTH_SHORT).show()
        }else if (codigo.isEmpty()){
            Toast.makeText(this, "Seleccione un código", Toast.LENGTH_SHORT).show()
        }else if (telefono.isEmpty()){
            Toast.makeText(this, "Ingrese un número de teléfono", Toast.LENGTH_SHORT).show()
        }else{
            actualizarInfo()
        }
    }

    private fun actualizarInfo(){
        progressDialog.setMessage("Actualizando Información")
        progressDialog.show()

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = "${nombres}"
        hashMap["fecha_nac"] = "${f_nac}"
        hashMap["codigoTelefono"] = "${codigo}"
        hashMap["telefono"] = "${telefono}"

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(firebaseAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener{
                progressDialog.dismiss()
                Toast.makeText(this, "Información actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e ->
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot){
                    val nombres = "${snapshot.child("nombres").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"
                    val f_nac = "${snapshot.child("fecha_nac").value}"
                    val telefono = "${snapshot.child("telefono").value}"
                    val codTelefono = "${snapshot.child("codigoTelefono").value}"


                    binding.EtNombres.setText(nombres)
                    binding.EtFNac.setText(f_nac)
                    binding.EtTelefono.setText(telefono)

                    // 1. Generar el avatar primero (esto borra el icono gris por defecto)
                    //AvatarGenerator.generateAvatarByUid(this@EditarPerfil, binding.imgPerfil, firebaseAuth.uid.toString())

                    try{
                        if(imagen.isNotEmpty() && imagen != "null"){
                            Glide.with(applicationContext)
                                .load(imagen)
                                .into(binding.imgPerfil)
                        }else{
                            AvatarGenerator.generateAvatarByUid(this@EditarPerfil, binding.imgPerfil, firebaseAuth.uid.toString())
                        }

                    }catch (e: Exception){
                        Toast.makeText(this@EditarPerfil, "${e.message}", Toast.LENGTH_SHORT).show()
                    }

                    try {
                        val codigo = codTelefono.replace("+", "").toInt()
                        binding.selectorCod.setCountryForPhoneCode(codigo)
                    }catch(e: Exception){
                        //Toast.makeText(this@EditarPerfil, "${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun subirImagenStorage(){
        progressDialog.setMessage("Subiendo Imagen de Perfil")
        progressDialog.show()

        val rutaImagen = "imagenesPerfil/" + firebaseAuth.uid
        val storageReference = FirebaseStorage.getInstance().getReference(rutaImagen)
        storageReference.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while(!uriTask.isSuccessful);
                val urlImagenCargada = "${uriTask.result}"
                if(uriTask.isSuccessful){
                    actualizarImagenBD(urlImagenCargada)
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarImagenBD(urlImagenCargada: String){
        progressDialog.setMessage("Actualizando Imagen de Perfil")
        progressDialog.show()

        val hashMap: HashMap<String, Any> = HashMap()
        if (imageUri != null) {
            hashMap["urlImagenPerfil"] = urlImagenCargada
        }

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(firebaseAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Imagen de perfil actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun select_imagen_de(){
        val popupMenu = PopupMenu(this, binding.FABCambiarImg)

        popupMenu.menu.add(Menu.NONE, 1, 1, "Cámara")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Galería")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if(itemId == 1) {
                //FUncionalidad para la cámara
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    concederPermisoCamara.launch(arrayOf(android.Manifest.permission.CAMERA))
                }else{
                    concederPermisoCamara.launch(arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ))
                }
            } else if (itemId == 2){
                //FUncionalidad para la galería
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    imagenGaleria()
                }else{
                    concederPermisosAlmacenamiento.launch(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }
            return@setOnMenuItemClickListener true
        }
    }



    private val concederPermisoCamara =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
            var concedidoTodos = true
            for (seConcede in resultado.values){
                concedidoTodos = concedidoTodos && seConcede
            }
            if(concedidoTodos) {
                imagenCamara()
            }else {
                Toast.makeText(this, "El permiso de la cámara de almacenamiento se denegaron", Toast.LENGTH_SHORT).show()
            }
        }

    private val concederPermisosAlmacenamiento =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()) { esConcedido ->
            if (esConcedido) {
                imagenGaleria()
            } else {
                Toast.makeText(
                    this,
                    "El permiso de almacenamiento se denegó",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleria_ARL.launch(intent)
    }

    private fun imagenCamara() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Titulo_imagen")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion_imagen")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        resultadoCamara_ARL.launch(intent)
    }

    private val resultadoCamara_ARL =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){resultado ->
            if(resultado.resultCode == RESULT_OK){
                subirImagenStorage()
                /*try{
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.img_perfil)
                        .into(binding.imgPerfil)
                }catch(e: Exception){

                }*/
            }else{
                Toast.makeText(this, "La captura de imagen se canceló", Toast.LENGTH_SHORT).show()
            }
        }

    private val resultadoGaleria_ARL =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){resultado ->
            if(resultado.resultCode == RESULT_OK){
                val data = resultado.data
                imageUri = data!!.data
                subirImagenStorage()
                /*try{
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.img_perfil) //Imagen de perfil por defecto (en esta arquitectura, ya esta el avatar imagen). Se puede quitar
                        .into(binding.imgPerfil)
                }catch(e: Exception){

                }*/
            }else{
                Toast.makeText(this, "La selección de imagen se canceló", Toast.LENGTH_SHORT).show()
            }
        }
}
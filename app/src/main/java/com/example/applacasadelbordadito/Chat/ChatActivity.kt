package com.example.applacasadelbordadito.Chat

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.applacasadelbordadito.Adaptadores.AdaptadorChat
import com.example.applacasadelbordadito.Constantes
import com.example.applacasadelbordadito.Modelos.Chat
import com.example.applacasadelbordadito.R
import com.example.applacasadelbordadito.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class ChatActivity : AppCompatActivity() {

    companion object {
        var uidChatActivo: String? = null
    }

    private lateinit var binding : ActivityChatBinding
    private var uid = ""

    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private var miUid = ""

    private var chatRuta = ""
    private var imagenUrl : Uri?= null

    private var bloqueadoPorMi = false
    private var bloqueadoPorOtro = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        uid = intent.getStringExtra("uid")!!
        miUid = firebaseAuth.uid!!
        chatRuta = Constantes.rutaChat(uid, miUid)

        binding.adjuntarFAB.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                imagenGaleria()
            }else{
                solicitarPermisoAlmacenamiento.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        binding.IbRegresar.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.enviarFAB.setOnClickListener {
            validarMensaje()
        }

        cargarInfo()
        cargarMensajes()
        marcarComoLeido()
        verificarBloqueos()

    }

    private fun verificarBloqueos() {
        val refBloqueoYo = FirebaseDatabase.getInstance().getReference("Usuarios")
            .child(miUid).child("usuariosBloqueados").child(uid)

        refBloqueoYo.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bloqueadoPorMi = snapshot.exists()
                actualizarUiBloqueo()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        val refBloqueoOtro = FirebaseDatabase.getInstance().getReference("Usuarios")
            .child(uid).child("usuariosBloqueados").child(miUid)

        refBloqueoOtro.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bloqueadoPorOtro = snapshot.exists()
                actualizarUiBloqueo()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun actualizarUiBloqueo() {
        if (bloqueadoPorMi) {
            binding.EtMensajeChat.visibility = android.view.View.GONE
            binding.enviarFAB.visibility = android.view.View.GONE
            binding.adjuntarFAB.visibility = android.view.View.GONE
            binding.tvBloqueado.visibility = android.view.View.VISIBLE
            binding.tvBloqueado.text = "Has bloqueado a este usuario"
        } else if (bloqueadoPorOtro) {
            binding.EtMensajeChat.visibility = android.view.View.GONE
            binding.enviarFAB.visibility = android.view.View.GONE
            binding.adjuntarFAB.visibility = android.view.View.GONE
            binding.tvBloqueado.visibility = android.view.View.VISIBLE
            binding.tvBloqueado.text = "Has sido bloqueado por soporte"
        } else {
            binding.EtMensajeChat.visibility = android.view.View.VISIBLE
            binding.enviarFAB.visibility = android.view.View.VISIBLE
            binding.adjuntarFAB.visibility = android.view.View.VISIBLE
            binding.tvBloqueado.visibility = android.view.View.GONE
        }
    }

    private fun marcarComoLeido() {
        val miUid = firebaseAuth.uid ?: return
        FirebaseDatabase.getInstance().getReference("ChatsUnreadCount")
            .child(miUid).child(uid).setValue(0)
    }

    override fun onResume() {
        super.onResume()
        uidChatActivo = uid
        actualizarEstadoChatting(uid)
    }

    override fun onPause() {
        super.onPause()
        uidChatActivo = null
        actualizarEstadoChatting("")
    }

    private fun actualizarEstadoChatting(receptorUid: String) {
        val miUid = firebaseAuth.uid ?: return
        FirebaseDatabase.getInstance().getReference("Usuarios").child(miUid)
            .child("chattingWith").setValue(receptorUid)
    }

    private fun cargarMensajes(){
        val mensajesArrayList = ArrayList<Chat>()
        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatRuta)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot){
                    mensajesArrayList.clear()
                    for (ds: DataSnapshot in snapshot.children){
                        try {
                            val chat = ds.getValue(Chat::class.java)
                            mensajesArrayList.add(chat!!)
                        }catch (e: Exception){
                            Log.e("FirebaseError", "Error al cargar los mensajes: ${e.message}")
                        }
                    }
                    val adaptadorChat = AdaptadorChat(this@ChatActivity, mensajesArrayList)
                    binding.chatsRV.adapter = adaptadorChat
                }
                override fun onCancelled(error: DatabaseError){
                    Log.e("FirebaseError", "Error al cargar los mensajes ${error.message}")
                }
            })
    }

    private fun validarMensaje() {
        val mensaje = binding.EtMensajeChat.text.toString().trim()
        val tiempo = Constantes.obtenerTiempoDis()

        if (mensaje.isEmpty()){
            Toast.makeText(this, "Ingrese un mensaje", Toast.LENGTH_SHORT).show()
        }else{
            enviarMensaje(Constantes.MENSAJE_TIPO_TEXTO, mensaje, tiempo)
        }
    }
    private fun cargarInfo(){
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uid)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"

                    binding.TxtNombreUsuario.text = nombres

                    try {
                        Glide.with(this@ChatActivity)
                            .load(imagen)
                            .placeholder(R.drawable.ic_imagen_perfil)
                            .into(binding.ToolbarIV)
                    } catch (e: Exception) {
                        Log.e("ChatActivity", "${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleriaARL.launch(intent)
    }

    private val resultadoGaleriaARL =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){resultado ->
            if (resultado.resultCode == Activity.RESULT_OK){
                val data = resultado.data
                imagenUrl = data!!.data
                subirImgStorage()
            }else{
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
            }
        }

    private val solicitarPermisoAlmacenamiento =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){esConcedido ->
            if (esConcedido){
                imagenGaleria()
            }else{
                Toast.makeText(this, "El permiso de almacenamiento nao ha sido concedido", Toast.LENGTH_SHORT).show()
            }
        }

    private fun subirImgStorage(){
        progressDialog.setMessage("Subiendo imagen")
        progressDialog.show()

        val tiempo = Constantes.obtenerTiempoDis()
        val nombreRutaImg = "ImagenesChat/$tiempo"
        val storageRef = FirebaseStorage.getInstance().getReference(nombreRutaImg)

        storageRef.putFile(imagenUrl!!)
            .addOnSuccessListener { taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                var urlImagen = uriTask.result.toString()
                if(uriTask.isSuccessful){
                    enviarMensaje(Constantes.MENSAJE_TIPO_IMAGEN, urlImagen, tiempo)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "No se pudo enviar la imagen debido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enviarMensaje (tipoMensaje: String, mensaje:String, tiempo: Long){
        if (bloqueadoPorMi || bloqueadoPorOtro) {
            Toast.makeText(this, "No puedes enviar mensajes", Toast.LENGTH_SHORT).show()
            return
        }
        progressDialog.setMessage("Enviando mensaje")
        progressDialog.show()

        val refChat = FirebaseDatabase.getInstance().getReference("Chats")
        val keyId = "${refChat.push().key}"
        val hashMap = HashMap<String,Any>()

        hashMap["idMensaje"] = "${keyId}"
        hashMap["tipoMensaje"] = "${tipoMensaje}"
        hashMap["mensaje"] = "${mensaje}"
        hashMap["emisorUid"] = "${miUid}"
        hashMap["receptorUid"] = "${uid}"
        hashMap["tiempo"] = tiempo

        refChat.child(chatRuta)
            .child(keyId)
            .setValue(hashMap)
            .addOnSuccessListener {
                incrementarContadorReceptor()
                enviarNotificacionPush(tipoMensaje, mensaje)
                progressDialog.dismiss()
                binding.EtMensajeChat.setText("")
            }.addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "No se pudo enviar el mensaje debido a ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun incrementarContadorReceptor() {
        if (bloqueadoPorMi || bloqueadoPorOtro) return

        // Verificar si el receptor ya está dentro de este chat para no sumarle notificaciones
        val refReceptor = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
        
        refReceptor.child("chattingWith").get().addOnSuccessListener { snapshot ->
            val chattingWith = snapshot.getValue(String::class.java) ?: ""
            
            // Solo si el receptor NO está chateando conmigo, sumamos al badge
            if (chattingWith != miUid) {
                val refCount = FirebaseDatabase.getInstance().getReference("ChatsUnreadCount")
                    .child(uid).child(miUid)

                refCount.get().addOnSuccessListener { countSnapshot ->
                    val count = countSnapshot.getValue(Int::class.java) ?: 0
                    refCount.setValue(count + 1)
                }
            }
        }
    }

    private fun enviarNotificacionPush(tipo: String, msg: String) {
        if (bloqueadoPorMi || bloqueadoPorOtro) return

        // Verificar si el receptor está en el chat o está online para no enviarle push
        val refReceptor = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
        refReceptor.get().addOnSuccessListener { snapshot ->
            val chattingWith = snapshot.child("chattingWith").getValue(String::class.java) ?: ""
            val isOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false

            // Solo enviamos PUSH si el usuario NO está viendo nuestro chat Y NO está online
            if (chattingWith != miUid && !isOnline) {
                FirebaseDatabase.getInstance().getReference("Usuarios").child(miUid).get().addOnSuccessListener { miSnapshot ->
                    val miNombre = "${miSnapshot.child("nombres").value}"
                    val miImagen = "${miSnapshot.child("urlImagenPerfil").value}"

                    val textoFinal = if (tipo == Constantes.MENSAJE_TIPO_IMAGEN) "📷 Imagen Enviada" else msg

                    com.example.applacasadelbordadito.notificaciones.FcmUtil.enviarNotificacionAUsuario(
                        uid, miNombre, textoFinal, miImagen
                    )
                }
            }
        }
    }
}
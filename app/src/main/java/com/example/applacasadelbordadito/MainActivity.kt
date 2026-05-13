package com.example.applacasadelbordadito

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.Carrito.CarritoActivity
import com.example.applacasadelbordadito.Historial.HistorialActivity
import com.example.applacasadelbordadito.Fragmentos.FragmentBordado
import com.example.applacasadelbordadito.Fragmentos.FragmentCafe
import com.example.applacasadelbordadito.Fragmentos.FragmentCuenta
import com.example.applacasadelbordadito.Fragmentos.FragmentInicio
import com.example.applacasadelbordadito.Taller.TallerActivity
import com.example.applacasadelbordadito.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.applacasadelbordadito.Chat.ChatActivity
import com.example.applacasadelbordadito.Modelos.Chat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var btnCarrito: FrameLayout
    private lateinit var btnHistorial: ImageView
    private lateinit var badgeCarrito: TextView

    private var inAppNotificationPopup: PopupWindow? = null
    private val handler = Handler(Looper.getMainLooper())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Opcional: explicar al usuario por qué las notificaciones son importantes
        }
    }

    // Flag para evitar re-entrada al actualizar el BottomNav programáticamente
    private var isProgrammaticSelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        comprobarSesion()
        ApplicationClass.actualizarEstadoOnline(true)

        btnCarrito = findViewById(R.id.btnCarrito)
        btnHistorial = findViewById(R.id.btnHistorial)
        badgeCarrito = findViewById(R.id.badgeCarrito)

        btnCarrito.setOnClickListener {
            startActivity(Intent(this, CarritoActivity::class.java))
        }

        btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        binding.FAB.setOnClickListener {
            startActivity(Intent(this, TallerActivity::class.java))
        }

        escucharCarrito()
        solicitarPermisoNotificaciones()
        escucharMensajesNuevos()

        // Carga inicial
        verFragmentInicio()

        val fragment = intent.getStringExtra("fragment")
        if (fragment == "cafe") {
            verFragmentCafe()
        } else if (fragment == "cuenta") {
            verFragmentCuenta()
        }

        binding.BottomNV.setOnItemSelectedListener { item ->
            if (isProgrammaticSelection) return@setOnItemSelectedListener true
            
            when(item.itemId) {
                R.id.Item_Inicio -> { verFragmentInicio(); true }
                R.id.Item_Cafe -> { verFragmentCafe(); true }
                R.id.Item_Taller -> {
                    startActivity(Intent(this, TallerActivity::class.java))
                    false 
                }
                R.id.Item_Bordado -> { verFragmentBordado(); true }
                R.id.Item_Cuenta -> { verFragmentCuenta(); true }
                else -> false
            }
        }
    }

    private fun updateBottomNavSelection(itemId: Int) {
        if (binding.BottomNV.selectedItemId != itemId) {
            isProgrammaticSelection = true
            binding.BottomNV.selectedItemId = itemId
            isProgrammaticSelection = false
        }
    }

    fun verFragmentInicio() {
        updateBottomNavSelection(R.id.Item_Inicio)
        binding.TituloRL.text = "Inicio"
        val fragment = FragmentInicio()
        supportFragmentManager.beginTransaction()
            .replace(binding.FragmentL1.id, fragment, "FragmentInicio")
            .commit()
    }

    fun verFragmentCafe() {
        updateBottomNavSelection(R.id.Item_Cafe)
        binding.TituloRL.text = "Cafe"
        val fragment = FragmentCafe()
        supportFragmentManager.beginTransaction()
            .replace(binding.FragmentL1.id, fragment, "FragmentCafe")
            .commit()
    }

    fun verFragmentBordado(patronId: String? = null) {
        updateBottomNavSelection(R.id.Item_Bordado)
        binding.TituloRL.text = "Bordado"
        val fragment = FragmentBordado().apply {
            if (patronId != null) {
                arguments = Bundle().apply {
                    putString("patronId", patronId)
                }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(binding.FragmentL1.id, fragment, "FragmentBordado")
            .commit()
    }

    fun verFragmentCuenta() {
        updateBottomNavSelection(R.id.Item_Cuenta)
        binding.TituloRL.text = "Cuenta"
        val fragment = FragmentCuenta()
        supportFragmentManager.beginTransaction()
            .replace(binding.FragmentL1.id, fragment, "FragmentCuenta")
            .commit()
    }

    private fun escucharCarrito() {
        val user = firebaseAuth.currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("carritos")
            .document(user.uid)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val count = snapshot.size()
                if (count > 0) {
                    badgeCarrito.text = count.toString()
                    badgeCarrito.visibility = View.VISIBLE
                } else {
                    badgeCarrito.visibility = View.GONE
                }
            }
    }

    private fun escucharMensajesNuevos() {
        val miUid = firebaseAuth.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("ChatsUnreadCount").child(miUid)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val senderUid = child.key ?: continue
                    val count = child.getValue(Int::class.java) ?: 0

                    // Solo si hay mensajes nuevos Y no estamos en ese chat
                    if (count > 0 && senderUid != ChatActivity.uidChatActivo) {
                        obtenerUltimoMensajeYMostrar(senderUid)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun obtenerUltimoMensajeYMostrar(senderUid: String) {
        val miUid = firebaseAuth.uid ?: return
        val chatRuta = Constantes.rutaChat(miUid, senderUid)
        
        FirebaseDatabase.getInstance().getReference("Chats").child(chatRuta)
            .orderByChild("tiempo")
            .limitToLast(1)
            .get().addOnSuccessListener { snapshot ->
                val lastChat = snapshot.children.firstOrNull()?.getValue(Chat::class.java)
                if (lastChat != null && lastChat.emisorUid != miUid) {
                    mostrarInAppNotification(lastChat)
                }
            }
    }

    private fun mostrarInAppNotification(chat: Chat) {
        // Obtener datos del emisor
        FirebaseDatabase.getInstance().getReference("Usuarios").child(chat.emisorUid)
            .get().addOnSuccessListener { snapshot ->
                val nombre = "${snapshot.child("nombres").value}"
                val imagen = "${snapshot.child("urlImagenPerfil").value}"
                val msg = if (chat.tipoMensaje == Constantes.MENSAJE_TIPO_IMAGEN) "📷 Imagen Enviada" else chat.mensaje

                mostrarPopup(nombre, msg, imagen, chat.emisorUid)
            }
    }

    private fun mostrarPopup(nombre: String, mensaje: String, imagenUrl: String, senderUid: String) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.item_notificacion_dentro_app, null)

        val img = view.findViewById<ImageView>(R.id.imgNotifPerfil)
        val txtNombre = view.findViewById<TextView>(R.id.txtNotifNombre)
        val txtMsg = view.findViewById<TextView>(R.id.txtNotifMensaje)

        txtNombre.text = nombre
        txtMsg.text = mensaje
        Glide.with(this).load(imagenUrl).placeholder(R.drawable.ic_imagen_perfil).into(img)

        // Cerrar previo si existe
        inAppNotificationPopup?.dismiss()

        inAppNotificationPopup = PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            elevation = 20f
            animationStyle = android.R.style.Animation_Dialog
            showAtLocation(binding.root, Gravity.TOP or Gravity.END, 20, 100)
        }

        view.setOnClickListener {
            val intent = Intent(this, com.example.applacasadelbordadito.Chat.ChatActivity::class.java)
            intent.putExtra("uid", senderUid)
            startActivity(intent)
            inAppNotificationPopup?.dismiss()
        }

        // Auto ocultar tras 4 segundos
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ inAppNotificationPopup?.dismiss() }, 4000)
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun comprobarSesion(){
        val user = firebaseAuth.currentUser
        if(user == null){
            startActivity(Intent(this, OpcionesLogin::class.java))
            finishAffinity()
        } else {
            if (user.providerData.any { it.providerId == "password" } && !user.isEmailVerified) {
                startActivity(Intent(this, VerificarEmailActivity::class.java))
                finish()
            } else {
                com.onesignal.OneSignal.login(user.uid) // solo si está verificado
            }
        }
    }
}

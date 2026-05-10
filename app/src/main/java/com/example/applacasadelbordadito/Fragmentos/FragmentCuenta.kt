package com.example.applacasadelbordadito.Fragmentos

import com.example.applacasadelbordadito.Perfil.PaymentMethod
import com.example.applacasadelbordadito.Perfil.PaymentMethodAdapter
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.applacasadelbordadito.Bordado.PatronBordado
import com.example.applacasadelbordadito.Bordado.PatronesAdminAdapter
import com.example.applacasadelbordadito.Cafe.AgregarCafeActivity
import com.example.applacasadelbordadito.Cafe.Cafe
import com.example.applacasadelbordadito.Cafe.CafeAdminAdapter
import com.example.applacasadelbordadito.Constantes
import com.example.applacasadelbordadito.OpcionesLogin
import com.example.applacasadelbordadito.Perfil.AvatarGenerator
import com.example.applacasadelbordadito.Perfil.EditarPerfil
import com.example.applacasadelbordadito.Perfil.TarjetaAgregarActivity
import com.example.applacasadelbordadito.R
import com.example.applacasadelbordadito.databinding.FragmentCuentaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson

class FragmentCuenta : Fragment() {

    private lateinit var binding: FragmentCuentaBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mContext: Context

    // Admin Cafes
    private lateinit var cafeAdminAdapter: CafeAdminAdapter
    private val listaCafesAdminFull = mutableListOf<Cafe>()
    private var paginaActualCafe = 0

    // Admin Bordados
    private lateinit var patronesAdminAdapter: PatronesAdminAdapter
    private val listaPatronesAdminFull = mutableListOf<PatronBordado>()
    private var paginaActualBordado = 0

    // Metodos de Pago
    private lateinit var paymentMethodAdapter: PaymentMethodAdapter
    private val listaMetodosPago = mutableListOf<PaymentMethod>()

    private val itemsPorPagina = 5
    private lateinit var progressDialog: ProgressDialog
    private var imageUri: Uri? = null

    // Callbacks de Actividad
    private val concederPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado.values.all { it }) imagenCamara()
        else Toast.makeText(mContext, "Permisos denegados", Toast.LENGTH_SHORT).show()
    }

    private val concederPermisosAlmacenamiento = registerForActivityResult(ActivityResultContracts.RequestPermission()) { esConcedido ->
        if (esConcedido) imagenGaleria()
        else Toast.makeText(mContext, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
    }

    private val resultadoCamara_ARL = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == android.app.Activity.RESULT_OK) subirFlyerStorage()
    }

    private val resultadoGaleria_ARL = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == android.app.Activity.RESULT_OK) {
            imageUri = resultado.data?.data
            subirFlyerStorage()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCuentaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNavAdmin.visibility = View.GONE

        firebaseAuth = FirebaseAuth.getInstance()
        
        progressDialog = ProgressDialog(mContext).apply {
            setTitle("Por favor espere")
            setCanceledOnTouchOutside(false)
        }

        setupToggleNav()
        setupProfileSection()
        setupConfigSection()
        setupAdminSection()
        setupPaymentMethods()
        
        leerInfoUsuario()

        val tab = arguments?.getString("tab")
        if (tab == "config") {
            selectTab(R.id.btnNavConfig)
        }
    }

    private fun setupToggleNav() {
        binding.btnNavPerfil.setOnClickListener { selectTab(R.id.btnNavPerfil) }
        binding.btnNavConfig.setOnClickListener { selectTab(R.id.btnNavConfig) }
        binding.btnNavAdmin.setOnClickListener { selectTab(R.id.btnNavAdmin) }
        
        // Selección por defecto
        selectTab(R.id.btnNavPerfil)
    }

    private fun selectTab(id: Int) {
        // Actualizar visibilidad de los layouts
        binding.layoutPerfil.root.visibility = if (id == R.id.btnNavPerfil) View.VISIBLE else View.GONE
        binding.layoutConfig.root.visibility = if (id == R.id.btnNavConfig) View.VISIBLE else View.GONE
        binding.layoutAdmin.root.visibility = if (id == R.id.btnNavAdmin) View.VISIBLE else View.GONE

        // Actualizar estado visual de los botones
        updateTabVisuals(id)
    }

    private fun updateTabVisuals(selectedId: Int) {
        val navItems = listOf(
            binding.btnNavPerfil to R.id.btnNavPerfil,
            binding.btnNavConfig to R.id.btnNavConfig,
            binding.btnNavAdmin to R.id.btnNavAdmin
        )

        navItems.forEach { (view, id) ->
            if (id == selectedId) {
                view.setBackgroundColor(androidx.core.content.ContextCompat.getColor(mContext, R.color.brown))
                view.setTextColor(androidx.core.content.ContextCompat.getColor(mContext, R.color.bone))
            } else {
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                view.setTextColor(androidx.core.content.ContextCompat.getColor(mContext, R.color.stone_brown))
            }
        }
    }

    // ── SECCIÓN PERFIL ──

    private fun setupProfileSection() {
        binding.layoutPerfil.BtnEditarPerfil.setOnClickListener {
            startActivity(Intent(mContext, EditarPerfil::class.java))
        }

        binding.layoutPerfil.BtnCerrarSesion.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(requireContext(), OpcionesLogin::class.java))
            activity?.finishAffinity()
        }
    }

    private fun leerInfoUsuario() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val email = "${snapshot.child("email").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"
                    val f_nac = "${snapshot.child("fecha_nac").value}"
                    var tiempo = "${snapshot.child("tiempo").value}"
                    val telefono = "${snapshot.child("telefono").value}"
                    val codTelefono = "${snapshot.child("codigoTelefono").value}"
                    val proveedor = "${snapshot.child("proveedor").value}"
                    val esAdmin = snapshot.child("esAdmin").getValue(Boolean::class.java) ?: false

                    if (esAdmin) {
                        binding.btnNavAdmin.visibility = View.VISIBLE
                    } else {
                        binding.btnNavAdmin.visibility = View.GONE
                    }

                    if (tiempo == "null") tiempo = "0"
                    val for_tiempo = Constantes.obtenerFecha(tiempo.toLong())

                    binding.layoutPerfil.run {
                        TvEmail.text = email
                        TvNombres.text = nombres
                        TvNacimiento.text = f_nac
                        TvTelefono.text = codTelefono + telefono
                        TvMiembro.text = for_tiempo

                        try {
                            if (imagen.isNotEmpty() && imagen != "null") {
                                Glide.with(mContext).load(imagen).into(TvPerfil)
                            } else {
                                AvatarGenerator.generateAvatarByUid(mContext, TvPerfil, firebaseAuth.uid.toString())
                            }
                        } catch (e: Exception) {
                            Toast.makeText(mContext, "${e.message}", Toast.LENGTH_SHORT).show()
                        }

                        TvEstadoCuenta.text = if (proveedor == "Email") {
                            if (firebaseAuth.currentUser?.isEmailVerified == true) "Verificado" else "No verificado"
                        } else "Verificado"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ── SECCIÓN CONFIGURACIÓN ──

    private fun setupConfigSection() {
        binding.layoutConfig.btnInstagram.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/la_casa_del_bordadito/"))
            startActivity(intent)
        }

        binding.layoutConfig.btnWhatsApp.setOnClickListener {
            val url = "https://api.whatsapp.com/send?phone=526621398836&text=Hola, vengo de la casa del bordadito"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        binding.layoutConfig.btnEmailContacto.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:andresbordados326@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Contacto desde la App")
            }
            startActivity(Intent.createChooser(intent, "Enviar correo"))
        }
    }

    private fun setupPaymentMethods() {
        paymentMethodAdapter = PaymentMethodAdapter(listaMetodosPago) { method ->
            eliminarMetodoPago(method)
        }

        binding.layoutConfig.rvPaymentMethods.layoutManager = LinearLayoutManager(mContext)
        binding.layoutConfig.rvPaymentMethods.adapter = paymentMethodAdapter

        binding.layoutConfig.btnAddCard.setOnClickListener {
            if (listaMetodosPago.size >= 5) {
                Toast.makeText(mContext, "Máximo 5 tarjetas permitidas", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(mContext, TarjetaAgregarActivity::class.java))
            }
        }

        cargarMetodosPago()
    }

    private fun cargarMetodosPago() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
            .child(firebaseAuth.uid!!).child("metodosPago")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaMetodosPago.clear()
                for (postSnapshot in snapshot.children) {
                    val method = postSnapshot.getValue(PaymentMethod::class.java)
                    method?.let { 
                        it.id = postSnapshot.key ?: ""
                        listaMetodosPago.add(it)
                    }
                }
                paymentMethodAdapter.updateList(listaMetodosPago)
                actualizarUIConfig()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun actualizarUIConfig() {
        if (listaMetodosPago.isEmpty()) {
            binding.layoutConfig.tvNoCards.visibility = View.VISIBLE
            binding.layoutConfig.rvPaymentMethods.visibility = View.GONE
            binding.layoutConfig.btnAddCard.text = "Agrega tu primera tarjeta"
        } else {
            binding.layoutConfig.tvNoCards.visibility = View.GONE
            binding.layoutConfig.rvPaymentMethods.visibility = View.VISIBLE
            binding.layoutConfig.btnAddCard.text = "Agregar otra tarjeta"
        }
    }

    private fun eliminarMetodoPago(method: PaymentMethod) {
        AlertDialog.Builder(mContext)
            .setTitle("Eliminar Tarjeta")
            .setMessage("¿Deseas eliminar la tarjeta terminada en ${method.last4}?")
            .setPositiveButton("Eliminar") { _, _ ->
                FirebaseDatabase.getInstance().getReference("Usuarios")
                    .child(firebaseAuth.uid!!).child("metodosPago")
                    .child(method.id).removeValue()
                    .addOnSuccessListener { Toast.makeText(mContext, "Tarjeta eliminada", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── SECCIÓN ADMINISTRACIÓN ──

    private fun setupAdminSection() {
        setupAdminCafes()
        setupAdminBordados()
        setupAdminTaller()
    }

    private fun setupAdminCafes() {
        cafeAdminAdapter = CafeAdminAdapter(
            { cafe -> toggleCafeStatus(cafe) },
            { cafe -> editarCafe(cafe) }
        )

        binding.layoutAdmin.run {
            rvAdminCafes.layoutManager = LinearLayoutManager(mContext)
            rvAdminCafes.adapter = cafeAdminAdapter

            btnAdminAddCafe.setOnClickListener {
                startActivity(Intent(mContext, AgregarCafeActivity::class.java))
            }

            btnAdminPrevPage.setOnClickListener {
                val totalPaginas = Math.ceil(listaCafesAdminFull.size.toDouble() / itemsPorPagina).toInt()
                paginaActualCafe = if (paginaActualCafe > 0) paginaActualCafe - 1 else (if (totalPaginas > 0) totalPaginas - 1 else 0)
                actualizarPaginaAdminCafes()
            }

            btnAdminNextPage.setOnClickListener {
                val totalPaginas = Math.ceil(listaCafesAdminFull.size.toDouble() / itemsPorPagina).toInt()
                paginaActualCafe = if (paginaActualCafe < totalPaginas - 1) paginaActualCafe + 1 else 0
                actualizarPaginaAdminCafes()
            }
        }
        cargarCafesAdmin()
    }

    private fun cargarCafesAdmin() {
        FirebaseFirestore.getInstance().collection("cafes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.let {
                    listaCafesAdminFull.clear()
                    for (doc in it.documents) {
                        doc.toObject(Cafe::class.java)?.let { cafe ->
                            cafe.id = doc.id
                            listaCafesAdminFull.add(cafe)
                        }
                    }
                    actualizarPaginaAdminCafes()
                }
            }
    }

    private fun actualizarPaginaAdminCafes() {
        val totalPaginas = Math.ceil(listaCafesAdminFull.size.toDouble() / itemsPorPagina).toInt()
        if (paginaActualCafe >= totalPaginas && totalPaginas > 0) paginaActualCafe = totalPaginas - 1

        val inicio = paginaActualCafe * itemsPorPagina
        val fin = Math.min(inicio + itemsPorPagina, listaCafesAdminFull.size)
        val nuevaPagina = if (inicio < listaCafesAdminFull.size) listaCafesAdminFull.subList(inicio, fin).toList() else emptyList()

        cafeAdminAdapter.updateList(nuevaPagina)
        val total = if (totalPaginas == 0) 1 else totalPaginas
        binding.layoutAdmin.txtAdminPageInfo.text = "Página ${paginaActualCafe + 1} de $total"
    }

    private fun toggleCafeStatus(cafe: Cafe) {
        val newStatus = !cafe.isActive
        FirebaseFirestore.getInstance().collection("cafes").document(cafe.id)
            .update("isActive", newStatus)
            .addOnSuccessListener {
                Toast.makeText(mContext, if (newStatus) "Café habilitado" else "Café deshabilitado", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editarCafe(cafe: Cafe) {
        val intent = Intent(mContext, AgregarCafeActivity::class.java).apply {
            putExtra("cafeId", cafe.id)
            putExtra("isEditing", true)
        }
        startActivity(intent)
    }

    private fun setupAdminBordados() {
        patronesAdminAdapter = PatronesAdminAdapter(
            { patron -> togglePatronStatus(patron) },
            { patron -> confirmarEliminarPatron(patron) }
        )

        binding.layoutAdmin.run {
            rvAdminBordados.layoutManager = LinearLayoutManager(mContext)
            rvAdminBordados.adapter = patronesAdminAdapter

            btnAdminAddBordado.setOnClickListener { mostrarDialogoSubirBordado() }

            btnAdminPrevPageBordado.setOnClickListener {
                val totalPaginas = Math.ceil(listaPatronesAdminFull.size.toDouble() / itemsPorPagina).toInt()
                paginaActualBordado = if (paginaActualBordado > 0) paginaActualBordado - 1 else (if (totalPaginas > 0) totalPaginas - 1 else 0)
                actualizarPaginaAdminBordado()
            }

            btnAdminNextPageBordado.setOnClickListener {
                val totalPaginas = Math.ceil(listaPatronesAdminFull.size.toDouble() / itemsPorPagina).toInt()
                paginaActualBordado = if (paginaActualBordado < totalPaginas - 1) paginaActualBordado + 1 else 0
                actualizarPaginaAdminBordado()
            }
        }
        cargarPatronesAdmin()
    }

    private fun cargarPatronesAdmin() {
        FirebaseDatabase.getInstance().getReference("patrones")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaPatronesAdminFull.clear()
                    for (postSnapshot in snapshot.children) {
                        postSnapshot.getValue(PatronBordado::class.java)?.let { patron ->
                            patron.id = postSnapshot.key ?: ""
                            listaPatronesAdminFull.add(patron)
                        }
                    }
                    actualizarPaginaAdminBordado()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun actualizarPaginaAdminBordado() {
        val totalPaginas = Math.ceil(listaPatronesAdminFull.size.toDouble() / itemsPorPagina).toInt()
        if (paginaActualBordado >= totalPaginas && totalPaginas > 0) paginaActualBordado = totalPaginas - 1

        val inicio = paginaActualBordado * itemsPorPagina
        val fin = Math.min(inicio + itemsPorPagina, listaPatronesAdminFull.size)
        val nuevaPagina = if (inicio < listaPatronesAdminFull.size) listaPatronesAdminFull.subList(inicio, fin).toList() else emptyList()

        patronesAdminAdapter.updateList(nuevaPagina)
        val total = if (totalPaginas == 0) 1 else totalPaginas
        binding.layoutAdmin.txtAdminPageInfoBordado.text = "Página ${paginaActualBordado + 1} de $total"
    }

    private fun togglePatronStatus(patron: PatronBordado) {
        val newStatus = !patron.activo
        FirebaseDatabase.getInstance().getReference("patrones").child(patron.id)
            .child("activo").setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(mContext, if (newStatus) "Patrón habilitado" else "Patrón deshabilitado", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmarEliminarPatron(patron: PatronBordado) {
        AlertDialog.Builder(mContext)
            .setTitle("Eliminar Patrón")
            .setMessage("¿Estás seguro de que deseas eliminar el patrón \"${patron.nombre}\"? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> eliminarPatron(patron) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPatron(patron: PatronBordado) {
        FirebaseDatabase.getInstance().getReference("patrones").child(patron.id)
            .removeValue().addOnSuccessListener {
                Toast.makeText(mContext, "Patrón eliminado", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoSubirBordado() {
        val editText = EditText(mContext).apply {
            hint = "Pega el código JSON aquí"
            minLines = 5
        }
        val layout = LinearLayout(mContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            addView(editText)
        }

        AlertDialog.Builder(mContext)
            .setTitle("Nuevo Patrón de Bordado")
            .setView(layout)
            .setPositiveButton("Subir") { _, _ ->
                val jsonRaw = editText.text.toString()
                if (jsonRaw.isNotEmpty()) subirPatronAFirebase(jsonRaw)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun subirPatronAFirebase(json: String) {
        try {
            val patron = Gson().fromJson(json, PatronBordado::class.java).apply { activo = false }
            FirebaseDatabase.getInstance().getReference("patrones").child(patron.id)
                .setValue(patron)
                .addOnSuccessListener { Toast.makeText(mContext, "¡Patrón ${patron.nombre} subido!", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { Toast.makeText(mContext, "Error al subir", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            Toast.makeText(mContext, "JSON Inválido", Toast.LENGTH_LONG).show()
        }
    }

    // ── GESTIÓN TALLER ──

    private fun setupAdminTaller() {
        binding.layoutAdmin.btnAdminChangeFlyer.setOnClickListener { select_imagen_de() }
        cargarFlyerAdmin()
    }

    private fun cargarFlyerAdmin() {
        FirebaseDatabase.getInstance().getReference("TallerInfo").child("flyerUrl")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val url = snapshot.value.toString()
                    if (url.isNotEmpty() && url != "null") {
                        Glide.with(mContext).load(url).placeholder(R.drawable.aro_bordado).into(binding.layoutAdmin.imgAdminFlyerTaller)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun select_imagen_de() {
        PopupMenu(mContext, binding.layoutAdmin.btnAdminChangeFlyer).apply {
            menu.add(Menu.NONE, 1, 1, "Cámara")
            menu.add(Menu.NONE, 2, 2, "Galería")
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) concederPermisoCamara.launch(arrayOf(android.Manifest.permission.CAMERA))
                         else concederPermisoCamara.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    2 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) imagenGaleria()
                         else concederPermisosAlmacenamiento.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                true
            }
            show()
        }
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        resultadoGaleria_ARL.launch(intent)
    }

    private fun imagenCamara() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Flyer_Taller")
            put(MediaStore.Images.Media.DESCRIPTION, "Imagen del Taller")
        }
        imageUri = mContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { putExtra(MediaStore.EXTRA_OUTPUT, imageUri) }
        resultadoCamara_ARL.launch(intent)
    }

    private fun subirFlyerStorage() {
        progressDialog.setMessage("Subiendo Imagen del Taller")
        progressDialog.show()
        val storageReference = FirebaseStorage.getInstance().getReference("flyerTaller/flyer_actual")
        imageUri?.let { uri ->
            storageReference.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { url -> actualizarFlyerBD(url.toString()) }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(mContext, "${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun actualizarFlyerBD(url: String) {
        progressDialog.setMessage("Actualizando Base de Datos")
        FirebaseDatabase.getInstance().getReference("TallerInfo").child("flyerUrl").setValue(url)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(mContext, "Flyer actualizado correctamente", Toast.LENGTH_SHORT).show()
                com.example.applacasadelbordadito.notificaciones.FcmUtil.enviarNotificacionATodos(
                    "¡Nuevo Taller Disponible!",
                    "Se ha abierto un nuevo taller de bordado. ¡Revisa la información!"
                )
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(mContext, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
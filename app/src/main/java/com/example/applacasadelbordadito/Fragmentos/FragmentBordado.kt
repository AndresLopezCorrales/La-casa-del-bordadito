package com.example.applacasadelbordadito.Fragmentos

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.Bordado.BordadoCanvasView
import com.example.applacasadelbordadito.Bordado.PatronBordado
import com.example.applacasadelbordadito.Bordado.PatronesAdapter
import com.example.applacasadelbordadito.Bordado.ProgresoUsuario
import com.example.applacasadelbordadito.Carrito.CarritoItem
import com.example.applacasadelbordadito.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class FragmentBordado : Fragment() {
    private lateinit var canvas: BordadoCanvasView
    private lateinit var paleta: LinearLayout
    private lateinit var layoutSeleccion: View
    private lateinit var layoutJuego: View
    private lateinit var rvPatrones: RecyclerView
    private lateinit var adaptador: PatronesAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var txtProgreso: TextView
    private lateinit var btnReiniciar: Button
    private lateinit var toolbarJuego: MaterialToolbar
    private lateinit var btnInfoBordados: MaterialButton

    private var ultimoBotonSeleccionado: View? = null
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private var patronActual: PatronBordado? = null
    private val MY_ADMIN_UID = "P3bMLh6zQcd60w0QX5nHN1hOiHe2"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fabAdmin = view.findViewById<FloatingActionButton>(R.id.fabAdminSubir)

        if (auth.currentUser?.uid == MY_ADMIN_UID) {
            fabAdmin.visibility = View.VISIBLE
        }

        fabAdmin.setOnClickListener {
            mostrarDialogoSubirJSON()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (layoutJuego.visibility == View.VISIBLE) {
                    mostrarSeleccion()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bordado, container, false)

        canvas = view.findViewById(R.id.bordadoCanvas)
        paleta = view.findViewById(R.id.contenedorPaleta)
        layoutSeleccion = view.findViewById(R.id.layoutSeleccion)
        layoutJuego = view.findViewById(R.id.layoutJuego)
        rvPatrones = view.findViewById(R.id.rvPatrones)
        progressBar = view.findViewById(R.id.progressBarBordado)
        txtProgreso = view.findViewById(R.id.txtProgreso)
        btnReiniciar = view.findViewById(R.id.btnReiniciar)
        toolbarJuego = view.findViewById(R.id.toolbarJuego)
        btnInfoBordados = view.findViewById(R.id.btnInfoBordados)

        toolbarJuego.setNavigationOnClickListener {
            mostrarSeleccion()
        }

        btnReiniciar.setOnClickListener { reiniciarBordado() }

        btnInfoBordados.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Información")
                .setMessage("Los bordados están hechos con la puntada Punto de Cruz")
                .setPositiveButton("Entendido", null)
                .show()
        }

        setupRecyclerView()
        listarPatronesDesdeFirebase()

        return view
    }

    private fun setupRecyclerView() {
        adaptador = PatronesAdapter(emptyList()) { patronSeleccionado ->
            iniciarJuego(patronSeleccionado)
        }
        rvPatrones.layoutManager = GridLayoutManager(context, 2)
        rvPatrones.adapter = adaptador
    }

    private fun listarPatronesDesdeFirebase() {
        db.child("patrones").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = mutableListOf<PatronBordado>()
                for (postSnapshot in snapshot.children) {
                    val patron = postSnapshot.getValue(PatronBordado::class.java)
                    patron?.let { lista.add(it) }
                }
                adaptador.updateData(lista)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun iniciarJuego(patron: PatronBordado) {
        val userId = auth.currentUser?.uid ?: return
        patronActual = patron

        layoutSeleccion.visibility = View.GONE
        layoutJuego.visibility = View.VISIBLE

        canvas.patron = patron
        actualizarPaleta(patron)

        db.child("progreso").child(userId).child(patron.id).get().addOnSuccessListener { snapshot ->
            val progreso = snapshot.getValue(ProgresoUsuario::class.java)
            canvas.puntosPintados.clear()
            progreso?.let { canvas.puntosPintados.putAll(it.puntosPintados) }
            canvas.invalidate()
            actualizarBarraProgreso()
        }

        canvas.onPuntoPintado = {
            db.child("progreso").child(userId).child(patron.id)
                .setValue(ProgresoUsuario(canvas.puntosPintados))
            actualizarBarraProgreso()
        }
    }

    private fun reiniciarBordado() {
        val userId = auth.currentUser?.uid ?: return
        val patron = patronActual ?: return

        canvas.puntosPintados.clear()
        canvas.invalidate()
        actualizarBarraProgreso()

        db.child("progreso").child(userId).child(patron.id).removeValue()
    }

    private fun actualizarBarraProgreso() {
        val p = patronActual ?: return
        val totalPuntos = p.matriz.flatten().count { it != 0 }
        val puntosCompletados = canvas.puntosPintados.size
        
        val porcentaje = if (totalPuntos > 0) (puntosCompletados * 100) / totalPuntos else 0
        progressBar.progress = porcentaje
        txtProgreso.text = "Progreso: $porcentaje%"
        
        p.porcentaje = porcentaje

        if (porcentaje == 100) {
            mostrarMensajeCompletado()
        }
    }

    private fun mostrarMensajeCompletado() {
        val p = patronActual ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("¡Felicidades!")
            .setMessage("Has completado el bordado de ${p.nombre}. ¿Deseas añadirlo al carrito para comprarlo físicamente?")
            .setPositiveButton("Añadir al Carrito") { _, _ ->
                anadirAlCarrito(p)
            }
            .setNegativeButton("Ahora no", null)
            .show()
    }

    private fun anadirAlCarrito(patron: PatronBordado) {
        val user = auth.currentUser ?: return
        val dbFirestore = FirebaseFirestore.getInstance()
        
        val item = CarritoItem(
            carritoItemId = "bordado_${patron.id}",
            nombre = "Bordado: ${patron.nombre}",
            tamano = "Único",
            precio = 250.0,
            cantidad = 1,
            imagenUrl = patron.urlImagen
        )

        val carritoRef = dbFirestore.collection("carritos").document(user.uid).collection("items")
        // ID CONSISTENTE: cafeId + tamano para que el adaptador pueda actualizarlo/borrarlo
        val itemId = "${item.carritoItemId}_${item.tamano}"
        val docRef = carritoRef.document(itemId)

        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val cantidadActual = document.getLong("cantidad") ?: 1
                docRef.update("cantidad", cantidadActual + 1)
                    .addOnSuccessListener { 
                        Toast.makeText(context, "Añadido al carrito", Toast.LENGTH_SHORT).show()
                    }
            } else {
                docRef.set(item)
                    .addOnSuccessListener { 
                        Toast.makeText(context, "Añadido al carrito", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun mostrarSeleccion() {
        layoutJuego.visibility = View.GONE
        layoutSeleccion.visibility = View.VISIBLE
        adaptador.notifyDataSetChanged()
    }

    private fun actualizarPaleta(patron: PatronBordado) {
        paleta.removeAllViews()
        val size = dpToPx(50)
        val margin = dpToPx(8)

        patron.paleta.forEach { colorInfo ->
            if (colorInfo.id == 0) return@forEach

            val frame = FrameLayout(requireContext())
            val frameParams = LinearLayout.LayoutParams(size, size)
            frameParams.setMargins(margin, margin, margin, margin)
            frame.layoutParams = frameParams

            val btn = View(context)
            btn.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            val colorBase = Color.parseColor(colorInfo.hex)
            btn.setBackgroundColor(colorBase)

            btn.setOnClickListener {
                ultimoBotonSeleccionado?.let {
                    val prevColorId = canvas.colorSeleccionadoId
                    val prevHex = patron.paleta.find { it.id == prevColorId }?.hex ?: "#FFFFFF"
                    it.background = ColorDrawable(Color.parseColor(prevHex))
                }

                canvas.colorSeleccionadoId = colorInfo.id
                val shape = GradientDrawable()
                shape.shape = GradientDrawable.RECTANGLE
                shape.setColor(colorBase)
                shape.setStroke(6, Color.BLACK)
                btn.background = shape

                ultimoBotonSeleccionado = btn
            }

            val tv = TextView(context)
            tv.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            tv.text = colorInfo.id.toString()
            tv.setTextColor(if (esColorOscuro(colorInfo.hex)) Color.WHITE else Color.BLACK)
            tv.gravity = Gravity.CENTER
            tv.textSize = 16f
            tv.setTypeface(null, android.graphics.Typeface.BOLD)

            frame.addView(btn)
            frame.addView(tv)
            paleta.addView(frame)
        }
    }

    private fun mostrarDialogoSubirJSON() {
        val editText = EditText(requireContext()).apply {
            hint = "Pega el código JSON aquí"
            gravity = Gravity.TOP
            minLines = 5
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Nuevo Patrón de Bordado")
            .setView(editText)
            .setPositiveButton("Subir a Firebase") { _, _ ->
                val jsonRaw = editText.text.toString()
                if (jsonRaw.isNotEmpty()) {
                    subirPatronAFirebase(jsonRaw)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun subirPatronAFirebase(json: String) {
        try {
            val patron = Gson().fromJson(json, PatronBordado::class.java)
            db.child("patrones").child(patron.id).setValue(patron)
                .addOnSuccessListener {
                    Toast.makeText(context, "¡Patrón ${patron.nombre} subido!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al subir", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "JSON Inválido: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    fun esColorOscuro(hex: String): Boolean {
        val color = Color.parseColor(hex)
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
}

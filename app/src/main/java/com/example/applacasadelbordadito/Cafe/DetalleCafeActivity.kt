package com.example.applacasadelbordadito

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.applacasadelbordadito.Cafe.Cafe
import com.example.applacasadelbordadito.Carrito.CarritoActivity
import com.example.applacasadelbordadito.Carrito.CarritoItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DetalleCafeActivity : AppCompatActivity() {

    private lateinit var imgCafe: ImageView
    private lateinit var txtNombre: TextView
    private lateinit var txtDescripcion: TextView
    private lateinit var txtCategoria: TextView
    private lateinit var radioPrecios: RadioGroup
    private lateinit var btnAgregar: Button
    private lateinit var toolbar: MaterialToolbar

    private var cafeActual: Cafe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_cafe)

        toolbar = findViewById(R.id.toolbar)
        imgCafe = findViewById(R.id.imgCafe)
        txtNombre = findViewById(R.id.txtNombre)
        txtDescripcion = findViewById(R.id.txtDescripcion)
        txtCategoria = findViewById(R.id.txtCategoria)
        radioPrecios = findViewById(R.id.radioPrecios)
        btnAgregar = findViewById(R.id.btnAgregar)

        // Configurar botón de retroceso
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val cafeId = intent.getStringExtra("cafeId")
        cargarCafe(cafeId)

        btnAgregar.setOnClickListener {
            val seleccionado = radioPrecios.checkedRadioButtonId

            if (seleccionado == -1) {
                Toast.makeText(this, "Selecciona un tamaño", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val radio = findViewById<RadioButton>(seleccionado)
            val datos = radio.tag as Pair<String, Double>

            val tamano = datos.first
            val precio = datos.second

            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                Toast.makeText(this,"Debes iniciar sesión",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val item = CarritoItem(
                carritoItemId = cafeActual!!.id,
                nombre = cafeActual!!.nombre,
                tamano = tamano,
                precio = precio,
                imagenUrl = cafeActual!!.imagenUrl
            )

            val db = FirebaseFirestore.getInstance()
            val carritoRef = db.collection("carritos").document(user.uid).collection("items")
            val itemId = "${item.carritoItemId}_${item.tamano}"
            val docRef = carritoRef.document(itemId)

            docRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val cantidadActual = document.getLong("cantidad") ?: 1
                    docRef.update("cantidad", cantidadActual + 1)
                        .addOnSuccessListener { irAlCarrito() }
                } else {
                    docRef.set(item)
                        .addOnSuccessListener { irAlCarrito() }
                }
            }
        }
    }

    private fun irAlCarrito() {
        Toast.makeText(this,"Agregado al carrito",Toast.LENGTH_SHORT).show()
        val intent = Intent(this, CarritoActivity::class.java)
        startActivity(intent)
    }

    private fun cargarCafe(id: String?) {
        if (id == null) return

        val db = FirebaseFirestore.getInstance()
        db.collection("cafes")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                val cafe = doc.toObject(Cafe::class.java)
                cafeActual = cafe

                if (cafe != null) {
                    txtNombre.text = cafe.nombre
                    txtDescripcion.text = cafe.descripcion
                    txtCategoria.text = "Categoría: ${cafe.categoria}"

                    imgCafe.load(cafe.imagenUrl)
                    radioPrecios.removeAllViews()

                    for ((tamano, precio) in cafe.tamano) {
                        val radio = RadioButton(this)
                        radio.text = "$tamano - $precio MXN"
                        radio.tag = Pair(tamano, precio)
                        radioPrecios.addView(radio)
                    }
                }
            }
    }
}
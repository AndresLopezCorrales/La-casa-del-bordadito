package com.example.applacasadelbordadito.Carrito

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.MainActivity
import com.example.applacasadelbordadito.R
import com.example.applacasadelbordadito.Perfil.PaymentMethod
import com.example.applacasadelbordadito.Perfil.PaymentMethodAdapter
import com.example.applacasadelbordadito.Perfil.PaymentSelectionAdapter
import com.example.applacasadelbordadito.Perfil.TarjetaAgregarActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class CarritoActivity : AppCompatActivity() {

    private lateinit var recyclerCarrito: RecyclerView
    private lateinit var txtTotal: TextView
    private lateinit var btnComprar: Button
    private lateinit var btnSeguir: Button
    private lateinit var toolbar: MaterialToolbar
    private lateinit var togglePayment: MaterialButtonToggleGroup

    private val lista = mutableListOf<CarritoItem>()
    private val listaMetodosPago = mutableListOf<PaymentMethod>()
    private lateinit var adapter: CarritoAdapter
    private var totalCompra = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        toolbar = findViewById(R.id.toolbar)
        recyclerCarrito = findViewById(R.id.recyclerCarrito)
        txtTotal = findViewById(R.id.txtTotal)
        btnComprar = findViewById(R.id.btnComprar)
        btnSeguir = findViewById(R.id.btnSeguir)
        togglePayment = findViewById(R.id.togglePayment)

        adapter = CarritoAdapter(lista)

        recyclerCarrito.layoutManager = LinearLayoutManager(this)
        recyclerCarrito.adapter = adapter

        // Configurar botón de retroceso para ir al fragment de café en MainActivity
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("fragment", "cafe")
            startActivity(intent)
            finish()
        }

        escucharCarrito() //Cambios en tiempo real cuando se agrega un item
        cargarMetodosPago()

        // Ir al fragment de café en MainActivity al hacer clic en el botón "Seguir"
        btnSeguir.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("fragment", "cafe")
            startActivity(intent)
            finish()
        }

        btnComprar.setOnClickListener {
            if (togglePayment.checkedButtonId == R.id.btnCard) {
                if (listaMetodosPago.isEmpty()) {
                    mostrarDialogoNoTarjetas()
                } else {
                    mostrarDialogoSeleccionarTarjeta()
                }
            } else {
                procesarCompra("Efectivo")
            }
        }
    }

    private fun cargarMetodosPago() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
            .child(user.uid).child("metodosPago")

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
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mostrarDialogoNoTarjetas() {
        AlertDialog.Builder(this)
            .setTitle("Sin Tarjetas")
            .setMessage("No tienes ninguna tarjeta agregada. ¿Deseas agregar una ahora?")
            .setPositiveButton("Agregar tarjeta") { _, _ ->
                val intent = Intent(this, TarjetaAgregarActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoSeleccionarTarjeta() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_payment, null)
        val recycler = dialogView.findViewById<RecyclerView>(R.id.rvSelectPayment)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Selecciona una tarjeta")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        val adapterCards = PaymentSelectionAdapter(listaMetodosPago) { selected ->
            procesarCompra("Tarjeta (${selected.brand} ****${selected.last4})")
            dialog.dismiss()
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapterCards

        dialog.show()
    }

    private fun procesarCompra(metodoPago: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // Creamos el objeto de la orden
        val orden = hashMapOf(
            "usuario" to user.email,
            "total" to "$totalCompra MXN",
            "fecha" to System.currentTimeMillis(),
            "items" to lista,
            "metodoPago" to metodoPago
        )

        btnComprar.isEnabled = false // Evitar múltiples clics

        db.collection("ordenes")
            .add(orden)
            .addOnSuccessListener { document ->

                // Guardamos copias de los datos para el QR antes de borrar
                val listaCopia = ArrayList(lista)
                val totalCopia = "$totalCompra MXN"
                val ordenId = document.id

                // VACIAR CARRITO EN FIRESTORE
                val cartRef = db.collection("carritos").document(user.uid).collection("items")
                cartRef.get().addOnSuccessListener { snapshot ->
                    val batch = db.batch()
                    for (doc in snapshot) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().addOnSuccessListener {
                        Toast.makeText(this, "Compra exitosa", Toast.LENGTH_SHORT).show()

                        // Ir a la pantalla del QR
                        val intent = Intent(this, QRActivity::class.java)
                        intent.putExtra("ordenId", ordenId)
                        intent.putExtra("usuario", user.email)
                        intent.putExtra("total", totalCopia)
                        intent.putExtra("items", listaCopia)
                        intent.putExtra("metodoPago", metodoPago)
                        startActivity(intent)

                        finish() // Cerrar carrito
                    }
                }.addOnFailureListener {
                    // Si falla vaciar, igual mostramos el QR pero avisamos
                    val intent = Intent(this, QRActivity::class.java)
                    intent.putExtra("ordenId", ordenId)
                    intent.putExtra("usuario", user.email)
                    intent.putExtra("total", totalCopia)
                    intent.putExtra("items", listaCopia)
                    intent.putExtra("metodoPago", metodoPago)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener {
                btnComprar.isEnabled = true
                Toast.makeText(this, "Error al procesar la compra", Toast.LENGTH_SHORT).show()
            }
    }

    private fun escucharCarrito() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseFirestore.getInstance()
            .collection("carritos")
            .document(user.uid)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    lista.clear()
                    totalCompra = 0.0

                    for (doc in snapshot.documents) {
                        val item = doc.toObject(CarritoItem::class.java)
                        if (item != null) {
                            lista.add(item)
                            totalCompra += item.precio * item.cantidad
                        }
                    }

                    adapter.notifyDataSetChanged()
                    txtTotal.text = "$totalCompra MXN"
                    btnComprar.isEnabled = totalCompra > 0
                }
            }
    }
}

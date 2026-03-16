package com.example.applacasadelbordadito.Carrito

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.MainActivity
import com.example.applacasadelbordadito.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CarritoActivity : AppCompatActivity() {

    private lateinit var recyclerCarrito: RecyclerView
    private lateinit var txtTotal: TextView
    private lateinit var btnComprar: Button
    private lateinit var btnSeguir: Button

    private val lista = mutableListOf<CarritoItem>()
    private lateinit var adapter: CarritoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        recyclerCarrito = findViewById(R.id.recyclerCarrito)
        txtTotal = findViewById(R.id.txtTotal)
        btnComprar = findViewById(R.id.btnComprar)
        btnSeguir = findViewById(R.id.btnSeguir)

        adapter = CarritoAdapter(lista)

        recyclerCarrito.layoutManager = LinearLayoutManager(this)
        recyclerCarrito.adapter = adapter

        // 👇 ESCUCHAR CAMBIOS EN TIEMPO REAL
        escucharCarrito()

        btnSeguir.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("fragment", "cafe")

            startActivity(intent)
            finish()
        }

        btnComprar.setOnClickListener {

            // siguiente paso será generar QR
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

                    var total = 0.0



                    for (doc in snapshot.documents) {

                        val item = doc.toObject(CarritoItem::class.java)

                        if (item != null) {

                            lista.add(item)

                            total += item.precio * item.cantidad
                        }

                    }

                    // actualizar lista
                    adapter.notifyDataSetChanged()

                    txtTotal.text = "Total: $total MXN"

                    // habilitar o deshabilitar botón
                    btnComprar.isEnabled = total > 0

                }
            }
    }
}
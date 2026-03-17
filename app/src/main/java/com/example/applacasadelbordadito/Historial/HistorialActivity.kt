package com.example.applacasadelbordadito.Historial

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HistorialActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private val listaOrdenes = mutableListOf<Orden>()
    private lateinit var adapter: HistorialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        toolbar = findViewById(R.id.toolbar)
        recycler = findViewById(R.id.recyclerHistorial)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // uso de LinearLayoutManager
        adapter = HistorialAdapter(listaOrdenes)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        cargarHistorial()
    }

    private fun cargarHistorial() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // Consulta de ordenes por usuario
        db.collection("ordenes")
            .whereEqualTo("usuario", user.email)
            .get()
            .addOnSuccessListener { result ->
                listaOrdenes.clear()
                for (doc in result) {
                    val orden = doc.toObject(Orden::class.java)
                    orden.id = doc.id
                    listaOrdenes.add(orden)
                }

                // Ordenamos por fecha de forma descendente localmente
                listaOrdenes.sortByDescending { it.fecha }

                adapter.notifyDataSetChanged()

                if (listaOrdenes.isEmpty()) {
                    Toast.makeText(this, "No tienes compras registradas", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
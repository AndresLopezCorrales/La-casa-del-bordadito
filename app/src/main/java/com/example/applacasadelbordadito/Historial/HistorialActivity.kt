package com.example.applacasadelbordadito.Historial

import android.content.Intent
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

        adapter = HistorialAdapter(listaOrdenes) { orden ->
            val intent = Intent(this, HistorialDetalleActivity::class.java)
            intent.putExtra("orden", orden)
            startActivity(intent)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        cargarHistorial()
    }

    private fun cargarHistorial() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

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

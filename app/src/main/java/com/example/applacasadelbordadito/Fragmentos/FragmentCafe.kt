package com.example.applacasadelbordadito.Fragmentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.Cafe.AgregarCafeActivity
import com.example.applacasadelbordadito.Cafe.Cafe
import com.example.applacasadelbordadito.Cafe.CafeAdapter
import com.example.applacasadelbordadito.DetalleCafeActivity
import com.example.applacasadelbordadito.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FragmentCafe : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var fabAddCafe: FloatingActionButton
    private val listaCafes = mutableListOf<Cafe>()
    private lateinit var adapter: CafeAdapter
    private val auth = FirebaseAuth.getInstance()

    private val MY_ADMIN_UID = "P3bMLh6zQcd60w0QX5nHN1hOiHe2"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_cafe, container, false)

        recycler = view.findViewById(R.id.recyclerCafes)
        fabAddCafe = view.findViewById(R.id.fabAddCafe)

        // Lógica de administrador para mostrar el botón
        if (auth.currentUser?.uid == MY_ADMIN_UID) {
            fabAddCafe.visibility = View.VISIBLE
        }

        fabAddCafe.setOnClickListener {
            startActivity(Intent(requireContext(), AgregarCafeActivity::class.java))
        }

        adapter = CafeAdapter(listaCafes) { cafeSeleccionado ->
            abrirDetalleCafe(cafeSeleccionado)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        cargarCafes()

        return view
    }

    private fun cargarCafes() {
        val db = FirebaseFirestore.getInstance()
        db.collection("cafes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    listaCafes.clear()
                    for (doc in snapshot.documents) {
                        val cafe = doc.toObject(Cafe::class.java)
                        if (cafe != null) {
                            cafe.id = doc.id
                            listaCafes.add(cafe)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun abrirDetalleCafe(cafe: Cafe) {
        val intent = Intent(requireContext(), DetalleCafeActivity::class.java)
        intent.putExtra("cafeId", cafe.id)
        startActivity(intent)
    }
}
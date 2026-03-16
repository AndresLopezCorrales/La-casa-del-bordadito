package com.example.applacasadelbordadito.Fragmentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.Cafe.Cafe
import com.example.applacasadelbordadito.Cafe.CafeAdapter
import com.example.applacasadelbordadito.DetalleCafeActivity
import com.example.applacasadelbordadito.R
import com.google.firebase.firestore.FirebaseFirestore

class FragmentCafe : Fragment() {

    private lateinit var recycler: RecyclerView
    private val listaCafes = mutableListOf<Cafe>()
    private lateinit var adapter: CafeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_cafe, container, false)

        recycler = view.findViewById(R.id.recyclerCafes)

        adapter = CafeAdapter(listaCafes) { cafeSeleccionado ->
            abrirDetalleCafe(cafeSeleccionado)
        }

        recycler.layoutManager = GridLayoutManager(requireContext(),1)
        recycler.adapter = adapter

        cargarCafes()

        return view
    }

    private fun cargarCafes() {

        val db = FirebaseFirestore.getInstance()

        db.collection("cafes")
            .get()
            .addOnSuccessListener { result ->

                listaCafes.clear()

                for (doc in result) {
                    val cafe = doc.toObject(Cafe::class.java)
                    listaCafes.add(cafe)
                }

                adapter.notifyDataSetChanged()
            }
    }

    private fun abrirDetalleCafe(cafe: Cafe) {

        val intent = Intent(requireContext(), DetalleCafeActivity::class.java)

        intent.putExtra("cafeId", cafe.id)

        startActivity(intent)
    }
}
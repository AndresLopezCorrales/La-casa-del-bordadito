package com.example.applacasadelbordadito.Fragmentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.Cafe.AgregarCafeActivity
import com.example.applacasadelbordadito.Cafe.Cafe
import com.example.applacasadelbordadito.Cafe.CafeAdapter
import com.example.applacasadelbordadito.Cafe.CafeInicioAdapter
import com.example.applacasadelbordadito.Carrito.CarritoItem
import com.example.applacasadelbordadito.DetalleCafeActivity
import com.example.applacasadelbordadito.R
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FragmentCafe : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var fabAddCafe: FloatingActionButton
    private lateinit var toggleViewType: MaterialButtonToggleGroup
    private val listaCafes = mutableListOf<Cafe>()
    private lateinit var adapterGrid: CafeInicioAdapter
    private lateinit var adapterList: CafeAdapter
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_cafe, container, false)

        recycler = view.findViewById(R.id.recyclerCafes)
        fabAddCafe = view.findViewById(R.id.fabAddCafe)
        toggleViewType = view.findViewById(R.id.toggleViewType)

        checkUserRole()

        fabAddCafe.setOnClickListener {
            startActivity(Intent(requireContext(), AgregarCafeActivity::class.java))
        }

        setupAdapters()
        setupViewTypeToggle()

        cargarCafes()

        return view
    }

    private fun setupAdapters() {
        adapterGrid = CafeInicioAdapter(listaCafes, { cafe ->
            anadirAlCarrito(cafe)
        }, { cafe ->
            abrirDetalleCafe(cafe)
        })

        adapterList = CafeAdapter(listaCafes) { cafe ->
            abrirDetalleCafe(cafe)
        }

        // Default: Grid
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapterGrid
    }

    private fun setupViewTypeToggle() {
        toggleViewType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnGrid -> {
                        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
                        recycler.adapter = adapterGrid
                    }
                    R.id.btnList -> {
                        recycler.layoutManager = LinearLayoutManager(requireContext())
                        recycler.adapter = adapterList
                    }
                }
            }
        }
    }

    private fun anadirAlCarrito(cafe: Cafe) {
        val user = auth.currentUser ?: return
        val dbFirestore = FirebaseFirestore.getInstance()

        val primerTamano = cafe.tamano.keys.firstOrNull() ?: "Mediano"
        val precio = cafe.tamano[primerTamano] ?: 0.0

        val item = CarritoItem(
            carritoItemId = cafe.id,
            nombre = cafe.nombre,
            tamano = primerTamano,
            precio = precio,
            cantidad = 1,
            imagenUrl = cafe.imagenUrl
        )

        val carritoRef = dbFirestore.collection("carritos").document(user.uid).collection("items")
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

    private fun cargarCafes() {
        val db = FirebaseFirestore.getInstance()
        db.collection("cafes")
            .whereEqualTo("isActive", true)
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
                    adapterGrid.notifyDataSetChanged()
                    adapterList.notifyDataSetChanged()
                }
            }
    }

    private fun checkUserRole() {
        val uid = auth.uid ?: return
        val db = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Usuarios")
        db.child(uid).child("esAdmin").get().addOnSuccessListener { snapshot ->
            val esAdmin = snapshot.getValue(Boolean::class.java) ?: false
            if (esAdmin) {
                fabAddCafe.visibility = View.VISIBLE
            } else {
                fabAddCafe.visibility = View.GONE
            }
        }
    }

    private fun abrirDetalleCafe(cafe: Cafe) {
        val intent = Intent(requireContext(), DetalleCafeActivity::class.java)
        intent.putExtra("cafeId", cafe.id)
        startActivity(intent)
    }
}
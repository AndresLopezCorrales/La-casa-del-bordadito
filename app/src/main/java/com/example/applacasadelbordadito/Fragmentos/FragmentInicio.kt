package com.example.applacasadelbordadito.Fragmentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.applacasadelbordadito.Cafe.Cafe
import com.example.applacasadelbordadito.Cafe.CafeAdapter
import com.example.applacasadelbordadito.Cafe.CafeInicioAdapter
import com.example.applacasadelbordadito.DetalleCafeActivity
import com.example.applacasadelbordadito.Bordado.PatronBordado
import com.example.applacasadelbordadito.Bordado.PatronesAdapter
import com.example.applacasadelbordadito.Carrito.CarritoItem
import com.example.applacasadelbordadito.MainActivity
import com.example.applacasadelbordadito.R
import com.example.applacasadelbordadito.Taller.TallerActivity
import com.example.applacasadelbordadito.databinding.FragmentInicioBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class FragmentInicio : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val listaCafes = mutableListOf<Cafe>()
    private val listaBordados = mutableListOf<PatronBordado>()
    
    private lateinit var adapterCafeGrid: CafeInicioAdapter
    private lateinit var adapterCafeList: CafeAdapter
    private lateinit var adapterBordado: PatronesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSlider()
        setupToggles()
        setupAdapters()
        setupViewTypeToggle()
        
        cargarCafes()
        cargarBordados()

        // Click del póster
        binding.layoutPosterBordado.root.setOnClickListener {
            (activity as? MainActivity)?.verFragmentBordado()
        }
    }

    private fun setupSlider() {
        val slides = listOf(
            SlideItem(R.drawable.img_cafe_inicio, "Descubre el Café", "Ver menú") { (activity as? MainActivity)?.verFragmentCafe() },
            SlideItem(R.drawable.img_bordado_inicio, "Borda y juega", "Ir a bordados") { (activity as? MainActivity)?.verFragmentBordado() },
            SlideItem(R.drawable.img_poster_inicio, "Talleres Presenciales", "Ver flyer") { startActivity(Intent(requireContext(), TallerActivity::class.java)) }
        )

        binding.viewPagerSlider.adapter = SliderAdapter(slides)
        TabLayoutMediator(binding.tabIndicator, binding.viewPagerSlider) { _, _ -> }.attach()
    }

    private fun setupToggles() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnToggleCafe -> {
                        binding.recyclerInicio.visibility = View.VISIBLE
                        binding.layoutPosterBordado.root.visibility = View.GONE
                        binding.layoutHeaderCafe.visibility = View.VISIBLE
                        
                        if (binding.toggleViewType.checkedButtonId == R.id.btnGrid) {
                            binding.recyclerInicio.adapter = adapterCafeGrid
                        } else {
                            binding.recyclerInicio.adapter = adapterCafeList
                        }
                    }
                    R.id.btnToggleBordado -> {
                        binding.recyclerInicio.visibility = View.GONE
                        binding.layoutHeaderCafe.visibility = View.GONE
                        binding.layoutPosterBordado.root.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupAdapters() {
        adapterCafeGrid = CafeInicioAdapter(listaCafes, { cafe ->
            anadirAlCarrito(cafe)
        }, { cafe ->
            irADetalle(cafe)
        })

        adapterCafeList = CafeAdapter(listaCafes) { cafe ->
            irADetalle(cafe)
        }

        adapterBordado = PatronesAdapter(listaBordados) { patron ->
            (activity as? MainActivity)?.verFragmentBordado(patron.id)
        }

        binding.recyclerInicio.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerInicio.adapter = adapterCafeGrid
    }

    private fun irADetalle(cafe: Cafe) {
        val intent = Intent(requireContext(), DetalleCafeActivity::class.java)
        intent.putExtra("cafeId", cafe.id)
        startActivity(intent)
    }

    private fun setupViewTypeToggle() {
        binding.toggleViewType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnGrid -> {
                        binding.recyclerInicio.layoutManager = GridLayoutManager(requireContext(), 2)
                        binding.recyclerInicio.adapter = adapterCafeGrid
                    }
                    R.id.btnList -> {
                        binding.recyclerInicio.layoutManager = LinearLayoutManager(requireContext())
                        binding.recyclerInicio.adapter = adapterCafeList
                    }
                }
            }
        }
    }

    private fun anadirAlCarrito(cafe: Cafe) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val dbFirestore = FirebaseFirestore.getInstance()

        // Tomar el primer tamaño disponible como predeterminado
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
        FirebaseFirestore.getInstance().collection("cafes").get().addOnSuccessListener { result ->
            listaCafes.clear()
            for (doc in result) {
                val cafe = doc.toObject(Cafe::class.java)
                cafe.id = doc.id // CORRECCIÓN: Asignamos el ID del documento
                listaCafes.add(cafe)
            }
            adapterCafeGrid.notifyDataSetChanged()
            adapterCafeList.notifyDataSetChanged()
        }
    }

    private fun cargarBordados() {
        FirebaseDatabase.getInstance().reference.child("patrones").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaBordados.clear()
                for (postSnapshot in snapshot.children) {
                    val patron = postSnapshot.getValue(PatronBordado::class.java)
                    if (patron != null) {
                        patron.id = postSnapshot.key ?: "" // CORRECCIÓN: Asignamos la clave de Realtime DB
                        listaBordados.add(patron)
                    }
                }
                adapterBordado.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SliderAdapter(private val items: List<SlideItem>) : RecyclerView.Adapter<SliderAdapter.SlideViewHolder>() {
        inner class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val img: ImageView = view.findViewById(R.id.ivSlide)
            val title: TextView = view.findViewById(R.id.tvSlideTitle)
            val btn: View = view.findViewById(R.id.btnSlideAction)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_slide, parent, false)
            return SlideViewHolder(view)
        }

        override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
            val item = items[position]
            holder.img.setImageResource(item.imageRes)
            holder.title.text = item.title
            holder.btn.setOnClickListener { item.action() }
        }

        override fun getItemCount() = items.size
    }

    data class SlideItem(val imageRes: Int, val title: String, val btnText: String, val action: () -> Unit)
}

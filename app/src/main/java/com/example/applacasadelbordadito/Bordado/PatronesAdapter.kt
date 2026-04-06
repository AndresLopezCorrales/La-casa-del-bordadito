package com.example.applacasadelbordadito.Bordado

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.applacasadelbordadito.Carrito.CarritoItem
import com.example.applacasadelbordadito.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class PatronesAdapter(
    private var lista: List<PatronBordado>,
    private val onSelected: (PatronBordado) -> Unit
) : RecyclerView.Adapter<PatronesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagen: ImageView = view.findViewById(R.id.imgPatron)
        val nombre: TextView = view.findViewById(R.id.txtNombrePatron)
        val txtPorcentaje: TextView = view.findViewById(R.id.txtPorcentajeItem)
        val progress: LinearProgressIndicator = view.findViewById(R.id.progressItem)
        val btnAnadir: MaterialButton = view.findViewById(R.id.btnAnadirCarritoItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patron, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patron = lista[position]
        holder.nombre.text = patron.nombre

        Glide.with(holder.itemView.context)
            .load(patron.urlImagen)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.imagen)

        // Mostrar valor actual de progreso inmediatamente
        holder.txtPorcentaje.text = "${patron.porcentaje}%"
        holder.progress.progress = patron.porcentaje

        // Cargar progreso desde Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().reference
                .child("progreso").child(userId).child(patron.id)
                .get().addOnSuccessListener { snapshot ->
                    val progreso = snapshot.getValue(ProgresoUsuario::class.java)
                    val puntosPintados = progreso?.puntosPintados?.size ?: 0
                    
                    val totalPuntos = patron.matriz.flatten().count { it != 0 }
                    val porcentaje = if (totalPuntos > 0) (puntosPintados * 100) / totalPuntos else 0
                    
                    patron.porcentaje = porcentaje
                    
                    holder.txtPorcentaje.text = "$porcentaje%"
                    holder.progress.progress = porcentaje
                }
        }

        holder.btnAnadir.setOnClickListener {
            anadirAlCarrito(patron, holder.itemView)
        }

        holder.itemView.setOnClickListener { onSelected(patron) }
    }

    private fun anadirAlCarrito(patron: PatronBordado, view: View) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(view.context, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

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
        val itemId = "${item.carritoItemId}_${item.tamano}"
        val docRef = carritoRef.document(itemId)

        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val cantidadActual = document.getLong("cantidad") ?: 1
                docRef.update("cantidad", cantidadActual + 1)
                    .addOnSuccessListener {
                        Toast.makeText(view.context, "Añadido al carrito", Toast.LENGTH_SHORT).show()
                    }
            } else {
                docRef.set(item)
                    .addOnSuccessListener {
                        Toast.makeText(view.context, "Añadido al carrito", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(view.context, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = lista.size

    fun updateData(nuevaLista: List<PatronBordado>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
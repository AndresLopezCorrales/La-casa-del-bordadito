package com.example.applacasadelbordadito.Cafe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.applacasadelbordadito.R

class CafeInicioAdapter(
    private var listaCafes: List<Cafe>,
    private val onAddClick: (Cafe) -> Unit,
    private val onItemClick: (Cafe) -> Unit
) : RecyclerView.Adapter<CafeInicioAdapter.CafeViewHolder>() {

    class CafeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.txtNombre)
        val imagen: ImageView = view.findViewById(R.id.imgCafe)
        val btnAdd: View = view.findViewById(R.id.btnAddCafe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CafeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cafe_no_desc, parent, false)
        return CafeViewHolder(view)
    }

    override fun onBindViewHolder(holder: CafeViewHolder, position: Int) {
        val cafe = listaCafes[position]
        holder.nombre.text = cafe.nombre
        holder.imagen.load(cafe.imagenUrl)

        holder.btnAdd.setOnClickListener {
            onAddClick(cafe)
        }

        holder.itemView.setOnClickListener {
            onItemClick(cafe)
        }
    }

    override fun getItemCount(): Int = listaCafes.size

    fun updateData(newList: List<Cafe>) {
        listaCafes = newList
        notifyDataSetChanged()
    }
}
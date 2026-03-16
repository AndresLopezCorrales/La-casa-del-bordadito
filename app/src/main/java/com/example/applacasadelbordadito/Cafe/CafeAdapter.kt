package com.example.applacasadelbordadito.Cafe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.applacasadelbordadito.R

class CafeAdapter(
    private val listaCafes: List<Cafe>,
    private val onClick: (Cafe) -> Unit
) : RecyclerView.Adapter<CafeAdapter.CafeViewHolder>() {

    class CafeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.txtNombre)
        val descripcion: TextView = view.findViewById(R.id.txtDescripcion)
        val imagen: ImageView = view.findViewById(R.id.imgCafe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CafeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cafe, parent, false)

        return CafeViewHolder(view)
    }

    override fun getItemCount(): Int = listaCafes.size

    override fun onBindViewHolder(holder: CafeViewHolder, position: Int) {

        val cafe = listaCafes[position]

        holder.nombre.text = cafe.nombre
        holder.descripcion.text = cafe.descripcion

        holder.imagen.load("https://thumbs.dreamstime.com/b/espreso-en-una-taza-blanca-68138823.jpg") //cafe.imagenUrl

        holder.itemView.setOnClickListener {
            onClick(cafe)
        }
    }
}
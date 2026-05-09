package com.example.applacasadelbordadito.Cafe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.applacasadelbordadito.R
import com.google.android.material.button.MaterialButton

class CafeAdminAdapter(
    private val onToggleStatus: (Cafe) -> Unit,
    private val onEdit: (Cafe) -> Unit
) : RecyclerView.Adapter<CafeAdminAdapter.CafeAdminViewHolder>() {

    private val listaCafes = mutableListOf<Cafe>()

    private class CafeDiffCallback(
        private val oldList: List<Cafe>,
        private val newList: List<Cafe>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos].id == newList[newPos].id

        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos] == newList[newPos]
    }

    fun updateList(nuevaLista: List<Cafe>) {
        val oldList = listaCafes.map { it.copy() }

        // LOG 1: ¿Qué trae la nueva lista?
        nuevaLista.forEach { cafe ->
            android.util.Log.d("CafeAdmin", "NUEVA LISTA -> id=${cafe.id} nombre=${cafe.nombre} isActive=${cafe.isActive}")
        }
        // LOG 2: ¿Qué tenía la lista vieja?
        oldList.forEach { cafe ->
            android.util.Log.d("CafeAdmin", "LISTA VIEJA -> id=${cafe.id} nombre=${cafe.nombre} isActive=${cafe.isActive}")
        }

        listaCafes.clear()
        listaCafes.addAll(nuevaLista)

        val diffResult = DiffUtil.calculateDiff(CafeDiffCallback(oldList, listaCafes))
        diffResult.dispatchUpdatesTo(this)
    }

    class CafeAdminViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.txtNombre)
        val estado: TextView = view.findViewById(R.id.txtEstado)
        val imagen: ImageView = view.findViewById(R.id.imgCafe)
        val btnToggle: MaterialButton = view.findViewById(R.id.btnToggleStatus)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CafeAdminViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cafe_admin, parent, false)
        return CafeAdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: CafeAdminViewHolder, position: Int) {
        val cafe = listaCafes[position]
        android.util.Log.d("CafeAdmin", "BIND -> id=${cafe.id} isActive=${cafe.isActive}")
        holder.nombre.text = cafe.nombre
        holder.imagen.load(cafe.imagenUrl)

        if (cafe.isActive) {
            holder.estado.text = holder.itemView.context.getString(R.string.status_activo)
            holder.estado.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.sage_green))
            holder.btnToggle.setIconResource(R.drawable.ic_remove)
            holder.btnToggle.setIconTintResource(R.color.terracotta)
        } else {
            holder.estado.text = holder.itemView.context.getString(R.string.status_deshabilitado)
            holder.estado.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.terracotta))
            holder.btnToggle.setIconResource(R.drawable.ic_add)
            holder.btnToggle.setIconTintResource(R.color.sage_green)
        }

        holder.btnToggle.setOnClickListener { onToggleStatus(cafe) }
        holder.btnEdit.setOnClickListener { onEdit(cafe) }
    }

    override fun getItemCount(): Int = listaCafes.size
}
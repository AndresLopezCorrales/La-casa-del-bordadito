package com.example.applacasadelbordadito.Bordado

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

class PatronesAdminAdapter(
    private val onToggleStatus: (PatronBordado) -> Unit,
    private val onDelete: (PatronBordado) -> Unit
) : RecyclerView.Adapter<PatronesAdminAdapter.PatronesAdminViewHolder>() {

    private val listaPatrones = mutableListOf<PatronBordado>()

    private class PatronDiffCallback(
        private val oldList: List<PatronBordado>,
        private val newList: List<PatronBordado>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos].id == newList[newPos].id

        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos] == newList[newPos]
    }

    fun updateList(nuevaLista: List<PatronBordado>) {
        val oldList = listaPatrones.map { it.copy() }
        listaPatrones.clear()
        listaPatrones.addAll(nuevaLista)

        val diffResult = DiffUtil.calculateDiff(PatronDiffCallback(oldList, listaPatrones))
        diffResult.dispatchUpdatesTo(this)
    }

    class PatronesAdminViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.txtNombre)
        val estado: TextView = view.findViewById(R.id.txtEstado)
        val imagen: ImageView = view.findViewById(R.id.imgPatron)
        val btnToggle: MaterialButton = view.findViewById(R.id.btnToggleStatus)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatronesAdminViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patron_admin, parent, false)
        return PatronesAdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatronesAdminViewHolder, position: Int) {
        val patron = listaPatrones[position]
        holder.nombre.text = patron.nombre
        holder.imagen.load(patron.urlImagen)

        if (patron.activo) {
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

        holder.btnToggle.setOnClickListener { onToggleStatus(patron) }
        holder.btnDelete.setOnClickListener { onDelete(patron) }
    }

    override fun getItemCount(): Int = listaPatrones.size
}

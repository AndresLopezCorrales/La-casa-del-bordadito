package com.example.applacasadelbordadito.Adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.applacasadelbordadito.Modelos.Usuario
import com.example.applacasadelbordadito.R

class AdaptadorUsuario(
    private val context: Context,
    private val listaUsuarios: List<Usuario>
) : RecyclerView.Adapter<AdaptadorUsuario.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listaUsuarios.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario = listaUsuarios[position]
        holder.uid.text = usuario.uid
        holder.email.text = usuario.email
        holder.nombres.text = usuario.nombres
        
        Glide.with(context)
            .load(usuario.imagen)
            .placeholder(R.drawable.ic_imagen_perfil)
            .into(holder.imagen)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uid: TextView = itemView.findViewById(R.id.item_uid)
        val email: TextView = itemView.findViewById(R.id.item_email)
        val nombres: TextView = itemView.findViewById(R.id.item_nombre)
        val imagen: ImageView = itemView.findViewById(R.id.item_imagen)
    }
}

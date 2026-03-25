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

class AdaptadorUsuario(context: Context, listaUsuarios: List<Usuario>)
    : RecyclerView.Adapter<AdaptadorUsuario.ViewHolder?>(){

    private val context: Context
    private val listaUsuarios : List<Usuario>

    init {
        this.context = context
        this.listaUsuarios = listaUsuarios
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdaptadorUsuario.ViewHolder {
        val view : View = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaUsuarios.size
    }

    override fun onBindViewHolder(holder: AdaptadorUsuario.ViewHolder, position: Int) {
        val usuario: Usuario = listaUsuarios[position]
        holder.uid.text = usuario.uid
        holder.email.text = usuario.email
        holder.nombres.text = usuario.nombres
        Glide.with(context).load(usuario.imagen).placeholder(R.drawable.ic_imagen_perfil).into(holder.imagen)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        var uid : TextView
        var email : TextView
        var nombres: TextView
        var imagen: ImageView

        init{
            uid = itemView.findViewById(R.id.item_uid)
            email = itemView.findViewById(R.id.item_email)
            nombres = itemView.findViewById(R.id.item_nombre)
            imagen = itemView.findViewById(R.id.item_imagen)
        }

    }



}
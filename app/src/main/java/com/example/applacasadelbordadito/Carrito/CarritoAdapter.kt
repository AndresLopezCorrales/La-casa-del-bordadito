package com.example.applacasadelbordadito.Carrito

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CarritoAdapter(private val lista: MutableList<CarritoItem>) :
    RecyclerView.Adapter<CarritoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtNombre: TextView = view.findViewById(R.id.txtNombreCarrito)
        val txtDetalles: TextView = view.findViewById(R.id.txtDetallesCarrito)
        val txtPrecio: TextView = view.findViewById(R.id.txtPrecioCarrito)
        val txtCantidad: TextView = view.findViewById(R.id.txtCantidad)

        val btnMas: Button = view.findViewById(R.id.btnMas)
        val btnMenos: Button = view.findViewById(R.id.btnMenos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = lista[position]

        holder.txtNombre.text = item.nombre
        holder.txtDetalles.text = item.tamano
        holder.txtPrecio.text = "$${item.precio}"
        holder.txtCantidad.text = item.cantidad.toString()

        // BOTON +
        holder.btnMas.setOnClickListener {

            val nuevaCantidad = item.cantidad + 1
            item.cantidad = nuevaCantidad

            actualizarCantidad(item, nuevaCantidad)

            holder.txtCantidad.text = nuevaCantidad.toString()
        }

        // BOTON -
        holder.btnMenos.setOnClickListener {

            val nuevaCantidad = item.cantidad - 1

            if (nuevaCantidad <= 0) {

                eliminarItem(item)

                lista.removeAt(position)
                notifyItemRemoved(position)

            } else {

                item.cantidad = nuevaCantidad

                actualizarCantidad(item, nuevaCantidad)

                holder.txtCantidad.text = nuevaCantidad.toString()
            }
        }
    }

    private fun actualizarCantidad(item: CarritoItem, cantidad: Int) {

        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseFirestore.getInstance()
            .collection("carritos")
            .document(user.uid)
            .collection("items")
            .document("${item.cafeId}_${item.tamano}")
            .update("cantidad", cantidad)
    }

    private fun eliminarItem(item: CarritoItem) {

        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseFirestore.getInstance()
            .collection("carritos")
            .document(user.uid)
            .collection("items")
            .document("${item.cafeId}_${item.tamano}")
            .delete()
    }

    override fun getItemCount(): Int = lista.size

}
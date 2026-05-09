package com.example.applacasadelbordadito.Carrito

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.applacasadelbordadito.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CarritoAdapter(
    private val lista: MutableList<CarritoItem>,
    private val isReadOnly: Boolean = false
) : RecyclerView.Adapter<CarritoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProducto: ImageView = view.findViewById(R.id.imgProductoCarrito)
        val txtNombre: TextView = view.findViewById(R.id.txtNombreCarrito)
        val txtDetalles: TextView = view.findViewById(R.id.txtDetallesCarrito)
        val txtPrecio: TextView = view.findViewById(R.id.txtPrecioCarrito)
        val txtCantidad: TextView = view.findViewById(R.id.txtCantidad)
        val btnMas: MaterialButton = view.findViewById(R.id.btnMas)
        val btnMenos: MaterialButton = view.findViewById(R.id.btnMenos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.imgProducto.load(item.imagenUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_cafe)
            error(R.drawable.ic_card_unknown)
        }
        holder.txtNombre.text = item.nombre
        holder.txtDetalles.text = item.tamano
        holder.txtPrecio.text = "$${item.precio}"
        holder.txtCantidad.text = item.cantidad.toString()

        if (isReadOnly) {
            holder.btnMas.visibility = View.GONE
            holder.btnMenos.visibility = View.GONE
            // Opcional: mostrar la cantidad con un prefijo
            holder.txtCantidad.text = "x${item.cantidad}"
        } else {
            holder.btnMas.visibility = View.VISIBLE
            holder.btnMenos.visibility = View.VISIBLE

            holder.btnMas.setOnClickListener {
                val nuevaCantidad = item.cantidad + 1
                item.cantidad = nuevaCantidad
                actualizarCantidad(item, nuevaCantidad)
                holder.txtCantidad.text = nuevaCantidad.toString()
            }

            holder.btnMenos.setOnClickListener {
                val nuevaCantidad = item.cantidad - 1
                if (nuevaCantidad <= 0) {
                    eliminarItem(item)
                    lista.removeAt(holder.bindingAdapterPosition)
                    notifyItemRemoved(holder.bindingAdapterPosition)
                } else {
                    item.cantidad = nuevaCantidad
                    actualizarCantidad(item, nuevaCantidad)
                    holder.txtCantidad.text = nuevaCantidad.toString()
                }
            }
        }
    }

    private fun actualizarCantidad(item: CarritoItem, cantidad: Int) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("carritos")
            .document(user.uid)
            .collection("items")
            .document("${item.carritoItemId}_${item.tamano}")
            .update("cantidad", cantidad)
    }

    private fun eliminarItem(item: CarritoItem) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("carritos")
            .document(user.uid)
            .collection("items")
            .document("${item.carritoItemId}_${item.tamano}")
            .delete()
    }

    override fun getItemCount(): Int = lista.size
}

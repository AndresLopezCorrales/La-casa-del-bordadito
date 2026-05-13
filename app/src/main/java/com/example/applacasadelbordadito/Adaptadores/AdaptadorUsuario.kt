package com.example.applacasadelbordadito.Adaptadores

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.applacasadelbordadito.Chat.ChatActivity
import com.example.applacasadelbordadito.Constantes
import com.example.applacasadelbordadito.Modelos.Usuario
import com.example.applacasadelbordadito.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdaptadorUsuario(
    private val context: Context,
    private val listaUsuarios: List<Usuario>,
    private val soySoporte: Boolean = false
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

        verificarBloqueo(usuario.uid, holder)

        obtenerContadorMensajes(usuario.uid, holder.notifCount)

        Glide.with(context)
            .load(usuario.imagen)
            .placeholder(R.drawable.ic_imagen_perfil)
            .into(holder.imagen)

        // Mostrar badge de STAFF si el usuario de la lista es soporte 
        // Y el usuario que está viendo la lista también es soporte
        if (soySoporte && usuario.esSoporte) {
            holder.badgeStaff.visibility = View.VISIBLE
        } else {
            // También lo mostramos para los clientes (UX: para que sepan que es personal oficial)
            if (usuario.esSoporte) {
                holder.badgeStaff.visibility = View.VISIBLE
            } else {
                holder.badgeStaff.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("uid", usuario.uid)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            if (soySoporte && !usuario.esSoporte) {
                mostrarOpcionesBloqueo(usuario)
            }
            true
        }
    }

    private fun verificarBloqueo(targetUid: String, holder: ViewHolder) {
        val miUid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
            .child(miUid).child("usuariosBloqueados").child(targetUid)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    holder.itemView.alpha = 0.5f
                    holder.layoutPrincipal.setBackgroundColor(Color.LTGRAY)
                } else {
                    holder.itemView.alpha = 1.0f
                    holder.layoutPrincipal.setBackgroundColor(ContextCompat.getColor(context, R.color.bone))
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mostrarOpcionesBloqueo(usuario: Usuario) {
        val miUid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
            .child(miUid).child("usuariosBloqueados").child(usuario.uid)

        ref.get().addOnSuccessListener { snapshot ->
            val estaBloqueado = snapshot.exists()
            val opciones = if (estaBloqueado) arrayOf("Quitar Bloqueo") else arrayOf("Bloquear Usuario")

            AlertDialog.Builder(context)
                .setTitle("Opciones para ${usuario.nombres}")
                .setItems(opciones) { _, _ ->
                    if (estaBloqueado) {
                        ref.removeValue()
                    } else {
                        ref.setValue(true).addOnSuccessListener {
                            enviarMensajeBloqueo(usuario.uid)
                        }
                    }
                }
                .show()
        }
    }

    private fun enviarMensajeBloqueo(targetUid: String) {
        val miUid = FirebaseAuth.getInstance().uid ?: return
        val chatRuta = Constantes.rutaChat(miUid, targetUid)
        val tiempo = Constantes.obtenerTiempoDis()
        
        val refChat = FirebaseDatabase.getInstance().getReference("Chats")
        val keyId = "${refChat.push().key}"
        
        val hashMap = HashMap<String, Any>()
        hashMap["idMensaje"] = keyId
        hashMap["tipoMensaje"] = Constantes.MENSAJE_TIPO_TEXTO
        hashMap["mensaje"] = "🚫 Has sido bloqueado por el personal de soporte. No podrás enviar más mensajes."
        hashMap["emisorUid"] = miUid
        hashMap["receptorUid"] = targetUid
        hashMap["tiempo"] = tiempo

        refChat.child(chatRuta).child(keyId).setValue(hashMap)
    }

    private fun obtenerContadorMensajes(userId: String, notifCount: TextView) {
        val miUid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("ChatsUnreadCount")
            .child(miUid).child(userId)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.getValue(Int::class.java) ?: 0
                if (count > 0) {
                    notifCount.text = if (count > 99) "99+" else count.toString()
                    notifCount.visibility = View.VISIBLE
                } else {
                    notifCount.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uid: TextView = itemView.findViewById(R.id.item_uid)
        val email: TextView = itemView.findViewById(R.id.item_email)
        val nombres: TextView = itemView.findViewById(R.id.item_nombre)
        val imagen: ImageView = itemView.findViewById(R.id.item_imagen)
        val badgeStaff: TextView = itemView.findViewById(R.id.item_badge_staff)
        val notifCount: TextView = itemView.findViewById(R.id.item_notif_count)
        val layoutPrincipal: View = itemView.findViewById(R.id.item_layout_principal)
    }
}

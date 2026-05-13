package com.example.applacasadelbordadito.Chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.applacasadelbordadito.Adaptadores.AdaptadorUsuario
import com.example.applacasadelbordadito.Modelos.Usuario
import com.example.applacasadelbordadito.databinding.ActivityListaChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ListaChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListaChatBinding

    private var usuarioAdaptador: AdaptadorUsuario? = null
    private var usuarioLista: ArrayList<Usuario>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.RVUsuarios.setHasFixedSize(true)
        binding.RVUsuarios.layoutManager = LinearLayoutManager(this)

        usuarioLista = ArrayList()

        binding.EtBuscarUsuario.doOnTextChanged { usuario, _, _, _ ->
            buscarUsuario(usuario.toString())
        }

        listarUsuarios()
    }

    private fun listarUsuarios() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser?.uid
        val reference = FirebaseDatabase.getInstance().reference.child("Usuarios").orderByChild("nombres")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usuarioLista?.clear()

                for (sn in snapshot.children) {
                    val usuario: Usuario? = sn.getValue(Usuario::class.java)
                    if (usuario != null && usuario.uid != firebaseUser) {
                        usuarioLista?.add(usuario)
                    }
                }

                // Si la lista está vacía, mostramos el mensaje y ocultamos el recycler
                if (usuarioLista.isNullOrEmpty()) {
                    binding.tvSinUsuarios.visibility = View.VISIBLE
                    binding.RVUsuarios.visibility = View.GONE
                } else {
                    binding.tvSinUsuarios.visibility = View.GONE
                    binding.RVUsuarios.visibility = View.VISIBLE

                    // Actualizamos el adaptador
                    usuarioAdaptador = AdaptadorUsuario(this@ListaChatActivity, usuarioLista!!)
                    binding.RVUsuarios.adapter = usuarioAdaptador
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error al leer usuarios: ${error.message}")
                Toast.makeText(this@ListaChatActivity, "Error al cargar usuarios: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun buscarUsuario(usuario: String) {
        // Obtenemos el UID del usuario actual
        val firebaseUser = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val reference = FirebaseDatabase.getInstance().reference
            .child("Usuarios")
            .orderByChild("nombres")
            .startAt(usuario)
            .endAt(usuario + "\uf8ff")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usuarioLista?.clear()

                for (ss in snapshot.children) {
                    val usuarioObj = ss.getValue(Usuario::class.java)

                    // Evitamos agregarnos a nosotros mismos
                    if (usuarioObj != null && usuarioObj.uid != firebaseUser) {
                        usuarioLista?.add(usuarioObj)
                    }
                }

                // Actualizamos el adaptador
                usuarioAdaptador = AdaptadorUsuario(this@ListaChatActivity, usuarioLista!!)
                binding.RVUsuarios.adapter = usuarioAdaptador
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error al buscar usuarios: ${error.message}")
                Toast.makeText(this@ListaChatActivity, "Error al buscar: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

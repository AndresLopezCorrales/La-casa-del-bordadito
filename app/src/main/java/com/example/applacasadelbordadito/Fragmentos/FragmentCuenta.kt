package com.example.applacasadelbordadito.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.applacasadelbordadito.Constantes
import com.example.applacasadelbordadito.OpcionesLogin
import com.example.applacasadelbordadito.Perfil.AvatarGenerator
import com.example.applacasadelbordadito.Perfil.EditarPerfil
import com.example.applacasadelbordadito.R
import com.example.applacasadelbordadito.databinding.FragmentCuentaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FragmentCuenta : Fragment() {

    private lateinit var binding: FragmentCuentaBinding
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCuentaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        leerInfo()

        binding.BtnEditarPerfil.setOnClickListener {
            startActivity(Intent(mContext, EditarPerfil::class.java))
        }

        binding.BtnCerrarSesion.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(requireContext(), OpcionesLogin::class.java))
            activity?.finishAffinity()
        }
    }

    private fun leerInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot){
                    val nombres = "${snapshot.child("nombres").value}"
                    val email = "${snapshot.child("email").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"
                    val f_nac = "${snapshot.child("fecha_nac").value}"
                    var tiempo = "${snapshot.child("tiempo").value}"
                    val telefono = "${snapshot.child("telefono").value}"
                    val codTelefono = "${snapshot.child("codigoTelefono").value}"

                    val proveedor = "${snapshot.child("proveedor").value}"

                    val cod_tel = codTelefono + telefono

                    if(tiempo == "null"){
                        tiempo = "0"
                    }

                    val for_tiempo = Constantes.obtenerFecha(tiempo.toLong())

                    binding.TvEmail.text = email
                    binding.TvNombres.text = nombres
                    binding.TvNacimiento.text = f_nac
                    binding.TvTelefono.text = cod_tel
                    binding.TvMiembro.text = for_tiempo

                    //Generar Imagen Avatar Random como base
                    AvatarGenerator.generateAvatarByUid(mContext, binding.TvPerfil, firebaseAuth.uid.toString())

                    try{
                        if(imagen.isNotEmpty() && imagen != "null"){
                            Glide.with(mContext)
                                .load(imagen)
                                .into(binding.TvPerfil)
                        }else{
                            // Si no hay URL, nos aseguramos que se quede el avatar
                            AvatarGenerator.generateAvatarByUid(mContext, binding.TvPerfil, firebaseAuth.uid.toString())
                        }

                    }catch (e: Exception){
                        Toast.makeText(mContext, "${e.message}", Toast.LENGTH_SHORT).show()
                    }

                    if(proveedor == "Email"){
                        val esVerificado = firebaseAuth.currentUser!!.isEmailVerified
                        if (esVerificado){
                            binding.TvEstadoCuenta.text = "Verificado"
                        }else{
                            binding.TvEstadoCuenta.text = "No verificado"
                        }
                    }else{
                        binding.TvEstadoCuenta.text = "Verificado"
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }


            })
    }

}
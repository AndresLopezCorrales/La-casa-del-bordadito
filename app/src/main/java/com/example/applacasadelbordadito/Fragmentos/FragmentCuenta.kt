package com.example.applacasadelbordadito.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.applacasadelbordadito.OpcionesLogin
import com.example.applacasadelbordadito.Perfil.AvatarGenerator
import com.example.applacasadelbordadito.databinding.FragmentCuentaBinding
import com.google.firebase.auth.FirebaseAuth

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

        //Generar Imagen Avatar Random cada que se cree la view
        AvatarGenerator.generateRandomAvatar(mContext, binding.TvPerfil)

        binding.BtnCerrarSesion.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(requireContext(), OpcionesLogin::class.java))
            activity?.finishAffinity()
        }
    }

}
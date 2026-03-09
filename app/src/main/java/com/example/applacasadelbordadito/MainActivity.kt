package com.example.applacasadelbordadito

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.Fragmentos.FragmentBordado
import com.example.applacasadelbordadito.Fragmentos.FragmentCafe
import com.example.applacasadelbordadito.Fragmentos.FragmentCuenta
import com.example.applacasadelbordadito.Fragmentos.FragmentInicio
import com.example.applacasadelbordadito.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        verFragmentInicio()

        binding.BottomNV.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Item_Inicio->{
                    verFragmentInicio()
                    true
                }
                R.id.Item_Cafe->{
                    verFragmentCafe()
                    true
                }
                R.id.Item_Bordado->{
                    verFragmentBordado()
                    true
                }
                R.id.Item_Cuenta->{
                    verFragmentCuenta()
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun comprobarSesion(){
        if(firebaseAuth.currentUser == null){
            startActivity(Intent(this, OpcionesLogin::class.java))
            finishAffinity()
        }
    }

    private fun verFragmentInicio(){
        binding.TituloRL.text="Inicio"
        val fragment = FragmentInicio()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentInicio")
        fragmentTransition.commit()
    }

    private fun verFragmentCafe(){
        binding.TituloRL.text="Cafe"
        val fragment = FragmentCafe()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentCafe")
        fragmentTransition.commit()
    }

    private fun verFragmentBordado(){
        binding.TituloRL.text="Bordado"
        val fragment = FragmentBordado()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentBordado")
        fragmentTransition.commit()
    }

    private fun verFragmentCuenta(){
        binding.TituloRL.text="Cuenta"
        val fragment = FragmentCuenta()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentCuenta")
        fragmentTransition.commit()
    }
}
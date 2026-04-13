package com.example.applacasadelbordadito

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.Carrito.CarritoActivity
import com.example.applacasadelbordadito.Historial.HistorialActivity
import com.example.applacasadelbordadito.Fragmentos.FragmentBordado
import com.example.applacasadelbordadito.Fragmentos.FragmentCafe
import com.example.applacasadelbordadito.Fragmentos.FragmentCuenta
import com.example.applacasadelbordadito.Fragmentos.FragmentInicio
import com.example.applacasadelbordadito.Taller.TallerActivity
import com.example.applacasadelbordadito.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var btnCarrito: FrameLayout
    private lateinit var btnHistorial: ImageView
    private lateinit var badgeCarrito: TextView

    // Flag para evitar re-entrada al actualizar el BottomNav programáticamente
    private var isProgrammaticSelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        btnCarrito = findViewById(R.id.btnCarrito)
        btnHistorial = findViewById(R.id.btnHistorial)
        badgeCarrito = findViewById(R.id.badgeCarrito)

        btnCarrito.setOnClickListener {
            startActivity(Intent(this, CarritoActivity::class.java))
        }

        btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        binding.FAB.setOnClickListener {
            startActivity(Intent(this, TallerActivity::class.java))
        }

        escucharCarrito()

        // Carga inicial
        verFragmentInicio()

        val fragment = intent.getStringExtra("fragment")
        if (fragment == "cafe") {
            verFragmentCafe()
        } else if (fragment == "cuenta") {
            verFragmentCuenta()
        }

        binding.BottomNV.setOnItemSelectedListener { item ->
            if (isProgrammaticSelection) return@setOnItemSelectedListener true
            
            when(item.itemId) {
                R.id.Item_Inicio -> { verFragmentInicio(); true }
                R.id.Item_Cafe -> { verFragmentCafe(); true }
                R.id.Item_Taller -> {
                    startActivity(Intent(this, TallerActivity::class.java))
                    false 
                }
                R.id.Item_Bordado -> { verFragmentBordado(); true }
                R.id.Item_Cuenta -> { verFragmentCuenta(); true }
                else -> false
            }
        }
    }

    private fun updateBottomNavSelection(itemId: Int) {
        if (binding.BottomNV.selectedItemId != itemId) {
            isProgrammaticSelection = true
            binding.BottomNV.selectedItemId = itemId
            isProgrammaticSelection = false
        }
    }

    fun verFragmentInicio() {
        updateBottomNavSelection(R.id.Item_Inicio)
        binding.TituloRL.text = "Inicio"
        val fragment = FragmentInicio()
        supportFragmentManager.beginTransaction()
            .replace(binding.FragmentL1.id, fragment, "FragmentInicio")
            .commit()
    }

    fun verFragmentCafe() {
        updateBottomNavSelection(R.id.Item_Cafe)
        binding.TituloRL.text = "Cafe"
        val fragment = FragmentCafe()
        supportFragmentManager.beginTransaction()
            .replace(binding.FragmentL1.id, fragment, "FragmentCafe")
            .commit()
    }

    fun verFragmentBordado(patronId: String? = null) {
        updateBottomNavSelection(R.id.Item_Bordado)
        binding.TituloRL.text = "Bordado"
        val fragment = FragmentBordado().apply {
            if (patronId != null) {
                arguments = Bundle().apply {
                    putString("patronId", patronId)
                }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(binding.FragmentL1.id, fragment, "FragmentBordado")
            .commit()
    }

    fun verFragmentCuenta() {
        updateBottomNavSelection(R.id.Item_Cuenta)
        binding.TituloRL.text = "Cuenta"
        val fragment = FragmentCuenta()
        supportFragmentManager.beginTransaction()
            .replace(binding.FragmentL1.id, fragment, "FragmentCuenta")
            .commit()
    }

    private fun escucharCarrito() {
        val user = firebaseAuth.currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("carritos")
            .document(user.uid)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val count = snapshot.size()
                if (count > 0) {
                    badgeCarrito.text = count.toString()
                    badgeCarrito.visibility = View.VISIBLE
                } else {
                    badgeCarrito.visibility = View.GONE
                }
            }
    }

    private fun comprobarSesion(){
        if(firebaseAuth.currentUser == null){
            startActivity(Intent(this, OpcionesLogin::class.java))
            finishAffinity()
        }
    }
}
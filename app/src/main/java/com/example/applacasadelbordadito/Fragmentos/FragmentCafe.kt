package com.example.applacasadelbordadito.Fragmentos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.applacasadelbordadito.Cafe.Cafe
import com.example.applacasadelbordadito.R
import com.google.firebase.firestore.FirebaseFirestore

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentCafe.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentCafe : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //insertarCafesIniciales() //Insertar cafes a firebase automatizado
        return inflater.inflate(R.layout.fragment_cafe, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FragmentCafe.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentCafe().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun insertarCafesIniciales() {

        val db = FirebaseFirestore.getInstance()

        val listaCafes = listOf(

            Cafe(
                id = "latte",
                nombre = "Latte",
                descripcion = "Café con leche espumosa",
                imagenUrl = "https://placehold.co/200",
                categoria = "Calientes",
                tamaños = mapOf("chico" to 45.0, "mediano" to 55.0, "grande" to 65.0)
            ),

            Cafe(
                id = "cappuccino",
                nombre = "Cappuccino",
                descripcion = "Café con espuma cremosa",
                imagenUrl = "https://placehold.co/200",
                categoria = "Calientes",
                tamaños = mapOf("chico" to 40.0, "mediano" to 50.0, "grande" to 60.0)
            ),

            Cafe(
                id = "americano",
                nombre = "Americano",
                descripcion = "Café negro clásico",
                imagenUrl = "https://placehold.co/200",
                categoria = "Calientes",
                tamaños = mapOf("chico" to 30.0, "mediano" to 40.0, "grande" to 50.0)
            ),

            Cafe(
                id = "mocha",
                nombre = "Mocha",
                descripcion = "Café con chocolate",
                imagenUrl = "https://placehold.co/200",
                categoria = "Especiales",
                tamaños = mapOf("chico" to 50.0, "mediano" to 60.0, "grande" to 70.0)
            ),

            Cafe(
                id = "caramel_macchiato",
                nombre = "Caramel Macchiato",
                descripcion = "Café con caramelo y leche",
                imagenUrl = "https://placehold.co/200",
                categoria = "Especiales",
                tamaños = mapOf("chico" to 55.0, "mediano" to 65.0, "grande" to 75.0)
            ),

            Cafe(
                id = "espresso",
                nombre = "Espresso",
                descripcion = "Café concentrado",
                imagenUrl = "https://placehold.co/200",
                categoria = "Calientes",
                tamaños = mapOf("único" to 25.0)
            ),

            Cafe(
                id = "latte_helado",
                nombre = "Latte Helado",
                descripcion = "Latte servido con hielo",
                imagenUrl = "https://placehold.co/200",
                categoria = "Fríos",
                tamaños = mapOf("chico" to 50.0, "mediano" to 60.0, "grande" to 70.0)
            ),

            Cafe(
                id = "mocha_helado",
                nombre = "Mocha Helado",
                descripcion = "Mocha frío con hielo",
                imagenUrl = "https://placehold.co/200",
                categoria = "Fríos",
                tamaños = mapOf("chico" to 55.0, "mediano" to 65.0, "grande" to 75.0)
            ),

            Cafe(
                id = "cold_brew",
                nombre = "Cold Brew",
                descripcion = "Café frío infusionado lentamente",
                imagenUrl = "https://placehold.co/200",
                categoria = "Fríos",
                tamaños = mapOf("chico" to 45.0, "mediano" to 55.0, "grande" to 65.0)
            ),

            Cafe(
                id = "flat_white",
                nombre = "Flat White",
                descripcion = "Café con microespuma",
                imagenUrl = "https://placehold.co/200",
                categoria = "Especiales",
                tamaños = mapOf("chico" to 48.0, "mediano" to 58.0, "grande" to 68.0)
            )
        )

        for (cafe in listaCafes) {
            db.collection("cafes")
                .document(cafe.id)
                .set(cafe)
        }
    }
}
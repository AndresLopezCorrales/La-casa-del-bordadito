package com.example.applacasadelbordadito.Cafe

import com.google.firebase.firestore.PropertyName

data class Cafe(
    var id: String = "",
    var nombre: String = "",
    var descripcion: String = "",
    var imagenUrl: String = "",
    var categoria: String = "",
    var tamano: Map<String, Double> = mapOf(),

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = false
)
package com.example.applacasadelbordadito.Cafe

data class Cafe(
    var id: String = "",
    var nombre: String = "",
    var descripcion: String = "",
    var imagenUrl: String = "",
    var categoria: String = "",
    var tamaños: Map<String, Double> = mapOf()
)
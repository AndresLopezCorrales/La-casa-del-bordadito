package com.example.applacasadelbordadito.Carrito

data class CarritoItem(
    var cafeId: String = "",
    var nombre: String = "",
    var tamano: String = "",
    var precio: Double = 0.0,
    var cantidad: Int = 1,
    var imagenUrl: String = ""
)
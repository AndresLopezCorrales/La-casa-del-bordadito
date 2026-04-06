package com.example.applacasadelbordadito.Carrito

import java.io.Serializable

data class CarritoItem(
    var carritoItemId: String = "",
    var nombre: String = "",
    var tamano: String = "",
    var precio: Double = 0.0,
    var cantidad: Int = 1,
    var imagenUrl: String = ""
): Serializable
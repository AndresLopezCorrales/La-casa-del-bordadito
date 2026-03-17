package com.example.applacasadelbordadito.Historial

import com.example.applacasadelbordadito.Carrito.CarritoItem

data class Orden(
    var id: String = "",
    var usuario: String = "",
    var total: String = "",
    var fecha: Long = 0,
    var items: List<CarritoItem> = emptyList()
)
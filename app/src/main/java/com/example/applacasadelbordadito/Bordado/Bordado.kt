package com.example.applacasadelbordadito.Bordado

// Modelo del Dibujo
data class PatronBordado(
    var id: String = "",
    var nombre: String = "",
    var columnas: Int = 0,
    var filas: Int = 0,
    var urlImagen: String = "",
    var paleta: List<ColorPatron> = emptyList(),
    var matriz: List<List<Int>> = emptyList(),
    var porcentaje: Int = 0
)

data class ColorPatron(
    var id: Int = 0,
    var hex: String = "",
    var nombre: String = ""
)


data class ProgresoUsuario(
    val puntosPintados: Map<String, Int> = emptyMap() // Clave: "fila_columna", Valor: colorID
)
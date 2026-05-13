package com.example.applacasadelbordadito.Modelos

class Chat {

    // Atributos

    var idMensaje : String = ""
    var tipoMensaje : String = ""
    var mensaje : String = ""
    var emisorUid : String = ""
    var receptorUid : String = ""
    var tiempo : Long = 0

    // Constructor Vacio
    constructor()

    // Constructor con todos los atributos
    constructor(
        idMensaje: String,
        tipoMensaje: String,
        emisorUid: String,
        mensaje: String,
        receptorUid: String,
        tiempo: Long
    ) {
        this.idMensaje = idMensaje
        this.tipoMensaje = tipoMensaje
        this.emisorUid = emisorUid
        this.mensaje = mensaje
        this.receptorUid = receptorUid
        this.tiempo = tiempo
    }
}
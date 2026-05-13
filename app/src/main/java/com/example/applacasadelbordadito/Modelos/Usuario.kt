package com.example.applacasadelbordadito.Modelos

class Usuario {

    //Atributos
    var uid: String = ""
    var email: String = ""
    var nombres: String = ""
    var imagen: String = ""
    var esSoporte: Boolean = false
    var esAdmin: Boolean = false

    //Constructor vacío
    constructor()

    //Constructor con todos los atributos
    constructor(uid: String, email: String, nombres: String, imagen: String, esSoporte: Boolean, esAdmin: Boolean){
        this.uid = uid
        this.email = email
        this.nombres = nombres
        this.imagen = imagen
        this.esSoporte = esSoporte
        this.esAdmin = esAdmin
    }
}
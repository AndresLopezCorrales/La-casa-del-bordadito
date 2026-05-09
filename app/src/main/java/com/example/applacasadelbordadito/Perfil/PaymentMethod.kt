package com.example.applacasadelbordadito.Perfil

data class PaymentMethod(
    var id: String         = "",
    var last4: String      = "",
    var brand: String      = "",   // "VISA", "MASTERCARD", "AMEX", "DISCOVER"
    var holderName: String = "",
    var expiry: String     = "",   // "MM/AA"
    var isDefault: Boolean = false
) {
    fun maskedNumber() = "**** **** **** $last4"
}

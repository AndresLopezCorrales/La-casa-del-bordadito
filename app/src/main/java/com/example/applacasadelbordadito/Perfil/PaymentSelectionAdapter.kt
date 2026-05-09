package com.example.applacasadelbordadito.Perfil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.R

class PaymentSelectionAdapter(
    private val paymentMethods: List<PaymentMethod>,
    private val onSelect: (PaymentMethod) -> Unit
) : RecyclerView.Adapter<PaymentSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBrand: ImageView = view.findViewById(R.id.ivCardBrand)
        val tvNumber: TextView = view.findViewById(R.id.tvCardNumber)
        val tvHolder: TextView = view.findViewById(R.id.tvCardHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payment_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val method = paymentMethods[position]
        holder.tvNumber.text = method.maskedNumber()
        holder.tvHolder.text = method.holderName.uppercase()

        val iconRes = when (method.brand.uppercase()) {
            "VISA" -> R.drawable.ic_visa
            "MASTERCARD" -> R.drawable.ic_mastercard
            "AMEX" -> R.drawable.ic_amex
            "DISCOVER" -> R.drawable.ic_discover
            else -> R.drawable.ic_card_unknown
        }
        holder.ivBrand.setImageResource(iconRes)

        holder.itemView.setOnClickListener { onSelect(method) }
    }

    override fun getItemCount() = paymentMethods.size
}

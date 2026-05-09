package com.example.applacasadelbordadito.Historial

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applacasadelbordadito.Carrito.CarritoAdapter
import com.example.applacasadelbordadito.R
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*

class HistorialDetalleActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvFolio: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvMetodoPago: TextView
    private lateinit var tvTotal: TextView
    private lateinit var rvProductos: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_detalle)

        toolbar = findViewById(R.id.toolbar)
        tvFolio = findViewById(R.id.tvFolio)
        tvFecha = findViewById(R.id.tvFecha)
        tvMetodoPago = findViewById(R.id.tvMetodoPago)
        tvTotal = findViewById(R.id.tvTotal)
        rvProductos = findViewById(R.id.rvProductos)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val orden = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("orden", Orden::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("orden") as? Orden
        }

        if (orden != null) {
            mostrarDatos(orden)
        } else {
            finish()
        }
    }

    private fun mostrarDatos(orden: Orden) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaFormateada = sdf.format(Date(orden.fecha))

        val folioShort = orden.id.takeLast(8).uppercase()
        
        tvFolio.text = getString(R.string.Txt_folio, folioShort)
        tvFecha.text = getString(R.string.Txt_fecha, fechaFormateada)
        tvMetodoPago.text = getString(R.string.Txt_pago, orden.metodoPago)
        tvTotal.text = getString(R.string.Txt_total_historial, orden.total)

        rvProductos.layoutManager = LinearLayoutManager(this)
        rvProductos.adapter = CarritoAdapter(orden.items.toMutableList(), isReadOnly = true)
    }
}

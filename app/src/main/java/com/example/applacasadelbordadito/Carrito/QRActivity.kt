package com.example.applacasadelbordadito.Carrito

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.applacasadelbordadito.MainActivity
import com.example.applacasadelbordadito.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class QRActivity : AppCompatActivity() {

    private lateinit var imgQR: ImageView
    private lateinit var txtInfo: TextView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnSeguirComprando: MaterialButton
    private lateinit var btnIrInicio: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)

        // Inicializar Vistas
        toolbar = findViewById(R.id.toolbar)
        imgQR = findViewById(R.id.imgQR)
        txtInfo = findViewById(R.id.txtInfo)
        btnSeguirComprando = findViewById(R.id.btnSeguirComprando)
        btnIrInicio = findViewById(R.id.btnIrInicio)

        // Configurar Navegación del Toolbar (Regresar a Inicio)
        toolbar.setNavigationOnClickListener {
            irAInicio()
        }

        // Manejar el botón "Atrás" del sistema
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                irAInicio()
            }
        })

        // Configurar Botones de Acción
        btnSeguirComprando.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("fragment", "cafe") // Indicamos que abra el fragment de café
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnIrInicio.setOnClickListener {
            irAInicio()
        }

        // Recuperar y Procesar Datos de la Compra
        val usuario = intent.getStringExtra("usuario") ?: "N/A"
        val total = intent.getStringExtra("total") ?: "0.00"
        val ordenId = intent.getStringExtra("ordenId") ?: "N/A"
        val items = intent.getSerializableExtra("items") as? ArrayList<CarritoItem> ?: arrayListOf()

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaFormateada = sdf.format(Date())

        // Formatear Texto
        val detallePantalla = StringBuilder()
        detallePantalla.append("Folio: ${ordenId.takeLast(8).uppercase()}\n")
        detallePantalla.append("Cliente: $usuario\n")
        detallePantalla.append("Fecha: $fechaFormateada\n")

        items.forEach { item ->
            val sub = item.precio * item.cantidad
            detallePantalla.append("• ${item.nombre}\n")
            detallePantalla.append("  ${item.tamano} | ${item.cantidad} x $${item.precio} = $$sub\n\n")
        }

        detallePantalla.append("TOTAL PAGADO: $total")
        txtInfo.text = detallePantalla.toString()

        // Generar JSON para el Lector de QR
        try {
            val json = JSONObject()
            json.put("id", ordenId)
            json.put("user", usuario)
            json.put("total", total)
            json.put("date", fechaFormateada)

            val itemsArray = JSONArray()
            items.forEach { item ->
                val itemObj = JSONObject()
                itemObj.put("name", item.nombre)
                itemObj.put("size", item.tamano)
                itemObj.put("qty", item.cantidad)
                itemObj.put("price", item.precio)
                itemsArray.put(itemObj)
            }
            json.put("items", itemsArray)

            generarQR(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun irAInicio() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun generarQR(texto: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            // Generar un QR
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(texto, BarcodeFormat.QR_CODE, 800, 800)
            imgQR.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
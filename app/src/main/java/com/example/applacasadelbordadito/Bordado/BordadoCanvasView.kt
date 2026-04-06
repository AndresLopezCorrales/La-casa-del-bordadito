package com.example.applacasadelbordadito.Bordado

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.sqrt

class BordadoCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var patron: PatronBordado? = null
    var puntosPintados = mutableMapOf<String, Int>()
    var colorSeleccionadoId: Int = -1

    // Para manejar el error visual
    private var puntoError: String? = null
    private val handlerError = Handler(Looper.getMainLooper())

    var onPuntoPintado: (() -> Unit)? = null

    private val paintText = Paint().apply {
        color = Color.DKGRAY
        textAlign = Paint.Align.CENTER
        textSize = 28f
        isAntiAlias = true
    }

    private val paintError = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    // --- Variables para Zoom y Pan ---
    private var mScaleFactor = 1.0f
    private var mPosX = 0f
    private var mPosY = 0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private val CLICK_THRESHOLD = 15f 

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val oldScale = mScaleFactor
            mScaleFactor *= detector.scaleFactor
            // Limitar el zoom entre 1.0x (mínimo para no dejar espacios blancos) y 10.0x
            mScaleFactor = 1.0f.coerceAtLeast(mScaleFactor.coerceAtMost(10.0f))

            // Ajustar posición para que el zoom sea hacia el foco de los dedos
            val focusX = detector.focusX
            val focusY = detector.focusY
            mPosX -= (focusX - mPosX) * (mScaleFactor / oldScale - 1)
            mPosY -= (focusY - mPosY) * (mScaleFactor / oldScale - 1)

            applyBounds()
            invalidate()
            return true
        }
    })

    // Función crucial para prohibir salir de los bordes del dibujo
    private fun applyBounds() {
        val p = patron ?: return
        val cellSize = width.toFloat() / p.columnas
        val contentWidth = width.toFloat()
        val contentHeight = p.filas * cellSize

        val scaledWidth = contentWidth * mScaleFactor
        val scaledHeight = contentHeight * mScaleFactor

        // Restringir Eje X
        if (scaledWidth > width) {
            mPosX = mPosX.coerceIn(width - scaledWidth, 0f)
        } else {
            mPosX = (width - scaledWidth) / 2f // Centrar si es más pequeño
        }

        // Restringir Eje Y
        if (scaledHeight > height) {
            mPosY = mPosY.coerceIn(height - scaledHeight, 0f)
        } else {
            mPosY = (height - scaledHeight) / 2f // Centrar si es más pequeño
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        val p = patron ?: return

        canvas.save()
        // Aplicar transformaciones: traslación y luego escala
        canvas.translate(mPosX, mPosY)
        canvas.scale(mScaleFactor, mScaleFactor)

        val cellSize = width.toFloat() / p.columnas

        for (f in 0 until p.filas) {
            for (c in 0 until p.columnas) {
                val colorIdReq = p.matriz[f][c]
                val rect = RectF(c * cellSize, f * cellSize, (c + 1) * cellSize, (f + 1) * cellSize)
                val key = "${f}_${c}"

                // Dibujar rejilla suave
                val pLine = Paint().apply { 
                    color = Color.parseColor("#D3D3D3")
                    style = Paint.Style.STROKE 
                    strokeWidth = 1f / mScaleFactor // Mantener grosor visual constante al hacer zoom
                }
                canvas.drawRect(rect, pLine)

                // Si el valor es 0, solo dejamos el delineado
                if (colorIdReq == 0) continue

                if (puntosPintados.containsKey(key)) {
                    val colorHex = p.paleta.find { it.id == puntosPintados[key] }?.hex ?: "#000000"
                    val pFill = Paint().apply { color = Color.parseColor(colorHex) }
                    canvas.drawRect(rect, pFill)
                } else {
                    val textHeight = paintText.descent() - paintText.ascent()
                    val textOffset = textHeight / 2 - paintText.descent()
                    canvas.drawText(colorIdReq.toString(), rect.centerX(), rect.centerY() + textOffset, paintText)
                }

                if (puntoError == key) {
                    val pErr = Paint(paintError).apply { strokeWidth = 4f / mScaleFactor }
                    canvas.drawRect(rect, pErr)
                }
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val action = event.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = event.x
                mStartY = event.y
                mLastTouchX = event.x
                mLastTouchY = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - mLastTouchX
                    val dy = event.y - mLastTouchY

                    mPosX += dx
                    mPosY += dy
                    
                    applyBounds() // Limitar movimiento aquí
                    invalidate()
                }
                mLastTouchX = event.x
                mLastTouchY = event.y
            }

            MotionEvent.ACTION_UP -> {
                val diffX = event.x - mStartX
                val diffY = event.y - mStartY
                val distance = sqrt((diffX * diffX + diffY * diffY).toDouble())

                // Si se movió menos que el umbral, es un TAP (pintar)
                if (distance < CLICK_THRESHOLD && !scaleDetector.isInProgress) {
                    realizarPintado(event.x, event.y)
                }
            }
        }
        return true
    }

    private fun realizarPintado(x: Float, y: Float) {
        val p = patron ?: return
        
        // Convertir coordenadas de pantalla a coordenadas del modelo (invertir escala y traslación)
        val touchX = (x - mPosX) / mScaleFactor
        val touchY = (y - mPosY) / mScaleFactor
        
        val cellSize = width.toFloat() / p.columnas
        val col = (touchX / cellSize).toInt()
        val fila = (touchY / cellSize).toInt()
        val key = "${fila}_${col}"

        if (fila >= 0 && fila < p.filas && col >= 0 && col < p.columnas) {
            val colorReq = p.matriz[fila][col]

            if (colorReq == 0) return

            if (colorReq == colorSeleccionadoId) {
                if (!puntosPintados.containsKey(key)) {
                    puntosPintados[key] = colorReq
                    onPuntoPintado?.invoke()
                    invalidate()
                }
            } else {
                mostrarError(key)
            }
        }
    }

    private fun mostrarError(key: String) {
        puntoError = key
        invalidate()
        handlerError.removeCallbacksAndMessages(null)
        handlerError.postDelayed({
            puntoError = null
            invalidate()
        }, 500)
    }
}

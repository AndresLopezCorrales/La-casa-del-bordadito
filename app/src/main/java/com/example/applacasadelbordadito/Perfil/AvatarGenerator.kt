package com.example.applacasadelbordadito.Perfil

import android.content.Context
import android.widget.ImageView
import coil3.ImageLoader
import coil3.load
import coil3.svg.SvgDecoder
import io.github.mew22.AvatarData
import io.github.mew22.MultiAvatar

object AvatarGenerator {

    fun generateRandomAvatar(context: Context, imageView: ImageView) {

        // Genera avatar random
        val avatarData = AvatarData.generateRandom()

        // Obtiene el SVG como ByteArray
        val svgBytes = MultiAvatar.getAvatarSvgBytes(avatarData)

        // Crea un ImageLoader con soporte SVG
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()

        // Carga los bytes SVG directo en el ImageView
        imageView.load(svgBytes, imageLoader)
    }
}
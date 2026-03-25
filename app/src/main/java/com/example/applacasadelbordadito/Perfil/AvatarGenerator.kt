package com.example.applacasadelbordadito.Perfil

import android.content.Context
import android.widget.ImageView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.load
import io.github.mew22.AvatarData
import io.github.mew22.MultiAvatar

object AvatarGenerator {

    // Genera Avatar Random
    fun generateRandomAvatar(context: Context, imageView: ImageView) {
        val avatarData = AvatarData.generateRandom()
        renderAvatar(context, imageView, avatarData)
    }

    // Genera el avatar basado en el UID
    fun generateAvatarByUid(context: Context, imageView: ImageView, uid: String) {
        //HASh del uid
        val result = AvatarData.generateWithSha256(uid)

        // Manejamos el resultado
        result.onSuccess { avatarData ->
            // Si la generación fue exitosa, renderizamos
            renderAvatar(context, imageView, avatarData)
        }

        result.onFailure { error ->
            // Si algo salió mal, usamos un avatar random de respaldo
            generateRandomAvatar(context, imageView)
        }
    }

    // Renderizar Avatar
    private fun renderAvatar(context: Context, imageView: ImageView, avatarData: AvatarData) {
        val svgBytes = MultiAvatar.getAvatarSvgBytes(avatarData)

        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()

        imageView.load(svgBytes, imageLoader)
    }
}
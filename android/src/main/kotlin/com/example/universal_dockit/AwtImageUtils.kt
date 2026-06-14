package com.example.universal_dockit

import android.graphics.Bitmap
import java.awt.image.BufferedImage

internal fun BufferedImage.toBitmap(): Bitmap {
    val pixels = IntArray(width * height)
    getRGB(0, 0, width, height, pixels, 0, width)
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
        it.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

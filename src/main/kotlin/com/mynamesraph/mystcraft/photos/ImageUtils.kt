package com.mynamesraph.mystcraft.util

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.plugins.jpeg.JPEGImageWriteParam

object ImageUtils {

    const val JPEG_QUALITY = 40
    const val TARGET_WIDTH  = 640
    const val TARGET_HEIGHT = 384

    fun compressToJpeg(image: BufferedImage, quality: Int = JPEG_QUALITY): ByteArray {
        val scaled = image.getScaledInstance(TARGET_WIDTH, TARGET_HEIGHT, java.awt.Image.SCALE_SMOOTH)
        val output = BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g2d = output.createGraphics()
        g2d.drawImage(scaled, 0, 0, null)
        g2d.dispose()

        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val param = JPEGImageWriteParam(null).apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = quality / 100f
        }
        val baos = ByteArrayOutputStream()
        writer.output = ImageIO.createImageOutputStream(baos)
        writer.write(null, IIOImage(output, null, null), param)
        writer.dispose()
        return baos.toByteArray()
    }
}
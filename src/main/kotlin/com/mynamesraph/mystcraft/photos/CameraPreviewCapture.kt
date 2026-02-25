package com.mynamesraph.mystcraft.client

import com.mynamesraph.mystcraft.item.CameraItem
import com.mojang.logging.LogUtils
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.plugins.jpeg.JPEGImageWriteParam



@OnlyIn(Dist.CLIENT)


object CameraPreviewCapture {

    const val CAPTURE_WIDTH = 640
    const val CAPTURE_HEIGHT = 384
    const val JPEG_QUALITY = 40

    // Video uses lower quality to keep frame sizes manageable
    private val VIDEO_CAPTURE_WIDTH = 256
    private val VIDEO_CAPTURE_HEIGHT = 64
    private val VIDEO_JPEG_QUALITY = 30

    private val compressionExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MystcraftCameraCompression").also { it.isDaemon = true }
    }

    fun captureFrame(onComplete: (ByteArray) -> Unit) {
        val mc = Minecraft.getInstance()
        val w = CameraItem.CAPTURE_WIDTH
        val h = CameraItem.CAPTURE_HEIGHT

        val screenW = mc.window.width
        val screenH = mc.window.height



        val cropH = screenW / 2
        val cropY = ((screenH - cropH) / 2).coerceAtLeast(0)
        val actualCropH = cropH.coerceAtMost(screenH)
        val pixelCount = screenW * actualCropH

        val pixelBuffer = java.nio.ByteBuffer.allocateDirect(pixelCount * 3)
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1)
        GL11.glReadPixels(0, cropY, screenW, actualCropH, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, pixelBuffer)

        val pixels = ByteArray(pixelCount * 3)
        pixelBuffer.get(pixels)

        compressionExecutor.submit {
            try {
                val jpeg = compressToJpeg(pixels, screenW, actualCropH, w, h, CameraItem.JPEG_QUALITY)
                mc.execute { onComplete(jpeg) }
            } catch (e: Exception) {
                LogUtils.getLogger().warn("Mystcraft: Failed to compress camera frame", e)
            }
        }
    }


    private fun compressToJpeg(
        pixels: ByteArray, srcW: Int, srcH: Int, destW: Int, destH: Int, quality: Int
    ): ByteArray {
        val image = BufferedImage(srcW, srcH, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until srcH) {
            for (x in 0 until srcW) {
                val srcIdx = ((srcH - 1 - y) * srcW + x) * 3
                val r = pixels[srcIdx].toInt() and 0xFF
                val g = pixels[srcIdx + 1].toInt() and 0xFF
                val b = pixels[srcIdx + 2].toInt() and 0xFF
                image.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
        }
        val scaled = image.getScaledInstance(destW, destH, java.awt.Image.SCALE_SMOOTH)
        val output = BufferedImage(destW, destH, BufferedImage.TYPE_INT_RGB)
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

    fun captureVideoFrame(onComplete: (ByteArray) -> Unit) {
        val mc = Minecraft.getInstance()
        val w = VIDEO_CAPTURE_WIDTH
        val h = VIDEO_CAPTURE_HEIGHT

        val screenW = mc.window.width
        val screenH = mc.window.height

        val cropH = screenW / 2
        val cropY = ((screenH - cropH) / 2).coerceAtLeast(0)
        val actualCropH = cropH.coerceAtMost(screenH)
        val pixelCount = screenW * actualCropH

        val pixelBuffer = java.nio.ByteBuffer.allocateDirect(pixelCount * 3)
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1)
        GL11.glReadPixels(0, cropY, screenW, actualCropH, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, pixelBuffer)

        val pixels = ByteArray(pixelCount * 3)
        pixelBuffer.get(pixels)

        compressionExecutor.submit {
            try {
                val jpeg = compressToJpeg(pixels, screenW, actualCropH, w, h, VIDEO_JPEG_QUALITY)
                mc.execute { onComplete(jpeg) }
            } catch (e: Exception) {
                LogUtils.getLogger().warn("Mystcraft: Failed to compress video frame", e)
            }
        }
    }
}


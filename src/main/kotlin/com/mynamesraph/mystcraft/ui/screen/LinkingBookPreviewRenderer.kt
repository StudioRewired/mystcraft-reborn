package com.mynamesraph.mystcraft.client

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.logging.LogUtils
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

@OnlyIn(Dist.CLIENT)
class LinkingBookPreviewRenderer {

    private var uploadedTextures: List<DynamicTexture> = emptyList()
    private var registeredLocations: List<ResourceLocation> = emptyList()
    private var lastUploadedHash: Int = -1

    // Unique ID per renderer instance so texture locations never collide
    private val instanceId = nextInstanceId()

    companion object {
        private val counter = AtomicInteger(0)
        private fun nextInstanceId() = counter.incrementAndGet()
    }

    fun render(
        graphics: GuiGraphics,
        component: PreviewImageComponent?,
        x: Int, y: Int, width: Int, height: Int
    ) {
        if (component == null) return
        if (component.frames.isEmpty()) return

        ensureUploaded(component)
        if (registeredLocations.isEmpty()) return

        val fps = 20.0 / PreviewImageComponent.TICKS_BETWEEN_FRAMES
        val msPerFrame = (1000.0 / fps).toLong()
        val totalMs = registeredLocations.size * msPerFrame
        val frameIndex = ((System.currentTimeMillis() % totalMs) / msPerFrame).toInt()
            .coerceIn(0, registeredLocations.size - 1)

        graphics.blit(registeredLocations[frameIndex], x, y, 0f, 0f, width, height, width, height)
    }

    private fun ensureUploaded(component: PreviewImageComponent) {
        val hash = component.hashCode()
        if (hash == lastUploadedHash && uploadedTextures.isNotEmpty()) return

        release()

        try {
            val textures = mutableListOf<DynamicTexture>()
            val locations = mutableListOf<ResourceLocation>()

            component.frames.forEachIndexed { index, jpegBytes ->
                val buffered: BufferedImage = ImageIO.read(ByteArrayInputStream(jpegBytes)) ?: return
                val w = buffered.width
                val h = buffered.height

                val native = NativeImage(NativeImage.Format.RGBA, w, h, false)
                for (py in 0 until h) {
                    for (px in 0 until w) {
                        val argb = buffered.getRGB(px, py)
                        val r = (argb shr 16) and 0xFF
                        val g = (argb shr 8) and 0xFF
                        val b = argb and 0xFF
                        native.setPixelRGBA(px, py, (0xFF shl 24) or (b shl 16) or (g shl 8) or r)
                    }
                }

                val texture = DynamicTexture(native)
                // Location is unique to this renderer instance, not the component hash
                val location = ResourceLocation.fromNamespaceAndPath(
                    "mystcraft_reborn", "preview/instance_${instanceId}_frame_$index"
                )

                Minecraft.getInstance().textureManager.register(location, texture)
                textures.add(texture)
                locations.add(location)
            }

            uploadedTextures = textures
            registeredLocations = locations
            lastUploadedHash = hash

        } catch (e: Exception) {
            LogUtils.getLogger().warn("Mystcraft: Failed to upload preview frames", e)
        }
    }

    fun release() {
        registeredLocations.forEach {
            Minecraft.getInstance().textureManager.release(it)
        }
        uploadedTextures.forEach { it.close() }
        uploadedTextures = emptyList()
        registeredLocations = emptyList()
        lastUploadedHash = -1
    }

    fun getCurrentFrameLocation(): ResourceLocation? {
        if (registeredLocations.isEmpty()) return null
        val fps = 20.0 / PreviewImageComponent.TICKS_BETWEEN_FRAMES
        val msPerFrame = (1000.0 / fps).toLong()
        val totalMs = registeredLocations.size * msPerFrame
        val frameIndex = ((System.currentTimeMillis() % totalMs) / msPerFrame).toInt()
            .coerceIn(0, registeredLocations.size - 1)
        return registeredLocations[frameIndex]
    }

    fun ensureUploadedPublic(component: PreviewImageComponent) {
        ensureUploaded(component)
    }
}
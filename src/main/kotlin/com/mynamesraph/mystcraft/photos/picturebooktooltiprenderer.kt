package com.mynamesraph.mystcraft.client

import com.mynamesraph.mystcraft.component.PreviewImageComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.WeakHashMap
import javax.imageio.ImageIO

@OnlyIn(Dist.CLIENT)
object PictureBookTooltipRenderer {

    // -------------------------------------------------------------------------
    // Tweak these to adjust the tooltip preview appearance
    // -------------------------------------------------------------------------
    private const val GRID_WIDTH  = 80       // characters per row — push up for more detail,
    //                                           but watch for tooltip overflow (~44 is usually safe)
    private const val GRID_HEIGHT = 8        // lines in tooltip
    private const val PIXEL_CHAR  = "|"      // character used for each pixel — swap freely
    // -------------------------------------------------------------------------

    // Playback speed for animated (video) previews
    private const val MS_PER_FRAME = 160L    // 6.67 fps

    // Cache: PreviewImageComponent -> list of frames, each frame = list of Component lines
    private val cache = WeakHashMap<PreviewImageComponent, List<List<Component>>>()

    fun getTooltipLines(preview: PreviewImageComponent): List<Component> {
        val frames = cache.getOrPut(preview) { buildAllFrames(preview) }
        if (frames.isEmpty()) return emptyList()

        val frameIndex = if (frames.size == 1) 0
        else ((System.currentTimeMillis() / MS_PER_FRAME) % frames.size).toInt()

        return frames[frameIndex]
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private fun buildAllFrames(preview: PreviewImageComponent): List<List<Component>> {
        return preview.frames.mapNotNull { jpeg ->
            try {
                val image = ImageIO.read(ByteArrayInputStream(jpeg)) ?: return@mapNotNull null
                buildFrameLines(image)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun buildFrameLines(image: BufferedImage): List<Component> {
        val lines = mutableListOf<Component>()

        for (row in 0 until GRID_HEIGHT) {
            val line: MutableComponent = Component.literal("")

            for (col in 0 until GRID_WIDTH) {
                val srcX = (col.toFloat() / GRID_WIDTH  * image.width ).toInt().coerceIn(0, image.width  - 1)
                val srcY = (row.toFloat() / GRID_HEIGHT * image.height).toInt().coerceIn(0, image.height - 1)

                val rgb = image.getRGB(srcX, srcY) and 0xFFFFFF

                line.append(
                    Component.literal(PIXEL_CHAR).withStyle(
                        Style.EMPTY.withColor(TextColor.fromRgb(rgb))
                    )
                )
            }

            lines.add(line)
        }

        return lines
    }
}
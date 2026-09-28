package com.mynamesraph.mystcraft.client

import com.mynamesraph.mystcraft.component.PreviewTooltipComponent
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import kotlin.math.roundToInt

/** Real photo/video tooltip. Video frames use the same animated preview renderer as books/frames. */
class ClientPreviewTooltipComponent(private val data: PreviewTooltipComponent) : ClientTooltipComponent {
    private val renderWidth: Int
    private val renderHeight: Int

    init {
        val sourceW = data.sourceWidth.coerceAtLeast(1)
        val sourceH = data.sourceHeight.coerceAtLeast(1)
        val scale = minOf(128.0 / sourceW, 72.0 / sourceH)
        renderWidth = (sourceW * scale).roundToInt().coerceAtLeast(1)
        renderHeight = (sourceH * scale).roundToInt().coerceAtLeast(1)
    }

    override fun getHeight(): Int = renderHeight + 2

    override fun getWidth(font: Font): Int = renderWidth

    override fun renderImage(font: Font, x: Int, y: Int, guiGraphics: GuiGraphics) {
        ItemFramePreviewCache.getRenderer(data.preview).render(
            guiGraphics,
            data.preview,
            x,
            y,
            renderWidth,
            renderHeight
        )
    }
}

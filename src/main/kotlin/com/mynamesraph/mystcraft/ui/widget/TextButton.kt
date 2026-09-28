package com.mynamesraph.mystcraft.ui.widget

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class TextButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
    private val tooltip: Tooltip? = null,
    val scale: Int,
    var onPress: () -> Unit
) : AbstractButton(x, y, width, height, message) {

    init {
        if (tooltip != null) {
            setTooltip(tooltip)
        }
    }


    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        return
    }

    override fun onPress() {
        onPress.invoke()
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val minecraft = Minecraft.getInstance()
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, this.alpha)
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        /*guiGraphics.blitSprite(
            SPRITES[active, this.isHoveredOrFocused],
            this.x,
            this.y,
            this.getWidth(),
            this.getHeight()
        )*/
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f)

        guiGraphics.pose().scale(scale.toFloat(),scale.toFloat(),scale.toFloat())
        guiGraphics.drawString(minecraft.font,message,(x/scale),(y/scale),fgColor or (Mth.ceil(this.alpha * 255.0f) shl 24),false)
        guiGraphics.pose().scale(0.5f,0.5f,0.5f)
        //this.renderString(guiGraphics, minecraft.font, i or (Mth.ceil(this.alpha * 255.0f) shl 24))
    }

    override fun renderString(guiGraphics: GuiGraphics, font: Font, color: Int) {
        this.renderScrollingString(guiGraphics, font, 2, color)
    }

    override fun renderScrollingString(guiGraphics: GuiGraphics, font: Font, width: Int, color: Int) {
        super.renderScrollingString(guiGraphics, font, width, color)
    }


}
package com.mynamesraph.mystcraft.ui

import com.mojang.blaze3d.systems.RenderSystem
import com.mynamesraph.mystcraft.item.MystcraftDyeing
import net.minecraft.world.item.ItemStack
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

fun drawCenteredStringNoDropShadow(guiGraphics: GuiGraphics, font: Font, text: Component, x: Int, y: Int, color: Int) {
    guiGraphics.drawString(font, text, x - font.width(text) / 2, y, color,false)
}

fun getDisplayCharacterForBiome(biome: ResourceLocation): String {
    return biome.path
        .replace("_","")
        .removeSuffix("s")
        .ifEmpty {"s"}
        .removeSuffix("e")
        .ifEmpty { "e" }
        .removeSuffix("land")
        .ifEmpty {"land"}
        .removeSuffix("ocean")
        .ifEmpty {"ocean"}
        .removeSuffix("plain")
        .ifEmpty {"plains"}
        .last().uppercase()
}

/** Draw only the authored mask, after the base and before media/items/text. */
fun drawDyeOverlay(
    graphics: GuiGraphics, stack: ItemStack, mask: ResourceLocation,
    x: Int, y: Int, width: Int, height: Int
) {
    val color = MystcraftDyeing.color(stack) ?: return
    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()
    graphics.setColor(
        ((color shr 16) and 255) / 255.0f,
        ((color shr 8) and 255) / 255.0f,
        (color and 255) / 255.0f,
        1.0f
    )
    try {
        graphics.blit(mask, x, y, 0, 0, width, height)
    } finally {
        // In particular, never let the dye color leak into captured preview frames.
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.disableBlend()
    }
}

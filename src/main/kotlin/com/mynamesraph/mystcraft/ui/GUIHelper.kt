package com.mynamesraph.mystcraft.ui

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

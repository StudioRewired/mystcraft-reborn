package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.client.LinkingBookPreviewRenderer
import com.mynamesraph.mystcraft.component.CameraPhotoComponent
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class CameraPhotoScreen(
    title: Component,
    private val photo: CameraPhotoComponent
) : Screen(title) {

    // Wrap the single frame in a PreviewImageComponent so we can reuse LinkingBookPreviewRenderer as-is
    private val wrappedPreview = PreviewImageComponent(listOf(photo.jpeg))
    private val previewRenderer = LinkingBookPreviewRenderer()

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun renderBackground(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        val screenW = width
        val screenH = height

        val displayW: Int
        val displayH: Int
        if (screenW.toFloat() / screenH >= 2f) {
            displayH = screenH
            displayW = screenH * 2
        } else {
            displayW = screenW
            displayH = screenW / 2
        }

        val x = (screenW - displayW) / 2
        val y = (screenH - displayH) / 2

        previewRenderer.render(guiGraphics, wrappedPreview, x, y, displayW, displayH)
    }

    override fun removed() {
        previewRenderer.release()
        super.removed()
    }

    override fun isPauseScreen() = false
}
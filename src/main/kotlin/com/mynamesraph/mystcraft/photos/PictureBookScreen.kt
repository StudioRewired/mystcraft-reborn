package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.client.LinkingBookPreviewRenderer
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class PictureBookScreen(
    title: Component,
    private val preview: PreviewImageComponent
) : Screen(title) {

    private val TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "textures/gui/book/linking_book_unusable.png")

    private val previewRenderer = LinkingBookPreviewRenderer()
    private var fullscreen = false

    private var BACKGROUND_X = 0
    private var BACKGROUND_Y = 0
    private var PREVIEW_X = 0
    private var PREVIEW_Y = 0
    private val PREVIEW_WIDTH = 84
    private val PREVIEW_HEIGHT = 60

    private var backButton: Button? = null

    override fun init() {
        super.init()
        BACKGROUND_X = (width / 2) - 128
        BACKGROUND_Y = (height / 2) - 103
        PREVIEW_X = BACKGROUND_X + 143
        PREVIEW_Y = BACKGROUND_Y + 28

        // Back button — only visible in fullscreen mode
        backButton = Button.builder(Component.literal("Back")) {
            fullscreen = false
            updateButtonVisibility()
        }
            .pos((width / 2) - 40, height - 30)
            .size(80, 20)
            .build()

        addRenderableWidget(backButton!!)
        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        backButton?.visible = fullscreen
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Let the back button handle its own click first
        if (super.mouseClicked(mouseX, mouseY, button)) return true

        if (!fullscreen) {
            // Click inside the preview area to enter fullscreen
            if (mouseX >= PREVIEW_X && mouseX <= PREVIEW_X + PREVIEW_WIDTH
                && mouseY >= PREVIEW_Y && mouseY <= PREVIEW_Y + PREVIEW_HEIGHT
            ) {
                fullscreen = true
                updateButtonVisibility()
                return true
            }
        }

        return false
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun renderBackground(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        if (fullscreen) {
            // Fill the whole screen maintaining 2:1 aspect ratio
            val displayW: Int
            val displayH: Int
            if (width.toFloat() / height >= 2f) {
                displayH = height
                displayW = height * 2
            } else {
                displayW = width
                displayH = width / 2
            }
            val x = (width - displayW) / 2
            val y = (height - displayH) / 2
            previewRenderer.render(guiGraphics, preview, x, y, displayW, displayH)
        } else {
            // Normal book view
            previewRenderer.render(guiGraphics, preview, PREVIEW_X, PREVIEW_Y, PREVIEW_WIDTH, PREVIEW_HEIGHT)
            guiGraphics.blit(TEXTURE, BACKGROUND_X, BACKGROUND_Y, 0, 0, 256, 181)
        }
    }

    override fun removed() {
        previewRenderer.release()
        super.removed()
    }

    override fun isPauseScreen() = false
}
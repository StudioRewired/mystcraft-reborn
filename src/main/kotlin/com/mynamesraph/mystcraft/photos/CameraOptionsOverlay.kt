package com.mynamesraph.mystcraft.ui.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand

class CameraOptionsOverlay(
    private val hand: InteractionHand,
    private val onPhoto: () -> Unit,
    private val onVideo: (durationSeconds: Int) -> Unit,
    private val onCancel: () -> Unit
) {
    private val mc = Minecraft.getInstance()

    private var showVideoControls = false
    private var videoDurationSeconds = 2
    private val durationRange = 1..20

    // Buttons for initial view
    private var photoButton: Button? = null
    private var videoButton: Button? = null
    private var cancelButton: Button? = null

    // Buttons for video controls view
    private var confirmButton: Button? = null
    private var cancelVideoButton: Button? = null
    private var durationSlider: AbstractSliderButton? = null

    var visible = true

    fun init(screenWidth: Int, screenHeight: Int) {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        val buttonWidth = 60
        val buttonHeight = 20
        val gap = 8
        val totalWidth = buttonWidth * 3 + gap * 2
        val startX = centerX - totalWidth / 2

        photoButton = Button.builder(Component.literal("Photo")) {
            visible = false
            onPhoto()
        }
            .pos(startX, centerY)
            .size(buttonWidth, buttonHeight)
            .build()

        videoButton = Button.builder(Component.literal("Video")) {
            showVideoControls = true
            rebuildVideoControls(screenWidth, screenHeight)
        }
            .pos(startX + buttonWidth + gap, centerY)
            .size(buttonWidth, buttonHeight)
            .build()

        cancelButton = Button.builder(Component.literal("Cancel")) {
            visible = false
            onCancel()
        }
            .pos(startX + (buttonWidth + gap) * 2, centerY)
            .size(buttonWidth, buttonHeight)
            .build()
    }

    private fun rebuildVideoControls(screenWidth: Int, screenHeight: Int) {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        val buttonWidth = 60
        val buttonHeight = 20
        val gap = 8
        val sliderWidth = 160

        durationSlider = object : AbstractSliderButton(
            centerX - sliderWidth / 2,
            centerY - buttonHeight - gap,
            sliderWidth,
            buttonHeight,
            Component.literal("Duration: ${videoDurationSeconds}s"),
            (videoDurationSeconds - durationRange.first).toDouble() /
                    (durationRange.last - durationRange.first).toDouble()
        ) {
            override fun updateMessage() {
                this.message = Component.literal("Duration: ${videoDurationSeconds}s")
            }

            override fun applyValue() {
                videoDurationSeconds = (durationRange.first +
                        (value * (durationRange.last - durationRange.first)).toInt())
                    .coerceIn(durationRange.first, durationRange.last)
                updateMessage()
            }
        }

        confirmButton = Button.builder(Component.literal("Confirm")) {
            visible = false
            onVideo(videoDurationSeconds)
        }
            .pos(centerX - buttonWidth - gap / 2, centerY)
            .size(buttonWidth, buttonHeight)
            .build()

        cancelVideoButton = Button.builder(Component.literal("Cancel")) {
            visible = false
            onCancel()
        }
            .pos(centerX + gap / 2, centerY)
            .size(buttonWidth, buttonHeight)
            .build()
    }

    fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!visible) return

        if (showVideoControls) {
            durationSlider?.render(graphics, mouseX, mouseY, partialTick)
            confirmButton?.render(graphics, mouseX, mouseY, partialTick)
            cancelVideoButton?.render(graphics, mouseX, mouseY, partialTick)
        } else {
            photoButton?.render(graphics, mouseX, mouseY, partialTick)
            videoButton?.render(graphics, mouseX, mouseY, partialTick)
            cancelButton?.render(graphics, mouseX, mouseY, partialTick)
        }
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!visible) return false
        return if (showVideoControls) {
            durationSlider?.mouseClicked(mouseX, mouseY, button) == true ||
                    confirmButton?.mouseClicked(mouseX, mouseY, button) == true ||
                    cancelVideoButton?.mouseClicked(mouseX, mouseY, button) == true
        } else {
            photoButton?.mouseClicked(mouseX, mouseY, button) == true ||
                    videoButton?.mouseClicked(mouseX, mouseY, button) == true ||
                    cancelButton?.mouseClicked(mouseX, mouseY, button) == true
        }
    }

    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (!visible) return false
        return durationSlider?.mouseDragged(mouseX, mouseY, button, dragX, dragY) == true
    }

    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!visible) return false
        return durationSlider?.mouseReleased(mouseX, mouseY, button) == true
    }
}
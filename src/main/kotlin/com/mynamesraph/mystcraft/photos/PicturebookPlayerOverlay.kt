package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.data.networking.packet.PictureBookPlayerUpdatePacket
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.network.PacketDistributor
import kotlin.math.roundToInt

class PictureBookPlayerOverlay(
    private val pos: BlockPos,
    initialWidth: Int,
    initialHeight: Int,
    initialBacklight: Boolean,
    initialVerticalOffset: Float = 0f,
    initialHorizontalOffset: Float = 0f,
    initialRotation: Float = 0f
) {
    private val mc = Minecraft.getInstance()

    var visible = true
    private var pendingClose = false

    private var currentWidth            = initialWidth
    private var currentHeight           = initialHeight
    private var currentBacklight        = initialBacklight
    private var currentVerticalOffset   = initialVerticalOffset
    private var currentHorizontalOffset = initialHorizontalOffset
    private var currentRotation = initialRotation

    private var removeButton:          Button? = null
    private var resetButton:           Button? = null
    private var backlightButton:       Button? = null
    private var widthSlider:           AbstractSliderButton? = null
    private var heightSlider:          AbstractSliderButton? = null
    private var verticalOffsetSlider:  AbstractSliderButton? = null
    private var horizontalOffsetSlider:AbstractSliderButton? = null
    private var rotationSlider: AbstractSliderButton? = null
    private var closeButton:           Button? = null

    private val btnW    = 100
    private val btnH    = 20
    private val sliderW = 160
    private val gap     = 6
    private val yOffset = -30  // shift up a bit to account for the extra rows

    // 4 sliders + backlight + reset + remove + close = 8 rows
    private fun totalH() = btnH * 9 + gap * 8

    private fun startY(screenHeight: Int): Int = screenHeight / 2 - totalH() / 2 + yOffset

    fun init(screenWidth: Int, screenHeight: Int) {
        val centerX = screenWidth / 2
        var y = startY(screenHeight)

        widthSlider = makeWidthSlider(centerX, y)
        y += btnH + gap

        heightSlider = makeHeightSlider(centerX, y)
        y += btnH + gap

        horizontalOffsetSlider = makeHorizontalOffsetSlider(centerX, y)
        y += btnH + gap

        verticalOffsetSlider = makeVerticalOffsetSlider(centerX, y)
        y += btnH + gap

        rotationSlider = makeRotationSlider(centerX, y)
        y += btnH + gap

        resetButton = Button.builder(Component.literal("Reset")) {
            currentWidth            = 1
            currentHeight           = 1
            currentVerticalOffset   = 0f
            currentHorizontalOffset = 0f
            currentRotation = 0f
            rebuildSliders(screenWidth, screenHeight)
            sendUpdate(removeBook = false)
        }.pos(centerX - btnW / 2, y).size(btnW, btnH).build()
        y += btnH + gap

        backlightButton = Button.builder(backlightLabel()) {
            currentBacklight = !currentBacklight
            it.message = backlightLabel()
            sendUpdate(removeBook = false)
        }.pos(centerX - btnW / 2, y).size(btnW, btnH).build()
        y += btnH + gap

        removeButton = Button.builder(Component.literal("Remove Book")) {
            sendUpdate(removeBook = true)
            visible = false
            mc.mouseHandler.grabMouse()
        }.pos(centerX - btnW / 2, y).size(btnW, btnH).build()
        y += btnH + gap

        closeButton = Button.builder(Component.literal("Close")) {
            pendingClose = true
        }.pos(centerX - btnW / 2, y).size(btnW, btnH).build()
    }

    // ── Slider factories ──────────────────────────────────────────────────────

    private fun makeWidthSlider(centerX: Int, y: Int) = object : AbstractSliderButton(
        centerX - sliderW / 2, y, sliderW, btnH,
        Component.literal("Width: $currentWidth"),
        (currentWidth - 1) / 35.0
    ) {
        override fun updateMessage() { message = Component.literal("Width: $currentWidth") }
        override fun applyValue() {
            currentWidth = (1 + (value * 35).toInt()).coerceIn(1, 36)
            updateMessage()
            sendUpdate(removeBook = false)
        }
    }

    private fun makeHeightSlider(centerX: Int, y: Int) = object : AbstractSliderButton(
        centerX - sliderW / 2, y, sliderW, btnH,
        Component.literal("Height: $currentHeight"),
        (currentHeight - 1) / 35.0
    ) {
        override fun updateMessage() { message = Component.literal("Height: $currentHeight") }
        override fun applyValue() {
            currentHeight = (1 + (value * 35).toInt()).coerceIn(1, 36)
            updateMessage()
            sendUpdate(removeBook = false)
        }
    }

    /**
     * Offset slider helper: range is [-16, 16] blocks in 0.5-block increments (65 steps).
     * We map 0.0 → -16, 0.5 → 0, 1.0 → +16.
     */
    private fun offsetToSliderValue(offset: Float) = ((offset + 17.5f) / 35f).toDouble().coerceIn(0.0, 1.0)
    private fun sliderValueToOffset(v: Double) = ((v * 35f) - 17.5f).let {
        // Snap to nearest 0.5
        ((it * 2).roundToInt() / 2f).coerceIn(-17.5f, 17.5f)
    }

    private fun makeHorizontalOffsetSlider(centerX: Int, y: Int) = object : AbstractSliderButton(
        centerX - sliderW / 2, y, sliderW, btnH,
        Component.literal("Horizontal Offset: ${fmtOffset(currentHorizontalOffset)}"),
        offsetToSliderValue(currentHorizontalOffset)
    ) {
        override fun updateMessage() { message = Component.literal("Horizontal Offset: ${fmtOffset(currentHorizontalOffset)}") }
        override fun applyValue() {
            currentHorizontalOffset = sliderValueToOffset(value)
            updateMessage()
            sendUpdate(removeBook = false)
        }
    }

    private fun makeVerticalOffsetSlider(centerX: Int, y: Int) = object : AbstractSliderButton(
        centerX - sliderW / 2, y, sliderW, btnH,
        Component.literal("Vertical Offset: ${fmtOffset(currentVerticalOffset)}"),
        offsetToSliderValue(currentVerticalOffset)
    ) {
        override fun updateMessage() { message = Component.literal("Vertical Offset: ${fmtOffset(currentVerticalOffset)}") }
        override fun applyValue() {
            currentVerticalOffset = sliderValueToOffset(value)
            updateMessage()
            sendUpdate(removeBook = false)
        }
    }

    private fun makeRotationSlider(centerX: Int, y: Int) = object : AbstractSliderButton(
        centerX - sliderW / 2, y, sliderW, btnH,
        Component.literal("Rotation: ${currentRotation.toInt()}°"),
        currentRotation / 350.0
    ) {
        override fun updateMessage() { message = Component.literal("Rotation: ${currentRotation.toInt()}°") }
        override fun applyValue() {
            currentRotation = ((value * 35).toInt() * 10f).coerceIn(0f, 350f)
            updateMessage()
            sendUpdate(removeBook = false)
        }
    }

    private fun fmtOffset(v: Float): String {
        val s = if (v >= 0) "+%.1f".format(v) else "%.1f".format(v)
        return s
    }

    private fun rebuildSliders(screenWidth: Int, screenHeight: Int) {
        val centerX = screenWidth / 2
        var y = startY(screenHeight)
        widthSlider            = makeWidthSlider(centerX, y);            y += btnH + gap
        heightSlider           = makeHeightSlider(centerX, y);           y += btnH + gap
        horizontalOffsetSlider = makeHorizontalOffsetSlider(centerX, y); y += btnH + gap
        verticalOffsetSlider   = makeVerticalOffsetSlider(centerX, y);   y += btnH + gap
        rotationSlider         = makeRotationSlider(centerX, y)
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private fun backlightLabel() =
        Component.literal(if (currentBacklight) "Backlight: ON" else "Backlight: OFF")

    private fun sendUpdate(removeBook: Boolean) {
        PacketDistributor.sendToServer(
            PictureBookPlayerUpdatePacket(
                pos,
                currentWidth,
                currentHeight,
                currentBacklight,
                removeBook,
                currentVerticalOffset,
                currentHorizontalOffset,
                currentRotation
            )
        )
    }

    fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!visible) return
        if (pendingClose) {
            visible = false
            mc.mouseHandler.grabMouse()
            return
        }

        val th = totalH()
        val panelW = sliderW + 20
        val centerX = mc.window.guiScaledWidth  / 2
        val centerY = mc.window.guiScaledHeight / 2
        val panelTop    = centerY - th / 2 + yOffset - 5
        val panelBottom = panelTop + th + 10

        graphics.fill(
            centerX - panelW / 2 - 5, panelTop,
            centerX + panelW / 2 + 5, panelBottom,
            0xAA000000.toInt()
        )

        widthSlider?.render(graphics, mouseX, mouseY, partialTick)
        heightSlider?.render(graphics, mouseX, mouseY, partialTick)
        horizontalOffsetSlider?.render(graphics, mouseX, mouseY, partialTick)
        verticalOffsetSlider?.render(graphics, mouseX, mouseY, partialTick)
        rotationSlider?.render(graphics, mouseX, mouseY, partialTick)
        resetButton?.render(graphics, mouseX, mouseY, partialTick)
        backlightButton?.render(graphics, mouseX, mouseY, partialTick)
        removeButton?.render(graphics, mouseX, mouseY, partialTick)
        closeButton?.render(graphics, mouseX, mouseY, partialTick)
    }

    private var draggingSlider: AbstractSliderButton? = null

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!visible) return false
        val allSliders = listOf(widthSlider, heightSlider, horizontalOffsetSlider, verticalOffsetSlider, rotationSlider)
        for (slider in allSliders) {
            if (slider?.isMouseOver(mouseX, mouseY) == true) {
                slider.mouseClicked(mouseX, mouseY, button)
                draggingSlider = slider
                return true
            }
        }
        return backlightButton?.mouseClicked(mouseX, mouseY, button) == true
                || resetButton?.mouseClicked(mouseX, mouseY, button) == true
                || removeButton?.mouseClicked(mouseX, mouseY, button) == true
                || closeButton?.mouseClicked(mouseX, mouseY, button) == true
    }

    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (!visible) return false
        val s = draggingSlider ?: return false
        s.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        return true
    }

    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!visible) return false
        draggingSlider?.mouseReleased(mouseX, mouseY, button)
        draggingSlider = null
        return true
    }
}
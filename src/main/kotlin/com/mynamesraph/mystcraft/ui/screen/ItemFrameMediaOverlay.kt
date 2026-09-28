package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.data.networking.packet.ItemFrameMediaUpdatePacket
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.network.PacketDistributor
import kotlin.math.roundToInt

/** Book-player-style controls for an item frame. Projection size intentionally stays fixed. */
class ItemFrameMediaOverlay(
    private val entityId: Int,
    initialBacklight: Boolean,
    initialVerticalOffset: Float,
    initialHorizontalOffset: Float,
    initialRotation: Float
) {
    private val mc = Minecraft.getInstance()

    var visible = true
    private var pendingClose = false

    private var currentBacklight = initialBacklight
    private var currentVerticalOffset = initialVerticalOffset
    private var currentHorizontalOffset = initialHorizontalOffset
    private var currentRotation = initialRotation

    private var horizontalOffsetSlider: AbstractSliderButton? = null
    private var verticalOffsetSlider: AbstractSliderButton? = null
    private var rotationSlider: AbstractSliderButton? = null
    private var resetButton: Button? = null
    private var backlightButton: Button? = null
    private var removeButton: Button? = null
    private var closeButton: Button? = null
    private var draggingSlider: AbstractSliderButton? = null

    private val btnW = 110
    private val btnH = 20
    private val sliderW = 170
    private val gap = 6

    private fun totalH() = btnH * 7 + gap * 6
    private fun startY(screenHeight: Int) = screenHeight / 2 - totalH() / 2

    fun init(screenWidth: Int, screenHeight: Int) {
        val centerX = screenWidth / 2
        var y = startY(screenHeight)

        horizontalOffsetSlider = makeHorizontalOffsetSlider(centerX, y); y += btnH + gap
        verticalOffsetSlider = makeVerticalOffsetSlider(centerX, y); y += btnH + gap
        rotationSlider = makeRotationSlider(centerX, y); y += btnH + gap

        resetButton = Button.builder(Component.literal("Reset")) {
            currentHorizontalOffset = 0f
            currentVerticalOffset = 0f
            currentRotation = 0f
            rebuildSliders(screenWidth, screenHeight)
            sendUpdate(removeItem = false)
        }.pos(centerX - btnW / 2, y).size(btnW, btnH).build(); y += btnH + gap

        backlightButton = Button.builder(backlightLabel()) {
            currentBacklight = !currentBacklight
            it.message = backlightLabel()
            sendUpdate(removeItem = false)
        }.pos(centerX - btnW / 2, y).size(btnW, btnH).build(); y += btnH + gap

        removeButton = Button.builder(Component.literal("Remove Media")) {
            sendUpdate(removeItem = true)
            visible = false
            mc.mouseHandler.grabMouse()
        }.pos(centerX - btnW / 2, y).size(btnW, btnH).build(); y += btnH + gap

        closeButton = Button.builder(Component.literal("Close")) {
            pendingClose = true
        }.pos(centerX - btnW / 2, y).size(btnW, btnH).build()
    }

    // Match the Book Player's offset controls exactly; only projection resizing is omitted.
    private fun offsetToSliderValue(offset: Float): Double =
        ((offset + 17.5f) / 35f).toDouble().coerceIn(0.0, 1.0)

    private fun sliderValueToOffset(value: Double): Float = ((value * 35f) - 17.5f).let {
        ((it * 2).roundToInt() / 2f).coerceIn(-17.5f, 17.5f)
    }

    private fun fmtOffset(value: Float): String = if (value >= 0f) "+%.1f".format(value) else "%.1f".format(value)

    private fun makeHorizontalOffsetSlider(centerX: Int, y: Int) = object : AbstractSliderButton(
        centerX - sliderW / 2, y, sliderW, btnH,
        Component.literal("Horizontal Offset: ${fmtOffset(currentHorizontalOffset)}"),
        offsetToSliderValue(currentHorizontalOffset)
    ) {
        override fun updateMessage() { message = Component.literal("Horizontal Offset: ${fmtOffset(currentHorizontalOffset)}") }
        override fun applyValue() {
            currentHorizontalOffset = sliderValueToOffset(value)
            updateMessage()
            sendUpdate(removeItem = false)
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
            sendUpdate(removeItem = false)
        }
    }

    private fun makeRotationSlider(centerX: Int, y: Int) = object : AbstractSliderButton(
        centerX - sliderW / 2, y, sliderW, btnH,
        Component.literal("Rotation: ${currentRotation.toInt()}°"),
        (currentRotation / 350.0).coerceIn(0.0, 1.0)
    ) {
        override fun updateMessage() { message = Component.literal("Rotation: ${currentRotation.toInt()}°") }
        override fun applyValue() {
            currentRotation = ((value * 35).roundToInt() * 10f).coerceIn(0f, 350f)
            updateMessage()
            sendUpdate(removeItem = false)
        }
    }

    private fun rebuildSliders(screenWidth: Int, screenHeight: Int) {
        val centerX = screenWidth / 2
        var y = startY(screenHeight)
        horizontalOffsetSlider = makeHorizontalOffsetSlider(centerX, y); y += btnH + gap
        verticalOffsetSlider = makeVerticalOffsetSlider(centerX, y); y += btnH + gap
        rotationSlider = makeRotationSlider(centerX, y)
    }

    private fun backlightLabel() = Component.literal(if (currentBacklight) "Backlight: ON" else "Backlight: OFF")

    private fun sendUpdate(removeItem: Boolean) {
        PacketDistributor.sendToServer(
            ItemFrameMediaUpdatePacket(
                entityId,
                currentBacklight,
                removeItem,
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

        val panelW = sliderW + 20
        val centerX = mc.window.guiScaledWidth / 2
        val panelTop = startY(mc.window.guiScaledHeight) - 5
        val panelBottom = panelTop + totalH() + 10
        graphics.fill(
            centerX - panelW / 2 - 5, panelTop,
            centerX + panelW / 2 + 5, panelBottom,
            0xAA000000.toInt()
        )

        horizontalOffsetSlider?.render(graphics, mouseX, mouseY, partialTick)
        verticalOffsetSlider?.render(graphics, mouseX, mouseY, partialTick)
        rotationSlider?.render(graphics, mouseX, mouseY, partialTick)
        resetButton?.render(graphics, mouseX, mouseY, partialTick)
        backlightButton?.render(graphics, mouseX, mouseY, partialTick)
        removeButton?.render(graphics, mouseX, mouseY, partialTick)
        closeButton?.render(graphics, mouseX, mouseY, partialTick)
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!visible) return false
        for (slider in listOf(horizontalOffsetSlider, verticalOffsetSlider, rotationSlider)) {
            if (slider?.isMouseOver(mouseX, mouseY) == true) {
                slider.mouseClicked(mouseX, mouseY, button)
                draggingSlider = slider
                return true
            }
        }
        return resetButton?.mouseClicked(mouseX, mouseY, button) == true ||
                backlightButton?.mouseClicked(mouseX, mouseY, button) == true ||
                removeButton?.mouseClicked(mouseX, mouseY, button) == true ||
                closeButton?.mouseClicked(mouseX, mouseY, button) == true
    }

    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        val slider = draggingSlider ?: return false
        slider.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        return true
    }

    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        draggingSlider?.mouseReleased(mouseX, mouseY, button)
        draggingSlider = null
        return visible
    }
}

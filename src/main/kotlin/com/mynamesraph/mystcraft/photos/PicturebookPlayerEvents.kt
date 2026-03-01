package com.mynamesraph.mystcraft.events

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.block.mediaplayer.PictureBookPlayerBlockEntity
import com.mynamesraph.mystcraft.ui.screen.PictureBookPlayerOverlay
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import org.lwjgl.glfw.GLFW

@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object PictureBookPlayerEvents {

    private var overlay: PictureBookPlayerOverlay? = null

    // Drag tracking state
    private var isMouseDown = false
    private var lastMouseX  = 0.0
    private var lastMouseY  = 0.0

    fun openOverlay(pos: BlockPos, be: PictureBookPlayerBlockEntity) {
        val mc = Minecraft.getInstance()
        overlay = PictureBookPlayerOverlay(
            pos                  = pos,
            initialWidth         = be.displayWidth,
            initialHeight        = be.displayHeight,
            initialBacklight     = be.backlightOn,
            initialVerticalOffset   = be.verticalOffset,
            initialHorizontalOffset = be.horizontalOffset,
            initialRotation = be.rotation
        ).also { it.init(mc.window.guiScaledWidth, mc.window.guiScaledHeight) }
        mc.mouseHandler.releaseMouse()
        mc.options.keyUse.setDown(false)
        mc.options.keyAttack.setDown(false)
    }

    @SubscribeEvent
    fun onRenderGui(event: RenderGuiEvent.Post) {
        val ov = overlay ?: return
        val mc = Minecraft.getInstance()

        val mouseX = mc.mouseHandler.xpos() / mc.window.guiScale
        val mouseY = mc.mouseHandler.ypos() / mc.window.guiScale

        // Poll drag every frame — NeoForge has no MouseDragged event outside of Screen
        if (isMouseDown) {
            val dragX = mouseX - lastMouseX
            val dragY = mouseY - lastMouseY
            if (dragX != 0.0 || dragY != 0.0) {
                ov.mouseDragged(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_LEFT, dragX, dragY)
            }
            lastMouseX = mouseX
            lastMouseY = mouseY
        }

        ov.render(
            event.guiGraphics,
            mouseX.toInt(),
            mouseY.toInt(),
            mc.timer.getGameTimeDeltaPartialTick(true)
        )

        if (!ov.visible) overlay = null
    }

    @SubscribeEvent
    fun onMouseClick(event: InputEvent.MouseButton.Pre) {
        val ov = overlay ?: return
        if (!ov.visible) return

        val mc = Minecraft.getInstance()
        event.isCanceled = true

        val mouseX = mc.mouseHandler.xpos() / mc.window.guiScale
        val mouseY = mc.mouseHandler.ypos() / mc.window.guiScale

        when (event.action) {
            GLFW.GLFW_PRESS -> {
                isMouseDown = true
                lastMouseX  = mouseX
                lastMouseY  = mouseY
                ov.mouseClicked(mouseX, mouseY, event.button)
            }
            GLFW.GLFW_RELEASE -> {
                isMouseDown = false
                ov.mouseReleased(mouseX, mouseY, event.button)
            }
        }
    }

    @SubscribeEvent
    fun onRenderCrosshair(event: net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre) {
        if (event.name != net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR) return
        if (overlay?.visible == true) event.isCanceled = true
    }

    @SubscribeEvent
    fun onScreenOpen(event: net.neoforged.neoforge.client.event.ScreenEvent.Opening) {
        val ov = overlay ?: return
        if (!ov.visible) return
        if (event.screen is net.minecraft.client.gui.screens.PauseScreen) {
            event.isCanceled = true
            ov.visible = false
            Minecraft.getInstance().mouseHandler.grabMouse()
        }
    }
}
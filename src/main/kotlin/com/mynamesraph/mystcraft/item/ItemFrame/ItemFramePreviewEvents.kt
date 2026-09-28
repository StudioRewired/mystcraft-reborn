package com.mynamesraph.mystcraft.events

import com.mojang.math.Axis
import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.client.ItemFramePreviewCache
import com.mynamesraph.mystcraft.component.ItemFrameMediaSettingsComponent
import com.mynamesraph.mystcraft.item.MystcraftMedia
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.ui.screen.ItemFrameMediaOverlay
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.decoration.ItemFrame
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import org.lwjgl.glfw.GLFW

@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object ItemFramePreviewEvents {
    private var overlay: ItemFrameMediaOverlay? = null
    private var isMouseDown = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    @SubscribeEvent
    fun onRenderItemInFrame(event: RenderItemInFrameEvent) {
        val stack = event.itemStack
        val preview = MystcraftMedia.preview(stack) ?: return
        val settings = (stack.get(MystcraftComponents.ITEM_FRAME_MEDIA_SETTINGS.get())
            ?: ItemFrameMediaSettingsComponent.DEFAULT).sanitized()

        val renderer = ItemFramePreviewCache.getRenderer(preview)
        renderer.ensureUploadedPublic(preview)
        val location = renderer.getCurrentFrameLocation() ?: return

        event.isCanceled = true

        val poseStack = event.poseStack
        poseStack.pushPose()
        poseStack.translate(
            settings.horizontalOffset.toDouble(),
            settings.verticalOffset.toDouble(),
            -0.03
        )
        poseStack.mulPose(Axis.ZP.rotationDegrees(settings.rotation))

        val matrix = poseStack.last().pose()
        val buffer = event.multiBufferSource.getBuffer(
            net.minecraft.client.renderer.RenderType.text(location)
        )
        val light = if (settings.backlight) 0xF000F0 else event.packedLight

        // Item-frame media is deliberately fixed at one-frame size. Large projections
        // remain exclusive to the dedicated Picture Book Player.
        buffer.addVertex(matrix, -0.45f, 0.45f, 0f).setUv(0f, 0f).setLight(light).setColor(255, 255, 255, 255)
        buffer.addVertex(matrix, 0.45f, 0.45f, 0f).setUv(1f, 0f).setLight(light).setColor(255, 255, 255, 255)
        buffer.addVertex(matrix, 0.45f, -0.45f, 0f).setUv(1f, 1f).setLight(light).setColor(255, 255, 255, 255)
        buffer.addVertex(matrix, -0.45f, -0.45f, 0f).setUv(0f, 1f).setLight(light).setColor(255, 255, 255, 255)

        poseStack.popPose()
    }

    @SubscribeEvent
    fun onEntityInteractSpecific(event: PlayerInteractEvent.EntityInteractSpecific) {
        if (!event.level.isClientSide) return
        val frame = event.target as? ItemFrame ?: return
        if (MystcraftMedia.preview(frame.item) == null) return

        event.isCanceled = true
        event.cancellationResult = InteractionResult.SUCCESS
        openOverlay(frame)
    }

    private fun openOverlay(frame: ItemFrame) {
        val mc = Minecraft.getInstance()
        val settings = (frame.item.get(MystcraftComponents.ITEM_FRAME_MEDIA_SETTINGS.get())
            ?: ItemFrameMediaSettingsComponent.DEFAULT).sanitized()
        overlay = ItemFrameMediaOverlay(
            entityId = frame.id,
            initialBacklight = settings.backlight,
            initialVerticalOffset = settings.verticalOffset,
            initialHorizontalOffset = settings.horizontalOffset,
            initialRotation = settings.rotation
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

        if (isMouseDown) {
            val dragX = mouseX - lastMouseX
            val dragY = mouseY - lastMouseY
            if (dragX != 0.0 || dragY != 0.0) {
                ov.mouseDragged(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_LEFT, dragX, dragY)
            }
            lastMouseX = mouseX
            lastMouseY = mouseY
        }

        ov.render(event.guiGraphics, mouseX.toInt(), mouseY.toInt(), mc.timer.getGameTimeDeltaPartialTick(true))
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
                lastMouseX = mouseX
                lastMouseY = mouseY
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
        if (event.screen is PauseScreen) {
            event.isCanceled = true
            ov.visible = false
            Minecraft.getInstance().mouseHandler.grabMouse()
        }
    }
}

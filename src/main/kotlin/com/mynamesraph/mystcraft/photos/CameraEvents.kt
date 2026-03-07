package com.mynamesraph.mystcraft.events

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.client.CameraPreviewCapture
import com.mynamesraph.mystcraft.component.CameraPhotoComponent
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import com.mynamesraph.mystcraft.data.networking.packet.SendCameraPhotoPacket
import com.mynamesraph.mystcraft.data.networking.packet.SendCameraVideoPacket
import com.mynamesraph.mystcraft.item.CameraItem
import com.mynamesraph.mystcraft.item.PictureBookItem
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.ui.screen.CameraOptionsOverlay
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent
import net.neoforged.neoforge.client.event.ViewportEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW
import net.minecraft.world.item.Items


@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object CameraEvents {

    private const val CAPTURE_DELAY_TICKS = 75
    private const val FINISHED_MESSAGE_TICKS = 60

    private var captureScheduled = false
    private var pendingCaptureTicks = 0
    private var captureHand: InteractionHand? = null
    private var finishedMessageTicks = 0

    private var postCountdownTicks = 0
    private const val POST_COUNTDOWN_DELAY = 3 // a few frames after "1..." clears

    // Video capture state
    private var isVideoMode = false
    private var videoFramesTarget = 0
    private var videoTicksTotal = 0
    private var videoTicksElapsed = 0
    private val capturedVideoFrames = mutableListOf<ByteArray>()

    // Zoom state
    internal var zoomActive = false
        private set
    private var zoomFov = 90f
    private const val FOV_STEP = 6f
    private const val FOV_MIN = 5f
    private const val FOV_MAX = 140f

    private var lastCaptureWasVideo = false

    // Options overlay
    private var optionsOverlay: CameraOptionsOverlay? = null

    fun showOptionsOverlay(hand: InteractionHand) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        val hasPaper = (0 until player.inventory.containerSize).any { i ->
            player.inventory.getItem(i).item == Items.PAPER
        }

        if (!hasPaper) {
            player.displayClientMessage(
                Component.literal("No paper in inventory."),
                true
            )
            return
        }

        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight

        optionsOverlay = CameraOptionsOverlay(
            hand = hand,
            onPhoto = {
                optionsOverlay = null
                mc.mouseHandler.grabMouse()
                scheduleCapture(hand, videoMode = false, durationSeconds = 0)
            },
            onVideo = { durationSeconds ->
                optionsOverlay = null
                mc.mouseHandler.grabMouse()
                scheduleCapture(hand, videoMode = true, durationSeconds = durationSeconds)
            },
            onCancel = {
                optionsOverlay = null
                mc.mouseHandler.grabMouse()
            }
        ).also { it.init(screenW, screenH) }

        // Release the cursor
        mc.mouseHandler.releaseMouse()

        // Tell Minecraft the right mouse button is no longer held so it doesn't
        // keep firing use() every tick while the overlay is open
        mc.options.keyUse.setDown(false)
        // Also release left click to prevent stuck mouse state
        mc.options.keyAttack.setDown(false)
    }

    fun toggleZoom() {
        zoomActive = !zoomActive
        if (zoomActive) zoomFov = 70f
    }

    fun scheduleCapture(hand: InteractionHand, videoMode: Boolean, durationSeconds: Int) {
        if (captureScheduled) return

        captureScheduled = true
        captureHand = hand
        pendingCaptureTicks = CAPTURE_DELAY_TICKS
        isVideoMode = videoMode

        if (videoMode) {
            videoTicksTotal = durationSeconds * 20
            videoFramesTarget = (durationSeconds * 20) / PreviewImageComponent.TICKS_BETWEEN_FRAMES
            videoTicksElapsed = 0
            capturedVideoFrames.clear()
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent.Post) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        // Countdown
        if (pendingCaptureTicks > 0) {
            pendingCaptureTicks--
            if (pendingCaptureTicks == 0) {
                postCountdownTicks = 6  // let GUI render clean frames before capturing
            }
        }

        // Post-countdown delay before actual capture
        if (postCountdownTicks > 0) {
            postCountdownTicks--
            if (postCountdownTicks == 0) {
                if (isVideoMode) startVideoCapture(mc)
                else doPhotoCapture(mc)
            }
        }

        // Video frame scheduling — runs after countdown ends
        if (isVideoMode && captureScheduled && pendingCaptureTicks == 0) {
            videoTicksElapsed++
            if (videoTicksElapsed % PreviewImageComponent.TICKS_BETWEEN_FRAMES == 0) {
                CameraPreviewCapture.captureVideoFrame { jpeg ->
                    capturedVideoFrames.add(jpeg)
                    if (capturedVideoFrames.size >= videoFramesTarget) {
                        finishVideoCapture()
                    }
                }
            }
        }

        if (finishedMessageTicks > 0) finishedMessageTicks--

        // FOV management
        val holdingCamera = player.mainHandItem.item is CameraItem
                || player.offhandItem.item is CameraItem

        if (!holdingCamera) {
            if (zoomActive) zoomActive = false
            if (optionsOverlay != null) {
                optionsOverlay = null
                mc.mouseHandler.grabMouse()
            }
        }
    }

    @SubscribeEvent
    fun onComputeFov(event: ViewportEvent.ComputeFov) {
        if (zoomActive) event.fov = zoomFov.toDouble()
    }

    @SubscribeEvent
    fun onMouseScroll(event: InputEvent.MouseScrollingEvent) {
        if (!zoomActive) return
        event.isCanceled = true
        val delta = event.scrollDeltaY
        zoomFov = (zoomFov - delta.toFloat() * FOV_STEP).coerceIn(FOV_MIN, FOV_MAX)
    }

    @SubscribeEvent
    fun onMouseClick(event: InputEvent.MouseButton.Pre) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (mc.screen != null) return

        val holdingCamera = player.mainHandItem.item is CameraItem
                || player.offhandItem.item is CameraItem
        if (!holdingCamera) return

        // Middle click toggles zoom — only when overlay is not open
        if (event.button == 2 && event.action == GLFW.GLFW_PRESS && optionsOverlay == null) {
            event.isCanceled = true
            toggleZoom()
            return
        }

        // If overlay is visible, cancel ALL mouse events and let the overlay
        // handle clicks via onRenderGui's mouse position tracking instead
        if (optionsOverlay?.visible == true) {
            event.isCanceled = true
            if (event.action == GLFW.GLFW_PRESS) {
                val mouseX = mc.mouseHandler.xpos() / mc.window.guiScale
                val mouseY = mc.mouseHandler.ypos() / mc.window.guiScale
                optionsOverlay?.mouseClicked(mouseX, mouseY, event.button)
            }
        }
    }

    @SubscribeEvent
    fun onRenderGui(event: RenderGuiEvent.Post) {
        val mc = Minecraft.getInstance()
        val graphics = event.guiGraphics
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight
        val font = mc.font

        // Render options overlay
        optionsOverlay?.render(
            graphics,
            (mc.mouseHandler.xpos() / mc.window.guiScale).toInt(),
            (mc.mouseHandler.ypos() / mc.window.guiScale).toInt(),
            mc.timer.getGameTimeDeltaPartialTick(true)
        )



        // Countdown
        if (pendingCaptureTicks > 0) {
            val message: Component = when {
                pendingCaptureTicks > 50 -> Component.literal("§e3...")
                pendingCaptureTicks > 25 -> Component.literal("§e2...")
                else -> Component.literal("§e1...")
            }
            val textW = font.width(message)
            graphics.drawString(font, message, (screenW - textW) / 2, screenH / 2 - 20, 0xFFFFFF, true)
        }

        // Finished message
        if (finishedMessageTicks > 0) {
            val msg = if (lastCaptureWasVideo) Component.literal("§aMedia captured!")
            else Component.literal("§aMedia captured!")
            val textW = font.width(msg)
            graphics.drawString(font, msg, (screenW - textW) / 2, screenH / 2 - 20, 0xFFFFFF, true)
        }
    }

    @SubscribeEvent
    fun onRenderCrosshairLayer(event: RenderGuiLayerEvent.Pre) {
        if (event.name != VanillaGuiLayers.CROSSHAIR) return
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val holdingCamera = player.mainHandItem.item is CameraItem
                || player.offhandItem.item is CameraItem
        if (holdingCamera) event.isCanceled = true
    }

    private fun startVideoCapture(mc: Minecraft) {
        // videoTicksElapsed starts incrementing in onTick now
        videoTicksElapsed = 0
    }

    private fun doPhotoCapture(mc: Minecraft) {
        val hand = captureHand ?: return
        captureScheduled = false
        captureHand = null
        zoomActive = false

        CameraPreviewCapture.captureFrame { jpeg ->
            finishedMessageTicks = FINISHED_MESSAGE_TICKS
            PacketDistributor.sendToServer(SendCameraPhotoPacket(jpeg))
        }
    }

    private fun finishVideoCapture() {
        if (!captureScheduled) return
        captureScheduled = false
        captureHand = null
        isVideoMode = false
        lastCaptureWasVideo = true  // add this
        zoomActive = false
        videoTicksElapsed = 0

        val frames = capturedVideoFrames.toList()
        capturedVideoFrames.clear()

        finishedMessageTicks = FINISHED_MESSAGE_TICKS
        PacketDistributor.sendToServer(SendCameraVideoPacket(frames))
    }
}
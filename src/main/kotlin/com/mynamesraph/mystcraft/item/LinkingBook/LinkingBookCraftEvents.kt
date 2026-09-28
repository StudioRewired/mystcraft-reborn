package com.mynamesraph.mystcraft.events

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.client.LinkingBookPreviewCapture
import com.mynamesraph.mystcraft.item.DescriptiveBookItem
import com.mynamesraph.mystcraft.item.LinkingBookItem
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.CraftingScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.ScreenEvent
import net.neoforged.neoforge.common.NeoForge

@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object LinkingBookCraftEvents {

    internal var captureScheduled = false
    private var pendingCaptureTicks = 0
    private var finishedMessageTicks = 0
    private const val CAPTURE_DELAY_TICKS = 60 // 3 seconds
    private const val FINISHED_MESSAGE_TICKS = 60 // 3 seconds

    private var observedScreen: Screen? = null
    private var booksAtOpen: List<ItemStack> = emptyList()

    @SubscribeEvent
    fun onScreenInit(event: ScreenEvent.Init.Post) {
        if (event.screen !is CraftingScreen && event.screen !is InventoryScreen) return
        // Resizing reinitializes the same screen; do not discard the original snapshot.
        if (observedScreen === event.screen) return
        observedScreen = event.screen
        booksAtOpen = Minecraft.getInstance().player?.let(::uncapturedBooks) ?: emptyList()
    }

    @SubscribeEvent
    fun onScreenClose(event: ScreenEvent.Closing) {
        if (event.screen !is CraftingScreen && event.screen !is InventoryScreen) return
        val wasObserved = observedScreen === event.screen
        val before = booksAtOpen.toMutableList()
        observedScreen = null
        booksAtOpen = emptyList()
        if (!wasObserved || captureScheduled) return

        val player = Minecraft.getInstance().player ?: return
        val hasNewBook = uncapturedBooks(player).any { stack ->
            val index = before.indexOfFirst { ItemStack.isSameItemSameComponents(it, stack) }
            if (index >= 0) {
                before.removeAt(index)
                false
            } else true
        }
        if (hasNewBook) {
            captureScheduled = true
            pendingCaptureTicks = CAPTURE_DELAY_TICKS
        }
    }

    private fun uncapturedBooks(player: Player): List<ItemStack> = buildList {
        fun include(stack: ItemStack) {
            if (!stack.isEmpty && stack.item is LinkingBookItem && stack.item !is DescriptiveBookItem
                && stack.has(MystcraftComponents.LOCATION) && !stack.has(MystcraftComponents.PREVIEW_IMAGE)) {
                // Normalize only the snapshot, not the real stack. Recoloring an old book,
                // even one without media, must not appear to be a newly crafted destination.
                val copy = stack.copyWithCount(1)
                copy.remove(DataComponents.DYED_COLOR)
                repeat(stack.count) { add(copy) }
            }
        }
        for (i in 0 until player.inventory.containerSize) include(player.inventory.getItem(i))
        include(player.containerMenu.carried)
        // Include inputs but never the untaken result slot. This also covers the 2x2 grid.
        for (slot in player.containerMenu.slots) {
            if (slot.container is CraftingContainer) include(slot.item)
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent.Post) {
        if (pendingCaptureTicks > 0) {
            pendingCaptureTicks--
            if (pendingCaptureTicks == 0) {
                startMultiFrameCapture(Minecraft.getInstance())
            }
        }
        if (finishedMessageTicks > 0) {
            finishedMessageTicks--
        }
    }

    @SubscribeEvent
    fun onRenderGui(event: RenderGuiEvent.Post) {
        val mc = Minecraft.getInstance()
        val graphics = event.guiGraphics
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight
        val font = mc.font

        // Show countdown during delay phase only
        if (pendingCaptureTicks > 0) {
            val message: Component = when {
                pendingCaptureTicks > 40 -> Component.literal("§e3...")
                pendingCaptureTicks > 20 -> Component.literal("§e2...")
                else                     -> Component.literal("§e1...")
            }
            val textW = font.width(message)
            graphics.drawString(font, message, (screenW - textW) / 2, screenH / 2 - 20, 0xFFFFFF, true)
        }

        // Show finished message after capture completes
        if (finishedMessageTicks > 0) {
            val message = Component.literal("§aBook recording finished!")
            val textW = font.width(message)
            graphics.drawString(font, message, (screenW - textW) / 2, screenH / 2 - 20, 0xFFFFFF, true)
        }
    }

    private fun startMultiFrameCapture(mc: Minecraft) {
        var ticks = 0
        val capturedFrames = mutableListOf<ByteArray>()

        val listener = object {
            @SubscribeEvent
            fun onTick(event: ClientTickEvent.Post) {
                ticks++

                if (ticks % PreviewImageComponent.TICKS_BETWEEN_FRAMES == 0) {
                    val frameIndex = ticks / PreviewImageComponent.TICKS_BETWEEN_FRAMES
                    if (frameIndex <= PreviewImageComponent.FRAME_COUNT) {
                        LinkingBookPreviewCapture.captureFrame { jpeg ->
                            capturedFrames.add(jpeg)

                            if (capturedFrames.size >= PreviewImageComponent.FRAME_COUNT) {
                                NeoForge.EVENT_BUS.unregister(this)
                                captureScheduled = false
                                finishedMessageTicks = FINISHED_MESSAGE_TICKS
                                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                    com.mynamesraph.mystcraft.data.networking.packet.SendPreviewImagePacket(
                                        capturedFrames.toList()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        NeoForge.EVENT_BUS.register(listener)
    }
}
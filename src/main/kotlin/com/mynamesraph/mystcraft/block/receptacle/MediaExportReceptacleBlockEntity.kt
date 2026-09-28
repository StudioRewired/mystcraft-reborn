package com.mynamesraph.mystcraft.block.receptacle

import com.mojang.logging.LogUtils
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import com.mynamesraph.mystcraft.container.SingleStackHandler
import com.mynamesraph.mystcraft.registry.MystcraftBlockEntities
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

class MediaExportReceptacleBlockEntity(
    pos: BlockPos,
    blockState: BlockState
) : BlockEntity(MystcraftBlockEntities.MEDIA_EXPORT_RECEPTACLE_BLOCK_ENTITY.get(), pos, blockState) {

    private val bookHandler = SingleStackHandler()

    val book: ItemStack get() = bookHandler.getItem()
    val hasBook: Boolean get() = !book.isEmpty

    private val exportExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MystcraftMediaExport").also { it.isDaemon = true }
    }

    fun insertBook(stack: ItemStack, player: Player) {
        bookHandler.setStackInSlot(0, stack)
        setChanged()
        triggerExport(player)
    }

    private fun triggerExport(player: Player) {
        val preview = book.components.get(MystcraftComponents.PREVIEW_IMAGE.get())
        if (preview == null) {
            // No media — eject immediately with no message
            ejectBook()
            return
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val exportDir = File("exports/mystcraft").also { it.mkdirs() }

        exportExecutor.submit {
            try {
                exportFrames(preview, exportDir, timestamp)

                // Hop back to server thread to eject and notify
                val level = level ?: return@submit
                (level as? net.minecraft.server.level.ServerLevel)?.server?.execute {
                    ejectBook()
                    if (player is ServerPlayer) {
                        player.displayClientMessage(
                            Component.literal("§aMedia exported to exports/mystcraft/"),
                            true
                        )
                    }
                }
            } catch (e: Exception) {
                LogUtils.getLogger().warn("Mystcraft: Failed to export media", e)
                val level = level ?: return@submit
                (level as? net.minecraft.server.level.ServerLevel)?.server?.execute {
                    ejectBook()
                }
            }
        }
    }

    private fun exportFrames(preview: PreviewImageComponent, dir: File, timestamp: String) {
        if (preview.frames.size == 1) {
            // Single image — write as plain jpeg
            val file = File(dir, "mystcraft_$timestamp.jpg")
            file.writeBytes(preview.frames[0])
            LogUtils.getLogger().info("Mystcraft: Exported image to ${file.absolutePath}")
        } else {
            // Video — write numbered frames
            preview.frames.forEachIndexed { index, frame ->
                val file = File(dir, "mystcraft_${timestamp}_frame_${"%03d".format(index + 1)}.jpg")
                file.writeBytes(frame)
            }
            LogUtils.getLogger().info(
                "Mystcraft: Exported ${preview.frames.size} frames to ${dir.absolutePath}"
            )
        }
    }

    private fun ejectBook() {
        val level = level ?: return
        val stack = bookHandler.extractItem(1, false)
        if (stack.isEmpty) return
        val entity = ItemEntity(
            level,
            blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5,
            stack
        )
        entity.setDefaultPickUpDelay()
        level.addFreshEntity(entity)
        setChanged()
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("Book", Tag.TAG_COMPOUND.toInt())) {
            bookHandler.setStackInSlot(
                0,
                ItemStack.parse(registries, tag.getCompound("Book")).orElse(ItemStack.EMPTY)
            )
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        if (hasBook) tag.put("Book", book.save(registries))
    }
}
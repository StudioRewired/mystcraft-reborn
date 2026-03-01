package com.mynamesraph.mystcraft.block.receptacle

import com.mynamesraph.mystcraft.component.DimensionIdentificatorComponent
import com.mynamesraph.mystcraft.container.SingleStackHandler
import com.mynamesraph.mystcraft.data.networking.packet.ConfirmAgeDeletePacket
import com.mynamesraph.mystcraft.item.DescriptiveBookItem
import com.mynamesraph.mystcraft.item.LinkingBookItem
import com.mynamesraph.mystcraft.item.PictureBookItem
import com.mynamesraph.mystcraft.registry.MystcraftBlockEntities
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.server.ServerLifecycleHooks



class BookShreddingReceptacleBlockEntity(
    pos: BlockPos,
    blockState: BlockState
) : BlockEntity(MystcraftBlockEntities.BOOK_SHREDDING_RECEPTACLE_BLOCK_ENTITY.get(), pos, blockState), Container {

    private val bookHandler = SingleStackHandler()

    val book: ItemStack get() = bookHandler.getItem()
    val hasBook: Boolean get() = !book.isEmpty

    override fun clearContent() {
        bookHandler.getItem().copyAndClear()
        setChanged()
    }

    fun clearContentAndAnnounce(player: ServerPlayer) {
        val stack = bookHandler.getItem()
        val preview = stack.components.get(MystcraftComponents.PREVIEW_IMAGE.get())

        if (preview != null) {
            val totalBytes = preview.frames.sumOf { it.size }
            val message = when {
                totalBytes >= 1024 * 1024 -> "Book shredded — ${"%.2f".format(totalBytes / (1024.0 * 1024.0))} MB freed."
                totalBytes >= 1024 -> "Book shredded — ${"%.2f".format(totalBytes / 1024.0)} KB freed."
                else -> "Book shredded — $totalBytes bytes freed."
            }
            player.displayClientMessage(
                Component.literal(message),
                true // action bar
            )
        } else {
            player.displayClientMessage(
                Component.literal("Book shredded."),
                true
            )
        }

        bookHandler.getItem().copyAndClear()
        setChanged()
    }

    override fun getContainerSize() = 1
    override fun isEmpty() = bookHandler.getItem().isEmpty
    override fun getItem(slot: Int) = book

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val stack = bookHandler.extractItem(slot, amount, false)
        if (!stack.isEmpty) setChanged()
        return stack
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        return bookHandler.extractItem(64, false)
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        bookHandler.setStackInSlot(slot, stack)
        setChanged()
        tryPromptDelete()
    }

    fun insertBook(stack: ItemStack): ItemStack {
        val leftover = bookHandler.insertItem(stack, false)
        setChanged()
        tryPromptDelete()
        return leftover
    }

    fun removeBook(): ItemStack {
        val removed = bookHandler.extractItem(1, false)
        setChanged()
        return removed
    }

    private fun tryPromptDelete() {
        val level = level ?: return
        if (level.isClientSide) return

        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        val nearestPlayer = server.playerList.players
            .filter { it.level() == level }
            .minByOrNull { it.distanceToSqr(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5) }
            ?: return

        when (book.item) {
            is DescriptiveBookItem -> {
                val idComponent = book.components.get(MystcraftComponents.DIMENSION_ID.get())
                if (idComponent !is DimensionIdentificatorComponent) return
                if (!idComponent.generated) return

                PacketDistributor.sendToPlayer(
                    nearestPlayer,
                    ConfirmAgeDeletePacket(idComponent.dimensionID, blockPos)
                )
                ejectCountdown = EJECT_DELAY_TICKS
            }

            is LinkingBookItem -> {
                clearContentAndAnnounce(nearestPlayer)
            }

            is PictureBookItem -> {
                clearContentAndAnnounce(nearestPlayer)
            }
        }
    }



    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("Book", Tag.TAG_COMPOUND.toInt())) {
            bookHandler.setStackInSlot(
                0,
                ItemStack.parse(registries, tag.getCompound("Book")).orElse(ItemStack.EMPTY)
            )
        } else {
            bookHandler.setStackInSlot(0, ItemStack.EMPTY)
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        if (hasBook) tag.put("Book", book.save(registries))
    }

    override fun stillValid(player: Player) = true

    private var ejectCountdown = -1
    private val EJECT_DELAY_TICKS = 60 // 3 seconds to respond before book is ejected

    fun serverTick() {
        if (ejectCountdown > 0) {
            ejectCountdown--
            if (ejectCountdown == 0) {
                ejectCountdown = -1
                if (hasBook && book.item is DescriptiveBookItem) {
                    ejectBook()
                }
            }
        }
    }

    private fun ejectBook() {
        val level = level ?: return
        val stack = removeBook()
        if (stack.isEmpty) return
        val itemEntity = ItemEntity(
            level,
            blockPos.x + 0.5,
            blockPos.y + 1.0,
            blockPos.z + 0.5,
            stack
        )
        itemEntity.setDefaultPickUpDelay()
        level.addFreshEntity(itemEntity)
    }
}
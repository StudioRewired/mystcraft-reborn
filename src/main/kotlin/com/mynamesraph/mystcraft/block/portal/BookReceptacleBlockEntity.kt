package com.mynamesraph.mystcraft.block.portal

import com.mynamesraph.mystcraft.container.SingleStackHandler
import com.mynamesraph.mystcraft.registry.MystcraftBlockEntities
import com.mynamesraph.mystcraft.registry.MystcraftBlocks
import com.mynamesraph.pastelpalettes.PastelDyeColor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Block.UPDATE_ALL
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.fml.ModList
import kotlin.random.Random

class BookReceptacleBlockEntity(
    pos: BlockPos,
    blockState: BlockState
) : BlockEntity(MystcraftBlockEntities.BOOK_RECEPTACLE_BLOCK_ENTITY.get(), pos, blockState),Container {

    private val bookHandler = SingleStackHandler()

    val validPortalBlocks: Set<Block> by lazy {
        setOf(
            MystcraftBlocks.BLUE_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.BLUE_BOOK_RECEPTACLE.get(),
            MystcraftBlocks.YELLOW_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.YELLOW_BOOK_RECEPTACLE.get(),
            MystcraftBlocks.GREEN_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.GREEN_BOOK_RECEPTACLE.get(),
            MystcraftBlocks.PINK_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.PINK_BOOK_RECEPTACLE.get(),
            MystcraftBlocks.RED_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.RED_BOOK_RECEPTACLE.get()
        )
    }

    val book:ItemStack
        get() {
            return bookHandler.getItem()
        }

    val hasBook: Boolean
        get() {
            return !book.isEmpty
        }

    override fun clearContent() {
        bookHandler.setStackInSlot(0, ItemStack.EMPTY)
        setChanged()
    }

    override fun getContainerSize(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return bookHandler.getItem().isEmpty
    }

    override fun getItem(slot: Int): ItemStack {
        return book
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val itemStack = bookHandler.extractItem(slot,amount,false)
        if (!itemStack.isEmpty) this.setChanged()
        breakPortal()
        return itemStack
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        return bookHandler.extractItem(64,false)
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        bookHandler.setStackInSlot(slot,stack)
        setChanged()
        tryGeneratePortal()
    }

    fun insertBook(stack: ItemStack):ItemStack {
        val leftover = bookHandler.insertItem(stack,false)
        setChanged()
        tryGeneratePortal()
        return leftover
    }

    fun removeBook():ItemStack {
        val book = bookHandler.extractItem(1,false)
        setChanged()
        breakPortal()
        return book
    }

    private fun tryGeneratePortal() {
        val level = level ?: return

        if (!level.isClientSide) {
            val cornersR = solveFrameCorners(blockPos, validPortalBlocks,level,128)

            if (cornersR.isSuccess) {
                val corners = cornersR.getOrThrow()

                val state = if (ModList.get().isLoaded("past_el_palettes")) {
                    val isPastel = Random.nextBoolean()
                    if (isPastel) {
                        MystcraftBlocks.LINK_PORTAL.get().defaultBlockState()
                            .setValue(LinkPortalBlock.PERSISTENT, false)
                            .setValue(LinkPortalBlock.IS_PASTEL_COLOR, true)
                            .setValue(LinkPortalBlock.PASTEL_COLOR!!,PastelDyeColor.entries.random())
                    }
                    else {
                        MystcraftBlocks.LINK_PORTAL.get().defaultBlockState()
                            .setValue(LinkPortalBlock.PERSISTENT, false)
                            .setValue(LinkPortalBlock.IS_PASTEL_COLOR, false)
                            .setValue(LinkPortalBlock.COLOR,DyeColor.entries.random())
                    }
                }
                else {
                    MystcraftBlocks.LINK_PORTAL.get().defaultBlockState()
                        .setValue(LinkPortalBlock.PERSISTENT, false)
                        .setValue(LinkPortalBlock.COLOR,DyeColor.entries.random())
                }

                val points = findAllPointsInsidePolygon(corners)

                for (point in points) {
                    if (level.getBlockState(point).isEmpty) {
                        level.setBlock(
                            point,
                            state,
                            UPDATE_ALL
                        )
                        val be = level.getBlockEntity(point)
                        if (be is LinkPortalBlockEntity) {
                            be.receptaclePosition = blockPos
                            be.setChanged()
                        }
                    }
                }
            }
        }
    }

    private fun breakPortal() {
        val level = level ?: return

        if (!level.isClientSide) {
            for (direction in Direction.entries) {
                val block = level.getBlockState(blockPos.relative(direction))
                if (block.`is`(MystcraftBlocks.LINK_PORTAL)) {
                    level.destroyBlock(blockPos.relative(direction),false)
                }
            }
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("Book", 10)) {
            bookHandler.setStackInSlot(
                0,
                ItemStack.parse(
                    registries,
                    tag.getCompound("Book")
                ).orElse(ItemStack.EMPTY)
            )
        } else {
            bookHandler.setStackInSlot(
                0,
                ItemStack.EMPTY
            )
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        if (this.hasBook) {
            tag.put("Book", this.book.save(registries))
        }
    }


    override fun stillValid(player: Player): Boolean {
        return true
    }


}
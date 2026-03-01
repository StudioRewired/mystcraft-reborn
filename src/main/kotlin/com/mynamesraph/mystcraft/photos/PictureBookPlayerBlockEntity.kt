package com.mynamesraph.mystcraft.block.mediaplayer

import com.mynamesraph.mystcraft.container.SingleStackHandler
import com.mynamesraph.mystcraft.registry.MystcraftBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LightBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import kotlin.math.roundToInt

class PictureBookPlayerBlockEntity(
    pos: BlockPos,
    blockState: BlockState
) : BlockEntity(MystcraftBlockEntities.PICTURE_BOOK_PLAYER_BLOCK_ENTITY.get(), pos, blockState) {

    private val bookHandler = SingleStackHandler()

    val book: ItemStack get() = bookHandler.getItem()
    val hasBook: Boolean get() = !book.isEmpty

    var displayWidth: Int = 1
        set(value) { field = value.coerceIn(1, 36); setChanged() }

    var displayHeight: Int = 1
        set(value) { field = value.coerceIn(1, 36); setChanged() }

    var backlightOn: Boolean = true
        set(value) { field = value; setChanged() }

    var verticalOffset: Float = 0f
        set(value) { field = value.coerceIn(-100f, 100f); setChanged() }

    var horizontalOffset: Float = 0f
        set(value) { field = value.coerceIn(-100f, 100f); setChanged() }

    var rotation: Float = 0f
        set(value) { field = value.coerceIn(0f, 350f); setChanged() }

    private val placedLightPositions = mutableSetOf<BlockPos>()

    // ── Light block management ────────────────────────────────────────────────

    fun clearLightBlocks(level: Level) {
        for (lightPos in placedLightPositions) {
            val existing = level.getBlockState(lightPos)
            if (existing.block is LightBlock) {
                level.setBlock(lightPos, Blocks.AIR.defaultBlockState(), 3)
            }
        }
        placedLightPositions.clear()
        setChanged()
    }

    fun placeLightBlocks(level: Level) {
        if (!backlightOn || !hasBook) return

        val facing = blockState.getValue(BlockStateProperties.FACING)

        // For each facing, define:
        //   rightX/rightZ — world-space axis for screen horizontal (matches renderer)
        //   upX/upY/upZ   — world-space axis for screen vertical
        //   fwdX/fwdY/fwdZ — world-space axis pointing away from the block face
        //
        // Horizontal and vertical offsets are contextual:
        //   Wall facings: horizontal = screen right, vertical = screen up (Y)
        //   UP:   horizontal = world East (+X), vertical = world South (+Z, away from player)
        //   DOWN: horizontal = world East (+X), vertical = world North (-Z, away from player)

        data class Axes(
            val rightX: Int, val rightY: Int, val rightZ: Int,
            val upX: Int, val upY: Int, val upZ: Int,
            val fwdX: Int, val fwdY: Int, val fwdZ: Int
        )

        val axes = when (facing) {
            Direction.NORTH -> Axes(1, 0, 0, 0, 1, 0, 0, 0, -1)
            Direction.SOUTH -> Axes(-1, 0, 0, 0, 1, 0, 0, 0, 1)
            Direction.EAST -> Axes(0, 0, 1, 0, 1, 0, 1, 0, 0)
            Direction.WEST -> Axes(0, 0, -1, 0, 1, 0, -1, 0, 0) // right was -1, flip to +1
            Direction.UP -> Axes(1, 0, 0, 0, 0, 1, 0, 1, 0)   // up Z was -1, flip to +1
            // DOWN: projection faces downward. Screen right = East, screen "up" = South (+Z).
            Direction.DOWN -> Axes(1, 0, 0, 0, 0, -1, 0, -1, 0)
            else -> Axes(1, 0, 0, 0, 1, 0, 0, 0, -1)
        }

        // World-space centre of the hologram
        val cx = blockPos.x + 0.5 + axes.fwdX * 0.51 + axes.rightX * horizontalOffset + axes.upX * verticalOffset
        val cy = blockPos.y + 0.5 + axes.fwdY * 0.51 + axes.rightY * horizontalOffset + axes.upY * verticalOffset
        val cz = blockPos.z + 0.5 + axes.fwdZ * 0.51 + axes.rightZ * horizontalOffset + axes.upZ * verticalOffset

        val halfW = displayWidth / 2f
        val halfH = displayHeight / 2f

        val lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 7)

        val angleRad = Math.toRadians(rotation.toDouble())
        val cosA = Math.cos(angleRad)
        val sinA = Math.sin(angleRad)

        for (wi in 0..displayWidth) {
            for (hi in 0..displayHeight) {
                val uFrac = wi.toFloat() / displayWidth
                val vFrac = hi.toFloat() / displayHeight

                val localRight = uFrac * displayWidth - halfW
                val localUp = vFrac * displayHeight - halfH

                // Rotate in screen plane
                val rotRight = localRight * cosA - localUp * sinA
                val rotUp = localRight * sinA + localUp * cosA

                val wx = (cx + axes.rightX * rotRight + axes.upX * rotUp).roundToInt()
                val wy = (cy + axes.rightY * rotRight + axes.upY * rotUp).roundToInt()
                val wz = (cz + axes.rightZ * rotRight + axes.upZ * rotUp).roundToInt()

                val lightPos = BlockPos(wx, wy, wz)
                if (level.getBlockState(lightPos).isAir) {
                    level.setBlock(lightPos, lightState, 3)
                    placedLightPositions.add(lightPos)
                }
            }
        }
    }

    // ── Book management ───────────────────────────────────────────────────────

    fun insertBook(stack: ItemStack): ItemStack {
        val leftover = bookHandler.insertItem(stack, false)
        setChanged()
        return leftover
    }

    fun removeBook(): ItemStack {
        val removed = bookHandler.extractItem(1, false)
        setChanged()
        return removed
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

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
        displayWidth     = tag.getInt("DisplayWidth").takeIf { it in 1..36 } ?: 1
        displayHeight    = tag.getInt("DisplayHeight").takeIf { it in 1..36 } ?: 1
        backlightOn      = if (tag.contains("Backlight")) tag.getBoolean("Backlight") else true
        verticalOffset   = if (tag.contains("VerticalOffset"))   tag.getFloat("VerticalOffset")   else 0f
        horizontalOffset = if (tag.contains("HorizontalOffset")) tag.getFloat("HorizontalOffset") else 0f
        rotation = if (tag.contains("Rotation")) tag.getFloat("Rotation") else 0f

        placedLightPositions.clear()
        if (tag.contains("LightPositions", Tag.TAG_LIST.toInt())) {
            val list = tag.getList("LightPositions", Tag.TAG_LONG.toInt())
            for (i in 0 until list.size) {
                placedLightPositions.add(BlockPos.of((list[i] as LongTag).asLong))
            }
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        if (hasBook) tag.put("Book", book.save(registries))
        tag.putInt("DisplayWidth",       displayWidth)
        tag.putInt("DisplayHeight",      displayHeight)
        tag.putBoolean("Backlight",      backlightOn)
        tag.putFloat("VerticalOffset",   verticalOffset)
        tag.putFloat("HorizontalOffset", horizontalOffset)
        tag.putFloat("Rotation", rotation)

        val list = ListTag()
        for (lp in placedLightPositions) list.add(LongTag.valueOf(lp.asLong()))
        tag.put("LightPositions", list)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registries)

    override fun getUpdatePacket(): net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket =
        net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this)

    override fun onDataPacket(
        net: net.minecraft.network.Connection,
        pkt: net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket,
        registries: HolderLookup.Provider
    ) {
        pkt.tag?.let { loadAdditional(it, registries) }
    }
}
package com.mynamesraph.mystcraft.block.writing

import com.mojang.serialization.MapCodec
import com.mynamesraph.mystcraft.registry.MystcraftBlocks
import com.mynamesraph.mystcraft.ui.menu.WritingDeskMenu
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuConstructor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.neoforge.items.IItemHandler

class WritingDeskBlock(properties: Properties) : HorizontalDirectionalBlock(properties),EntityBlock {

    companion object {
        val FACING = BlockStateProperties.HORIZONTAL_FACING
        val SIDE = EnumProperty.create("side",WritingDeskSide::class.java)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, SIDE)
    }

    override fun codec(): MapCodec<out HorizontalDirectionalBlock> {
        TODO("Not yet implemented")
    }

    fun menuProvider(pos: BlockPos,itemHandler: IItemHandler): MenuProvider {
        return SimpleMenuProvider(
            (MenuConstructor { containerId: Int, playerInventory: Inventory, _: Player? ->
                WritingDeskMenu(
                    containerId,
                    playerInventory,
                    pos,
                    itemHandler
                )
            }),
            Component.literal("writing_desk")
        )
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val be: BlockEntity? = if (state.getValue(SIDE) == WritingDeskSide.LEFT) {
                level.getBlockEntity(pos.relative(state.getValue(FACING)))
            } else {
                level.getBlockEntity(pos)
            }

            if (be is WritingDeskBlockEntity) {
                player.openMenu(menuProvider(pos,be.container),pos)
                be.setChanged()
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val direction = context.horizontalDirection.counterClockWise
        val pos = context.clickedPos
        val posOther = pos.relative(direction)
        val level = context.level
        return if (level.getBlockState(posOther).canBeReplaced(context) && level.worldBorder.isWithinBounds(posOther))
            defaultBlockState().setValue(FACING, direction).setValue(SIDE,WritingDeskSide.RIGHT)
        else null
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)
        if (!level.isClientSide) {
            val blockPos = pos.relative(state.getValue(FACING))
            level.setBlock(blockPos, state.setValue(FACING,state.getValue(FACING).opposite).setValue(SIDE,WritingDeskSide.LEFT), 3)
            level.blockUpdated(pos, Blocks.AIR)
            state.updateNeighbourShapes(level, pos, 3)
        }
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val side = state.getValue(FACING)

        if (state.getValue(SIDE) == WritingDeskSide.RIGHT) {
            popItems(state,level,pos)
        }

        if (level.getBlockState(pos.relative(side)).`is`(MystcraftBlocks.WRITING_DESK)) {
            level.destroyBlock(pos.relative(side),false)
        }

        return super.playerWillDestroy(level, pos, state, player)
    }

    private fun popItems(state:BlockState,level: Level,pos: BlockPos) {
        val be = level.getBlockEntity(pos)
        if (be is WritingDeskBlockEntity) {
            val direction = state.getValue(FACING)
            val itemStack1 = be.container.getStackInSlot(0)
            val itemStack2 = be.container.getStackInSlot(1)
            val f = 0.25f * direction.stepX.toFloat()
            val f1 = 0.25f * direction.stepZ.toFloat()
            val itemEntity1 = ItemEntity(
                level,
                pos.x.toDouble() + 0.5 + f.toDouble(),
                (pos.y + 1).toDouble(),
                pos.z.toDouble() + 0.5 + f1.toDouble(),
                itemStack1
            )
            val itemEntity2 = ItemEntity(
                level,
                pos.x.toDouble() + 0.5 + f.toDouble(),
                (pos.y + 1).toDouble(),
                pos.z.toDouble() + 0.5 + f1.toDouble(),
                itemStack2
            )
            itemEntity1.setDefaultPickUpDelay()
            itemEntity2.setDefaultPickUpDelay()

            level.addFreshEntity(itemEntity1)
            level.addFreshEntity(itemEntity2)
            be.container.setStackInSlot(0,ItemStack.EMPTY)
            be.container.setStackInSlot(1,ItemStack.EMPTY)
            be.setChanged()
        }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return WritingDeskBlockEntity(pos,state)
    }

}
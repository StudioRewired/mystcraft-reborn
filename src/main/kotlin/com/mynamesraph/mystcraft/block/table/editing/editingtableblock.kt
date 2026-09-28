package com.mynamesraph.mystcraft.block.editing

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuConstructor
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.neoforge.items.IItemHandler

class EditingTableBlock(properties: Properties) : HorizontalDirectionalBlock(properties), EntityBlock {

    companion object {
        val FACING = BlockStateProperties.HORIZONTAL_FACING
        val CODEC: MapCodec<EditingTableBlock> = simpleCodec(::EditingTableBlock)
    }

    override fun codec(): MapCodec<out HorizontalDirectionalBlock> = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    private fun menuProvider(pos: BlockPos, itemHandler: IItemHandler): MenuProvider {
        return SimpleMenuProvider(
            MenuConstructor { containerId, playerInventory, _ ->
                com.mynamesraph.mystcraft.ui.menu.EditingTableMenu(
                    containerId, playerInventory, pos, itemHandler
                )
            },
            Component.translatable("container.mystcraft_reborn.editing_table")
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
            val be = level.getBlockEntity(pos)
            if (be is EditingTableBlockEntity) {
                player.openMenu(menuProvider(pos, be.container), pos)
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        movedByPiston: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            popContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    private fun popContents(level: Level, pos: BlockPos) {
        val be = level.getBlockEntity(pos) as? EditingTableBlockEntity ?: return
        for (i in 0 until be.container.slots) {
            val stack = be.container.getStackInSlot(i)
            if (!stack.isEmpty) {
                val entity = ItemEntity(
                    level,
                    pos.x + 0.5, pos.y + 1.0, pos.z + 0.5,
                    stack.copy()
                )
                entity.setDefaultPickUpDelay()
                level.addFreshEntity(entity)
            }
        }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        EditingTableBlockEntity(pos, state)
}
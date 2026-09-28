package com.mynamesraph.mystcraft.block.receptacle

import com.mojang.serialization.MapCodec
import com.mynamesraph.mystcraft.registry.MystcraftTags
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
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

class MediaExportReceptacleBlock(properties: Properties) : HorizontalDirectionalBlock(properties), EntityBlock {

    companion object {
        val CODEC: MapCodec<MediaExportReceptacleBlock> = simpleCodec(::MediaExportReceptacleBlock)
    }

    override fun codec() = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.horizontalDirection.opposite)

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        val be = level.getBlockEntity(pos) as? MediaExportReceptacleBlockEntity
            ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

        if (be.hasBook) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

        if (stack.`is`(MystcraftTags.LINKING_BOOK_TAG)) {
            if (!level.isClientSide) {
                be.insertBook(stack.copy(), player)
                stack.shrink(1)
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun onRemove(
        state: BlockState, level: Level, pos: BlockPos,
        newState: BlockState, movedByPiston: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val be = level.getBlockEntity(pos) as? MediaExportReceptacleBlockEntity
            if (be != null && be.hasBook) {
                val entity = ItemEntity(level, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, be.book.copy())
                entity.setDefaultPickUpDelay()
                level.addFreshEntity(entity)
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        MediaExportReceptacleBlockEntity(pos, state)
}
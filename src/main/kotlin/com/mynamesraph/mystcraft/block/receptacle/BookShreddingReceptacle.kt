package com.mynamesraph.mystcraft.block.receptacle

import com.mynamesraph.mystcraft.registry.MystcraftTags
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType

class BookShreddingReceptacleBlock(properties: Properties) : Block(properties), EntityBlock {

    companion object {
        val FACING: DirectionProperty = DirectionalBlock.FACING
        val LOCKED = BlockStateProperties.LOCKED
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState()
            .setValue(FACING, context.nearestLookingDirection.opposite)
            .setValue(LOCKED, false)

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING).add(LOCKED)
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos,
        player: Player, hitResult: BlockHitResult
    ): InteractionResult {
        if (!state.getValue(LOCKED)) {
            if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty) {
                val be = level.getBlockEntity(pos)
                if (be is BookShreddingReceptacleBlockEntity && be.hasBook) {
                    val stack = be.removeBook()
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack)
                }
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult)
    }

    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hitResult: BlockHitResult
    ): ItemInteractionResult {
        if (state.getValue(LOCKED)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION

        val be = level.getBlockEntity(pos)
        if (be is BookShreddingReceptacleBlockEntity && be.hasBook)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

        if (be is BookShreddingReceptacleBlockEntity && !be.hasBook) {
            // Accept any linking book tag item — descriptive books AND linking books
            if (stack.`is`(MystcraftTags.LINKING_BOOK_TAG)) {
                if (!level.isClientSide) {
                    val leftover = be.insertBook(stack)
                    if (leftover != stack) {
                        player.setItemInHand(hand, leftover)
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide)
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        BookShreddingReceptacleBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (!level.isClientSide) {
            BlockEntityTicker<T> { _, _, _, be ->
                if (be is BookShreddingReceptacleBlockEntity) be.serverTick()
            }
        } else null
    }
}
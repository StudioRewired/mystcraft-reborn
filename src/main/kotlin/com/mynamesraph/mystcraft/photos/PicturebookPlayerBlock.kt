package com.mynamesraph.mystcraft.block.mediaplayer

import com.mojang.serialization.MapCodec
import com.mynamesraph.mystcraft.registry.MystcraftTags
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import com.mynamesraph.mystcraft.events.PictureBookPlayerEvents

class PictureBookPlayerBlock(properties: Properties) : DirectionalBlock(properties), EntityBlock {

    companion object {
        val FACING = BlockStateProperties.FACING
        val CODEC: MapCodec<PictureBookPlayerBlock> = simpleCodec(::PictureBookPlayerBlock)
    }

    override fun codec(): MapCodec<out DirectionalBlock> = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val clickedFace = context.clickedFace
        return if (clickedFace == Direction.UP || clickedFace == Direction.DOWN) {
            defaultBlockState().setValue(FACING, clickedFace)
        } else {
            defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getLightEmission(state: BlockState, level: BlockGetter, pos: BlockPos): Int {
        val be = level.getBlockEntity(pos) as? PictureBookPlayerBlockEntity ?: return 0
        return if (be.backlightOn && be.hasBook) 7 else 0
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        val be = level.getBlockEntity(pos) as? PictureBookPlayerBlockEntity
            ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

        if (be.hasBook) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

        if (stack.`is`(MystcraftTags.LINKING_BOOK_TAG)) {
            if (!level.isClientSide) {
                val leftover = be.insertBook(stack)
                if (leftover != stack) {
                    player.setItemInHand(hand, leftover)
                    level.sendBlockUpdated(pos, state, state, 3)
                    be.placeLightBlocks(level)
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        val be = level.getBlockEntity(pos) as? PictureBookPlayerBlockEntity
            ?: return InteractionResult.PASS

        if (!be.hasBook) return InteractionResult.PASS

        if (level.isClientSide) {
            PictureBookPlayerEvents.openOverlay(pos, be)
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
            val be = level.getBlockEntity(pos) as? PictureBookPlayerBlockEntity
            if (be != null) {
                be.clearLightBlocks(level)
                if (be.hasBook) {
                    val entity = ItemEntity(
                        level,
                        pos.x + 0.5, pos.y + 1.0, pos.z + 0.5,
                        be.book.copy()
                    )
                    entity.setDefaultPickUpDelay()
                    level.addFreshEntity(entity)
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PictureBookPlayerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = null
}
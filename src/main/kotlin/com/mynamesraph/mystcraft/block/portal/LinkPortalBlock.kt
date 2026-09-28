package com.mynamesraph.mystcraft.block.portal

import com.mynamesraph.mystcraft.component.LocationComponent
import com.mynamesraph.mystcraft.component.RotationComponent
import com.mynamesraph.mystcraft.item.LinkingBookItem
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.registry.MystcraftTags
import com.mynamesraph.pastelpalettes.PastelDyeColor
import com.mynamesraph.pastelpalettes.item.PastelDyeItem
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.portal.DimensionTransition
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.fml.ModList
import com.mynamesraph.mystcraft.RewindTool.RewindEvents
import com.mynamesraph.mystcraft.block.receptacle.BookReceptacleBlock
import com.mynamesraph.mystcraft.block.receptacle.BookReceptacleBlockEntity


class LinkPortalBlock(properties: Properties) : HalfTransparentBlock(properties), EntityBlock, Portal {

    companion object {
        val PERSISTENT: BooleanProperty = BlockStateProperties.PERSISTENT
        val COLOR = EnumProperty.create("portal_color",DyeColor::class.java)
        val IS_PASTEL_COLOR = BooleanProperty.create("is_pastel_color")
        val PASTEL_COLOR: EnumProperty<PastelDyeColor>? by lazy {
            if (ModList.get().isLoaded("past_el_palettes")) {
                EnumProperty.create("pastel_color", PastelDyeColor::class.java)
            }
            else {
                null
            }
        }
    }



    init {
        this.registerDefaultState(
            this.defaultBlockState().setValue(PERSISTENT,true)
        )
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
        val item = stack.item
        if (item is DyeItem) {
            if (ModList.get().isLoaded("past_el_palettes")) {
                level.setBlock(pos,state.setValue(COLOR,item.dyeColor).setValue(IS_PASTEL_COLOR,false),Block.UPDATE_ALL)
                stack.consume(1,player)
                return ItemInteractionResult.SUCCESS

            }
            else {
                level.setBlock(pos,state.setValue(COLOR,item.dyeColor),Block.UPDATE_ALL)
                stack.consume(1,player)
                return ItemInteractionResult.SUCCESS
            }

        }
        else if (ModList.get().isLoaded("past_el_palettes")) {
            if (item is PastelDyeItem) {
                level.setBlock(pos,state.setValue(PASTEL_COLOR!!,item.dyeColor).setValue(IS_PASTEL_COLOR,true),Block.UPDATE_ALL)
                stack.consume(1,player)
                return ItemInteractionResult.SUCCESS
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(PERSISTENT).add(COLOR)

        if (ModList.get().isLoaded("past_el_palettes")) {
            builder.add(IS_PASTEL_COLOR).add(PASTEL_COLOR!!)
        }
    }

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (state.getValue(PERSISTENT)) {
            return
        }
        val be = level.getBlockEntity(pos)

        if (be is LinkPortalBlockEntity) {
            if (level.hasChunkAt(be.receptaclePosition)) {
                val receptacle = level.getBlockState(be.receptaclePosition).block

                if (receptacle !is BookReceptacleBlock) {
                    level.setBlock(pos,Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
                }
            }
        }
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (!level.isClientSide) {
            if (neighborState.block is LinkPortalBlock) {
                level.setBlock(pos,neighborState, UPDATE_ALL_IMMEDIATE)
            }

            if (!state.getValue(PERSISTENT)) {
                val be = level.getBlockEntity(pos)

                if (be is LinkPortalBlockEntity) {
                    if (level.hasChunkAt(be.receptaclePosition)) {
                        val receptacle = level.getBlockEntity(be.receptaclePosition)

                        if (receptacle is BookReceptacleBlockEntity) {
                            if (!receptacle.hasBook) {
                                level.setBlock(pos,Blocks.AIR.defaultBlockState(), UPDATE_ALL)
                            }
                        }
                        else {
                            level.setBlock(pos,Blocks.AIR.defaultBlockState(), UPDATE_ALL)
                        }
                    }
                }
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos)
    }

    override fun entityInside(state: BlockState, level: Level, pos: BlockPos, entity: Entity) {
        if (entity.canUsePortal(true) && !level.isClientSide)
        {
            entity.setAsInsidePortal(this,pos)
        }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return LinkPortalBlockEntity(pos,state)
    }

    override fun getPortalDestination(level: ServerLevel, entity: Entity, pos: BlockPos): DimensionTransition? {

        // SAVE PREVIOUS LOCATION TO REWINDING BOOK
        if (entity is Player) {
            RewindEvents.saveRewindPosition(entity)
        }

        if (!level.isClientSide) {
            val portalBE = level.getBlockEntity(pos)

            if (portalBE is LinkPortalBlockEntity) {
                val receptacleBE = level.getBlockEntity(portalBE.receptaclePosition)
                if (receptacleBE is BookReceptacleBlockEntity && receptacleBE.hasBook) {
                    val book = receptacleBE.book

                    if (book.`is`(MystcraftTags.LINKING_BOOK_TAG)) {
                        val bookItem = book.item
                        if (bookItem is LinkingBookItem) {
                            val location = book.components.get(MystcraftComponents.LOCATION.get())
                            val rotation = book.components.get(MystcraftComponents.ROTATION.get())

                            if (location is LocationComponent && rotation is RotationComponent) {
                                return bookItem.getDestination(level,entity,location,rotation)
                            }
                        }
                    }
                }
            }
        }
        return null
    }


}
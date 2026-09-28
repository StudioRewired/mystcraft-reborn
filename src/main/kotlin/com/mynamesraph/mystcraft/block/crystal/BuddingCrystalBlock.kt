package com.mynamesraph.mystcraft.block.crystal

import com.mynamesraph.mystcraft.registry.MystcraftBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.AmethystClusterBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.BuddingAmethystBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluids

class BuddingCrystalBlock(properties: Properties, private val crystalColor: CrystalColor) : BuddingAmethystBlock(properties) {

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (random.nextInt(5) == 0) {
            val direction = DIRECTIONS[random.nextInt(DIRECTIONS.size)]
            val blockPos = pos.relative(direction)
            val blockState = level.getBlockState(blockPos)
            var block: Block? = null
            if (canClusterGrowAtState(blockState)) {
                block = when (crystalColor) {
                    CrystalColor.YELLOW -> MystcraftBlocks.SMALL_YELLOW_CRYSTAL_BUD.get()
                    CrystalColor.GREEN -> MystcraftBlocks.SMALL_GREEN_CRYSTAL_BUD.get()
                    CrystalColor.BLUE -> MystcraftBlocks.SMALL_BLUE_CRYSTAL_BUD.get()
                    CrystalColor.RED -> MystcraftBlocks.SMALL_RED_CRYSTAL_BUD.get()
                    CrystalColor.PINK -> MystcraftBlocks.SMALL_PINK_CRYSTAL_BUD.get()
                }
            }
            else if (blockState.block is CrystalClusterBlock && blockState.getValue(CrystalClusterBlock.FACING) == direction) {
                when (crystalColor) {
                    CrystalColor.YELLOW ->
                        if (blockState.`is`(MystcraftBlocks.SMALL_YELLOW_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.MEDIUM_YELLOW_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.MEDIUM_YELLOW_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.LARGE_YELLOW_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.LARGE_YELLOW_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.YELLOW_CRYSTAL_CLUSTER.get()
                        }
                    CrystalColor.GREEN ->
                        if (blockState.`is`(MystcraftBlocks.SMALL_GREEN_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.MEDIUM_GREEN_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.MEDIUM_GREEN_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.LARGE_GREEN_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.LARGE_GREEN_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.GREEN_CRYSTAL_CLUSTER.get()
                        }
                    CrystalColor.BLUE ->
                        if (blockState.`is`(MystcraftBlocks.SMALL_BLUE_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.MEDIUM_BLUE_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.MEDIUM_BLUE_CRYSTAL_BUD)) {
                           block = MystcraftBlocks.LARGE_BLUE_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.LARGE_BLUE_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.BLUE_CRYSTAL_CLUSTER.get()
                        }
                    CrystalColor.RED ->
                        if (blockState.`is`(MystcraftBlocks.SMALL_RED_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.MEDIUM_RED_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.MEDIUM_RED_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.LARGE_RED_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.LARGE_RED_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.RED_CRYSTAL_CLUSTER.get()
                        }
                    CrystalColor.PINK ->
                        if (blockState.`is`(MystcraftBlocks.SMALL_PINK_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.MEDIUM_PINK_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.MEDIUM_PINK_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.LARGE_PINK_CRYSTAL_BUD.get()
                        }
                        else if (blockState.`is`(MystcraftBlocks.LARGE_PINK_CRYSTAL_BUD)) {
                            block = MystcraftBlocks.PINK_CRYSTAL_CLUSTER.get()
                        }
                }
            }

            if (block != null) {
                val blockState1 = block.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, direction)
                    .setValue(AmethystClusterBlock.WATERLOGGED, blockState.fluidState.type === Fluids.WATER)
                level.setBlockAndUpdate(blockPos, blockState1)
            }
        }
    }

    companion object {
        private val DIRECTIONS: Array<Direction> = Direction.entries.toTypedArray()
    }
}
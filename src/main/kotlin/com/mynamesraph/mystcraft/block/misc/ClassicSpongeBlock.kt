package com.mynamesraph.mystcraft.block.sponge

import com.mynamesraph.mystcraft.data.saved.SpongeRadiusData
import com.mynamesraph.mystcraft.data.saved.SpongeRegistry
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluids

class ClassicSpongeBlock(properties: Properties) : Block(properties) {

    override fun onPlace(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        oldState: BlockState,
        movedByPiston: Boolean
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        if (level !is ServerLevel) return
        if (oldState.block === this) return

        val radius = level.server.overworld().dataStorage
            .computeIfAbsent(SpongeRadiusData.FACTORY, SpongeRadiusData.FILE_NAME)
            .radius

        // Register BEFORE draining. The drain triggers neighbor updates which
        // call canSpreadTo on adjacent fluids — those calls must already see
        // the protection in place to prevent water immediately flowing back.
        level.server.overworld().dataStorage
            .computeIfAbsent(SpongeRegistry.FACTORY, SpongeRegistry.FILE_NAME)
            .register(pos, radius)

        drainWater(level, pos, radius)
    }

    /**
     * Belt-and-braces wake-up: if water somehow appears adjacent to the sponge
     * despite the mixin guard, re-drain. The mixin should make this redundant,
     * but the cost of an early-exit on dry neighbors is near-zero.
     */
    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        sourceBlock: Block,
        sourcePos: BlockPos,
        movedByPiston: Boolean
    ) {
        super.neighborChanged(state, level, pos, sourceBlock, sourcePos, movedByPiston)
        if (level !is ServerLevel) return

        val sourceState = level.getBlockState(sourcePos)
        if (sourceState.fluidState.isEmpty) return
        if (sourceState.fluidState.type != Fluids.WATER &&
            sourceState.fluidState.type != Fluids.FLOWING_WATER
        ) return

        val radius = level.server.overworld().dataStorage
            .computeIfAbsent(SpongeRadiusData.FACTORY, SpongeRadiusData.FILE_NAME)
            .radius
        drainWater(level, pos, radius)
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        movedByPiston: Boolean
    ) {
        if (newState.block !== this && level is ServerLevel) {
            level.server.overworld().dataStorage
                .computeIfAbsent(SpongeRegistry.FACTORY, SpongeRegistry.FILE_NAME)
                .unregister(pos)

            scheduleBoundaryRefill(level, pos)
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    /**
     * Drains every water-containing cell in the cube. Handles three cases:
     *  - Standalone water blocks (Blocks.WATER) → replaced with air
     *  - Waterlogged blocks (WATERLOGGED property = true) → set to false
     *  - Pure flowing-water cells (no LiquidBlock, just FluidState) → air
     *
     * Order matters: check the WATERLOGGED property FIRST. A waterlogged stairs
     * block reports fluidState.type == WATER but its block is STAIRS, not WATER.
     * The previous bug was caused by an ordering issue that let waterlogged
     * blocks fall through to a case that didn't apply to them.
     */
    private fun drainWater(level: ServerLevel, center: BlockPos, radius: Int) {
        BlockPos.betweenClosed(
            center.offset(-radius, -radius, -radius),
            center.offset(radius, radius, radius)
        ).forEach { pos ->
            // betweenClosed reuses a mutable BlockPos — immutable copy needed
            // for setBlock to avoid aliasing bugs on subsequent iterations.
            val immutable = pos.immutable()
            val target = level.getBlockState(immutable)

            when {
                // De-waterlog FIRST — this is the case for stairs, fences,
                // slabs, signs, chests etc. that hold water as a property.
                target.hasProperty(BlockStateProperties.WATERLOGGED) &&
                        target.getValue(BlockStateProperties.WATERLOGGED) -> {
                    level.setBlock(
                        immutable,
                        target.setValue(BlockStateProperties.WATERLOGGED, false),
                        UPDATE_ALL
                    )
                }
                // Inherently-wet blocks: seagrass, kelp, bubble columns, etc.
                // These report a water fluidState but aren't LiquidBlocks and
                // don't have a WATERLOGGED property to toggle — the water is
                // baked into the block's identity. Break them so they drop
                // normally, then overwrite with air to remove the residual
                // water source that vanilla leaves behind from the block's
                // fluidState.
                target.fluidState.type == Fluids.WATER &&
                        target.block !is LiquidBlock &&
                        !target.hasProperty(BlockStateProperties.WATERLOGGED) -> {
                    level.destroyBlock(immutable, true)
                    level.setBlock(immutable, Blocks.AIR.defaultBlockState(), UPDATE_ALL)
                }
                // Standalone water source or flowing-water block. LiquidBlock
                // is the common supertype for both Blocks.WATER and any modded
                // water-equivalent fluid blocks.
                target.block is LiquidBlock &&
                        (target.fluidState.type == Fluids.WATER ||
                                target.fluidState.type == Fluids.FLOWING_WATER) -> {
                    level.setBlock(immutable, Blocks.AIR.defaultBlockState(), UPDATE_ALL)
                }
            }
        }
    }

    private fun scheduleBoundaryRefill(level: ServerLevel, center: BlockPos) {
        val radius = level.server.overworld().dataStorage
            .computeIfAbsent(SpongeRadiusData.FACTORY, SpongeRadiusData.FILE_NAME)
            .radius
        val outer = radius + 1

        BlockPos.betweenClosed(
            center.offset(-outer, -outer, -outer),
            center.offset(outer, outer, outer)
        ).forEach { p ->
            val fluid = level.getFluidState(p)
            if (fluid.type == Fluids.WATER || fluid.type == Fluids.FLOWING_WATER) {
                level.scheduleTick(p.immutable(), fluid.type, fluid.type.getTickDelay(level))
            }
        }
    }
}
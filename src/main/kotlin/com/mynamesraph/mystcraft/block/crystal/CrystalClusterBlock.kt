package com.mynamesraph.mystcraft.block.crystal

import net.minecraft.world.level.block.AmethystClusterBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty

class CrystalClusterBlock(
    height: Float,
    aabbOffset: Float,
    properties: Properties,
    val color: CrystalColor
) : AmethystClusterBlock(height, aabbOffset, properties) {

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.FACING
    }
}
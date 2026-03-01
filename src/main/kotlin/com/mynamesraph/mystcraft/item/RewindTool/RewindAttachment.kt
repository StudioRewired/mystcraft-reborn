package com.mynamesraph.mystcraft.RewindTool

import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

data class RewindAttachment(
    val x: Double,
    val y: Double,
    val z: Double,
    val yRot: Float,
    val xRot: Float,
    val dimension: ResourceKey<Level>
)
package com.mynamesraph.mystcraft.block.portal

import com.mynamesraph.mystcraft.registry.MystcraftBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class LinkPortalBlockEntity(
    pos: BlockPos,
    blockState: BlockState,
    var receptaclePosition: BlockPos = BlockPos(0,0,0)
) : BlockEntity(MystcraftBlockEntities.LINK_PORTAL_BLOCK_ENTITY.get(), pos, blockState) {


    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        val arr = tag.getIntArray("receptacle_position")
        this.receptaclePosition = BlockPos(arr[0],arr[1],arr[2])
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putIntArray("receptacle_position", listOf(receptaclePosition.x,receptaclePosition.y,receptaclePosition.z))
    }
}
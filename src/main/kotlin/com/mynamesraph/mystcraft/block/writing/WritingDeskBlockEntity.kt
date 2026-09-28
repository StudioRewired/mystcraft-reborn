package com.mynamesraph.mystcraft.block.writing

import com.mynamesraph.mystcraft.registry.MystcraftBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

class WritingDeskBlockEntity(
    pos: BlockPos,
    blockState: BlockState
) : BlockEntity(MystcraftBlockEntities.WRITING_DESK_BLOCK_ENTITY.get(), pos, blockState) {

    val container = object : ItemStackHandler(2) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        container.deserializeNBT(registries,tag)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.merge(container.serializeNBT(registries))
    }




}
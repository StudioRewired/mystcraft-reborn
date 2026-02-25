package com.mynamesraph.mystcraft.data.saved

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.saveddata.SavedData

class DimensionIdentificatorCounter: SavedData() {

    var id = 1

    companion object {
        val FILE_NAME = "mystcraft_reborn_id_counter"
    }



    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        tag.putInt("count", id)

        return tag
    }

}
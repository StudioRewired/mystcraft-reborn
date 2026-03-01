package com.mynamesraph.mystcraft.data.saved

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.saveddata.SavedData

class IsolateWorldgenData(var isolate: Boolean = true) : SavedData() {

    companion object {
        const val FILE_NAME = "mystcraft_reborn_isolate_worldgen"

        val FACTORY = Factory(
            { IsolateWorldgenData() },
            { tag, _ -> IsolateWorldgenData(tag.getBoolean("isolate")) }
        )
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        tag.putBoolean("isolate", isolate)
        return tag
    }
}
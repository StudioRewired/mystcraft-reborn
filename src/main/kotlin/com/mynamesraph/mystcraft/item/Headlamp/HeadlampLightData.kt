package com.mynamesraph.mystcraft.data.saved

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.saveddata.SavedData

class HeadlampLightData(var lightLevel: Int = DEFAULT_LIGHT_LEVEL) : SavedData() {

    companion object {
        const val FILE_NAME = "mystcraft_reborn_headlamp_light"
        const val DEFAULT_LIGHT_LEVEL = 10
        const val MIN_LIGHT_LEVEL = 1
        const val MAX_LIGHT_LEVEL = 15

        val FACTORY = Factory(
            { HeadlampLightData() },
            { tag, _ -> HeadlampLightData(tag.getInt("lightLevel").coerceIn(MIN_LIGHT_LEVEL, MAX_LIGHT_LEVEL)) }
        )
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        tag.putInt("lightLevel", lightLevel)
        return tag
    }
}
package com.mynamesraph.mystcraft.data.saved

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.saveddata.SavedData

class SpongeRadiusData : SavedData() {

    var radius: Int = DEFAULT_RADIUS
        set(value) {
            field = value.coerceIn(MIN_RADIUS, MAX_RADIUS)
            setDirty()
        }

    companion object {
        const val FILE_NAME = "mystcraft_reborn_sponge_radius"
        const val DEFAULT_RADIUS = 4
        const val MIN_RADIUS = 1
        const val MAX_RADIUS = 32

        val FACTORY = Factory(
            ::SpongeRadiusData,
            { tag, _ -> SpongeRadiusData().also { it.radius = tag.getInt("radius") } },
            null
        )
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        tag.putInt("radius", radius)
        return tag
    }
}
package com.mynamesraph.mystcraft.data.saved

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.saveddata.SavedData

class NoMobDimensions : SavedData() {

    val dimensions = mutableSetOf<ResourceLocation>()

    companion object {
        const val FILE_NAME = "mystcraft_reborn_no_mob_dimensions"
    }

    fun add(location: ResourceLocation) {
        dimensions.add(location)
        setDirty()
    }

    fun contains(location: ResourceLocation): Boolean = dimensions.contains(location)

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val list = ListTag()
        for (dim in dimensions) {
            list.add(StringTag.valueOf(dim.toString()))
        }
        tag.put("dimensions", list)
        return tag
    }
}
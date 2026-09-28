package com.mynamesraph.mystcraft.data.saved

import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path

class DimensionIdentificatorCounter : SavedData() {
    var id = 1

    companion object {
        const val FILE_NAME = "mystcraft_reborn_id_counter"
        val FACTORY = Factory(
            { DimensionIdentificatorCounter() },
            { tag, _ -> DimensionIdentificatorCounter().also {
                if (tag.contains("count", Tag.TAG_INT.toInt())) it.id = tag.getInt("count").coerceAtLeast(1)
            } }
        )

        fun get(server: MinecraftServer): DimensionIdentificatorCounter =
            server.overworld().dataStorage.computeIfAbsent(FACTORY, FILE_NAME)

        fun location(number: Int): ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "age_$number")

        fun directory(server: MinecraftServer, number: Int): Path =
            server.getWorldPath(LevelResource.ROOT).resolve("dimensions").resolve(Mystcraft.MOD_ID).resolve("age_$number")

        fun inUse(server: MinecraftServer, number: Int): Boolean {
            val location = location(number)
            return server.getLevel(ResourceKey.create(Registries.DIMENSION, location)) != null
                || server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).containsKey(location)
                || Files.exists(directory(server, number), NOFOLLOW_LINKS)
        }
    }

    /** Server-thread only. Check loaded, registered, and on-disk Ages (including unloaded/orphaned folders). */
    fun nextAvailable(server: MinecraftServer, preferred: Int? = null): Int {
        if (preferred != null) {
            require(preferred in 1 until Int.MAX_VALUE) { "Age IDs must be between 1 and ${Int.MAX_VALUE - 1}." }
            require(!inUse(server, preferred)) { "Age $preferred already exists or its directory is occupied; nothing was overwritten." }
            return preferred
        }
        var candidate = id.coerceAtLeast(1)
        while (candidate < Int.MAX_VALUE && inUse(server, candidate)) candidate++
        check(candidate < Int.MAX_VALUE) { "No further Mystcraft Age IDs are available." }
        return candidate
    }

    fun advancePast(number: Int) {
        require(number in 1 until Int.MAX_VALUE)
        id = maxOf(id, number + 1)
        setDirty()
    }

    fun allocate(server: MinecraftServer): Int = nextAvailable(server).also(::advancePast)

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        tag.putInt("count", id)
        return tag
    }
}

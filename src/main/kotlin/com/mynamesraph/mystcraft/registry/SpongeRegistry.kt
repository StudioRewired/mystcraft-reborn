package com.mynamesraph.mystcraft.data.saved

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.saveddata.SavedData
import kotlin.math.ceil

/**
 * Tracks all classic-sponge protected zones in a world.
 *
 * Sponges are indexed by the chunk they live in, not by every cell they cover.
 * Lookups for "is this position protected?" check the event's chunk plus all
 * neighboring chunks that could possibly contain a sponge whose radius reaches in.
 */
class SpongeRegistry : SavedData() {

    data class SpongeEntry(val pos: BlockPos, val radius: Int)

    // chunkKey (ChunkPos.toLong()) -> sponges whose origin sits in that chunk
    private val spongesByChunk: MutableMap<Long, MutableList<SpongeEntry>> = HashMap()

    companion object {
        const val FILE_NAME = "mystcraft_reborn_sponge_registry"

        // Worst-case chunk-radius reach for any sponge, based on the max radius
        // allowed by SpongeRadiusData. Used to know how far out to scan when
        // testing protection.
        private const val MAX_RADIUS_CHUNKS = 2  // ceil(32 / 16)

        @JvmField
        val FACTORY: Factory<SpongeRegistry> = Factory(
            ::SpongeRegistry,
            ::load,
            null
        )

        private fun load(tag: CompoundTag, registries: HolderLookup.Provider): SpongeRegistry {
            val registry = SpongeRegistry()
            val list = tag.getList("sponges", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until list.size) {
                val entryTag = list.getCompound(i)
                val pos = BlockPos(
                    entryTag.getInt("x"),
                    entryTag.getInt("y"),
                    entryTag.getInt("z")
                )
                val radius = entryTag.getInt("radius")
                registry.registerInternal(SpongeEntry(pos, radius))
            }
            return registry
        }
    }

    fun register(pos: BlockPos, radius: Int) {
        registerInternal(SpongeEntry(pos, radius))
        setDirty()
    }

    private fun registerInternal(entry: SpongeEntry) {
        val key = ChunkPos.asLong(entry.pos)
        spongesByChunk.getOrPut(key) { mutableListOf() }.add(entry)
    }

    fun unregister(pos: BlockPos) {
        val key = ChunkPos.asLong(pos)
        val list = spongesByChunk[key] ?: return
        if (list.removeAll { it.pos == pos }) {
            if (list.isEmpty()) spongesByChunk.remove(key)
            setDirty()
        }
    }

    /**
     * O(neighbor chunks) — typically 9 to 25 hashmap lookups followed by a few
     * cheap bounding-box checks. No iteration over protected cells.
     */
    fun isProtected(pos: BlockPos): Boolean {
        val centerChunk = ChunkPos(pos)
        val reach = MAX_RADIUS_CHUNKS
        for (dz in -reach..reach) {
            for (dx in -reach..reach) {
                val key = ChunkPos.asLong(centerChunk.x + dx, centerChunk.z + dz)
                val sponges = spongesByChunk[key] ?: continue
                for (entry in sponges) {
                    if (containsPos(entry, pos)) return true
                }
            }
        }
        return false
    }

    private fun containsPos(entry: SpongeEntry, pos: BlockPos): Boolean {
        val r = entry.radius
        val dx = pos.x - entry.pos.x
        val dy = pos.y - entry.pos.y
        val dz = pos.z - entry.pos.z
        return dx in -r..r && dy in -r..r && dz in -r..r
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val list = ListTag()
        for ((_, entries) in spongesByChunk) {
            for (entry in entries) {
                val entryTag = CompoundTag()
                entryTag.putInt("x", entry.pos.x)
                entryTag.putInt("y", entry.pos.y)
                entryTag.putInt("z", entry.pos.z)
                entryTag.putInt("radius", entry.radius)
                list.add(entryTag)
            }
        }
        tag.put("sponges", list)
        return tag
    }
}
package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

/**
 * Attached to a descriptive book by the Editing Table.
 * Presence of this component means the age uses deterministic worldgen
 * instead of the default randomized generation.
 *
 * Tier 1 — numeric levels (1–16, stored as Int):
 *   [terrainTurbulence]  Diamond  — terrain jaggedness / scale
 *   [seaLevel]           Gold     — water table height
 *   [caveDensity]        Iron     — cave frequency
 *   [biomeSize]          Copper   — biome patch scale
 *   [verticalRange]      Coal     — world height / depth
 *   [ambientLight]       Torch    — base light level (0 = dark, 16 = always-lit)
 *   [coordinateScale]    Rail     — movement speed multiplier vs overworld
 *
 * Tier 2 — boolean flags (presence of a single item):
 *   [superFlat]          Golden Shovel — flat world (uses flatNoiseRouter)
 *   [noMobs]             Mob Spawner   — disable all mob spawning
 *   [netherMode]         Nether Star   — ceiling + ultrawarm + no skylight
 *   [caveWorld]          Ender Pearl   — no skylight (perpetual darkness)
 *   [noAquifers]         Sponge        — disable underground water pockets
 *   [glowingAir]         Glowstone     — elevated ambient light (overrides ambientLight tier1)
 */
data class WorldgenParametersComponent(
    val terrainTurbulence: Int = 6,
    val seaLevel: Int = 6,
    val caveDensity: Int = 6,
    val biomeSize: Int = 6,
    val verticalRange: Int = 6,
    val superFlat: Boolean = false,
    val noMobs: Boolean = false,
    val caveWorld: Boolean = false,
    val noAquifers: Boolean = false,
){
    companion object {
        val CODEC: Codec<WorldgenParametersComponent> =
            RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.INT.optionalFieldOf("terrain_turbulence", 6).forGetter { it.terrainTurbulence },
                    Codec.INT.optionalFieldOf("sea_level", 6).forGetter { it.seaLevel },
                    Codec.INT.optionalFieldOf("cave_density", 6).forGetter { it.caveDensity },
                    Codec.INT.optionalFieldOf("biome_size", 6).forGetter { it.biomeSize },
                    Codec.INT.optionalFieldOf("vertical_range", 6).forGetter { it.verticalRange },
                    Codec.BOOL.optionalFieldOf("super_flat", false).forGetter { it.superFlat },
                    Codec.BOOL.optionalFieldOf("no_mobs", false).forGetter { it.noMobs },
                    Codec.BOOL.optionalFieldOf("cave_world", false).forGetter { it.caveWorld },
                    Codec.BOOL.optionalFieldOf("no_aquifers", false).forGetter { it.noAquifers },
                ).apply(instance, ::WorldgenParametersComponent)
            }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WorldgenParametersComponent> =
            object : StreamCodec<RegistryFriendlyByteBuf, WorldgenParametersComponent> {
                override fun decode(buf: RegistryFriendlyByteBuf) = WorldgenParametersComponent(
                    terrainTurbulence = buf.readInt(),
                    seaLevel = buf.readInt(),
                    caveDensity = buf.readInt(),
                    biomeSize = buf.readInt(),
                    verticalRange = buf.readInt(),
                    superFlat = buf.readBoolean(),
                    noMobs = buf.readBoolean(),
                    caveWorld = buf.readBoolean(),
                    noAquifers = buf.readBoolean(),
                )

                override fun encode(buf: RegistryFriendlyByteBuf, value: WorldgenParametersComponent) {
                    buf.writeInt(value.terrainTurbulence)
                    buf.writeInt(value.seaLevel)
                    buf.writeInt(value.caveDensity)
                    buf.writeInt(value.biomeSize)
                    buf.writeInt(value.verticalRange)
                    buf.writeBoolean(value.superFlat)
                    buf.writeBoolean(value.noMobs)
                    buf.writeBoolean(value.caveWorld)
                    buf.writeBoolean(value.noAquifers)
                }
            }
    }
}


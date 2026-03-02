package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

/**
 * Attached to a descriptive book by the Editing Table.
 * Presence of this component means the age uses deterministic worldgen
 * instead of the default randomized generation.
 *
 * Tier-1 fields are nullable — null means "not set by player, randomize this parameter."
 * A non-null value is the explicit level chosen at the editing table (1–16).
 *
 * Tier 2 — boolean flags (presence of a single item):
 *   [superFlat]    Golden Shovel — flat world
 *   [noMobs]       Mob Spawner   — disable all mob spawning
 *   [caveWorld]    Ender Pearl   — no skylight
 *   [noAquifers]   Sponge        — disable underground water pockets
 */
data class WorldgenParametersComponent(
    val terrainTurbulence: Int? = null,
    val seaLevel: Int? = null,
    val caveDensity: Int? = null,
    val biomeSize: Int? = null,
    val verticalRange: Int? = null,
    val superFlat: Boolean = false,
    val noMobs: Boolean = false,
    val caveWorld: Boolean = false,
    val noAquifers: Boolean = false,
) {
    companion object {
        val CODEC: Codec<WorldgenParametersComponent> =
            RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.INT.optionalFieldOf("terrain_turbulence").forGetter { java.util.Optional.ofNullable(it.terrainTurbulence) },
                    Codec.INT.optionalFieldOf("sea_level").forGetter { java.util.Optional.ofNullable(it.seaLevel) },
                    Codec.INT.optionalFieldOf("cave_density").forGetter { java.util.Optional.ofNullable(it.caveDensity) },
                    Codec.INT.optionalFieldOf("biome_size").forGetter { java.util.Optional.ofNullable(it.biomeSize) },
                    Codec.INT.optionalFieldOf("vertical_range").forGetter { java.util.Optional.ofNullable(it.verticalRange) },
                    Codec.BOOL.optionalFieldOf("super_flat", false).forGetter { it.superFlat },
                    Codec.BOOL.optionalFieldOf("no_mobs", false).forGetter { it.noMobs },
                    Codec.BOOL.optionalFieldOf("cave_world", false).forGetter { it.caveWorld },
                    Codec.BOOL.optionalFieldOf("no_aquifers", false).forGetter { it.noAquifers },
                ).apply(instance) { tt, sl, cd, bs, vr, sf, nm, cw, na ->
                    WorldgenParametersComponent(
                        terrainTurbulence = tt.orElse(null),
                        seaLevel          = sl.orElse(null),
                        caveDensity       = cd.orElse(null),
                        biomeSize         = bs.orElse(null),
                        verticalRange     = vr.orElse(null),
                        superFlat         = sf,
                        noMobs            = nm,
                        caveWorld         = cw,
                        noAquifers        = na,
                    )
                }
            }

        // Each nullable Int is written as: 1 byte presence flag, then the Int if present.
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WorldgenParametersComponent> =
            object : StreamCodec<RegistryFriendlyByteBuf, WorldgenParametersComponent> {
                override fun decode(buf: RegistryFriendlyByteBuf) = WorldgenParametersComponent(
                    terrainTurbulence = if (buf.readBoolean()) buf.readInt() else null,
                    seaLevel          = if (buf.readBoolean()) buf.readInt() else null,
                    caveDensity       = if (buf.readBoolean()) buf.readInt() else null,
                    biomeSize         = if (buf.readBoolean()) buf.readInt() else null,
                    verticalRange     = if (buf.readBoolean()) buf.readInt() else null,
                    superFlat  = buf.readBoolean(),
                    noMobs     = buf.readBoolean(),
                    caveWorld  = buf.readBoolean(),
                    noAquifers = buf.readBoolean(),
                )

                override fun encode(buf: RegistryFriendlyByteBuf, value: WorldgenParametersComponent) {
                    fun writeNullableInt(v: Int?) { buf.writeBoolean(v != null); if (v != null) buf.writeInt(v) }
                    writeNullableInt(value.terrainTurbulence)
                    writeNullableInt(value.seaLevel)
                    writeNullableInt(value.caveDensity)
                    writeNullableInt(value.biomeSize)
                    writeNullableInt(value.verticalRange)
                    buf.writeBoolean(value.superFlat)
                    buf.writeBoolean(value.noMobs)
                    buf.writeBoolean(value.caveWorld)
                    buf.writeBoolean(value.noAquifers)
                }
            }
    }
}
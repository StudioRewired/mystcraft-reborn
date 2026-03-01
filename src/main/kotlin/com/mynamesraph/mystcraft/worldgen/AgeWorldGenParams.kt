package com.mynamesraph.mystcraft.worldgen

import com.mynamesraph.mystcraft.component.WorldgenParametersComponent
import net.minecraft.core.HolderGetter
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.DensityFunction
import net.minecraft.world.level.levelgen.NoiseSettings
import net.minecraft.world.level.levelgen.synth.NormalNoise

/**
 * Translates WorldgenParametersComponent integer levels (1–16) into the actual
 * values used when constructing NoiseGeneratorSettings / DimensionType.
 */
object AgeWorldgenParams {

    const val LEVEL_MIN = 1
    const val LEVEL_MAX = 16

    const val SEA_LEVEL_MIN = -64
    const val SEA_LEVEL_MAX = 160

    const val DEPTH_OFFSET_MIN = -0.8
    const val DEPTH_OFFSET_MAX =  1.2
    const val LARGE_BIOMES_THRESHOLD = 9

    const val CAVE_DENSITY_MULTIPLIER_MIN = 0.2
    const val CAVE_DENSITY_MULTIPLIER_MAX = 2.5

    const val BIOME_SPAN_MIN = 0.05f
    const val BIOME_SPAN_MAX = 1.0f

    const val MIN_Y_FLOOR = -64
    const val MIN_Y_DEEP  = -128
    const val HEIGHT_MIN  =  128
    const val HEIGHT_MAX  =  384

    // Cave world noise tweaks — extreme values to simulate cave-like terrain
    const val CAVE_WORLD_DEPTH_OFFSET = -2.0
    const val CAVE_WORLD_CAVE_MULTI   = 2.5

    private fun lerp(t: Float, min: Double, max: Double): Double =
        min + (max - min) * t

    private fun lerp(t: Float, min: Float, max: Float): Float =
        min + (max - min) * t

    private fun norm(level: Int): Float =
        ((level - LEVEL_MIN).toFloat() / (LEVEL_MAX - LEVEL_MIN).toFloat()).coerceIn(0f, 1f)

    fun seaLevel(level: Int): Int =
        lerp(norm(level), SEA_LEVEL_MIN.toDouble(), SEA_LEVEL_MAX.toDouble()).toInt()

    fun depthOffset(level: Int): Double =
        lerp(norm(level), DEPTH_OFFSET_MIN, DEPTH_OFFSET_MAX)

    fun useLargeBiomes(level: Int): Boolean = level >= LARGE_BIOMES_THRESHOLD

    fun caveDensityMultiplier(level: Int): Double =
        lerp(norm(level), CAVE_DENSITY_MULTIPLIER_MIN, CAVE_DENSITY_MULTIPLIER_MAX)

    fun biomeSpanHalfWidth(level: Int): Float =
        lerp(norm(level), BIOME_SPAN_MIN, BIOME_SPAN_MAX)

    fun noiseSettings(level: Int): NoiseSettings {
        val t = norm(level)
        val minY   = lerp(t, MIN_Y_FLOOR.toDouble(), MIN_Y_DEEP.toDouble()).toInt()
        val height = lerp(t, HEIGHT_MIN.toDouble(), HEIGHT_MAX.toDouble()).toInt()
        val roundedMinY   = (minY / 16) * 16
        val roundedHeight = ((height + 15) / 16) * 16
        val safeMinY   = roundedMinY.coerceIn(-2032, 2031)
        val safeHeight = roundedHeight.coerceIn(16, 2031 - safeMinY)
        val finalHeight = (safeHeight / 16) * 16
        return NoiseSettings.create(safeMinY, finalHeight.coerceAtLeast(16), 1, 2)
    }

    val FLAT_NOISE_SETTINGS: NoiseSettings = NoiseSettings.create(-64, 128, 1, 2)

    /**
     * Returns true if ages should use isolated vanilla worldgen.
     * Reads from per-world SavedData, falling back to config default.
     */
    fun shouldIsolateWorldgen(server: MinecraftServer): Boolean {
        return try {
            val data = server.overworld().dataStorage
                .computeIfAbsent(
                    com.mynamesraph.mystcraft.data.saved.IsolateWorldgenData.FACTORY,
                    com.mynamesraph.mystcraft.data.saved.IsolateWorldgenData.FILE_NAME
                )
            data.isolate
        } catch (e: Exception) {
            com.mynamesraph.mystcraft.Config.isolateWorldgenDefault
        }
    }

    /**
     * Builds the noise router for an age, respecting the isolate worldgen setting.
     * The router is already constructed from hardcoded vanilla keys via NoiseRouterDataMixin,
     * so it is inherently more isolated than using the server's registered NoiseGeneratorSettings.
     * The isolated flag is passed through for documentation and future use.
     */
    fun buildNoiseRouter(
        server: MinecraftServer,
        large: Boolean,
        depthOffset: Double,
        caveMultiplier: Double
    ): net.minecraft.world.level.levelgen.NoiseRouter {
        val isolated = shouldIsolateWorldgen(server)

        val densityFunctions: HolderGetter<DensityFunction> = server.registryAccess()
            .asGetterLookup()
            .lookupOrThrow(Registries.DENSITY_FUNCTION)
        val noiseParams: HolderGetter<NormalNoise.NoiseParameters> = server.registryAccess()
            .asGetterLookup()
            .lookupOrThrow(Registries.NOISE)

        return OverworldNoiseRouter(
            densityFunctions,
            noiseParams,
            large          = large,
            depthOffset    = depthOffset,
            caveMultiplier = caveMultiplier,
            isolated       = isolated
        )
    }
}
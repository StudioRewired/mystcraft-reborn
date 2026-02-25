package com.mynamesraph.mystcraft.worldgen

import com.mynamesraph.mystcraft.mixin.NoiseRouterDataMixin
import net.minecraft.core.HolderGetter
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.levelgen.*
import net.minecraft.world.level.levelgen.OreVeinifier.VeinType
import net.minecraft.world.level.levelgen.synth.NormalNoise
import java.util.stream.Stream



fun OverworldNoiseRouter(
densityFunctions: HolderGetter<DensityFunction>,
noiseParameters: HolderGetter<NormalNoise.NoiseParameters>,
large: Boolean
):NoiseRouter {

    val barrier = DensityFunctions.noise(noiseParameters.getOrThrow(Noises.AQUIFER_BARRIER), 0.5)
    val fluidLevelFloodedness = DensityFunctions.noise(noiseParameters.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67)
    val fluidLevelSpread = DensityFunctions.noise(noiseParameters.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143)
    val lava = DensityFunctions.noise(noiseParameters.getOrThrow(Noises.AQUIFER_LAVA))

    val shiftX = NoiseRouterDataMixin.invokeGetFunction(densityFunctions,
        NoiseRouterDataMixin.getSHIFT_X())
    val shiftZ = NoiseRouterDataMixin.invokeGetFunction(densityFunctions,
        NoiseRouterDataMixin.getSHIFT_Z())

    val temperature = DensityFunctions.shiftedNoise2d(
        shiftX,
        shiftZ,
        0.25,
        noiseParameters.getOrThrow( if (large) Noises.TEMPERATURE_LARGE else Noises.TEMPERATURE)
    )

    val vegetation = DensityFunctions.shiftedNoise2d(
        shiftX,
        shiftZ,
        0.25,
        noiseParameters.getOrThrow( if (large) Noises.VEGETATION_LARGE else Noises.VEGETATION)
    )

    val continents = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterData.CONTINENTS_LARGE else NoiseRouterData.CONTINENTS
    )

    val erosion = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterData.EROSION_LARGE else NoiseRouterData.EROSION_LARGE
    )

    val depth = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterDataMixin.getDEPTH_LARGE() else NoiseRouterData.DEPTH //TODO: Amplified
    )

    val ridges = NoiseRouterDataMixin.invokeGetFunction(densityFunctions,NoiseRouterData.RIDGES)

    val factor = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterDataMixin.getFACTOR_LARGE() else NoiseRouterData.FACTOR //TODO: Amplified
    )

    val depthGradient = NoiseRouterDataMixin.invokeNoiseGradientDensity(
        DensityFunctions.cache2d(factor),
        depth
    )

    val initialDensity = NoiseRouterDataMixin.invokeSlideOverworld(
        false,
        DensityFunctions.add(
            depthGradient,
            DensityFunctions.constant(-0.703125)
        ).clamp(
            -64.0,
            64.0
        )
    )

    val slopedCheese = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterDataMixin.getSLOPED_CHEESE_LARGE() else NoiseRouterDataMixin.getSLOPED_CHEESE()
    )

    val entrances = DensityFunctions.min(
        slopedCheese,
        DensityFunctions.mul(
            DensityFunctions.constant(5.0),
            NoiseRouterDataMixin.invokeGetFunction(
                densityFunctions,
                NoiseRouterDataMixin.getENTRANCES()
            )
        )
    )

    val underground = DensityFunctions.rangeChoice(
        slopedCheese,
        -1_000_000.0,
        1.5625,
        entrances,
        NoiseRouterDataMixin.invokeUnderground(
            densityFunctions,
            noiseParameters,
            slopedCheese
        )
    )

    val finalDensity = DensityFunctions.min(
        NoiseRouterDataMixin.invokePostProcess(
            NoiseRouterDataMixin.invokeSlideOverworld(
                false,
                underground
            )
        ),
        NoiseRouterDataMixin.invokeGetFunction(
            densityFunctions,
            NoiseRouterDataMixin.getNOODLE()
        )
    )

    val y = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        NoiseRouterDataMixin.getY()
    )

    val minY = Stream.of(*VeinType.entries.toTypedArray())
        .mapToInt { it.minY }
        .min()
        .orElse(-DimensionType.MIN_Y * 2)


    val maxY = Stream.of(*VeinType.entries.toTypedArray())
        .mapToInt { it.maxY }
        .min()
        .orElse(-DimensionType.MIN_Y * 2)


    val veinToggle = NoiseRouterDataMixin.invokeYLimitedInterpolatable(
        y,
        DensityFunctions.noise(
            noiseParameters.getOrThrow(
                Noises.ORE_VEININESS
            ),
            1.5,
            1.5
        ),
        minY,
        maxY,
        0
    )

    val oreVeinA = NoiseRouterDataMixin.invokeYLimitedInterpolatable(
        y,
        DensityFunctions.noise(
            noiseParameters.getOrThrow(Noises.ORE_VEIN_A),4.0,4.0),minY,maxY,0
    ).abs()

    val oreVeinB = NoiseRouterDataMixin.invokeYLimitedInterpolatable(
        y,
        DensityFunctions.noise(
            noiseParameters.getOrThrow(Noises.ORE_VEIN_B),4.0,4.0),minY,maxY,0
    ).abs()

    val veinRidged = DensityFunctions.add(
        DensityFunctions.constant(-0.08),
        DensityFunctions.max(
            oreVeinA,
            oreVeinB
        )
    )

    val veinGap = DensityFunctions.noise(
        noiseParameters.getOrThrow(
            Noises.ORE_GAP
        )
    )

    return NoiseRouter(
        barrier,
        fluidLevelFloodedness,
        fluidLevelSpread,
        lava,
        temperature,
        vegetation,
        continents,
        erosion,
        depth,
        ridges,
        initialDensity,
        finalDensity,
        veinToggle,
        veinRidged,
        veinGap,
    )
}

/**
 * Will flood caves under the sea-level, preferably used with sea-level=-64
 */
fun flatNoiseRouter(
    densityFunctions: HolderGetter<DensityFunction>,
    noiseParameters: HolderGetter<NormalNoise.NoiseParameters>,
    large: Boolean
):NoiseRouter {
    val lava = DensityFunctions.noise(noiseParameters.getOrThrow(Noises.AQUIFER_LAVA))

    val shiftX = NoiseRouterDataMixin.invokeGetFunction(densityFunctions,
        NoiseRouterDataMixin.getSHIFT_X())
    val shiftZ = NoiseRouterDataMixin.invokeGetFunction(densityFunctions,
        NoiseRouterDataMixin.getSHIFT_Z())

    val temperature = DensityFunctions.shiftedNoise2d(
        shiftX,
        shiftZ,
        0.25,
        noiseParameters.getOrThrow( if (large) Noises.TEMPERATURE_LARGE else Noises.TEMPERATURE)
    )

    val vegetation = DensityFunctions.shiftedNoise2d(
        shiftX,
        shiftZ,
        0.25,
        noiseParameters.getOrThrow( if (large) Noises.VEGETATION_LARGE else Noises.VEGETATION)
    )

    val continents = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterData.CONTINENTS_LARGE else NoiseRouterData.CONTINENTS
    )

    val erosion = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterData.EROSION_LARGE else NoiseRouterData.EROSION_LARGE
    )

    val depth = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterDataMixin.getDEPTH_LARGE() else NoiseRouterData.DEPTH //TODO: Amplified
    )

    val ridges = NoiseRouterDataMixin.invokeGetFunction(densityFunctions,NoiseRouterData.RIDGES)

    val factor = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterDataMixin.getFACTOR_LARGE() else NoiseRouterData.FACTOR //TODO: Amplified
    )

    val depthGradient = NoiseRouterDataMixin.invokeNoiseGradientDensity(
        DensityFunctions.cache2d(factor),
        depth
    )

    val initialDensity = NoiseRouterDataMixin.invokeSlideOverworld(
        false,
        DensityFunctions.add(
            depthGradient,
            DensityFunctions.constant(-0.703125)
        ).clamp(
            -64.0,
            64.0
        )
    )

    val slopedCheese = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        if (large) NoiseRouterDataMixin.getSLOPED_CHEESE_LARGE() else NoiseRouterDataMixin.getSLOPED_CHEESE()
    )

    val entrances = DensityFunctions.min(
        slopedCheese,
        DensityFunctions.mul(
            DensityFunctions.constant(5.0),
            NoiseRouterDataMixin.invokeGetFunction(
                densityFunctions,
                NoiseRouterDataMixin.getENTRANCES()
            )
        )
    )

    val finalDensity = DensityFunctions.min(
        NoiseRouterDataMixin.invokePostProcess(
            NoiseRouterDataMixin.invokeSlideOverworld(
                false,
                DensityFunctions.constant(64.0)
            )
        ),
        NoiseRouterDataMixin.invokeGetFunction(
            densityFunctions,
            NoiseRouterDataMixin.getNOODLE()
        )
    )

    val y = NoiseRouterDataMixin.invokeGetFunction(
        densityFunctions,
        NoiseRouterDataMixin.getY()
    )

    val minY = Stream.of(*VeinType.entries.toTypedArray())
        .mapToInt { it.minY }
        .min()
        .orElse(-DimensionType.MIN_Y * 2)


    val maxY = Stream.of(*VeinType.entries.toTypedArray())
        .mapToInt { it.maxY }
        .min()
        .orElse(-DimensionType.MIN_Y * 2)


    val veinToggle = NoiseRouterDataMixin.invokeYLimitedInterpolatable(
        y,
        DensityFunctions.noise(
            noiseParameters.getOrThrow(
                Noises.ORE_VEININESS
            ),
            1.5,
            1.5
        ),
        minY,
        maxY,
        0
    )

    val oreVeinA = NoiseRouterDataMixin.invokeYLimitedInterpolatable(
        y,
        DensityFunctions.noise(
            noiseParameters.getOrThrow(Noises.ORE_VEIN_A),4.0,4.0),minY,maxY,0
    ).abs()

    val oreVeinB = NoiseRouterDataMixin.invokeYLimitedInterpolatable(
        y,
        DensityFunctions.noise(
            noiseParameters.getOrThrow(Noises.ORE_VEIN_B),4.0,4.0),minY,maxY,0
    ).abs()

    val veinRidged = DensityFunctions.add(
        DensityFunctions.constant(-0.08),
        DensityFunctions.max(
            oreVeinA,
            oreVeinB
        )
    )

    val veinGap = DensityFunctions.noise(
        noiseParameters.getOrThrow(
            Noises.ORE_GAP
        )
    )

    return NoiseRouter(
        DensityFunctions.zero(),
        DensityFunctions.zero(),
        DensityFunctions.zero(),
        lava,
        temperature,
        vegetation,
        continents,
        erosion,
        depth,
        ridges,
        initialDensity,
        finalDensity,
        veinToggle,
        veinRidged,
        veinGap,
    )
}
package com.mynamesraph.mystcraft.item

import com.mojang.logging.LogUtils
import com.mojang.serialization.DynamicOps
import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.component.*
import com.mynamesraph.mystcraft.data.saved.DimensionIdentificatorCounter
import com.mynamesraph.mystcraft.data.saved.NoMobDimensions
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.worldgen.AgeWorldgenParams
import com.mynamesraph.mystcraft.worldgen.OverworldNoiseRouter
import com.mynamesraph.mystcraft.worldgen.flatNoiseRouter
import net.commoble.infiniverse.api.InfiniverseAPI
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.SurfaceRuleData
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.*
import net.minecraft.world.level.biome.Climate.ParameterPoint
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.dimension.LevelStem
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.NoiseSettings
import net.minecraft.world.level.saveddata.SavedData.Factory
import org.joml.Vector3f
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random

class DescriptiveBookItem(properties: Properties) : LinkingBookItem(properties) {

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (!level.isClientSide) {
            val counter = level.server!!.overworld().dataStorage.computeIfAbsent(
                Factory(this::create, this::load),
                DimensionIdentificatorCounter.FILE_NAME
            )

            val item = player.getItemInHand(usedHand)
            val idComponent = item.components.get(MystcraftComponents.DIMENSION_ID.get())

            if (idComponent is DimensionIdentificatorComponent) {
                if (!idComponent.generated) {
                    val levelKey = ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.fromNamespaceAndPath(
                            Mystcraft.MOD_ID,
                            "age_${counter.id++}"
                        )
                    )
                    counter.setDirty()

                    val worldgenParams = item.components.get(MystcraftComponents.WORLDGEN_PARAMETERS.get())
                    val serverLevel = level.server!!

                    InfiniverseAPI.get().getOrCreateLevel(
                        level.server,
                        levelKey
                    ) {
                        when {
                            worldgenParams != null && worldgenParams.superFlat ->
                                createFlatLevel(serverLevel, item, worldgenParams)
                            worldgenParams != null ->
                                createParameterizedNoiseLevel(serverLevel, item, worldgenParams)
                            else -> when (Random.nextInt(0, 2)) {
                                0    -> createNoiseLevel(serverLevel, item)
                                else -> createFlatLevel(serverLevel, item, null)
                            }
                        }
                    }

                    if (worldgenParams?.noMobs == true) {
                        val noMobData = serverLevel.overworld().dataStorage.computeIfAbsent(
                            Factory({ NoMobDimensions() }, { tag, _ ->
                                val data = NoMobDimensions()
                                val list = tag.getList("dimensions", Tag.TAG_STRING.toInt())
                                for (i in 0 until list.size) {
                                    ResourceLocation.tryParse(list.getString(i))?.let { data.dimensions.add(it) }
                                }
                                data
                            }),
                            NoMobDimensions.FILE_NAME
                        )
                        noMobData.add(levelKey.location())
                    }

                    val spawnPos = level.sharedSpawnPos
                    val safePos  = BlockPos(spawnPos.x, spawnPos.y, spawnPos.z)
                    val newServerLevel = serverLevel.getLevel(levelKey)
                    if (newServerLevel != null) {
                        newServerLevel.setBlock(safePos, Blocks.GOLD_BLOCK.defaultBlockState(), 3)
                    }

                    val patched = PatchedDataComponentMap(item.components)

                    val display = item.components.get(MystcraftComponents.LOCATION_DISPLAY.get())
                    if (display is LocationDisplayComponent) {
                        if (display.name.plainCopy().contains(
                                Component.translatable("mystcraft_reborn.unknown_age")
                            )
                        ) {
                            patched.set(
                                MystcraftComponents.LOCATION_DISPLAY.get(),
                                LocationDisplayComponent(
                                    Component.translatable("mystcraft_reborn.age", counter.id - 1)
                                        .withStyle(Style.EMPTY.withItalic(false).withColor(0xAAAAAA))
                                )
                            )
                        }
                    }

                    patched.set(
                        MystcraftComponents.DIMENSION_ID.get(),
                        DimensionIdentificatorComponent(true, levelKey.location())
                    )

                    val spawnCenter = level.sharedSpawnPos.bottomCenter
                    patched.set(
                        MystcraftComponents.LOCATION.get(),
                        LocationComponent(
                            levelKey,
                            Vector3f(
                                spawnCenter.x.toFloat(),
                                spawnCenter.y.toFloat() + 1,
                                spawnCenter.z.toFloat()
                            )
                        )
                    )

                    patched.set(
                        MystcraftComponents.ROTATION.get(),
                        RotationComponent(0.0f, 0.0f)
                    )

                    item.applyComponentsAndValidate(patched.asPatch())
                }
            }
        }

        return super.use(level, player, usedHand)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Level creation helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Fully deterministic generation driven by [WorldgenParametersComponent].
     *  Any null tier-1 parameter is individually randomized in range [1,16],
     *  keeping all values within the safe bounds accepted by AgeWorldgenParams. */
    private fun createParameterizedNoiseLevel(
        server: MinecraftServer,
        book: ItemStack,
        params: WorldgenParametersComponent
    ): LevelStem {
        val dimensionType = buildDimensionType(server, params)

        // Resolve each nullable param — null means "not set by player, randomize it".
        // All values are clamped to [1,16] which is the safe input range for every
        // AgeWorldgenParams function, preventing the out-of-range values that previously
        // caused level.dat corruption.
        val verticalRange     = (params.verticalRange     ?: Random.nextInt(1, 17)).coerceIn(1, 16)
        val seaLevelParam     = (params.seaLevel          ?: Random.nextInt(1, 17)).coerceIn(1, 16)
        val terrainTurbulence = (params.terrainTurbulence ?: Random.nextInt(1, 17)).coerceIn(1, 16)
        val caveDensityParam  = (params.caveDensity       ?: Random.nextInt(1, 17)).coerceIn(1, 16)
        val biomeSizeParam    = (params.biomeSize         ?: Random.nextInt(1, 17)).coerceIn(1, 16)

        val noiseSettings = AgeWorldgenParams.noiseSettings(verticalRange)
        val seaLevel      = AgeWorldgenParams.seaLevel(seaLevelParam)
        val large         = AgeWorldgenParams.useLargeBiomes(terrainTurbulence)

        // Cave world overrides depth offset and cave multiplier for inverted terrain feel
        val depthOffset = if (params.caveWorld)
            AgeWorldgenParams.CAVE_WORLD_DEPTH_OFFSET
        else
            AgeWorldgenParams.depthOffset(terrainTurbulence)

        val caveMulti = if (params.caveWorld)
            AgeWorldgenParams.CAVE_WORLD_CAVE_MULTI
        else
            AgeWorldgenParams.caveDensityMultiplier(caveDensityParam)

        val noiseRouter = AgeWorldgenParams.buildNoiseRouter(server, large, depthOffset, caveMulti)

        val biomeSource = MultiNoiseBiomeSource.createFromList(
            Climate.ParameterList(getBiomeList(book, server, biomeSizeParam))
        )

        val generator = NoiseBasedChunkGenerator(
            biomeSource,
            Holder.direct(
                NoiseGeneratorSettings(
                    noiseSettings,
                    Blocks.STONE.defaultBlockState(),
                    Blocks.WATER.defaultBlockState(),
                    noiseRouter,
                    SurfaceRuleData.overworld(),
                    OverworldBiomeBuilder().spawnTarget(),
                    seaLevel,
                    params.noMobs,
                    !params.noAquifers,
                    true,
                    false
                )
            )
        )

        return LevelStem(dimensionType, generator)
    }

    /** Randomized noise generation (original behaviour). */
    private fun createNoiseLevel(server: MinecraftServer, book: ItemStack): LevelStem {
        val dimensionType = server.overworld().dimensionTypeRegistration()
        val biomeSource = MultiNoiseBiomeSource.createFromList(
            Climate.ParameterList(getBiomeList(book, server, null))
        )

        val noiseSettings: NoiseSettings = if (Random.nextInt(0, 10) == 0)
            NoiseSettings.create(64, 128, 1, 1)
        else
            NoiseSettings.create(-64, 384, 1, 2)

        val surfaceRule = when (Random.nextInt(0, 20)) {
            0    -> SurfaceRuleData.nether()
            1    -> SurfaceRuleData.end()
            else -> SurfaceRuleData.overworld()
        }

        val fluid = when (Random.nextInt(0, 100)) {
            0    -> Blocks.LAVA.defaultBlockState()
            else -> Blocks.WATER.defaultBlockState()
        }

        val stone = randomStone()
        val seaLevel = when (Random.nextInt(0, 100)) {
            0    -> 0
            1    -> 128
            2    -> 256
            else -> 64
        }

        // nextInt upper bound is exclusive, so (1, 17) gives the full [1,16] range
        val noiseRouter = AgeWorldgenParams.buildNoiseRouter(
            server,
            large          = Random.nextBoolean(),
            depthOffset    = AgeWorldgenParams.depthOffset(Random.nextInt(1, 17)),
            caveMultiplier = AgeWorldgenParams.caveDensityMultiplier(Random.nextInt(1, 17))
        )

        val generator = NoiseBasedChunkGenerator(
            biomeSource,
            Holder.direct(
                NoiseGeneratorSettings(
                    noiseSettings, stone, fluid,
                    noiseRouter,
                    surfaceRule,
                    OverworldBiomeBuilder().spawnTarget(),
                    seaLevel,
                    false, true, true, Random.nextBoolean()
                )
            )
        )

        return LevelStem(dimensionType, generator)
    }

    /**
     * Flat level — used when [WorldgenParametersComponent.superFlat] is set,
     * or randomly when params is null.
     */
    private fun createFlatLevel(
        server: MinecraftServer,
        book: ItemStack,
        params: WorldgenParametersComponent?
    ): LevelStem {
        val dimensionType = if (params != null) buildDimensionType(server, params)
        else server.overworld().dimensionTypeRegistration()

        // Resolve biomeSize — null means randomize, same safe range as elsewhere
        val biomeSizeParam: Int? = params?.biomeSize?.coerceIn(1, 16)
            ?: params?.let { Random.nextInt(1, 17) }

        val biomeSource = MultiNoiseBiomeSource.createFromList(
            Climate.ParameterList(getBiomeList(book, server, biomeSizeParam))
        )

        val densityFunctions = server.registryAccess().asGetterLookup()
            .lookupOrThrow(Registries.DENSITY_FUNCTION)
        val noiseParams = server.registryAccess().asGetterLookup()
            .lookupOrThrow(Registries.NOISE)

        // Resolve seaLevel — null means randomize; 0 when no params at all (pure random flat)
        val seaLevel = when {
            params == null        -> 0
            params.seaLevel != null -> AgeWorldgenParams.seaLevel(params.seaLevel.coerceIn(1, 16))
            else                  -> AgeWorldgenParams.seaLevel(Random.nextInt(1, 17))
        }

        val generator = NoiseBasedChunkGenerator(
            biomeSource,
            Holder.direct(
                NoiseGeneratorSettings(
                    AgeWorldgenParams.FLAT_NOISE_SETTINGS,
                    Blocks.STONE.defaultBlockState(),
                    Blocks.WATER.defaultBlockState(),
                    flatNoiseRouter(densityFunctions, noiseParams, false),
                    SurfaceRuleData.overworld(),
                    OverworldBiomeBuilder().spawnTarget(),
                    seaLevel,
                    params?.noMobs ?: false,
                    !(params?.noAquifers ?: false),
                    true,
                    false
                )
            )
        )

        return LevelStem(dimensionType, generator)
    }

    /** Copies the overworld's chunk generator via codec round-trip. */
    private fun createSimpleLevel(server: MinecraftServer): LevelStem {
        val oldLevel = server.overworld()
        val ops: DynamicOps<Tag> = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess())
        val oldChunkGenerator = oldLevel.chunkSource.generator
        val newChunkGenerator: ChunkGenerator = ChunkGenerator.CODEC.encodeStart(ops, oldChunkGenerator)
            .flatMap { ChunkGenerator.CODEC.parse(ops, it) }
            .getOrThrow { RuntimeException("Error copying dimension: $it") }
        val typeHolder: Holder<DimensionType> = oldLevel.dimensionTypeRegistration()
        return LevelStem(typeHolder, newChunkGenerator)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DimensionType
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildDimensionType(
        server: MinecraftServer,
        params: WorldgenParametersComponent
    ): Holder<DimensionType> {
        return server.overworld().dimensionTypeRegistration()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Biome list
    // ─────────────────────────────────────────────────────────────────────────

    private fun getBiomeList(
        book: ItemStack,
        server: MinecraftServer,
        biomeSizeLevel: Int?
    ): List<com.mojang.datafixers.util.Pair<ParameterPoint, Holder<Biome>>> {
        val biomeSymbols = book.components.get(MystcraftComponents.BIOME_SYMBOLS.get())
        // When biomeSizeLevel is set, all biomes share the same fixed span (deterministic).
        // When null (random generation), each biome gets its own random span for variety.
        val fixedSpan: Float? = if (biomeSizeLevel != null)
            AgeWorldgenParams.biomeSpanHalfWidth(biomeSizeLevel)
        else
            null

        fun makePoint(): ParameterPoint {
            val span = fixedSpan ?: Random.nextFloat()
            return ParameterPoint(
                Climate.Parameter.span(-span, span),
                Climate.Parameter.span(-span, span),
                Climate.Parameter.span(-span, span),
                Climate.Parameter.span(-span, span),
                Climate.Parameter.span(-2.0f, 2.0f),
                Climate.Parameter.span(-span, span),
                // Climate.ParameterPoint.CODEC encodes offset as (double)offset, validated
                // against [0.0:1.0]. Large random longs fail this check and cause
                // WorldGenSettings to be silently omitted from level.dat on save.
                0L
            )
        }

        return if (biomeSymbols is BiomeSymbolsComponent) {
            buildList {
                for (symbol in biomeSymbols.biomes) {
                    var biome = server.registryAccess()
                        .registryOrThrow(Registries.BIOME)
                        .getHolder(symbol)
                        .getOrNull()
                    if (biome == null) {
                        LogUtils.getLogger().warn(
                            "Found non-existent biome while generating world! Defaulting to plains!"
                        )
                        biome = server.registryAccess()
                            .registryOrThrow(Registries.BIOME)
                            .getHolderOrThrow(Biomes.PLAINS)
                    }
                    add(com.mojang.datafixers.util.Pair(makePoint(), biome))
                }
            }
        } else {
            buildList {
                for (biome in server.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .holders()
                ) {
                    add(com.mojang.datafixers.util.Pair(makePoint(), biome))
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Misc
    // ─────────────────────────────────────────────────────────────────────────

    private fun randomStone() = when (Random.nextInt(0, 100)) {
        0  -> Blocks.ANDESITE.defaultBlockState()
        1  -> Blocks.DIORITE.defaultBlockState()
        2  -> Blocks.GRANITE.defaultBlockState()
        3  -> Blocks.SMOOTH_QUARTZ.defaultBlockState()
        4  -> Blocks.DEEPSLATE.defaultBlockState()
        5  -> Blocks.ICE.defaultBlockState()
        6  -> Blocks.BLACKSTONE.defaultBlockState()
        7  -> Blocks.END_STONE.defaultBlockState()
        8  -> Blocks.NETHERRACK.defaultBlockState()
        9  -> Blocks.TUFF.defaultBlockState()
        10 -> Blocks.SANDSTONE.defaultBlockState()
        11 -> Blocks.SMOOTH_SANDSTONE.defaultBlockState()
        12 -> Blocks.RED_SANDSTONE.defaultBlockState()
        13 -> Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState()
        14 -> Blocks.PRISMARINE.defaultBlockState()
        15 -> Blocks.BASALT.defaultBlockState()
        16 -> Blocks.SMOOTH_BASALT.defaultBlockState()
        else -> Blocks.STONE.defaultBlockState()
    }

    fun create(): DimensionIdentificatorCounter = DimensionIdentificatorCounter()

    fun load(tag: CompoundTag, lookupProvider: HolderLookup.Provider): DimensionIdentificatorCounter {
        val counter = create()
        if (tag.contains("count", Tag.TAG_INT.toInt())) {
            counter.id = tag.getInt("count")
        }
        return counter
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)

        val symbols = stack.components.get(MystcraftComponents.BIOME_SYMBOLS.get())
        if (symbols is BiomeSymbolsComponent && symbols.biomes.isNotEmpty()) {
            tooltipComponents.add(
                Component.literal("Biomes:")
                    .withStyle(Style.EMPTY.withItalic(false).withColor(0xAAAAAA))
            )
            symbols.biomes.forEach { biome ->
                tooltipComponents.add(
                    Component.literal("  • ").append(
                        Component.translatable("biome.${biome.toLanguageKey()}")
                            .withStyle(Style.EMPTY.withItalic(true).withColor(0x888888))
                    )
                )
            }
        }

        val wgp = stack.components.get(MystcraftComponents.WORLDGEN_PARAMETERS.get())
        if (wgp != null) {
            tooltipComponents.add(
                Component.literal("Edited Age")
                    .withStyle(Style.EMPTY.withItalic(false).withColor(0x55FF55))
            )

            // Helper: show "X/16" if the player set a value, or "randomized" in grey italic if not
            fun paramLine(label: String, value: Int?) {
                if (value != null)
                    tooltipComponents.add(
                        Component.literal("  $label: $value/16")
                            .withStyle(Style.EMPTY.withItalic(false).withColor(0xAAAAAA))
                    )
                else
                    tooltipComponents.add(
                        Component.literal("  $label: randomized")
                            .withStyle(Style.EMPTY.withItalic(true).withColor(0x666666))
                    )
            }

            paramLine("Turbulence",     wgp.terrainTurbulence)
            paramLine("Sea Level",      wgp.seaLevel)
            paramLine("Cave Density",   wgp.caveDensity)
            paramLine("Biome Size",     wgp.biomeSize)
            paramLine("Vertical Range", wgp.verticalRange)

            if (wgp.superFlat)  tooltipComponents.add(Component.literal("  ✦ Super Flat")  .withStyle(Style.EMPTY.withItalic(false).withColor(0x55FF55)))
            if (wgp.noMobs)     tooltipComponents.add(Component.literal("  ✦ No Mobs")     .withStyle(Style.EMPTY.withItalic(false).withColor(0x55FF55)))
            if (wgp.caveWorld)  tooltipComponents.add(Component.literal("  ✦ Cave World")  .withStyle(Style.EMPTY.withItalic(false).withColor(0x55FF55)))
            if (wgp.noAquifers) tooltipComponents.add(Component.literal("  ✦ No Aquifers") .withStyle(Style.EMPTY.withItalic(false).withColor(0x55FF55)))
        }
    }
}
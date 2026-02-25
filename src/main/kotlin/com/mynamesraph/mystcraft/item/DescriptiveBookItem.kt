package com.mynamesraph.mystcraft.item

import com.mojang.logging.LogUtils
import com.mojang.serialization.DynamicOps
import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.component.*
import com.mynamesraph.mystcraft.data.saved.DimensionIdentificatorCounter
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.worldgen.OverworldNoiseRouter
import com.mynamesraph.mystcraft.worldgen.flatNoiseRouter
import net.commoble.infiniverse.api.InfiniverseAPI
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
import com.mynamesraph.mystcraft.component.BiomeSymbolsComponent
import net.minecraft.world.item.TooltipFlag


class DescriptiveBookItem(properties: Properties) : LinkingBookItem(properties) {

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {

        // TODO: move to click link panel
        if (!level.isClientSide) {
            val counter = level.server!!.overworld().dataStorage.computeIfAbsent(Factory(this::create,this::load),DimensionIdentificatorCounter.FILE_NAME)

            val item = player.getItemInHand(usedHand)

            val idComponent = item.components.get(MystcraftComponents.DIMENSION_ID.get())

            if (idComponent is DimensionIdentificatorComponent) {
                if (!idComponent.generated) {
                    val levelKey = ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.fromNamespaceAndPath(
                            Mystcraft.MOD_ID,
                            "age_${counter.id++}" // Increment the counter here to make sure if something else fails the counter still gets incremented
                        )
                    )
                    counter.setDirty()

                    InfiniverseAPI.get().getOrCreateLevel(
                        level.server,
                        levelKey
                    ) {
                        when (Random.nextInt(0,2)) {
                            0 -> createNoiseLevel(level.server!!,item)
                            1 -> createFlatLevel(level.server!!,item)
                            else -> {createSimpleLevel(level.server!!)} //This should never happen
                        }
                    }

                    val patched = PatchedDataComponentMap(item.components)

                    val display = item.components.get(MystcraftComponents.LOCATION_DISPLAY.get())

                    if (display is LocationDisplayComponent) {
                        if (display.name.plainCopy().contains(Component.translatable("mystcraft_reborn.unknown_age"))) {
                            patched.set(
                                MystcraftComponents.LOCATION_DISPLAY.get(),
                                LocationDisplayComponent (
                                    Component.translatable("mystcraft_reborn.age",counter.id-1)
                                        .withStyle(Style.EMPTY.withItalic(false).withColor(0xAAAAAA))
                                )
                            )
                        }
                    }

                    patched.set(
                        MystcraftComponents.DIMENSION_ID.get(),
                        DimensionIdentificatorComponent(
                            true,
                            levelKey.location()
                        )
                    )

                    val spawnPos = level.sharedSpawnPos.bottomCenter

                    patched.set(
                        MystcraftComponents.LOCATION.get(),
                        LocationComponent(
                            levelKey,
                            Vector3f(spawnPos.x.toFloat(),spawnPos.y.toFloat()+1,spawnPos.z.toFloat())
                        )
                    )

                    patched.set(
                        MystcraftComponents.ROTATION.get(),
                        RotationComponent(
                            0.0f,
                            0.0f
                        )
                    )

                    item.applyComponentsAndValidate(patched.asPatch())

                }
            }
        }

        return super.use(level, player, usedHand)
    }

    private fun createSimpleLevel(server: MinecraftServer): LevelStem {
        val oldLevel = server.overworld()
        val ops: DynamicOps<Tag> = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess())
        val oldChunkGenerator = oldLevel.chunkSource.generator
        val newChunkGenerator: ChunkGenerator = ChunkGenerator.CODEC.encodeStart(ops, oldChunkGenerator)
            .flatMap { ChunkGenerator.CODEC.parse(ops,it) }
            .getOrThrow { RuntimeException("Error copying dimension: $it") }
        val typeHolder: Holder<DimensionType> = oldLevel.dimensionTypeRegistration()
        return LevelStem(typeHolder, newChunkGenerator)
    }

    private fun createNoiseLevel(server: MinecraftServer,book: ItemStack): LevelStem {

        val dimensionType: Holder<DimensionType> = server.overworld().dimensionTypeRegistration()
        val biomeSource = MultiNoiseBiomeSource.createFromList(
            Climate.ParameterList(
                getBiomeList(book,server)
            )
        )

        val noiseSettings: NoiseSettings = if (Random.nextInt(0,10) == 0) {
            // Make the world sky mountains
            NoiseSettings.create(64,128,1,1)
        } else {
            NoiseSettings.create(-64, 384, 1, 2)
        }

        val surfaceRule = when (Random.nextInt(0,20)) {
            0 -> SurfaceRuleData.nether()
            1 -> SurfaceRuleData.end()
            else -> SurfaceRuleData.overworld()
        }

        val fluid = when(Random.nextInt(0,100)) {
            0 -> Blocks.LAVA.defaultBlockState()
            else -> Blocks.WATER.defaultBlockState()
        }

        val stone = when(Random.nextInt(0,100)) {
            0 -> Blocks.ANDESITE.defaultBlockState()
            1 -> Blocks.DIORITE.defaultBlockState()
            2 -> Blocks.GRANITE.defaultBlockState()
            3 -> Blocks.SMOOTH_QUARTZ.defaultBlockState()
            4 -> Blocks.DEEPSLATE.defaultBlockState()
            5 -> Blocks.ICE.defaultBlockState()
            6 -> Blocks.BLACKSTONE.defaultBlockState()
            7 -> Blocks.END_STONE.defaultBlockState()
            8 -> Blocks.NETHERRACK.defaultBlockState()
            9 -> Blocks.TUFF.defaultBlockState()
            10 -> Blocks.SANDSTONE.defaultBlockState()
            11 -> Blocks.SMOOTH_SANDSTONE.defaultBlockState()
            12 -> Blocks.RED_SANDSTONE.defaultBlockState()
            13 -> Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState()
            14 -> Blocks.PRISMARINE.defaultBlockState()
            15 -> Blocks.BASALT.defaultBlockState()
            16 -> Blocks.SMOOTH_BASALT.defaultBlockState()
            else -> Blocks.STONE.defaultBlockState()
        }

        val seaLevel = when(Random.nextInt(0,100)) {
            0 -> 0
            1 -> 128
            2 -> 256
            else -> 64
        }

        val generator = NoiseBasedChunkGenerator(
            biomeSource,
            Holder.direct(
                NoiseGeneratorSettings(
                    noiseSettings,
                    stone,
                    fluid,
                    OverworldNoiseRouter(
                        server.registryAccess().asGetterLookup().lookupOrThrow(Registries.DENSITY_FUNCTION),
                        server.registryAccess().asGetterLookup().lookupOrThrow(Registries.NOISE),
                        large = Random.nextBoolean()
                    ),
                    surfaceRule,
                    OverworldBiomeBuilder().spawnTarget(),
                    seaLevel,
                    false,
                    true,
                    true,
                    /*useLegacyRandomSource*/ Random.nextBoolean()
                )
            )
        )

        return LevelStem(dimensionType,generator)
    }

    private fun createFlatLevel(server: MinecraftServer,book: ItemStack): LevelStem {
        val dimensionType: Holder<DimensionType> = server.overworld().dimensionTypeRegistration()


        val biomeSource = MultiNoiseBiomeSource.createFromList(
            Climate.ParameterList(
                getBiomeList(book,server)
            )
        )

        val flatNoiseSettings = NoiseSettings.create(-64, 128, 1, 2)

        val generator = NoiseBasedChunkGenerator(
            biomeSource,
            Holder.direct(
                NoiseGeneratorSettings(
                    flatNoiseSettings,
                    Blocks.STONE.defaultBlockState(),
                    Blocks.WATER.defaultBlockState(),
                    flatNoiseRouter(
                        server.registryAccess().asGetterLookup().lookupOrThrow(Registries.DENSITY_FUNCTION),
                        server.registryAccess().asGetterLookup().lookupOrThrow(Registries.NOISE),
                        false
                    ),
                    SurfaceRuleData.overworld(),
                    OverworldBiomeBuilder().spawnTarget(),
                    0,
                    false,
                    false,
                    true,
                    false
                )
            )
        )

        return LevelStem(dimensionType,generator)
    }

    private fun getBiomeList(book: ItemStack,server: MinecraftServer): List<com.mojang.datafixers.util.Pair<ParameterPoint,Holder<Biome>>> {
        val biomeSymbols = book.components.get(MystcraftComponents.BIOME_SYMBOLS.get())

        val biomeList: List<com.mojang.datafixers.util.Pair<ParameterPoint,Holder<Biome>>>

        //TODO: Replace this horrible garbage with a custom BiomeSource that doesn't use ParameterPoints
        if (biomeSymbols is BiomeSymbolsComponent) {
            biomeList = buildList {
                for (symbol in biomeSymbols.biomes) {
                    var biome = server.registryAccess().registryOrThrow(Registries.BIOME).getHolder(symbol).getOrNull()

                    if (biome == null) {
                        LogUtils.getLogger().warn("Found non-existent biome while generating world! Defaulting to plains!")
                        biome = server.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS)
                    }

                    add(
                        com.mojang.datafixers.util.Pair(
                            ParameterPoint(
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Climate.Parameter.span(-2.0f,2.0f),
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Random.nextLong(0,1)
                            ),
                            biome
                        )
                    )
                }
            }
        }
        else {
            biomeList = buildList {
                for (biome in server.registryAccess().registryOrThrow(Registries.BIOME).holders()) {
                    add(
                        com.mojang.datafixers.util.Pair(
                            ParameterPoint(
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Climate.Parameter.span(-2.0f,2.0f),
                                Climate.Parameter.span(-Random.nextFloat(), Random.nextFloat()),
                                Random.nextLong(0,1)
                            ),
                            biome
                        )
                    )
                }
            }
        }

        return biomeList
    }


    fun create(): DimensionIdentificatorCounter {
        return DimensionIdentificatorCounter()
    }

    fun load(tag: CompoundTag, lookupProvider: HolderLookup.Provider):DimensionIdentificatorCounter {
        val counter = this.create()

        if (tag.contains("count", Tag.TAG_INT.toInt())) {
            val count = tag.getInt("count")
            counter.id = count
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
                    Component.literal("  • ")
                        .append(
                            Component.translatable("biome.${biome.toLanguageKey()}")
                                .withStyle(Style.EMPTY.withItalic(true).withColor(0x888888))
                        )
                )
            }
        }
    }

}
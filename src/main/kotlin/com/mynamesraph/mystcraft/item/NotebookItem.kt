package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.component.BiomeSymbolsComponent
import com.mynamesraph.mystcraft.component.IsCreativeSpawnedComponent
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.network.chat.Style

class NotebookItem(properties: Properties) : Item(properties) {

    companion object {
        const val MAX_BIOMES = 16
    }

    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)

        // Creative-spawned notebooks already have all biomes; skip recording
        val creativeSpawned = stack.components.get(MystcraftComponents.IS_CREATIVE_SPAWNED.get())
        if (creativeSpawned is IsCreativeSpawnedComponent && creativeSpawned.generated) {
            return InteractionResultHolder.pass(stack)
        }

        if (!level.isClientSide) {
            val serverLevel = level as? net.minecraft.server.level.ServerLevel
                ?: return InteractionResultHolder.fail(stack)

            // Get the biome the player is currently standing in
            val biomeHolder = serverLevel.getBiome(player.blockPosition())
            val biomeKey: ResourceLocation = biomeHolder.unwrapKey()
                .map { it.location() }
                .orElse(null)
                ?: run {
                    player.displayClientMessage(
                        Component.literal("Could not determine current biome."),
                        true
                    )
                    return InteractionResultHolder.fail(stack)
                }

            val current = stack.components.get(MystcraftComponents.BIOME_SYMBOLS.get())
            val currentList: List<ResourceLocation> = current?.biomes ?: emptyList()

            // Duplicate check
            if (currentList.contains(biomeKey)) {
                player.displayClientMessage(
                    Component.translatable(
                        "mystcraft_reborn.notebook.biome_already_recorded",
                        biomeKey.toString()
                    ),
                    true
                )
                return InteractionResultHolder.fail(stack)
            }

            // Cap check
            if (currentList.size >= MAX_BIOMES) {
                player.displayClientMessage(
                    Component.translatable(
                        "mystcraft_reborn.notebook.biome_list_full",
                        MAX_BIOMES
                    ),
                    true
                )
                return InteractionResultHolder.fail(stack)
            }

            // Record the biome
            val updatedList = currentList + biomeKey
            stack.set(MystcraftComponents.BIOME_SYMBOLS.get(), BiomeSymbolsComponent(updatedList))

            player.displayClientMessage(
                Component.translatable(
                    "mystcraft_reborn.notebook.biome_recorded",
                    biomeKey.toString(),
                    updatedList.size,
                    MAX_BIOMES
                ),
                true
            )
        }

        return InteractionResultHolder.success(stack)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val creativeSpawned = stack.components.get(MystcraftComponents.IS_CREATIVE_SPAWNED.get())
        if (creativeSpawned is IsCreativeSpawnedComponent) {
            tooltipComponents.add(Component.translatable("mystcraft_reborn.tooltip_messages.creative_spawned"))
        }

        val biomes = stack.components.get(MystcraftComponents.BIOME_SYMBOLS.get())
        if (biomes is BiomeSymbolsComponent && biomes.biomes.isNotEmpty()) {
            tooltipComponents.add(
                Component.translatable("mystcraft_reborn.tooltip_messages.biomes", biomes.biomes.count())
                    .withStyle(Style.EMPTY.withItalic(false).withColor(0xAAAAAA))
            )
            biomes.biomes.forEach { biome ->
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

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        if (!level.isClientSide) {
            val server = entity.server!!

            if (stack.components.has(MystcraftComponents.IS_CREATIVE_SPAWNED.get())) {
                if (stack.components.get(MystcraftComponents.IS_CREATIVE_SPAWNED.get())!!.generated) return
                val patchedComponents = PatchedDataComponentMap(stack.components)

                patchedComponents.set(
                    MystcraftComponents.IS_CREATIVE_SPAWNED.get(),
                    IsCreativeSpawnedComponent(true)
                )

                patchedComponents.set(
                    MystcraftComponents.BIOME_SYMBOLS.get(),
                    BiomeSymbolsComponent(
                        server.registryAccess().registryOrThrow(Registries.BIOME).keySet().toList()
                    )
                )
                stack.applyComponentsAndValidate(patchedComponents.asPatch())
            }
        }
    }

}
package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.component.BiomeSymbolsComponent
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

class BiomeEncyclopediaItem(properties: Properties) : Item(properties) {

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
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
}
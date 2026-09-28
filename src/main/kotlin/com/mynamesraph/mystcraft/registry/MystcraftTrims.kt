package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.Util
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.armortrim.TrimMaterial

object MystcraftTrims {
    val PINK_MATERIAL = ResourceKey.create(
        Registries.TRIM_MATERIAL,
        ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID,
            "pink_crystal"
        )
    )

    fun bootstrap(context: BootstrapContext<TrimMaterial>) {
        register(
            context,
            PINK_MATERIAL,
            MystcraftItems.PINK_CRYSTAL.get(),
            Style.EMPTY.withColor(TextColor.parseColor("#e3b7e8").getOrThrow()),
            1.0F
        )
    }


    private fun register(
        context:BootstrapContext<TrimMaterial>,
        trimKey: ResourceKey<TrimMaterial>,
        item: Item,
        style: Style,
        itemModelIndex: Float
    ) {
        val trimMaterial = TrimMaterial.create(
            trimKey.location().path,
            item,
            itemModelIndex,
            Component.translatable(
                Util.makeDescriptionId(
                    "trim_material",
                    trimKey.location()
                )).withStyle(style),
            mapOf()
        )
        context.register(trimKey,trimMaterial)
    }
}
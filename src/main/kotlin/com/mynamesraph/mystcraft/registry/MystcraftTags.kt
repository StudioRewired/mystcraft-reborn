package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item

object MystcraftTags {
    val LINKING_BOOK_TAG: TagKey<Item> = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID,"books/linking_book")
    )

    val NOTEBOOK_TAG: TagKey<Item> = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID,"books/notebook")
    )
}
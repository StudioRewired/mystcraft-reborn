package com.mynamesraph.mystcraft.data.datagen.item

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.registry.MystcraftItems
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.ItemTagsProvider
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.registries.DeferredItem
import java.util.concurrent.CompletableFuture

class MystcraftItemTagProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    blockTags: CompletableFuture<TagLookup<Block>>,
    existingFileHelper: ExistingFileHelper
): ItemTagsProvider(output,lookupProvider,blockTags,Mystcraft.MOD_ID,existingFileHelper) {

    override fun addTags(provider: HolderLookup.Provider) {
        addBookTags(MystcraftItems.LINKING_BOOK)

        addCrystalTags(MystcraftItems.BLUE_CRYSTAL)
        addCrystalTags(MystcraftItems.YELLOW_CRYSTAL)
        addCrystalTags(MystcraftItems.GREEN_CRYSTAL)
        addCrystalTags(MystcraftItems.RED_CRYSTAL)
        addCrystalTags(MystcraftItems.PINK_CRYSTAL)
        tag(ItemTags.TRIM_MATERIALS).add(MystcraftItems.PINK_CRYSTAL.get())
    }


    private fun addCrystalTags(item:DeferredItem<Item>) {
        tag(Tags.Items.GEMS)
            .add(item.get())

        /*tag(ItemTags.TRIM_MATERIALS)
            .add(item.get())*/
    }


    private fun addBookTags(item:DeferredItem<Item>) {
        tag(ItemTags.LECTERN_BOOKS)
            .add(item.get())

        tag(ItemTags.BOOKSHELF_BOOKS)
            .add(item.get())
    }
}
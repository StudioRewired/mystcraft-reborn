package com.mynamesraph.mystcraft.data.datagen.block

import com.mynamesraph.mystcraft.registry.MystcraftBlocks
import com.mynamesraph.mystcraft.registry.MystcraftItems
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.loot.BlockLootSubProvider
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredItem


class MystcraftBlockLootTableProvider(
registries: HolderLookup.Provider
): BlockLootSubProvider(setOf(),FeatureFlags.REGISTRY.allFlags(),registries) {
    override fun generate() {
        dropSelf(MystcraftBlocks.WRITING_DESK.get())

        generateCrystalLoots(
            MystcraftBlocks.BLUE_CRYSTAL_BLOCK,
            MystcraftBlocks.BLUE_CRYSTAL_BLOCK_ITEM,
            MystcraftBlocks.BUDDING_BLUE_CRYSTAL,
            MystcraftBlocks.SMALL_BLUE_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_BLUE_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_BLUE_CRYSTAL_BUD,
            MystcraftBlocks.BLUE_CRYSTAL_CLUSTER,
            MystcraftItems.BLUE_CRYSTAL,
            MystcraftBlocks.BLUE_BOOK_RECEPTACLE
        )

        generateCrystalLoots(
            MystcraftBlocks.YELLOW_CRYSTAL_BLOCK,
            MystcraftBlocks.YELLOW_CRYSTAL_BLOCK_ITEM,
            MystcraftBlocks.BUDDING_YELLOW_CRYSTAL,
            MystcraftBlocks.SMALL_YELLOW_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_YELLOW_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_YELLOW_CRYSTAL_BUD,
            MystcraftBlocks.YELLOW_CRYSTAL_CLUSTER,
            MystcraftItems.YELLOW_CRYSTAL,
            MystcraftBlocks.YELLOW_BOOK_RECEPTACLE
        )

        generateCrystalLoots(
            MystcraftBlocks.GREEN_CRYSTAL_BLOCK,
            MystcraftBlocks.GREEN_CRYSTAL_BLOCK_ITEM,
            MystcraftBlocks.BUDDING_GREEN_CRYSTAL,
            MystcraftBlocks.SMALL_GREEN_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_GREEN_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_GREEN_CRYSTAL_BUD,
            MystcraftBlocks.GREEN_CRYSTAL_CLUSTER,
            MystcraftItems.GREEN_CRYSTAL,
            MystcraftBlocks.GREEN_BOOK_RECEPTACLE
        )

        generateCrystalLoots(
            MystcraftBlocks.PINK_CRYSTAL_BLOCK,
            MystcraftBlocks.PINK_CRYSTAL_BLOCK_ITEM,
            MystcraftBlocks.BUDDING_PINK_CRYSTAL,
            MystcraftBlocks.SMALL_PINK_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_PINK_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_PINK_CRYSTAL_BUD,
            MystcraftBlocks.PINK_CRYSTAL_CLUSTER,
            MystcraftItems.PINK_CRYSTAL,
            MystcraftBlocks.PINK_BOOK_RECEPTACLE
        )

        generateCrystalLoots(
            MystcraftBlocks.RED_CRYSTAL_BLOCK,
            MystcraftBlocks.RED_CRYSTAL_BLOCK_ITEM,
            MystcraftBlocks.BUDDING_RED_CRYSTAL,
            MystcraftBlocks.SMALL_RED_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_RED_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_RED_CRYSTAL_BUD,
            MystcraftBlocks.RED_CRYSTAL_CLUSTER,
            MystcraftItems.RED_CRYSTAL,
            MystcraftBlocks.RED_BOOK_RECEPTACLE
        )
    }

    private fun generateCrystalLoots(
        block:DeferredBlock<Block>,
        blockItem: DeferredItem<BlockItem>,
        budding: DeferredBlock<Block>,
        small: DeferredBlock<Block>,
        medium: DeferredBlock<Block>,
        large: DeferredBlock<Block>,
        cluster: DeferredBlock<Block>,
        item: DeferredItem<Item>,
        receptacle: DeferredBlock<Block>
    ) {
        dropSelf(receptacle.get())
        dropSelf(block.get())
        add(budding.get(),
            dropOtherUnlessSilkTouch(
                budding.get(),
                blockItem.get()
            )
        )
        dropWhenSilkTouch(small.get())
        dropWhenSilkTouch(medium.get())
        dropWhenSilkTouch(large.get())
        add(cluster.get(),
            createMultipleOreDrops(
                cluster.get(),
                item.get(),
                2.0f,
                4.0f
            )
        )
    }

    override fun getKnownBlocks(): MutableIterable<Block> {
        return MystcraftBlocks.BLOCKS.entries.stream().map{
            it.value() as Block
        }.iterator().asSequence().toMutableList()
    }

    private fun dropOtherUnlessSilkTouch(block: Block, item: Item): LootTable.Builder {
        return this.createSilkTouchDispatchTable(
            block,
            LootItem.lootTableItem(item)
        )
    }

    // From : https://github.com/Tutorials-By-Kaupenjoe/NeoForge-Tutorial-1.21.X/blob/11-datagen/src/main/java/net/kaupenjoe/tutorialmod/datagen/ModBlockLootTableProvider.java
    private fun createMultipleOreDrops(
        pBlock: Block,
        item: Item,
        minDrops: Float,
        maxDrops: Float
    ): LootTable.Builder {
        val registryLookup = registries.lookupOrThrow(Registries.ENCHANTMENT)
        return this.createSilkTouchDispatchTable(pBlock,
            this.applyExplosionDecay(pBlock, LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                .apply(ApplyBonusCount.addOreBonusCount(registryLookup.getOrThrow(Enchantments.FORTUNE)))
            )
        )
    }
}
package com.mynamesraph.mystcraft.data.datagen.block

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.registry.MystcraftBlocks
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.BlockTagsProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.registries.DeferredBlock
import java.util.concurrent.CompletableFuture

class MystcraftBlockTagProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    existingFileHelper: ExistingFileHelper?
) : BlockTagsProvider(output, lookupProvider,Mystcraft.MOD_ID, existingFileHelper) {

    override fun addTags(provider: HolderLookup.Provider) {

        tag(BlockTags.MINEABLE_WITH_AXE)
            .add(MystcraftBlocks.WRITING_DESK.get())

        addCrystalBlockTags(
            MystcraftBlocks.BLUE_CRYSTAL_BLOCK,
            MystcraftBlocks.BUDDING_BLUE_CRYSTAL,
            MystcraftBlocks.SMALL_BLUE_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_BLUE_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_BLUE_CRYSTAL_BUD,
            MystcraftBlocks.BLUE_CRYSTAL_CLUSTER,
            MystcraftBlocks.BLUE_BOOK_RECEPTACLE
        )
        
        addCrystalBlockTags(
            MystcraftBlocks.YELLOW_CRYSTAL_BLOCK,
            MystcraftBlocks.BUDDING_YELLOW_CRYSTAL,
            MystcraftBlocks.SMALL_YELLOW_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_YELLOW_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_YELLOW_CRYSTAL_BUD,
            MystcraftBlocks.YELLOW_CRYSTAL_CLUSTER,
            MystcraftBlocks.YELLOW_BOOK_RECEPTACLE
        )

        addCrystalBlockTags(
            MystcraftBlocks.GREEN_CRYSTAL_BLOCK,
            MystcraftBlocks.BUDDING_GREEN_CRYSTAL,
            MystcraftBlocks.SMALL_GREEN_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_GREEN_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_GREEN_CRYSTAL_BUD,
            MystcraftBlocks.GREEN_CRYSTAL_CLUSTER,
            MystcraftBlocks.GREEN_BOOK_RECEPTACLE
        )

        addCrystalBlockTags(
            MystcraftBlocks.PINK_CRYSTAL_BLOCK,
            MystcraftBlocks.BUDDING_PINK_CRYSTAL,
            MystcraftBlocks.SMALL_PINK_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_PINK_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_PINK_CRYSTAL_BUD,
            MystcraftBlocks.PINK_CRYSTAL_CLUSTER,
            MystcraftBlocks.PINK_BOOK_RECEPTACLE

        )

        addCrystalBlockTags(
            MystcraftBlocks.RED_CRYSTAL_BLOCK,
            MystcraftBlocks.BUDDING_RED_CRYSTAL,
            MystcraftBlocks.SMALL_RED_CRYSTAL_BUD,
            MystcraftBlocks.MEDIUM_RED_CRYSTAL_BUD,
            MystcraftBlocks.LARGE_RED_CRYSTAL_BUD,
            MystcraftBlocks.RED_CRYSTAL_CLUSTER,
            MystcraftBlocks.RED_BOOK_RECEPTACLE
        )
    }

    private fun addCrystalBlockTags(
        block: DeferredBlock<Block>,
        budding: DeferredBlock<Block>,
        small: DeferredBlock<Block>,
        medium: DeferredBlock<Block>,
        large: DeferredBlock<Block>,
        cluster: DeferredBlock<Block>,
        receptacle: DeferredBlock<Block>
    ) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(block.get())
            .add(budding.get())
            .add(small.get())
            .add(medium.get())
            .add(large.get())
            .add(cluster.get())
            .add(receptacle.get())
        tag(Tags.Blocks.BUDS)
            .add(small.get())
            .add(medium.get())
            .add(large.get())
        tag(Tags.Blocks.CLUSTERS)
            .add(cluster.get())
        tag(Tags.Blocks.BUDDING_BLOCKS)
            .add(budding.get())
    }
}
package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft.Companion.MOD_ID
import com.mynamesraph.mystcraft.block.crystal.BuddingCrystalBlock
import com.mynamesraph.mystcraft.block.crystal.CrystalBlock
import com.mynamesraph.mystcraft.block.crystal.CrystalClusterBlock
import com.mynamesraph.mystcraft.block.crystal.CrystalColor
import com.mynamesraph.mystcraft.block.editing.EditingTableBlock
import com.mynamesraph.mystcraft.block.receptacle.BookReceptacleBlock
import com.mynamesraph.mystcraft.block.portal.LinkPortalBlock
import com.mynamesraph.mystcraft.block.table.writing.WritingDeskBlock
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.PushReaction
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier
import com.mynamesraph.mystcraft.block.receptacle.BookShreddingReceptacleBlock
import com.mynamesraph.mystcraft.block.printing.PrintingTableBlock
import com.mynamesraph.mystcraft.block.mediaplayer.PictureBookPlayerBlock
import com.mynamesraph.mystcraft.block.receptacle.MediaExportReceptacleBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.item.Item

object MystcraftBlocks {
    val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(
        MOD_ID
    )

    val WRITING_DESK:DeferredBlock<Block> = BLOCKS.register(
        "writing_desk",
        Supplier {
            WritingDeskBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE))
        }
    )

    val WRITING_DESK_ITEM:DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "writing_desk",
        WRITING_DESK
    )

    val LINK_PORTAL: DeferredBlock<Block> = BLOCKS.register(
        "link_portal",
        Supplier {
            LinkPortalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL)
                .randomTicks()
                .requiresCorrectToolForDrops()
                .strength(3.0f, 6.0f))
        }
    )

    val LINK_PORTAL_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "link_portal",
        LINK_PORTAL
    )
    
    ///////////////////
    // BLUE CRYSTAL //
    //////////////////
    val BLUE_CRYSTAL_BLOCK: DeferredBlock<Block> = BLOCKS.register(
        "blue_crystal_block",
        Supplier {
            CrystalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel{5})
        }
    )
    val BLUE_CRYSTAL_BLOCK_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "blue_crystal_block",
        BLUE_CRYSTAL_BLOCK
    )

    val BUDDING_BLUE_CRYSTAL: DeferredBlock<Block> = BLOCKS.register(
        "budding_blue_crystal",
        Supplier {
            BuddingCrystalBlock(BlockBehaviour.Properties.ofFullCopy(
                Blocks.BUDDING_AMETHYST).lightLevel{4}.pushReaction(PushReaction.NORMAL),
                CrystalColor.BLUE
            )
        }
    )
    val BUDDING_BLUE_CRYSTAL_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "budding_blue_crystal",
        BUDDING_BLUE_CRYSTAL
    )

    val SMALL_BLUE_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "small_blue_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                3.0f,
                4.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD).lightLevel{1},
                CrystalColor.BLUE
            )
        }
    )

    val SMALL_BLUE_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "small_blue_crystal_bud",
        SMALL_BLUE_CRYSTAL_BUD
    )

    val MEDIUM_BLUE_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "medium_blue_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                4.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.MEDIUM_AMETHYST_BUD).lightLevel{2},
                CrystalColor.BLUE
            )
        }
    )

    val MEDIUM_BLUE_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "medium_blue_crystal_bud",
        MEDIUM_BLUE_CRYSTAL_BUD
    )

    val LARGE_BLUE_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "large_blue_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                5.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_AMETHYST_BUD).lightLevel{3},
                CrystalColor.BLUE
            )
        }
    )

    val LARGE_BLUE_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "large_blue_crystal_bud",
        LARGE_BLUE_CRYSTAL_BUD
    )

    val BLUE_CRYSTAL_CLUSTER: DeferredBlock<Block> = BLOCKS.register(
        "blue_crystal_cluster",
        Supplier {
            CrystalClusterBlock(
                7.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel{4},
                CrystalColor.BLUE
            )
        }
    )

    val BLUE_CRYSTAL_CLUSTER_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "blue_crystal_cluster",
        BLUE_CRYSTAL_CLUSTER
    )

    val BLUE_BOOK_RECEPTACLE: DeferredBlock<Block> = BLOCKS.register(
        "blue_book_receptacle",
        Supplier {
            BookReceptacleBlock(BlockBehaviour.Properties.ofFullCopy(BLUE_CRYSTAL_BLOCK.get()).noOcclusion())
        }
    )

    val BLUE_BOOK_RECEPTACLE_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "blue_book_receptacle",
        BLUE_BOOK_RECEPTACLE
    )

    //////////////////////////////////////////////////////////////

    ///////////////////
    // YELLOW CRYSTAL //
    //////////////////
    val YELLOW_CRYSTAL_BLOCK: DeferredBlock<Block> = BLOCKS.register(
        "yellow_crystal_block",
        Supplier {
            CrystalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel{5})
        }
    )
    val YELLOW_CRYSTAL_BLOCK_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "yellow_crystal_block",
        YELLOW_CRYSTAL_BLOCK
    )

    val BUDDING_YELLOW_CRYSTAL: DeferredBlock<Block> = BLOCKS.register(
        "budding_yellow_crystal",
        Supplier {
            BuddingCrystalBlock(BlockBehaviour.Properties.ofFullCopy(
                Blocks.BUDDING_AMETHYST).lightLevel{4}.pushReaction(PushReaction.NORMAL),
                CrystalColor.YELLOW
            )
        }
    )
    val BUDDING_YELLOW_CRYSTAL_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "budding_yellow_crystal",
        BUDDING_YELLOW_CRYSTAL
    )

    val SMALL_YELLOW_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "small_yellow_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                3.0f,
                4.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD).lightLevel{1},
                CrystalColor.YELLOW
            )
        }
    )

    val SMALL_YELLOW_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "small_yellow_crystal_bud",
        SMALL_YELLOW_CRYSTAL_BUD
    )

    val MEDIUM_YELLOW_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "medium_yellow_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                4.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.MEDIUM_AMETHYST_BUD).lightLevel{2},
                CrystalColor.YELLOW
            )
        }
    )

    val MEDIUM_YELLOW_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "medium_yellow_crystal_bud",
        MEDIUM_YELLOW_CRYSTAL_BUD
    )

    val LARGE_YELLOW_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "large_yellow_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                5.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_AMETHYST_BUD).lightLevel{3},
                CrystalColor.YELLOW
            )
        }
    )

    val LARGE_YELLOW_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "large_yellow_crystal_bud",
        LARGE_YELLOW_CRYSTAL_BUD
    )

    val YELLOW_CRYSTAL_CLUSTER: DeferredBlock<Block> = BLOCKS.register(
        "yellow_crystal_cluster",
        Supplier {
            CrystalClusterBlock(
                7.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel{4},
                CrystalColor.YELLOW
            )
        }
    )

    val YELLOW_CRYSTAL_CLUSTER_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "yellow_crystal_cluster",
        YELLOW_CRYSTAL_CLUSTER
    )

    val YELLOW_BOOK_RECEPTACLE: DeferredBlock<Block> = BLOCKS.register(
        "yellow_book_receptacle",
        Supplier {
            BookReceptacleBlock(BlockBehaviour.Properties.ofFullCopy(YELLOW_CRYSTAL_BLOCK.get()).noOcclusion())
        }
    )

    val YELLOW_BOOK_RECEPTACLE_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "yellow_book_receptacle",
        YELLOW_BOOK_RECEPTACLE
    )
    //////////////////////////////////////////////////////////////

    ///////////////////
    // GREEN CRYSTAL //
    //////////////////
    val GREEN_CRYSTAL_BLOCK: DeferredBlock<Block> = BLOCKS.register(
        "green_crystal_block",
        Supplier {
            CrystalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel{5})
        }
    )
    val GREEN_CRYSTAL_BLOCK_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "green_crystal_block",
        GREEN_CRYSTAL_BLOCK
    )

    val BUDDING_GREEN_CRYSTAL: DeferredBlock<Block> = BLOCKS.register(
        "budding_green_crystal",
        Supplier {
            BuddingCrystalBlock(BlockBehaviour.Properties.ofFullCopy(
                Blocks.BUDDING_AMETHYST).lightLevel{4}.pushReaction(PushReaction.NORMAL),
                CrystalColor.GREEN
            )
        }
    )
    val BUDDING_GREEN_CRYSTAL_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "budding_green_crystal",
        BUDDING_GREEN_CRYSTAL
    )

    val SMALL_GREEN_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "small_green_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                3.0f,
                4.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD).lightLevel{1},
                CrystalColor.GREEN
            )
        }
    )

    val SMALL_GREEN_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "small_green_crystal_bud",
        SMALL_GREEN_CRYSTAL_BUD
    )

    val MEDIUM_GREEN_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "medium_green_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                4.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.MEDIUM_AMETHYST_BUD).lightLevel{2},
                CrystalColor.GREEN
            )
        }
    )

    val MEDIUM_GREEN_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "medium_green_crystal_bud",
        MEDIUM_GREEN_CRYSTAL_BUD
    )

    val LARGE_GREEN_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "large_green_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                5.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_AMETHYST_BUD).lightLevel{3},
                CrystalColor.GREEN
            )
        }
    )

    val LARGE_GREEN_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "large_green_crystal_bud",
        LARGE_GREEN_CRYSTAL_BUD
    )

    val GREEN_CRYSTAL_CLUSTER: DeferredBlock<Block> = BLOCKS.register(
        "green_crystal_cluster",
        Supplier {
            CrystalClusterBlock(
                7.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel{4},
                CrystalColor.GREEN
            )
        }
    )

    val GREEN_CRYSTAL_CLUSTER_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "green_crystal_cluster",
        GREEN_CRYSTAL_CLUSTER
    )

    val GREEN_BOOK_RECEPTACLE: DeferredBlock<Block> = BLOCKS.register(
        "green_book_receptacle",
        Supplier {
            BookReceptacleBlock(BlockBehaviour.Properties.ofFullCopy(GREEN_CRYSTAL_BLOCK.get()).noOcclusion())
        }
    )

    val GREEN_BOOK_RECEPTACLE_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "green_book_receptacle",
        GREEN_BOOK_RECEPTACLE
    )

    val BOOK_SHREDDING_RECEPTACLE: DeferredBlock<Block> = BLOCKS.register(
        "book_shredding_receptacle",
        Supplier {
            BookShreddingReceptacleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).noOcclusion())
        }
    )

    val BOOK_SHREDDING_RECEPTACLE_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "book_shredding_receptacle",
        BOOK_SHREDDING_RECEPTACLE
    )
    //////////////////////////////////////////////////////////////

    ///////////////////
    // PINK CRYSTAL //
    //////////////////
    val PINK_CRYSTAL_BLOCK: DeferredBlock<Block> = BLOCKS.register(
        "pink_crystal_block",
        Supplier {
            CrystalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel{5})
        }
    )
    val PINK_CRYSTAL_BLOCK_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "pink_crystal_block",
        PINK_CRYSTAL_BLOCK
    )

    val BUDDING_PINK_CRYSTAL: DeferredBlock<Block> = BLOCKS.register(
        "budding_pink_crystal",
        Supplier {
            BuddingCrystalBlock(BlockBehaviour.Properties.ofFullCopy(
                Blocks.BUDDING_AMETHYST).lightLevel{4}.pushReaction(PushReaction.NORMAL),
                CrystalColor.PINK
            )
        }
    )
    val BUDDING_PINK_CRYSTAL_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "budding_pink_crystal",
        BUDDING_PINK_CRYSTAL
    )

    val SMALL_PINK_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "small_pink_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                3.0f,
                4.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD).lightLevel{1},
                CrystalColor.PINK
            )
        }
    )

    val SMALL_PINK_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "small_pink_crystal_bud",
        SMALL_PINK_CRYSTAL_BUD
    )

    val MEDIUM_PINK_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "medium_pink_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                4.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.MEDIUM_AMETHYST_BUD).lightLevel{2},
                CrystalColor.PINK
            )
        }
    )

    val MEDIUM_PINK_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "medium_pink_crystal_bud",
        MEDIUM_PINK_CRYSTAL_BUD
    )

    val LARGE_PINK_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "large_pink_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                5.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_AMETHYST_BUD).lightLevel{3},
                CrystalColor.PINK
            )
        }
    )

    val LARGE_PINK_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "large_pink_crystal_bud",
        LARGE_PINK_CRYSTAL_BUD
    )

    val PINK_CRYSTAL_CLUSTER: DeferredBlock<Block> = BLOCKS.register(
        "pink_crystal_cluster",
        Supplier {
            CrystalClusterBlock(
                7.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel{4},
                CrystalColor.PINK
            )
        }
    )

    val PINK_CRYSTAL_CLUSTER_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "pink_crystal_cluster",
        PINK_CRYSTAL_CLUSTER
    )

    val PINK_BOOK_RECEPTACLE: DeferredBlock<Block> = BLOCKS.register(
        "pink_book_receptacle",
        Supplier {
            BookReceptacleBlock(BlockBehaviour.Properties.ofFullCopy(PINK_CRYSTAL_BLOCK.get()).noOcclusion())
        }
    )

    val PINK_BOOK_RECEPTACLE_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "pink_book_receptacle",
        PINK_BOOK_RECEPTACLE
    )
    //////////////////////////////////////////////////////////////



    ///////////////////
    // RED CRYSTAL //
    //////////////////
    val RED_CRYSTAL_BLOCK: DeferredBlock<Block> = BLOCKS.register(
        "red_crystal_block",
        Supplier {
            CrystalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel{5})
        }
    )
    val RED_CRYSTAL_BLOCK_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "red_crystal_block",
        RED_CRYSTAL_BLOCK
    )

    val BUDDING_RED_CRYSTAL: DeferredBlock<Block> = BLOCKS.register(
        "budding_red_crystal",
        Supplier {
            BuddingCrystalBlock(BlockBehaviour.Properties.ofFullCopy(
                Blocks.BUDDING_AMETHYST).lightLevel{4}.pushReaction(PushReaction.NORMAL),
                CrystalColor.RED
            )
        }
    )
    val BUDDING_RED_CRYSTAL_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "budding_red_crystal",
        BUDDING_RED_CRYSTAL
    )

    val SMALL_RED_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "small_red_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                3.0f,
                4.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD).lightLevel{1},
                CrystalColor.RED
            )
        }
    )

    val SMALL_RED_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "small_red_crystal_bud",
        SMALL_RED_CRYSTAL_BUD
    )

    val MEDIUM_RED_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "medium_red_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                4.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.MEDIUM_AMETHYST_BUD).lightLevel{2},
                CrystalColor.RED
            )
        }
    )

    val MEDIUM_RED_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "medium_red_crystal_bud",
        MEDIUM_RED_CRYSTAL_BUD
    )

    val LARGE_RED_CRYSTAL_BUD: DeferredBlock<Block> = BLOCKS.register(
        "large_red_crystal_bud",
        Supplier {
            CrystalClusterBlock(
                5.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_AMETHYST_BUD).lightLevel{3},
                CrystalColor.RED
            )
        }
    )

    val LARGE_RED_CRYSTAL_BUD_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "large_red_crystal_bud",
        LARGE_RED_CRYSTAL_BUD
    )

    val RED_CRYSTAL_CLUSTER: DeferredBlock<Block> = BLOCKS.register(
        "red_crystal_cluster",
        Supplier {
            CrystalClusterBlock(
                7.0f,
                3.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel{4},
                CrystalColor.RED
            )
        }
    )

    val RED_CRYSTAL_CLUSTER_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "red_crystal_cluster",
        RED_CRYSTAL_CLUSTER
    )

    val RED_BOOK_RECEPTACLE: DeferredBlock<Block> = BLOCKS.register(
        "red_book_receptacle",
        Supplier {
            BookReceptacleBlock(BlockBehaviour.Properties.ofFullCopy(RED_CRYSTAL_BLOCK.get()).noOcclusion())
        }
    )

    val RED_BOOK_RECEPTACLE_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "red_book_receptacle",
        RED_BOOK_RECEPTACLE
    )

    val PRINTING_TABLE: DeferredBlock<Block> = BLOCKS.register(
        "printing_table",
        Supplier {
            PrintingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE))
        }
    )

    val PRINTING_TABLE_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "printing_table",
        PRINTING_TABLE
    )

    val PICTURE_BOOK_PLAYER: DeferredBlock<Block> = BLOCKS.register(
        "picture_book_player",
        Supplier {
            PictureBookPlayerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion())
        }
    )

    val PICTURE_BOOK_PLAYER_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "picture_book_player",
        PICTURE_BOOK_PLAYER
    )

    val MEDIA_EXPORT_RECEPTACLE: DeferredBlock<Block> = BLOCKS.register(
        "media_export_receptacle",
        Supplier {
            MediaExportReceptacleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).noOcclusion())
        }
    )

    val MEDIA_EXPORT_RECEPTACLE_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "media_export_receptacle",
        MEDIA_EXPORT_RECEPTACLE
    )

    val EDITING_TABLE: DeferredBlock<Block> = BLOCKS.register(
        "editing_table",
        Supplier {
            EditingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE))
        }
    )

    val EDITING_TABLE_ITEM: DeferredItem<BlockItem> = MystcraftItems.ITEMS.registerSimpleBlockItem(
        "editing_table",
        EDITING_TABLE
    )

    //////////////////////////////////////////////////////////////

    fun register(eventBus: IEventBus) {
        BLOCKS.register(eventBus)
    }
}
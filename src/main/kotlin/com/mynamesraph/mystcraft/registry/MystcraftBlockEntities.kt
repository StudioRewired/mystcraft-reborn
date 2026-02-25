package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.block.portal.BookReceptacleBlockEntity
import com.mynamesraph.mystcraft.block.portal.LinkPortalBlockEntity
import com.mynamesraph.mystcraft.block.writing.WritingDeskBlockEntity
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier
import com.mynamesraph.mystcraft.block.portal.BookShreddingReceptacleBlockEntity

object MystcraftBlockEntities {
    val BLOCK_ENTITY_TYPES: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(
        Registries.BLOCK_ENTITY_TYPE,
        Mystcraft.MOD_ID
    )

    val LINK_PORTAL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
        "link_portal",
        Supplier {
            BlockEntityType.Builder.of(
                ::LinkPortalBlockEntity,
                MystcraftBlocks.LINK_PORTAL.get()
            ).build(null)
        }
    )

    val BOOK_RECEPTACLE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
        "book_receptacle",
        Supplier {
            BlockEntityType.Builder.of(
                ::BookReceptacleBlockEntity,
                MystcraftBlocks.BLUE_BOOK_RECEPTACLE.get(),
                MystcraftBlocks.YELLOW_BOOK_RECEPTACLE.get(),
                MystcraftBlocks.GREEN_BOOK_RECEPTACLE.get(),
                MystcraftBlocks.PINK_BOOK_RECEPTACLE.get(),
                MystcraftBlocks.RED_BOOK_RECEPTACLE.get()
            ).build(null)
        }
    )

    val WRITING_DESK_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
        "writing_desk",
        Supplier {
            BlockEntityType.Builder.of(
                ::WritingDeskBlockEntity,
                MystcraftBlocks.WRITING_DESK.get(),
            ).build(null)
        }
    )

    val BOOK_SHREDDING_RECEPTACLE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
        "book_shredding_receptacle",
        Supplier {
            BlockEntityType.Builder.of(
                ::BookShreddingReceptacleBlockEntity,
                MystcraftBlocks.BOOK_SHREDDING_RECEPTACLE.get()
            ).build(null)
        }
    )

    fun register(eventBus: IEventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus)
    }
}
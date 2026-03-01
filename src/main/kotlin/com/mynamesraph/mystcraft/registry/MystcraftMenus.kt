package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft.Companion.MOD_ID
import com.mynamesraph.mystcraft.ui.menu.BookBagMenu
import com.mynamesraph.mystcraft.ui.menu.LinkingBookMenu
import com.mynamesraph.mystcraft.ui.menu.WritingDeskMenu
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier
import com.mynamesraph.mystcraft.ui.menu.PrintingTableMenu
import com.mynamesraph.mystcraft.ui.menu.EditingTableMenu
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack

object MystcraftMenus {
    private val MENU_TYPES: DeferredRegister<MenuType<*>> = DeferredRegister.create(
        Registries.MENU,
        MOD_ID
    )

    val LINKING_BOOK_MENU: Supplier<MenuType<LinkingBookMenu>> = MENU_TYPES.register(
        "linking_book_menu",
        Supplier {
            IMenuTypeExtension.create(::LinkingBookMenu)
        }
    )

    val WRITING_DESK_MENU: Supplier<MenuType<WritingDeskMenu>> = MENU_TYPES.register(
        "writing_desk_menu",
        Supplier {
            IMenuTypeExtension.create(::WritingDeskMenu)
        }
    )

    val PRINTING_TABLE_MENU: Supplier<MenuType<PrintingTableMenu>> = MENU_TYPES.register(
        "printing_table_menu",
        Supplier {
            IMenuTypeExtension.create(::PrintingTableMenu)
        }
    )

    val EDITING_TABLE_MENU: Supplier<MenuType<EditingTableMenu>> = MENU_TYPES.register(
        "editing_table_menu",
        Supplier {
            IMenuTypeExtension.create(::EditingTableMenu)
        }
    )

    val BOOK_BAG_MENU: Supplier<MenuType<BookBagMenu>> = MENU_TYPES.register(
        "book_bag_menu",
        Supplier {
            IMenuTypeExtension.create { id, inv, buf -> BookBagMenu(id, inv, ItemStack.EMPTY, InteractionHand.MAIN_HAND) }
        }
    )


    fun register(eventBus: IEventBus) {
        MENU_TYPES.register(eventBus)
    }
}
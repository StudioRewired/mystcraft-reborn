package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft.Companion.MOD_ID
import com.mynamesraph.mystcraft.ui.menu.LinkingBookMenu
import com.mynamesraph.mystcraft.ui.menu.WritingDeskMenu
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

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

    fun register(eventBus: IEventBus) {
        MENU_TYPES.register(eventBus)
    }
}
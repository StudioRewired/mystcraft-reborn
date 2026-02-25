package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft.Companion.MOD_ID
import com.mynamesraph.mystcraft.component.IsCreativeSpawnedComponent
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters
import net.minecraft.world.item.Items
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object MystcraftTabs {
    private val CREATIVE_MODE_TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(
        Registries.CREATIVE_MODE_TAB,
        MOD_ID
    )

    val MYSTCRAFT_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> = CREATIVE_MODE_TABS.register("mystcraft_reborn_tab",
        Supplier {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mystcraft_reborn"))
                .icon { MystcraftItems.LINKING_BOOK.get().defaultInstance }
                .displayItems { parameters: ItemDisplayParameters?, output: CreativeModeTab.Output ->
                    output.accept(Items.BOOK)

                    for (item in MystcraftItems.ITEMS.entries) {
                        output.accept(item.get())
                    }

                    val patchedComponents = PatchedDataComponentMap(MystcraftItems.NOTEBOOK.get().components())

                    patchedComponents.set(
                        MystcraftComponents.IS_CREATIVE_SPAWNED.get(),
                        IsCreativeSpawnedComponent()
                    )

                    val creativeNotebook =MystcraftItems.NOTEBOOK.get().defaultInstance
                    creativeNotebook.applyComponentsAndValidate(patchedComponents.asPatch())

                    output.accept(creativeNotebook)
                }.build()
        })

    fun register(eventBus: IEventBus) {
        CREATIVE_MODE_TABS.register(eventBus)
    }
}
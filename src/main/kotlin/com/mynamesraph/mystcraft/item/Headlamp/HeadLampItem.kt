package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.registry.MystcraftItems.ITEMS
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.core.Holder
import net.minecraft.world.item.ArmorMaterials
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.neoforged.neoforge.registries.DeferredItem
import java.util.function.Supplier

class HeadlampItem(material: Holder<ArmorMaterial>, properties: Properties) :
    ArmorItem(material, Type.HELMET, properties)

val HEADLAMP: DeferredItem<Item> = ITEMS.register(
    "headlamp",
    Supplier {
        HeadlampItem(
            ArmorMaterials.CHAIN,
            Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
        )
    }
)
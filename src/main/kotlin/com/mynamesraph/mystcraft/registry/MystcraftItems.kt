package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft.Companion.MOD_ID
import com.mynamesraph.mystcraft.component.*
import com.mynamesraph.mystcraft.item.DescriptiveBookItem
import com.mynamesraph.mystcraft.item.LinkingBookItem
import com.mynamesraph.mystcraft.item.NotebookItem
import com.mynamesraph.mystcraft.registry.MystcraftComponents.LOCATION
import com.mynamesraph.mystcraft.registry.MystcraftComponents.ROTATION
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import org.joml.Vector3f
import java.util.function.Supplier
import com.mynamesraph.mystcraft.RewindTool.RewindBookItem
import com.mynamesraph.mystcraft.item.ColoringBookItem
import com.mynamesraph.mystcraft.item.CameraItem
import com.mynamesraph.mystcraft.item.PictureBookItem

object MystcraftItems {
    val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(
        MOD_ID
    )

    val NOTEBOOK: DeferredItem<Item> = ITEMS.register(
        "notebook",
        Supplier {
            NotebookItem(
                Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .component(
                        MystcraftComponents.BIOME_SYMBOLS,
                        BiomeSymbolsComponent(
                            emptyList()
                        )
                    )
            )
        }
    )

    val LINKING_BOOK: DeferredItem<Item> = ITEMS.register(
        "linking_book",
        Supplier {
            LinkingBookItem(
                Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .component(
                        MystcraftComponents.LOCATION_DISPLAY,
                        LocationDisplayComponent(
                            Component.literal("Overworld Origin")
                                .withStyle(Style.EMPTY.withItalic(false).withColor(0xAAAAAA))
                        )
                    )
                    .component(
                        LOCATION, LocationComponent(ServerLevel.OVERWORLD, Vector3f(0.0f, 0.0f, 0.0f))
                    )
                    .component(
                        ROTATION, RotationComponent(0.0f,0.0f)
                    )
            )
        }
    )

    val DESCRIPTIVE_BOOK: DeferredItem<Item> = ITEMS.register(
        "descriptive_book",
        Supplier {
            DescriptiveBookItem(
                Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .component(
                        MystcraftComponents.DIMENSION_ID,
                        DimensionIdentificatorComponent(false,ResourceLocation.fromNamespaceAndPath(MOD_ID,"unknown_age"))
                    )
                    .component(
                        MystcraftComponents.LOCATION_DISPLAY,
                        LocationDisplayComponent(
                            Component.translatable("mystcraft_reborn.unknown_age")
                                .withStyle(Style.EMPTY.withItalic(false).withColor(0xAAAAAA))
                        )
                    )
                    .component(
                        LOCATION, LocationComponent(ServerLevel.OVERWORLD, Vector3f(0.0f, 0.0f, 0.0f))
                    )
                    .component(
                        ROTATION, RotationComponent(0.0f,0.0f)
                    )
            )
        }
    )

    val REWIND_BOOK: DeferredItem<Item> = ITEMS.register(
        "rewind_book",
        Supplier {
            RewindBookItem(
                Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
            )
        }
    )


    val COLORING_BOOK: DeferredItem<Item> = ITEMS.register(
        "coloring_book",
        Supplier {
            ColoringBookItem(
                Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
            )
        }
    )

    val BLUE_CRYSTAL: DeferredItem<Item> = ITEMS.registerSimpleItem(
        "blue_crystal",
        Item.Properties().stacksTo(64)
    )

    val YELLOW_CRYSTAL: DeferredItem<Item> = ITEMS.registerSimpleItem(
        "yellow_crystal",
        Item.Properties().stacksTo(64)
    )

    val GREEN_CRYSTAL: DeferredItem<Item> = ITEMS.registerSimpleItem(
        "green_crystal",
        Item.Properties().stacksTo(64)
    )

    val PINK_CRYSTAL: DeferredItem<Item> = ITEMS.registerSimpleItem(
        "pink_crystal",
        Item.Properties().stacksTo(64)
    )

    val RED_CRYSTAL: DeferredItem<Item> = ITEMS.registerSimpleItem(
        "red_crystal",
        Item.Properties().stacksTo(64)
    )
    
    fun register(eventBus: IEventBus) {
        ITEMS.register(eventBus)
    }

    val PICTURE_BOOK: DeferredItem<Item> = ITEMS.register(
        "picture_book",
        Supplier {
            PictureBookItem(
                Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
            )
        }
    )

    val CAMERA: DeferredItem<Item> = ITEMS.register(
        "camera",
        Supplier {
            CameraItem(
                Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
            )
        }
    )
}
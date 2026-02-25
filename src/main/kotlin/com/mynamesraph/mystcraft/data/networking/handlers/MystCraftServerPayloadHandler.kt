package com.mynamesraph.mystcraft.data.networking.handlers

import com.mojang.logging.LogUtils
import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.block.portal.BookShreddingReceptacleBlockEntity
import com.mynamesraph.mystcraft.block.writing.WritingDeskBlockEntity
import com.mynamesraph.mystcraft.component.BiomeSymbolsComponent
import com.mynamesraph.mystcraft.component.LocationComponent
import com.mynamesraph.mystcraft.component.LocationDisplayComponent
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import com.mynamesraph.mystcraft.component.RotationComponent
import com.mynamesraph.mystcraft.data.networking.packet.AgeDeleteConfirmedPacket
import com.mynamesraph.mystcraft.data.networking.packet.LinkingBookLecternTravelPacket
import com.mynamesraph.mystcraft.data.networking.packet.LinkingBookTravelPacket
import com.mynamesraph.mystcraft.data.networking.packet.SendPreviewImagePacket
import com.mynamesraph.mystcraft.data.networking.packet.WritingDeskRenamePacket
import com.mynamesraph.mystcraft.data.networking.packet.WritingDeskSymbolPacket
import com.mynamesraph.mystcraft.item.LinkingBookItem
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.registry.MystcraftItems
import net.commoble.infiniverse.api.InfiniverseAPI
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.neoforged.neoforge.network.handling.IPayloadContext
import com.mynamesraph.mystcraft.item.DescriptiveBookItem
import com.mynamesraph.mystcraft.component.CameraPhotoComponent
import com.mynamesraph.mystcraft.data.networking.packet.SendCameraPhotoPacket
import com.mynamesraph.mystcraft.item.CameraItem
import com.mynamesraph.mystcraft.data.networking.packet.SendCameraVideoPacket

class MystCraftServerPayloadHandler {
    companion object {

        fun handleWritingDeskAddingSymbol(data: WritingDeskSymbolPacket, context: IPayloadContext) {
            when (data.type) {
                "BIOME" -> handleWritingDeskAddingBiomeSymbol(data, context)
                "REMOVE_BIOME" -> handleWritingDeskRemovingBiomeSymbol(data, context)
                else -> LogUtils.getLogger().error("MystCraftServerPayloadHandler#handleWritingDeskAddingSymbol: Unknown symbol type: ${data.type}")
            }
        }

        private fun handleWritingDeskRemovingBiomeSymbol(data: WritingDeskSymbolPacket, context: IPayloadContext) {
            val player = context.player()
            val level = player.level()
            val writingDesk = level.getBlockEntity(data.writingDeskPos)

            if (writingDesk is WritingDeskBlockEntity) {
                val item = writingDesk.container.getStackInSlot(0)

                if (item.`is`(MystcraftItems.DESCRIPTIVE_BOOK)) {
                    val patched = PatchedDataComponentMap(item.components)
                    val biomeSymbols = item.components.get(MystcraftComponents.BIOME_SYMBOLS.get())

                    if (biomeSymbols is BiomeSymbolsComponent) {
                        patched.set(
                            MystcraftComponents.BIOME_SYMBOLS.get(),
                            BiomeSymbolsComponent(
                                biomeSymbols.biomes.filter { it != data.symbol }
                            )
                        )
                        item.applyComponentsAndValidate(patched.asPatch())
                    }
                }
            }
        }

        private fun handleWritingDeskAddingBiomeSymbol(data: WritingDeskSymbolPacket, context: IPayloadContext) {
            val player = context.player()
            val level = player.level()
            val writingDesk = level.getBlockEntity(data.writingDeskPos)

            if (writingDesk is WritingDeskBlockEntity) {
                val item = writingDesk.container.getStackInSlot(0)

                if (item.`is`(MystcraftItems.DESCRIPTIVE_BOOK)) {
                    val patched = PatchedDataComponentMap(item.components)
                    val biomeSymbols = item.components.get(MystcraftComponents.BIOME_SYMBOLS.get())

                    if (biomeSymbols is BiomeSymbolsComponent) {
                        patched.set(
                            MystcraftComponents.BIOME_SYMBOLS.get(),
                            BiomeSymbolsComponent(
                                listOf(
                                    *biomeSymbols.biomes.toTypedArray(),
                                    data.symbol
                                )
                            )
                        )
                    } else {
                        patched.set(
                            MystcraftComponents.BIOME_SYMBOLS.get(),
                            BiomeSymbolsComponent(listOf(data.symbol))
                        )
                    }

                    item.applyComponentsAndValidate(patched.asPatch())
                }
            }
        }

        fun handleWritingDeskRenamedBook(data: WritingDeskRenamePacket, context: IPayloadContext) {
            val player = context.player()
            val level = player.level()
            val writingDesk = level.getBlockEntity(data.writingDeskPos)

            if (writingDesk is WritingDeskBlockEntity) {
                val item = writingDesk.container.getStackInSlot(0)

                if (item.has(MystcraftComponents.LOCATION_DISPLAY)) {
                    val patchedComponents = PatchedDataComponentMap(item.components)

                    if (patchedComponents.get(MystcraftComponents.LOCATION_DISPLAY.get()) != null) {
                        patchedComponents.set(
                            MystcraftComponents.LOCATION_DISPLAY.get(),
                            LocationDisplayComponent(
                                Component.literal(data.name).withStyle(
                                    Style.EMPTY.withItalic(false).withColor(0xAAAAAA)
                                )
                            )
                        )
                    }

                    item.applyComponentsAndValidate(patchedComponents.asPatch())
                }
            }
        }

        fun handleLinkingBookButtonPressed(data: LinkingBookTravelPacket, context: IPayloadContext) {
            val player = context.player()
            val level = player.level()
            val item = player.getItemInHand(data.interactionHand).item

            println("Received Packet From ${player.uuid}: $data")

            if (item is LinkingBookItem) {
                item.teleportToLocationFromHand(level, player, data.interactionHand)
            }
        }

        fun handleLecternLinkingBookButtonPressed(data: LinkingBookLecternTravelPacket, context: IPayloadContext) {
            val player = context.player()
            val level = player.level()
            val lectern = level.getBlockEntity(data.pos)

            if (lectern is LecternBlockEntity) {
                val book = lectern.book.item

                if (book is LinkingBookItem) {
                    val location = lectern.book.components.get(
                        BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "location")
                        )!!
                    )

                    val rotation = lectern.book.components.get(
                        BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "rotation")
                        )!!
                    )

                    if (location is LocationComponent && rotation is RotationComponent) {
                        println("location:$location, rotation:$rotation")
                        book.teleportToLocationFromLectern(level, player, location, rotation)
                    }
                }
            }
        }

        fun handleCameraPhotoReceived(data: SendCameraPhotoPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val inventory = player.inventory

                for (i in 0 until inventory.containerSize) {
                    val stack = inventory.getItem(i)
                    if (stack.item is com.mynamesraph.mystcraft.item.PictureBookItem
                        && !stack.has(MystcraftComponents.PREVIEW_IMAGE)
                    ) {
                        stack.set(
                            MystcraftComponents.PREVIEW_IMAGE.get(),
                            PreviewImageComponent(listOf(data.jpeg))
                        )
                        player.displayClientMessage(
                            Component.literal("Photo saved to picture book."),
                            true
                        )
                        return@enqueueWork
                    }
                }

                // No empty picture book found — photo is lost
                player.displayClientMessage(
                    Component.literal("No empty picture book in inventory. Photo lost."),
                    true
                )
            }
        }

        fun handleCameraVideoReceived(data: SendCameraVideoPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val inventory = player.inventory

                for (i in 0 until inventory.containerSize) {
                    val stack = inventory.getItem(i)
                    if (stack.item is com.mynamesraph.mystcraft.item.PictureBookItem
                        && !stack.has(MystcraftComponents.PREVIEW_IMAGE)
                    ) {
                        stack.set(
                            MystcraftComponents.PREVIEW_IMAGE.get(),
                            PreviewImageComponent(data.frames)
                        )
                        player.displayClientMessage(
                            Component.literal("Video saved to picture book."),
                            true
                        )
                        return@enqueueWork
                    }
                }

                player.displayClientMessage(
                    Component.literal("No empty picture book in inventory. Video lost."),
                    true
                )
            }
        }

        fun handleAgeDeleteConfirmed(data: AgeDeleteConfirmedPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val server = player.server ?: return@enqueueWork

                val protectedDimensions = setOf(
                    ServerLevel.OVERWORLD.location(),
                    ServerLevel.NETHER.location(),
                    ServerLevel.END.location()
                )

                if (data.dimensionId in protectedDimensions) {
                    player.displayClientMessage(
                        Component.literal("This dimension cannot be deleted."),
                        true
                    )
                    return@enqueueWork
                }

                val dimensionKey = ResourceKey.create(Registries.DIMENSION, data.dimensionId)
                InfiniverseAPI.get().markDimensionForUnregistration(server, dimensionKey)

                val level = player.level()
                val be = level.getBlockEntity(data.receptaclePos)
                if (be is BookShreddingReceptacleBlockEntity) {
                    be.clearContent()
                }

                player.displayClientMessage(
                    Component.literal("Age \"${data.dimensionId.path}\" has been deleted."),
                    false
                )
            }
        }

        @Suppress("unused")
        fun handlePreviewImageReceived(data: SendPreviewImagePacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val inventory = player.inventory
                for (i in 0 until inventory.containerSize) {
                    val stack = inventory.getItem(i)
                    if (stack.item is LinkingBookItem
                        && stack.item !is DescriptiveBookItem
                        && !stack.has(MystcraftComponents.PREVIEW_IMAGE)
                    ) {
                        stack.set(
                            MystcraftComponents.PREVIEW_IMAGE.get(),
                            PreviewImageComponent(data.frames)
                        )
                        break  // First book only
                    }
                }
            }
        }

    } // end companion object
} // end class
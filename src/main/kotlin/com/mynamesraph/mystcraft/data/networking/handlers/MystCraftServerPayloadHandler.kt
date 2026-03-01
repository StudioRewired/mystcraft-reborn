package com.mynamesraph.mystcraft.data.networking.handlers

import com.mojang.logging.LogUtils
import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.block.editing.EditingTableBlockEntity
import com.mynamesraph.mystcraft.block.receptacle.BookShreddingReceptacleBlockEntity
import com.mynamesraph.mystcraft.block.table.writing.WritingDeskBlockEntity
import com.mynamesraph.mystcraft.component.BiomeSymbolsComponent
import com.mynamesraph.mystcraft.component.LocationComponent
import com.mynamesraph.mystcraft.component.LocationDisplayComponent
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import com.mynamesraph.mystcraft.component.RotationComponent
import com.mynamesraph.mystcraft.data.networking.packet.AgeDeleteConfirmedPacket
import com.mynamesraph.mystcraft.data.networking.packet.EditingTableConfirmPacket
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
import com.mynamesraph.mystcraft.data.networking.packet.SendCameraPhotoPacket
import com.mynamesraph.mystcraft.data.networking.packet.SendCameraVideoPacket
import com.mynamesraph.mystcraft.data.networking.packet.PictureBookPlayerUpdatePacket
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

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

                // Find paper in inventory
                val paperSlot = (0 until inventory.containerSize).firstOrNull { i ->
                    inventory.getItem(i).item == Items.PAPER
                }

                if (paperSlot == null) {
                    player.displayClientMessage(
                        Component.literal("No paper in inventory. Photo lost."),
                        true
                    )
                    return@enqueueWork
                }

                // Consume one paper
                inventory.getItem(paperSlot).shrink(1)

                // Give picture book with photo
                val pictureBook = ItemStack(MystcraftItems.PICTURE_BOOK.get())
                pictureBook.set(
                    MystcraftComponents.PREVIEW_IMAGE.get(),
                    PreviewImageComponent(listOf(data.jpeg))
                )
                player.inventory.add(pictureBook)
                player.displayClientMessage(Component.literal("Photo saved to picture book."), true)
            }
        }

        fun handleCameraVideoReceived(data: SendCameraVideoPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val inventory = player.inventory

                val paperSlot = (0 until inventory.containerSize).firstOrNull { i ->
                    inventory.getItem(i).item == Items.PAPER
                }

                if (paperSlot == null) {
                    player.displayClientMessage(
                        Component.literal("No paper in inventory. Video lost."),
                        true
                    )
                    return@enqueueWork
                }

                inventory.getItem(paperSlot).shrink(1)

                val pictureBook = ItemStack(MystcraftItems.PICTURE_BOOK.get())
                pictureBook.set(
                    MystcraftComponents.PREVIEW_IMAGE.get(),
                    PreviewImageComponent(data.frames)
                )
                player.inventory.add(pictureBook)
                player.displayClientMessage(Component.literal("Video saved to picture book."), true)
            }
        }

        fun handlePictureBookPlayerUpdate(data: PictureBookPlayerUpdatePacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val level = player.level()
                val be = level.getBlockEntity(data.pos) as?
                        com.mynamesraph.mystcraft.block.mediaplayer.PictureBookPlayerBlockEntity ?: return@enqueueWork

                if (data.removeBook) {
                    be.clearLightBlocks(level)
                    val stack = be.removeBook()
                    if (!stack.isEmpty) {
                        val state = level.getBlockState(data.pos)
                        val facing = state.getValue(BlockStateProperties.FACING)
                        // UP/DOWN have no meaningful stepX/Z for ejection, so just drop above the block
                        val ejectX = if (facing.axis.isHorizontal) facing.stepX * 0.7 else 0.0
                        val ejectY = if (facing == Direction.UP) 1.0 else 0.5
                        val ejectZ = if (facing.axis.isHorizontal) facing.stepZ * 0.7 else 0.0
                        val entity = ItemEntity(
                            level,
                            data.pos.x + 0.5 + ejectX,
                            data.pos.y + ejectY,
                            data.pos.z + 0.5 + ejectZ,
                            stack
                        )
                        entity.setDefaultPickUpDelay()
                        level.addFreshEntity(entity)
                    }
                } else {
                    be.displayWidth     = data.width
                    be.displayHeight    = data.height
                    be.backlightOn      = data.backlight
                    be.verticalOffset   = data.verticalOffset
                    be.horizontalOffset = data.horizontalOffset
                    be.clearLightBlocks(level)
                    be.placeLightBlocks(level)
                    be.rotation = data.rotation
                }

                level.sendBlockUpdated(data.pos, level.getBlockState(data.pos), level.getBlockState(data.pos), 3)
            }
        }

        fun handleEditingTableConfirm(data: EditingTableConfirmPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val level = player.level()
                val be = level.getBlockEntity(data.pos) as? EditingTableBlockEntity ?: return@enqueueWork

                val output = be.confirmAndBuild()
                println("DEBUG confirm: output=$output, hasWGP=${output.has(MystcraftComponents.WORLDGEN_PARAMETERS)}, hasDimID=${output.has(MystcraftComponents.DIMENSION_ID)}")

                if (!output.isEmpty) {
                    if (!player.inventory.add(output)) {
                        player.drop(output, false)
                    }
                }
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
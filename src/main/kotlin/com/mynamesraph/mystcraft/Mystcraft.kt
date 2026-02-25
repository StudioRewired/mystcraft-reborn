package com.mynamesraph.mystcraft

import com.mojang.logging.LogUtils
import com.mynamesraph.mystcraft.RewindTool.MystcraftAttachments
import com.mynamesraph.mystcraft.block.portal.LinkPortalBlock
import com.mynamesraph.mystcraft.data.networking.handlers.MystCraftClientPayloadHandler
import com.mynamesraph.mystcraft.data.networking.handlers.MystCraftServerPayloadHandler
import com.mynamesraph.mystcraft.data.networking.packet.AgeDeleteConfirmedPacket
import com.mynamesraph.mystcraft.data.networking.packet.ConfirmAgeDeletePacket
import com.mynamesraph.mystcraft.data.networking.packet.LinkingBookLecternTravelPacket
import com.mynamesraph.mystcraft.data.networking.packet.LinkingBookTravelPacket
import com.mynamesraph.mystcraft.data.networking.packet.SendPreviewImagePacket
import com.mynamesraph.mystcraft.data.networking.packet.TriggerPreviewCapturePacket
import com.mynamesraph.mystcraft.data.networking.packet.WritingDeskRenamePacket
import com.mynamesraph.mystcraft.data.networking.packet.WritingDeskSymbolPacket
import com.mynamesraph.mystcraft.events.LinkingBookCraftEvents
import com.mynamesraph.mystcraft.registry.*
import com.mynamesraph.mystcraft.registry.MystcraftMenus.LINKING_BOOK_MENU
import com.mynamesraph.mystcraft.registry.MystcraftMenus.WRITING_DESK_MENU
import com.mynamesraph.mystcraft.ui.screen.LecternLinkingBookScreen
import com.mynamesraph.mystcraft.ui.screen.WritingDeskScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import com.mynamesraph.mystcraft.data.networking.packet.SendCameraPhotoPacket
import com.mynamesraph.mystcraft.events.CameraEvents
import com.mynamesraph.mystcraft.data.networking.packet.SendCameraVideoPacket

@Mod(Mystcraft.MOD_ID)
class Mystcraft(modEventBus: IEventBus, modContainer: ModContainer) {
    companion object {
        const val MOD_ID = "mystcraft_reborn"
        private val LOGGER = LogUtils.getLogger()

        // Handles packet registration for both sides symmetrically.
        // playToClient lambdas are only ever *invoked* on the client,
        // so referencing MystCraftClientPayloadHandler here is safe —
        // the lambda body is not classloaded on the server.
        @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
        object CommonModEvents {
            @SubscribeEvent
            fun register(event: RegisterPayloadHandlersEvent) {
                val registrar: PayloadRegistrar = event.registrar("1")

                registrar.playToServer(
                    LinkingBookTravelPacket.TYPE,
                    LinkingBookTravelPacket.STREAM_CODEC,
                    MystCraftServerPayloadHandler::handleLinkingBookButtonPressed
                )
                registrar.playToServer(
                    LinkingBookLecternTravelPacket.TYPE,
                    LinkingBookLecternTravelPacket.STREAM_CODEC,
                    MystCraftServerPayloadHandler::handleLecternLinkingBookButtonPressed
                )
                registrar.playToServer(
                    WritingDeskRenamePacket.TYPE,
                    WritingDeskRenamePacket.STREAM_CODEC,
                    MystCraftServerPayloadHandler::handleWritingDeskRenamedBook
                )
                registrar.playToServer(
                    WritingDeskSymbolPacket.TYPE,
                    WritingDeskSymbolPacket.STREAM_CODEC,
                    MystCraftServerPayloadHandler::handleWritingDeskAddingSymbol
                )
                registrar.playToServer(
                    AgeDeleteConfirmedPacket.TYPE,
                    AgeDeleteConfirmedPacket.STREAM_CODEC,
                    MystCraftServerPayloadHandler::handleAgeDeleteConfirmed
                )
                registrar.playToServer(
                    SendPreviewImagePacket.TYPE,
                    SendPreviewImagePacket.STREAM_CODEC,
                    { data, context -> MystCraftServerPayloadHandler.handlePreviewImageReceived(data, context) }
                )
                registrar.playToClient(
                    ConfirmAgeDeletePacket.TYPE,
                    ConfirmAgeDeletePacket.STREAM_CODEC,
                    { data, context -> MystCraftClientPayloadHandler.handleConfirmAgeDelete(data, context) }
                )
                registrar.playToClient(
                    TriggerPreviewCapturePacket.TYPE,
                    TriggerPreviewCapturePacket.STREAM_CODEC,
                    { data, context -> MystCraftClientPayloadHandler.handleTriggerPreviewCapture(data, context) }
                )

                registrar.playToServer(
                    SendCameraPhotoPacket.TYPE,
                    SendCameraPhotoPacket.STREAM_CODEC,
                    { data, context -> MystCraftServerPayloadHandler.handleCameraPhotoReceived(data, context) }
                )

                registrar.playToServer(
                    SendCameraVideoPacket.TYPE,
                    SendCameraVideoPacket.STREAM_CODEC,
                    { data, context -> MystCraftServerPayloadHandler.handleCameraVideoReceived(data, context) }
                )
            }
        }

        // Client-only events: screens, colors, client setup
        @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
        object ClientModEvents {
            @SubscribeEvent
            fun onClientSetup(event: FMLClientSetupEvent?) {
                // Client setup code
            }

            @SubscribeEvent
            fun registerScreens(event: RegisterMenuScreensEvent) {
                event.register(LINKING_BOOK_MENU.get(), ::LecternLinkingBookScreen)
                event.register(WRITING_DESK_MENU.get(), ::WritingDeskScreen)
            }

            @SubscribeEvent
            fun registerBlockColorHandlers(event: RegisterColorHandlersEvent.Block) {
                event.register(
                    { blockState: BlockState, _: BlockAndTintGetter?, _: BlockPos?, _: Int ->
                        if (blockState.block is LinkPortalBlock) {
                            if (ModList.get().isLoaded("past_el_palettes")) {
                                if (blockState.getValue(LinkPortalBlock.IS_PASTEL_COLOR)) {
                                    blockState.getValue(LinkPortalBlock.PASTEL_COLOR!!).textColor
                                } else {
                                    blockState.getValue(LinkPortalBlock.COLOR).textColor
                                }
                            } else {
                                blockState.getValue(LinkPortalBlock.COLOR).textColor
                            }
                        } else {
                            DyeColor.BLACK.textColor
                        }
                    },
                    MystcraftBlocks.LINK_PORTAL.get()
                )
            }
        }
    }

    init {
        modEventBus.addListener(::commonSetup)
        MystcraftBlocks.register(modEventBus)
        MystcraftComponents.register(modEventBus)
        MystcraftItems.register(modEventBus)
        MystcraftBlockEntities.register(modEventBus)
        MystcraftTabs.register(modEventBus)
        MystcraftMenus.register(modEventBus)
        MystcraftRecipes.register(modEventBus)
        NeoForge.EVENT_BUS.register(this)
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC)
        MystcraftAttachments.REGISTRY.register(modEventBus)
        if (FMLEnvironment.dist.isClient) {
            NeoForge.EVENT_BUS.register(LinkingBookCraftEvents)
        }
        if (FMLEnvironment.dist.isClient) {
            NeoForge.EVENT_BUS.register(LinkingBookCraftEvents)
            NeoForge.EVENT_BUS.register(CameraEvents)  // add this
        }

    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
        // Common setup code
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        // Do something when the server starts
    }


}
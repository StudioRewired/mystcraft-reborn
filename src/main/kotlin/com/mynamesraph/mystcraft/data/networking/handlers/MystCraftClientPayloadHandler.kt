package com.mynamesraph.mystcraft.data.networking.handlers

import com.mynamesraph.mystcraft.client.LinkingBookPreviewCapture
import com.mynamesraph.mystcraft.data.networking.packet.ConfirmAgeDeletePacket
import com.mynamesraph.mystcraft.data.networking.packet.TriggerPreviewCapturePacket
import com.mynamesraph.mystcraft.ui.screen.ConfirmAgeDeleteScreen
import net.minecraft.client.Minecraft
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.bus.api.SubscribeEvent
import com.mynamesraph.mystcraft.data.networking.packet.OpenLecternScreenPacket
import com.mynamesraph.mystcraft.ui.screen.LecternLinkingBookScreen
import com.mynamesraph.mystcraft.component.LocationDisplayComponent
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import net.minecraft.network.chat.Component


class MystCraftClientPayloadHandler {
    companion object {
        fun handleConfirmAgeDelete(data: ConfirmAgeDeletePacket, context: IPayloadContext) {
            context.enqueueWork {
                Minecraft.getInstance().setScreen(
                    ConfirmAgeDeleteScreen(data.dimensionId, data.receptaclePos)
                )
            }
        }

        fun handleTriggerPreviewCapture(data: TriggerPreviewCapturePacket, context: IPayloadContext) {
            // No longer used — capture is triggered by screen close event instead
        }


        fun handleOpenLecternScreen(data: OpenLecternScreenPacket, context: IPayloadContext) {
            context.enqueueWork {
                val mc = Minecraft.getInstance()
                val locationDisplay = data.bookStack.components.get(MystcraftComponents.LOCATION_DISPLAY.get())
                mc.setScreen(
                    LecternLinkingBookScreen(
                        data.pos,
                        data.bookStack,
                        locationDisplay?.name ?: Component.empty()
                    )
                )
            }
        }

    }
}

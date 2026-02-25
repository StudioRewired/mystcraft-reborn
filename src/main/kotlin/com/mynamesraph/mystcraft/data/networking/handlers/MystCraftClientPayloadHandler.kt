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

    }
}

package com.mynamesraph.mystcraft.data.networking.packet

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Sent server → client immediately after a linking book is crafted.
 * Tells the client to capture a preview screenshot of the current view
 * and send it back via SendPreviewImagePacket.
 */
class TriggerPreviewCapturePacket : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<TriggerPreviewCapturePacket> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<TriggerPreviewCapturePacket>(
            ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "trigger_preview_capture")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, TriggerPreviewCapturePacket> =
            StreamCodec.of(
                { _, _ -> },  // encoder — nothing to write
                { _ -> TriggerPreviewCapturePacket() }  // decoder — just return a new instance
            )
    }
}
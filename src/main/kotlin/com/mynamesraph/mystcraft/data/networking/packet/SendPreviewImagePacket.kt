package com.mynamesraph.mystcraft.data.networking.packet

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class SendPreviewImagePacket(val frames: List<ByteArray>) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SendPreviewImagePacket> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<SendPreviewImagePacket>(
            ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "send_preview_image")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, SendPreviewImagePacket> =
            ByteBufCodecs.BYTE_ARRAY.apply(ByteBufCodecs.list()).map(
                { list -> SendPreviewImagePacket(list) },
                { packet -> packet.frames }
            )
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is SendPreviewImagePacket) return false
        if (frames.size != other.frames.size) return false
        return frames.zip(other.frames).all { (a, b) -> a.contentEquals(b) }
    }

    override fun hashCode(): Int = frames.fold(0) { acc, bytes -> 31 * acc + bytes.contentHashCode() }
}
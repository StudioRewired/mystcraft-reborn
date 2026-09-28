package com.mynamesraph.mystcraft.data.networking.packet

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class SendCameraVideoPacket(val frames: List<ByteArray>) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SendCameraVideoPacket> = TYPE

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is SendCameraVideoPacket) return false
        if (frames.size != other.frames.size) return false
        return frames.zip(other.frames).all { (a, b) -> a.contentEquals(b) }
    }

    override fun hashCode(): Int = frames.fold(0) { acc, bytes -> 31 * acc + bytes.contentHashCode() }

    companion object {
        val TYPE = CustomPacketPayload.Type<SendCameraVideoPacket>(
            ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "send_camera_video")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, SendCameraVideoPacket> =
            ByteBufCodecs.BYTE_ARRAY.apply(ByteBufCodecs.list()).map(
                { SendCameraVideoPacket(it) },
                { it.frames }
            )
    }
}
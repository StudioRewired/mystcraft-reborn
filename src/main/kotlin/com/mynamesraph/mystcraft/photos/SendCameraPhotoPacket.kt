package com.mynamesraph.mystcraft.data.networking.packet

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class SendCameraPhotoPacket(val jpeg: ByteArray) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SendCameraPhotoPacket> = TYPE

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is SendCameraPhotoPacket) return false
        return jpeg.contentEquals(other.jpeg)
    }

    override fun hashCode(): Int = jpeg.contentHashCode()

    companion object {
        val TYPE = CustomPacketPayload.Type<SendCameraPhotoPacket>(
            ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "send_camera_photo")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, SendCameraPhotoPacket> =
            ByteBufCodecs.BYTE_ARRAY.map(
                { SendCameraPhotoPacket(it) },
                { it.jpeg }
            )
    }
}
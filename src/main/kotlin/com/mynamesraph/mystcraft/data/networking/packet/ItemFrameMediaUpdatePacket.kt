package com.mynamesraph.mystcraft.data.networking.packet

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Client request to update a Mystcraft media stack currently mounted in an item frame. */
data class ItemFrameMediaUpdatePacket(
    val entityId: Int,
    val backlight: Boolean,
    val removeItem: Boolean,
    val verticalOffset: Float,
    val horizontalOffset: Float,
    val rotation: Float
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<ItemFrameMediaUpdatePacket>(
            ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "item_frame_media_update")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, ItemFrameMediaUpdatePacket> = StreamCodec.of(
            { buf, packet ->
                ByteBufCodecs.VAR_INT.encode(buf, packet.entityId)
                ByteBufCodecs.BOOL.encode(buf, packet.backlight)
                ByteBufCodecs.BOOL.encode(buf, packet.removeItem)
                buf.writeFloat(packet.verticalOffset)
                buf.writeFloat(packet.horizontalOffset)
                buf.writeFloat(packet.rotation)
            },
            { buf ->
                ItemFrameMediaUpdatePacket(
                    entityId = ByteBufCodecs.VAR_INT.decode(buf),
                    backlight = ByteBufCodecs.BOOL.decode(buf),
                    removeItem = ByteBufCodecs.BOOL.decode(buf),
                    verticalOffset = buf.readFloat(),
                    horizontalOffset = buf.readFloat(),
                    rotation = buf.readFloat()
                )
            }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

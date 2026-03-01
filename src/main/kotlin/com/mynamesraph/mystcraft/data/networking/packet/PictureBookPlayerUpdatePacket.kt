package com.mynamesraph.mystcraft.data.networking.packet

import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class PictureBookPlayerUpdatePacket(
    val pos: BlockPos,
    val width: Int,
    val height: Int,
    val backlight: Boolean,
    val removeBook: Boolean,
    val verticalOffset: Float = 0f,
    val horizontalOffset: Float = 0f,
    val rotation: Float = 0f
) : CustomPacketPayload {

    companion object {
        val ID = CustomPacketPayload.Type<PictureBookPlayerUpdatePacket>(
            ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "picture_book_player_update")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, PictureBookPlayerUpdatePacket> =
            StreamCodec.of(
                { buf, pkt ->
                    ByteBufCodecs.VAR_LONG.encode(buf, pkt.pos.asLong())
                    ByteBufCodecs.VAR_INT.encode(buf, pkt.width)
                    ByteBufCodecs.VAR_INT.encode(buf, pkt.height)
                    ByteBufCodecs.BOOL.encode(buf, pkt.backlight)
                    ByteBufCodecs.BOOL.encode(buf, pkt.removeBook)
                    buf.writeFloat(pkt.verticalOffset)
                    buf.writeFloat(pkt.horizontalOffset)
                    buf.writeFloat(pkt.rotation)
                },
                { buf ->
                    PictureBookPlayerUpdatePacket(
                        pos              = BlockPos.of(ByteBufCodecs.VAR_LONG.decode(buf)),
                        width            = ByteBufCodecs.VAR_INT.decode(buf),
                        height           = ByteBufCodecs.VAR_INT.decode(buf),
                        backlight        = ByteBufCodecs.BOOL.decode(buf),
                        removeBook       = ByteBufCodecs.BOOL.decode(buf),
                        verticalOffset   = buf.readFloat(),
                        horizontalOffset = buf.readFloat(),
                        rotation         = buf.readFloat()
                    )
                }
            )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
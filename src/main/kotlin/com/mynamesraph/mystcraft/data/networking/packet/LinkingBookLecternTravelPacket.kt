package com.mynamesraph.mystcraft.data.networking.packet

import com.mynamesraph.mystcraft.Mystcraft
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

class LinkingBookLecternTravelPacket(val pos: BlockPos) : CustomPacketPayload {
    companion object {
        val TYPE:CustomPacketPayload.Type<LinkingBookLecternTravelPacket> = CustomPacketPayload.Type<LinkingBookLecternTravelPacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID,"linking_book_lectern_travel_packet")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, LinkingBookLecternTravelPacket> = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            LinkingBookLecternTravelPacket::pos,
            ::LinkingBookLecternTravelPacket
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    override fun toString(): String {
        return "[Position:(${pos.x},${pos.y},${pos.z})]"
    }
}
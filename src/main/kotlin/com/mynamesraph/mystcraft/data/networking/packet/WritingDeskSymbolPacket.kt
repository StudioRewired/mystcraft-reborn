package com.mynamesraph.mystcraft.data.networking.packet

import com.mynamesraph.mystcraft.Mystcraft
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

class WritingDeskSymbolPacket(val type: String, val symbol: ResourceLocation, val writingDeskPos: BlockPos) : CustomPacketPayload {

    companion object {
        val TYPE:CustomPacketPayload.Type<WritingDeskSymbolPacket> = CustomPacketPayload.Type<WritingDeskSymbolPacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID,"writing_desk_symbol_packet")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, WritingDeskSymbolPacket> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            WritingDeskSymbolPacket::type,
            ResourceLocation.STREAM_CODEC,
            WritingDeskSymbolPacket::symbol,
            BlockPos.STREAM_CODEC,
            WritingDeskSymbolPacket::writingDeskPos,
            ::WritingDeskSymbolPacket
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }
}
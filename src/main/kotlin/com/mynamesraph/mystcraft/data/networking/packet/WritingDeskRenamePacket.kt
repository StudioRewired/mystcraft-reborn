package com.mynamesraph.mystcraft.data.networking.packet

import com.mynamesraph.mystcraft.Mystcraft
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

class WritingDeskRenamePacket(val name: String, val writingDeskPos: BlockPos) : CustomPacketPayload {

    companion object {
        val TYPE:CustomPacketPayload.Type<WritingDeskRenamePacket> = CustomPacketPayload.Type<WritingDeskRenamePacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID,"writing_desk_rename_packet")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, WritingDeskRenamePacket> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            WritingDeskRenamePacket::name,
            BlockPos.STREAM_CODEC,
            WritingDeskRenamePacket::writingDeskPos,
            ::WritingDeskRenamePacket
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }
}
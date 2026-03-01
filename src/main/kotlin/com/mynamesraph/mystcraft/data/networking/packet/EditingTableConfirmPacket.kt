package com.mynamesraph.mystcraft.data.networking.packet

import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class EditingTableConfirmPacket(val pos: BlockPos) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<EditingTableConfirmPacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "editing_table_confirm_packet")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, EditingTableConfirmPacket> = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            EditingTableConfirmPacket::pos,
            ::EditingTableConfirmPacket
        )

        fun sendToServer(pos: BlockPos) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(EditingTableConfirmPacket(pos))
        }
    }

    override fun type() = TYPE
}
package com.mynamesraph.mystcraft.data.networking.packet

import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class AgeDeleteConfirmedPacket(
    val dimensionId: ResourceLocation,
    val receptaclePos: BlockPos
) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<AgeDeleteConfirmedPacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "age_delete_confirmed_packet")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, AgeDeleteConfirmedPacket> = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            AgeDeleteConfirmedPacket::dimensionId,
            BlockPos.STREAM_CODEC,
            AgeDeleteConfirmedPacket::receptaclePos,
            ::AgeDeleteConfirmedPacket
        )
    }

    override fun type() = TYPE
}
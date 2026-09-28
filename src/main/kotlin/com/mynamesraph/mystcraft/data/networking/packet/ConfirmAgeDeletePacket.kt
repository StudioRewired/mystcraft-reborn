package com.mynamesraph.mystcraft.data.networking.packet

import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class ConfirmAgeDeletePacket(
    val dimensionId: ResourceLocation,
    val receptaclePos: BlockPos
) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<ConfirmAgeDeletePacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "confirm_age_delete_packet")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ConfirmAgeDeletePacket> = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ConfirmAgeDeletePacket::dimensionId,
            BlockPos.STREAM_CODEC,
            ConfirmAgeDeletePacket::receptaclePos,
            ::ConfirmAgeDeletePacket
        )
    }

    override fun type() = TYPE
}
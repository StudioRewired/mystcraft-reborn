package com.mynamesraph.mystcraft.data.networking.packet

import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs

data class LinkingBookTravelPacket(val interactionHand: InteractionHand) : CustomPacketPayload {

    companion object {
        val TYPE:CustomPacketPayload.Type<LinkingBookTravelPacket> = CustomPacketPayload.Type<LinkingBookTravelPacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID,"linking_book_travel_packet")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, LinkingBookTravelPacket> = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(InteractionHand::class.java),
            LinkingBookTravelPacket::interactionHand,
            ::LinkingBookTravelPacket
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    override fun toString(): String {
        return "[interaction_hand:$interactionHand]"
    }
}

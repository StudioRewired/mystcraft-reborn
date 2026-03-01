package com.mynamesraph.mystcraft.data.networking.packet

import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

class OpenLecternScreenPacket(val pos: BlockPos, val bookStack: ItemStack) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<OpenLecternScreenPacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "open_lectern_screen")
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, OpenLecternScreenPacket> =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                OpenLecternScreenPacket::pos,
                ItemStack.STREAM_CODEC,
                OpenLecternScreenPacket::bookStack,
                ::OpenLecternScreenPacket
            )
    }

    override fun type() = TYPE
}
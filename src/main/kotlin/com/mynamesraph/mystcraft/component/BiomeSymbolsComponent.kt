package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation

@JvmRecord
data class BiomeSymbolsComponent(val biomes: List<ResourceLocation>) {
    companion object {
        val CODEC: Codec<BiomeSymbolsComponent> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.list(ResourceLocation.CODEC).fieldOf("biomes").forGetter(BiomeSymbolsComponent::biomes),
            ).apply(instance,::BiomeSymbolsComponent)
        }

        val STREAM_CODEC: StreamCodec<ByteBuf, BiomeSymbolsComponent> = StreamCodec.composite(
            ByteBufCodecs.fromCodec(Codec.list(ResourceLocation.CODEC)), BiomeSymbolsComponent::biomes,
            ::BiomeSymbolsComponent
        )
    }
}

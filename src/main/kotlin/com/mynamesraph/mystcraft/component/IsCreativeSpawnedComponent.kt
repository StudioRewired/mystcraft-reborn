package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

@JvmRecord
data class IsCreativeSpawnedComponent(val generated: Boolean = false) {
    companion object {
        val CODEC: Codec<IsCreativeSpawnedComponent> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.BOOL.fieldOf("generated").forGetter(IsCreativeSpawnedComponent::generated),
            ).apply(instance,::IsCreativeSpawnedComponent)
        }

        val STREAM_CODEC: StreamCodec<ByteBuf, IsCreativeSpawnedComponent> = StreamCodec.composite(
            ByteBufCodecs.BOOL, IsCreativeSpawnedComponent::generated,
            ::IsCreativeSpawnedComponent
        )
    }
}
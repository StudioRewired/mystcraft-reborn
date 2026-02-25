package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

@JvmRecord
data class RotationComponent(val rotX: Float, val rotY: Float) {
    companion object {
        val CODEC: Codec<RotationComponent> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.FLOAT.fieldOf("rotation_x").forGetter(RotationComponent::rotX),
                Codec.FLOAT.fieldOf("rotation_y").forGetter(RotationComponent::rotY)
            ).apply(instance,::RotationComponent)
        }

        val STREAM_CODEC: StreamCodec<ByteBuf, RotationComponent> = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RotationComponent::rotX,
            ByteBufCodecs.FLOAT, RotationComponent::rotY,
            ::RotationComponent
        )
    }
}
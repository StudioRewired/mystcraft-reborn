package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import java.util.*

class DimensionIdentificatorComponent(val generated: Boolean, val dimensionID: ResourceLocation) {

    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else {
            other is DimensionIdentificatorComponent && this.generated == other.generated && this.dimensionID == other.dimensionID
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(generated,dimensionID)
    }

    companion object {
        val CODEC: Codec<DimensionIdentificatorComponent> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.BOOL.fieldOf("generated").forGetter(DimensionIdentificatorComponent::generated),
                ResourceLocation.CODEC.fieldOf("dimension_id").forGetter(DimensionIdentificatorComponent::dimensionID)
            ).apply(instance,::DimensionIdentificatorComponent)
        }

        val STREAM_CODEC: StreamCodec<ByteBuf, DimensionIdentificatorComponent> = StreamCodec.composite(
            ByteBufCodecs.BOOL, DimensionIdentificatorComponent::generated,
            ResourceLocation.STREAM_CODEC, DimensionIdentificatorComponent::dimensionID,
            ::DimensionIdentificatorComponent
        )
    }
}
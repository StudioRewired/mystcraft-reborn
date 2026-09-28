package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.StreamCodec
import java.util.*

class LocationDisplayComponent(val name: Component) {

    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else {
            other is LocationDisplayComponent && this.name === other.name
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(name)
    }

    companion object {
        val CODEC: Codec<LocationDisplayComponent> = RecordCodecBuilder.create { instance ->
            instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(LocationDisplayComponent::name)
            ).apply(instance,::LocationDisplayComponent)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, LocationDisplayComponent> = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, LocationDisplayComponent::name,
            ::LocationDisplayComponent
        )
    }
}
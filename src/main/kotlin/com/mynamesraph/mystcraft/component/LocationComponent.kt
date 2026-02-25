package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.Level
import org.joml.Vector3f
import java.util.*

data class LocationComponent(val levelKey: ResourceKey<Level>, val position: Vector3f) {

    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else {
            other is LocationComponent && this.levelKey === other.levelKey && this.position === other.position
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(levelKey,position)
    }

    companion object {
        val CODEC: Codec<LocationComponent> = RecordCodecBuilder.create { instance ->
            instance.group(
                ServerLevel.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(LocationComponent::levelKey),
                ExtraCodecs.VECTOR3F.fieldOf("position").forGetter(LocationComponent::position)
            ).apply(instance,::LocationComponent)
        }

        val STREAM_CODEC: StreamCodec<ByteBuf, LocationComponent> = StreamCodec.composite(
            ByteBufCodecs.fromCodec(ServerLevel.RESOURCE_KEY_CODEC), LocationComponent::levelKey,
            ByteBufCodecs.VECTOR3F, LocationComponent::position,
            ::LocationComponent
        )
    }
}

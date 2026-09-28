package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.util.Base64

data class CameraPhotoComponent(val jpeg: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is CameraPhotoComponent) return false
        return jpeg.contentEquals(other.jpeg)
    }

    override fun hashCode(): Int = jpeg.contentHashCode()

    companion object {
        val CODEC: Codec<CameraPhotoComponent> = Codec.STRING.xmap(
            { CameraPhotoComponent(Base64.getDecoder().decode(it)) },
            { Base64.getEncoder().encodeToString(it.jpeg) }
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, CameraPhotoComponent> =
            ByteBufCodecs.BYTE_ARRAY.map(
                { CameraPhotoComponent(it) },
                { it.jpeg }
            )
    }
}
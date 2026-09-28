package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.util.Base64

data class PreviewImageComponent(val frames: List<ByteArray>) {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is PreviewImageComponent) return false
        if (frames.size != other.frames.size) return false
        return frames.zip(other.frames).all { (a, b) -> a.contentEquals(b) }
    }

    override fun hashCode(): Int = frames.fold(0) { acc, bytes -> 31 * acc + bytes.contentHashCode() }

    companion object {
        const val CAPTURE_WIDTH = 256
        const val CAPTURE_HEIGHT = 72
        const val JPEG_QUALITY = 50
        const val FRAME_COUNT = 12
        const val TICKS_BETWEEN_FRAMES = 3  // 6 FPS at 20 TPS
        const val TOTAL_CAPTURE_TICKS = FRAME_COUNT * TICKS_BETWEEN_FRAMES  // 40 ticks = 2 seconds

        val CODEC: Codec<PreviewImageComponent> = Codec.STRING
            .listOf()
            .xmap(
                { list -> PreviewImageComponent(list.map { Base64.getDecoder().decode(it) }) },
                { component -> component.frames.map { Base64.getEncoder().encodeToString(it) } }
            )

        val STREAM_CODEC: StreamCodec<ByteBuf, PreviewImageComponent> =
            ByteBufCodecs.BYTE_ARRAY.apply(ByteBufCodecs.list()).map(
                { list -> PreviewImageComponent(list) },
                { component -> component.frames }
            )
    }
}
package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

/** Per-stack presentation settings used only while Mystcraft media is displayed in an item frame. */
data class ItemFrameMediaSettingsComponent(
    val backlight: Boolean = true,
    val verticalOffset: Float = 0f,
    val horizontalOffset: Float = 0f,
    val rotation: Float = 0f
) {
    fun sanitized(): ItemFrameMediaSettingsComponent {
        val safeVertical = verticalOffset.takeIf { it.isFinite() } ?: 0f
        val safeHorizontal = horizontalOffset.takeIf { it.isFinite() } ?: 0f
        val safeRotation = rotation.takeIf { it.isFinite() } ?: 0f
        return copy(
            verticalOffset = safeVertical.coerceIn(-17.5f, 17.5f),
            horizontalOffset = safeHorizontal.coerceIn(-17.5f, 17.5f),
            rotation = ((safeRotation % 360f) + 360f) % 360f
        )
    }

    companion object {
        val DEFAULT = ItemFrameMediaSettingsComponent()

        val CODEC: Codec<ItemFrameMediaSettingsComponent> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.BOOL.optionalFieldOf("backlight", true).forGetter(ItemFrameMediaSettingsComponent::backlight),
                Codec.FLOAT.optionalFieldOf("vertical_offset", 0f).forGetter(ItemFrameMediaSettingsComponent::verticalOffset),
                Codec.FLOAT.optionalFieldOf("horizontal_offset", 0f).forGetter(ItemFrameMediaSettingsComponent::horizontalOffset),
                Codec.FLOAT.optionalFieldOf("rotation", 0f).forGetter(ItemFrameMediaSettingsComponent::rotation)
            ).apply(instance, ::ItemFrameMediaSettingsComponent)
        }

        val STREAM_CODEC: StreamCodec<ByteBuf, ItemFrameMediaSettingsComponent> = StreamCodec.composite(
            ByteBufCodecs.BOOL, ItemFrameMediaSettingsComponent::backlight,
            ByteBufCodecs.FLOAT, ItemFrameMediaSettingsComponent::verticalOffset,
            ByteBufCodecs.FLOAT, ItemFrameMediaSettingsComponent::horizontalOffset,
            ByteBufCodecs.FLOAT, ItemFrameMediaSettingsComponent::rotation,
            ::ItemFrameMediaSettingsComponent
        )
    }
}

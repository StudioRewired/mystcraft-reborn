package com.mynamesraph.mystcraft.RewindTool

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.registries.Registries
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

object MystcraftAttachments {

    val REGISTRY: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Mystcraft.MOD_ID)

    // Codec for a non-null RewindAttachment
    private val REWIND_ATTACHMENT_CODEC: Codec<RewindAttachment> =
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter { it.x },
                Codec.DOUBLE.fieldOf("y").forGetter { it.y },
                Codec.DOUBLE.fieldOf("z").forGetter { it.z },
                Codec.FLOAT.fieldOf("yRot").forGetter { it.yRot },
                Codec.FLOAT.fieldOf("xRot").forGetter { it.xRot },
                Codec.STRING.fieldOf("dimension").forGetter { it.dimension.location().toString() }
            ).apply(instance) { x, y, z, yRot, xRot, dimString ->
                RewindAttachment(
                    x, y, z, yRot, xRot,
                    ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimString))
                )
            }
        }

    // Wraps the codec so null values are skipped during serialization entirely
    private val REWIND_NULLABLE_CODEC: Codec<RewindAttachment?> =
        REWIND_ATTACHMENT_CODEC.optionalFieldOf("value")
            .xmap(
                { optional -> optional.orElse(null) },
                { value -> java.util.Optional.ofNullable(value) }
            )
            .codec()

    val REWIND_POSITION: DeferredHolder<AttachmentType<*>, AttachmentType<RewindAttachment?>> =
        REGISTRY.register("rewind_position", Supplier {
            @Suppress("UNCHECKED_CAST")
            AttachmentType.builder<RewindAttachment?>(Supplier { null })
                .serialize(REWIND_NULLABLE_CODEC as Codec<RewindAttachment?>)
                .copyOnDeath()
                .build()
        })
}
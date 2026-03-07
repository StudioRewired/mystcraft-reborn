package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object MystcraftSounds {
    private val SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Mystcraft.Companion.MOD_ID)

    val LINK_TRAVEL: Supplier<SoundEvent> = SOUNDS.register(
        "link_travel",
        Supplier {
            SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "link_travel")
            )
        }
    )
    fun register(modEventBus: IEventBus) = SOUNDS.register(modEventBus)
}
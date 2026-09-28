package com.mynamesraph.mystcraft.events

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.item.MystcraftMedia
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.decoration.ItemFrame
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

/** Stops vanilla item-frame rotation on the logical server for frames acting as media players. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object ItemFrameMediaInteractionEvents {
    @SubscribeEvent
    fun onEntityInteractSpecific(event: PlayerInteractEvent.EntityInteractSpecific) {
        if (event.level.isClientSide) return
        val frame = event.target as? ItemFrame ?: return
        if (MystcraftMedia.preview(frame.item) == null) return

        event.isCanceled = true
        event.cancellationResult = InteractionResult.SUCCESS
    }
}

package com.mynamesraph.mystcraft.block.sponge

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.data.saved.SpongeRegistry
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.level.BlockEvent

/**
 * Backup safety net for the rare fluid-to-block conversion case
 * (e.g. water + lava → cobblestone/obsidian) happening inside a protected
 * zone. The FlowingFluidMixin handles all general water spread; this only
 * exists to catch edge cases where vanilla converts a fluid into a solid
 * block without going through canSpreadTo.
 *
 * In normal operation this should rarely fire — only when lava and water
 * physically meet at a sponge-protected boundary.
 */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
object ClassicSpongeFluidGuard {

    @SubscribeEvent
    fun onFluidPlace(event: BlockEvent.FluidPlaceBlockEvent) {
        val level = event.level as? ServerLevel ?: return

        val registry = level.server.overworld().dataStorage
            .computeIfAbsent(SpongeRegistry.FACTORY, SpongeRegistry.FILE_NAME)

        if (registry.isProtected(event.pos)) {
            event.newState = Blocks.AIR.defaultBlockState()
        }
    }
}
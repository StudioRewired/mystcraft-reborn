package com.mynamesraph.mystcraft.events

import com.mynamesraph.mystcraft.data.saved.HeadlampLightData
import com.mynamesraph.mystcraft.registry.MystcraftItems
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LightBlock
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent

object HeadlampEvents {

    // Tracks each player's last light block position so we can remove it when they move
    private val lastLightPos = mutableMapOf<java.util.UUID, BlockPos>()

    private fun isWearingHeadlamp(player: Player): Boolean =
        player.getItemBySlot(EquipmentSlot.HEAD).item is com.mynamesraph.mystcraft.item.HeadlampItem

    private fun getLightLevel(player: Player): Int {
        val data = player.level().server?.overworld()?.dataStorage
            ?.computeIfAbsent(HeadlampLightData.FACTORY, HeadlampLightData.FILE_NAME)
        return data?.lightLevel ?: HeadlampLightData.DEFAULT_LIGHT_LEVEL
    }

    private fun clearLightAt(player: Player, pos: BlockPos) {
        val level = player.level()
        if (level.getBlockState(pos).block is LightBlock) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
        }
    }

    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.level().isClientSide) return

        val pos = player.blockPosition().above()
        val uuid = player.uuid

        if (!isWearingHeadlamp(player)) {
            // Clean up any lingering light block if the headlamp was removed mid-tick
            lastLightPos.remove(uuid)?.let { clearLightAt(player, it) }
            return
        }

        val last = lastLightPos[uuid]

        // Only update if the player has moved to a new block position
        if (last == pos) return

        // Remove old light block
        last?.let { clearLightAt(player, it) }

        // Place new light block only if the position is air (don't overwrite blocks)
        val level = player.level()
        if (level.getBlockState(pos).isAir) {
            val lightLevel = getLightLevel(player)
            level.setBlock(
                pos,
                Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel),
                3
            )
            lastLightPos[uuid] = pos
        } else {
            // Position is occupied — still track it so we don't keep retrying the same spot
            lastLightPos[uuid] = pos
        }
    }

    @SubscribeEvent
    fun onEquipmentChange(event: LivingEquipmentChangeEvent) {
        val entity = event.entity
        if (entity.level().isClientSide) return
        if (entity !is Player) return
        if (event.slot != EquipmentSlot.HEAD) return

        // Headlamp was removed from head slot — clean up the light block
        if (event.from.item is com.mynamesraph.mystcraft.item.HeadlampItem) {
            lastLightPos.remove(entity.uuid)?.let { clearLightAt(entity, it) }
        }
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity
        if (player.level().isClientSide) return
        lastLightPos.remove(player.uuid)?.let { clearLightAt(player, it) }
    }
}
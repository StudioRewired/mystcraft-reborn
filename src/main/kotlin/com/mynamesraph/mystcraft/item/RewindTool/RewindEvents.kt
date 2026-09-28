package com.mynamesraph.mystcraft.RewindTool

import net.minecraft.world.entity.player.Player
import com.mynamesraph.mystcraft.block.portal.LinkPortalBlock


object RewindEvents {

    // onPlayerLogout removed — NeoForge serializes attachment data automatically
    // via the codec registered in MystcraftAttachments. Explicitly removing it
    // on logout was preventing it from ever being saved.

    fun saveRewindPosition(player: Player) {
        val safePos = findSafePosition(player)
        if (safePos == null) {
            println("MYSTCRAFT DEBUG: No safe position found for ${player.name.string}")
            return
        }
        println("MYSTCRAFT DEBUG: Saving rewind position ${safePos.first}, ${safePos.second}, ${safePos.third} in ${player.level().dimension().location()}")

        player.setData(
            MystcraftAttachments.REWIND_POSITION.get(),
            RewindAttachment(
                safePos.first,
                safePos.second,
                safePos.third,
                player.yRot,
                player.xRot,
                player.level().dimension()
            )
        )
    }

    fun findSafePosition(player: Player): Triple<Double, Double, Double>? {
        val level = player.level()
        val origin = player.blockPosition()

        for (radius in 0..8) {
            for (x in -radius..radius) {
                for (y in -2..4) {
                    for (z in -radius..radius) {
                        if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue

                        val checkPos = origin.offset(x, y, z)
                        val blockAtFeet = level.getBlockState(checkPos)
                        val blockAtHead = level.getBlockState(checkPos.above())
                        val blockBelow = level.getBlockState(checkPos.below())

                        val feetClear = blockAtFeet.isAir || blockAtFeet.block.isPossibleToRespawnInThis(blockAtFeet)
                        val headClear = blockAtHead.isAir || blockAtHead.block.isPossibleToRespawnInThis(blockAtHead)
                        val hasGround = !blockBelow.isAir && blockBelow.isSolid
                        val notPortal = blockAtFeet.block !is LinkPortalBlock

                        if (feetClear && headClear && hasGround && notPortal) {
                            return Triple(
                                checkPos.x + 0.5,
                                checkPos.y.toDouble(),
                                checkPos.z + 0.5
                            )
                        }
                    }
                }
            }
        }
        return null
    }
}
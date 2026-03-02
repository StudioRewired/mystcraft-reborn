package com.mynamesraph.mystcraft.RewindTool

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.portal.DimensionTransition
import net.minecraft.world.phys.Vec3

class RewindBookItem(properties: Properties) : Item(properties) {

    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)

        if (!level.isClientSide) {
            val saved: RewindAttachment? = player.getData(MystcraftAttachments.REWIND_POSITION.get())
            println("MFC DEBUG: Rewind position retrieved: $saved")
            if (saved == null) {
                player.displayClientMessage(
                    Component.literal("You must use a linking book or linking portal before rewinding."),
                    true
                )
                return InteractionResultHolder.fail(stack)
            }

            val targetLevel: ServerLevel? = level.server!!.getLevel(saved.dimension)

            if (targetLevel == null) {
                player.displayClientMessage(
                    Component.literal("Rewind dimension no longer exists."),
                    true
                )
                return InteractionResultHolder.fail(stack)
            }

            // Collect nearby mobs before the player teleports, same 6-block radius as linking books
            val nearbyMobs = level.getEntitiesOfClass(
                net.minecraft.world.entity.Mob::class.java,
                player.boundingBox.inflate(6.0)
            )

            // Clear so you can't rewind twice
            player.removeData(MystcraftAttachments.REWIND_POSITION.get())

            val destination = Vec3(saved.x, saved.y, saved.z)

            player.changeDimension(
                DimensionTransition(
                    targetLevel,
                    destination,
                    player.deltaMovement,
                    saved.yRot,
                    saved.xRot,
                    DimensionTransition.DO_NOTHING
                )
            )

            // Teleport nearby mobs to the same destination
            nearbyMobs.forEach { mob ->
                if (targetLevel == mob.level()) {
                    mob.teleportTo(destination.x, destination.y, destination.z)
                } else {
                    mob.changeDimension(
                        DimensionTransition(
                            targetLevel,
                            destination,
                            mob.deltaMovement,
                            mob.yRot,
                            mob.xRot,
                            DimensionTransition.DO_NOTHING
                        )
                    )
                }
            }

            return InteractionResultHolder.success(stack)
        }

        return InteractionResultHolder.success(stack)
    }
}

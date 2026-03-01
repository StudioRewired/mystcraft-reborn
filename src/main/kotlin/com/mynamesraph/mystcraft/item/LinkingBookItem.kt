package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.component.LocationComponent
import com.mynamesraph.mystcraft.component.LocationDisplayComponent
import com.mynamesraph.mystcraft.component.RotationComponent
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.ui.screen.LinkingBookScreen
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.portal.DimensionTransition
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVec3
import com.mynamesraph.mystcraft.RewindTool.RewindEvents

open class LinkingBookItem(properties: Properties) : Item(properties) {



    override fun getHighlightTip(item: ItemStack, displayName: Component): Component {
        if (item.has(MystcraftComponents.LOCATION_DISPLAY)) {
            val display = item.components.get(MystcraftComponents.LOCATION_DISPLAY.get())

            if (display is LocationDisplayComponent) {
                return display.name.plainCopy()
            }
        }
        return super.getHighlightTip(item, displayName)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val display = stack.components.get(MystcraftComponents.LOCATION_DISPLAY.get())

        if (display is LocationDisplayComponent) {
            tooltipComponents.add(display.name)
        }
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        openScreen(level,player,usedHand)
        return super.use(level, player, usedHand)
    }

    protected open fun openScreen(level: Level, player: Player, usedHand: InteractionHand) {
        if (level.isClientSide) {
            if (com.mynamesraph.mystcraft.events.LinkingBookCraftEvents.captureScheduled) {
                Minecraft.getInstance().player?.displayClientMessage(
                    Component.literal("The linking book is still being recorded..."),
                    true
                )
                return
            }
            Minecraft.getInstance().setScreen(
                LinkingBookScreen(
                    Component.literal("linking_book_screen"),
                    usedHand,
                    player
                )
            )
        }
    }



    open fun teleportToLocationFromHand(level: Level, player: Player, usedHand: InteractionHand) {
        if(!level.isClientSide()) {

            val location = player.getItemInHand(usedHand).get(MystcraftComponents.LOCATION.get())
            val rotation = player.getItemInHand(usedHand).get(MystcraftComponents.ROTATION.get())

            if (location is LocationComponent && rotation is RotationComponent) {
                teleportToLocationFromLectern(level,player,location,rotation)
            }
        }
    }

    open fun teleportToLocationFromLectern(level: Level, entity: Entity, location: LocationComponent, rotation: RotationComponent) {
        if (!level.isClientSide()) {
            if (entity is Player) {
                RewindEvents.saveRewindPosition(entity)
            }

            val transition = getDestination(level, entity, location, rotation)
            val destLevel = transition.newLevel()

            // Collect nearby mobs before teleporting the player
            val nearbyMobs = if (entity is Player) {
                level.getEntitiesOfClass(
                    net.minecraft.world.entity.Mob::class.java,
                    entity.boundingBox.inflate(6.0)
                )
            } else emptyList()

            if (destLevel == entity.level()) {
                val pos = transition.pos()
                entity.teleportTo(pos.x, pos.y, pos.z)
                entity.setYRot(transition.yRot())
                entity.setXRot(transition.xRot())
            } else {
                entity.changeDimension(transition)
            }

            // Teleport nearby mobs to the same destination
            nearbyMobs.forEach { mob ->
                val mobTransition = DimensionTransition(
                    transition.newLevel(),
                    transition.pos(),
                    mob.deltaMovement,
                    mob.yRot,
                    mob.xRot,
                    DimensionTransition.DO_NOTHING
                )
                if (destLevel == mob.level()) {
                    mob.teleportTo(transition.pos().x, transition.pos().y, transition.pos().z)
                } else {
                    mob.changeDimension(mobTransition)
                }
            }
        }
    }

    open fun getDestination(level: Level, entity: Entity,location: LocationComponent,rotation: RotationComponent): DimensionTransition {
        var locationLevel:ServerLevel? = level.server!!.getLevel(location.levelKey)
        if (locationLevel == null) {
            // Dimension is gone — if the entity is holding this book, destroy it
            if (entity is Player) {
                for (hand in InteractionHand.entries) {
                    val held = entity.getItemInHand(hand)
                    if (held.item is LinkingBookItem) {
                        val loc = held.components.get(MystcraftComponents.LOCATION.get())
                        if (loc is LocationComponent && loc.levelKey == location.levelKey) {
                            entity.setItemInHand(hand, ItemStack.EMPTY)
                            entity.displayClientMessage(
                                Component.literal("The age no longer exists. The book crumbles to dust."),
                                true
                            )
                        }
                    }
                }
            }
            // Return a no-op transition to the current level at the entity's current position
            return DimensionTransition(
                entity.level() as ServerLevel,
                entity.position(),
                entity.deltaMovement,
                entity.yRot,
                entity.xRot,
                DimensionTransition.DO_NOTHING
            )
        }
         return DimensionTransition(
                locationLevel,
                location.position.toVec3(),
                entity.deltaMovement,
                rotation.rotY,
                rotation.rotX,
                DimensionTransition.DO_NOTHING
        )
    }
}
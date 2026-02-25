package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.events.CameraEvents
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.ui.screen.CameraPhotoScreen
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class CameraItem(properties: Properties) : Item(properties) {

    companion object {
        // Tweak independently from linking book settings
        const val CAPTURE_WIDTH = 640
        const val CAPTURE_HEIGHT = 384
        const val JPEG_QUALITY = 40
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) {
            val stack = player.getItemInHand(usedHand)
            val existing = stack.get(MystcraftComponents.CAMERA_PHOTO.get())
            if (existing != null) {
                Minecraft.getInstance().setScreen(
                    CameraPhotoScreen(Component.literal("camera_photo_screen"), existing)
                )
            } else {
                CameraEvents.showOptionsOverlay(usedHand)
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide)
    }
}
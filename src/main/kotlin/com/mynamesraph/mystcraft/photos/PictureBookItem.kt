package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.ui.screen.PictureBookScreen
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class PictureBookItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) {
            val stack = player.getItemInHand(usedHand)
            val preview = stack.get(MystcraftComponents.PREVIEW_IMAGE.get())
            if (preview != null) {
                Minecraft.getInstance().setScreen(
                    PictureBookScreen(Component.literal("picture_book_screen"), preview)
                )
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
            }
        }
        return InteractionResultHolder.pass(player.getItemInHand(usedHand))
    }
}
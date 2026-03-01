package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.client.PictureBookTooltipRenderer
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.ui.screen.PictureBookScreen
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.loading.FMLEnvironment

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

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)

        // Guard — tooltip rendering is client-only
        if (!FMLEnvironment.dist.isClient) return

        val preview = stack.get(MystcraftComponents.PREVIEW_IMAGE.get()) ?: return

        // Label line
        val frameCount = preview.frames.size
        val label = if (frameCount > 1)
            Component.literal("§7▶ Video ($frameCount frames)")
        else
            Component.literal("§7Photo")
        tooltipComponents.add(label)

        // Pixel art preview lines
        tooltipComponents.addAll(PictureBookTooltipRenderer.getTooltipLines(preview))
    }
}
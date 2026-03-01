package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.component.BookBagComponent
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.registry.MystcraftMenus
import com.mynamesraph.mystcraft.ui.menu.BookBagMenu
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuConstructor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class BookBagItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (!level.isClientSide) {
            player.openMenu(createMenuProvider(stack, usedHand), player.blockPosition())
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)

        val component = stack.get(MystcraftComponents.BOOK_BAG.get()) ?: run {
            tooltipComponents.add(Component.literal("§7Empty"))
            return
        }

        val contents = component.stacks.filter { !it.isEmpty }

        if (contents.isEmpty()) {
            tooltipComponents.add(Component.literal("§7Empty"))
            return
        }

        tooltipComponents.add(Component.literal("§7Contains:"))
        contents.forEach { itemStack ->
            tooltipComponents.add(Component.literal("§7- ").append(itemStack.hoverName))
        }
    }

    private fun createMenuProvider(stack: ItemStack, hand: InteractionHand): MenuProvider {
        return SimpleMenuProvider(
            MenuConstructor { containerId, playerInventory, player ->
                BookBagMenu(containerId, playerInventory, stack, hand)
            },
            Component.translatable("container.mystcraft_reborn.book_bag")
        )
    }

    companion object {
        /**
         * Saves the handler contents back into the item stack's data component.
         * Called by BookBagMenu when the player closes the menu.
         */
        fun saveContents(stack: ItemStack, handler: net.neoforged.neoforge.items.ItemStackHandler) {
            stack.set(MystcraftComponents.BOOK_BAG.get(), BookBagComponent.fromHandler(handler))
        }
    }
}
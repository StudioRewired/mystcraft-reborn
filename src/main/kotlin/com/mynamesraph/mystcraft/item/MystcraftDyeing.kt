package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.registry.MystcraftItems
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor

/** Shared, server-safe color handling. Absence of DYED_COLOR means the original artwork. */
object MystcraftDyeing {
    fun supports(stack: ItemStack): Boolean =
        stack.`is`(MystcraftItems.LINKING_BOOK.get()) || stack.`is`(MystcraftItems.BOOK_BAG.get())

    fun color(stack: ItemStack): Int? =
        if (supports(stack)) stack.get(DataComponents.DYED_COLOR)?.rgb()?.and(0xFFFFFF) else null

    fun colorizeName(stack: ItemStack, name: Component): Component {
        val rgb = color(stack) ?: return name
        return name.copy().withStyle(Style.EMPTY.withColor(rgb))
    }

    fun dyeCopy(stack: ItemStack, dye: DyeColor): ItemStack {
        if (!supports(stack)) return ItemStack.EMPTY
        // Copy the stack, never construct a new bag/book or replace its component map.
        // This preserves contents, destinations, media, custom names and third-party data.
        return stack.copyWithCount(1).also {
            it.set(DataComponents.DYED_COLOR, DyedItemColor(dye.textureDiffuseColor and 0xFFFFFF, false))
        }
    }
}

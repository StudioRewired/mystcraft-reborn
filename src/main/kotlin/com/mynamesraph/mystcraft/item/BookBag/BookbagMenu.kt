package com.mynamesraph.mystcraft.ui.menu

import com.mynamesraph.mystcraft.component.BookBagComponent
import com.mynamesraph.mystcraft.item.BookBagItem
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.registry.MystcraftMenus
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler

class BookBagMenu(
    containerID: Int,
    private val playerInventory: Inventory,
    private val bagStack: ItemStack,
    private val hand: InteractionHand
) : AbstractContainerMenu(MystcraftMenus.BOOK_BAG_MENU.get(), containerID) {

    companion object {
        const val BAG_SLOT_X      = 8
        const val BAG_SLOT_Y      = 19
        const val INVENTORY_SLOT_X = 8
        const val INVENTORY_SLOT_Y = 69
        const val HOTBAR_Y         = 127
        const val SLOT_STRIDE      = 18
        const val BAG_ROWS         = 2
        const val BAG_COLS         = 9
    }

    val handler: ItemStackHandler = run {
        val component = bagStack.get(MystcraftComponents.BOOK_BAG.get())
        if (component != null) BookBagComponent.toHandler(component)
        else ItemStackHandler(BookBagComponent.SLOT_COUNT)
    }

    init {
        // Bag slots
        for (row in 0 until BAG_ROWS) {
            for (col in 0 until BAG_COLS) {
                val index = row * BAG_COLS + col
                addSlot(object : SlotItemHandler(handler, index, BAG_SLOT_X + col * SLOT_STRIDE, BAG_SLOT_Y + row * SLOT_STRIDE) {
                    override fun mayPlace(stack: ItemStack): Boolean {
                        // Prevent placing book bags inside book bags
                        return stack.item !is BookBagItem
                    }
                })
            }
        }

        // Player inventory (rows 0-2)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, INVENTORY_SLOT_X + col * SLOT_STRIDE, INVENTORY_SLOT_Y + row * SLOT_STRIDE))
            }
        }

        // Hotbar
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, INVENTORY_SLOT_X + col * SLOT_STRIDE, HOTBAR_Y))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var returnStack = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return returnStack
        if (!slot.hasItem()) return returnStack

        val stack = slot.item
        returnStack = stack.copy()

        val bagSlotCount = BAG_ROWS * BAG_COLS

        when {
            // Taking from bag — move to player inventory
            index < bagSlotCount -> {
                if (!moveItemStackTo(stack, bagSlotCount, slots.size, true)) return ItemStack.EMPTY
            }
            // Moving from player inventory — move to bag
            else -> {
                if (!moveItemStackTo(stack, 0, bagSlotCount, false)) return ItemStack.EMPTY
            }
        }

        if (stack.isEmpty) slot.set(ItemStack.EMPTY)
        else slot.setChanged()

        if (stack.count == returnStack.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        return returnStack
    }

    override fun removed(player: Player) {
        super.removed(player)
        // Save handler contents back to the item stack on close
        BookBagItem.saveContents(bagStack, handler)
    }

    override fun stillValid(player: Player): Boolean {
        // Invalidate if the player no longer holds the bag
        val main = player.mainHandItem
        val off  = player.offhandItem
        return main == bagStack || off == bagStack
    }
}
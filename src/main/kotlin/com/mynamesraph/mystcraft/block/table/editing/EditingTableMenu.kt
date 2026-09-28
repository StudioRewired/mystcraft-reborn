package com.mynamesraph.mystcraft.ui.menu

import com.mynamesraph.mystcraft.block.editing.EditingTableBlockEntity
import com.mynamesraph.mystcraft.block.editing.EditingTableBlockEntity.Companion.SLOT_BOOK
import com.mynamesraph.mystcraft.item.DescriptiveBookItem
import com.mynamesraph.mystcraft.registry.MystcraftMenus
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler

class EditingTableMenu(
    containerID: Int,
    private val inventory: Inventory,
    val pos: BlockPos,
    val container: IItemHandler
) : AbstractContainerMenu(MystcraftMenus.EDITING_TABLE_MENU.get(), containerID) {

    companion object {
        // Slot x positions — stride 18, starting at x=7
        val SLOT_XS = intArrayOf(7, 25, 43, 61, 79, 97, 115, 133)

        // Row y positions
        const val ROW_A_Y     = 89   // parameter items row 1 (slots 0–7)
        const val ROW_B_Y     = 107  // parameter items row 2 (slots 8–15) + book at x=151
        const val ROW_C_Y     = 139  // player inventory row 1
        const val ROW_D_Y     = 157  // player inventory row 2
        const val ROW_E_Y     = 175  // player inventory row 3
        const val ROW_F_Y     = 197  // hotbar

        const val BOOK_SLOT_X = 151
        const val CHECKMARK_X = 151
        const val CHECKMARK_Y = 89

        // Container slot index range for quick-move
        const val FIRST_PLAYER_SLOT = EditingTableBlockEntity.TOTAL_SLOTS  // 17
        const val LAST_PLAYER_SLOT  = FIRST_PLAYER_SLOT + 36               // 53
    }

    init {
        // ── Free parameter slots: row A (0–7) ────────────────────────────────
        for (i in 0..7) {
            addSlot(SlotItemHandler(container, i, SLOT_XS[i], ROW_A_Y))
        }

        // ── Free parameter slots: row B (8–15) ───────────────────────────────
        for (i in 0..7) {
            addSlot(SlotItemHandler(container, i + 8, SLOT_XS[i], ROW_B_Y))
        }

        // ── Book input slot (slot 16, x=151, y=107) ──────────────────────────
        addSlot(object : SlotItemHandler(container, SLOT_BOOK, BOOK_SLOT_X, ROW_B_Y) {
            override fun mayPlace(stack: ItemStack) = stack.item is DescriptiveBookItem
        })

        // ── Player inventory (rows C–E) ───────────────────────────────────────
        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(inventory, col + row * 9 + 9, 7 + col * 18, ROW_C_Y + row * 18))
            }
        }

        // ── Hotbar ────────────────────────────────────────────────────────────
        for (i in 0..8) {
            addSlot(Slot(inventory, i, 7 + i * 18, ROW_F_Y))
        }
    }

    constructor(
        containerID: Int,
        inventory: Inventory,
        buf: FriendlyByteBuf
    ) : this(containerID, inventory, buf.readBlockPos(), ItemStackHandler(EditingTableBlockEntity.TOTAL_SLOTS))

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var returnStack = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return returnStack
        if (!slot.hasItem()) return returnStack

        val stack = slot.item
        returnStack = stack.copy()

        when {
            // From player inventory → try parameter slots first, then book slot
            index >= FIRST_PLAYER_SLOT -> {
                if (stack.item is DescriptiveBookItem) {
                    if (!moveItemStackTo(stack, SLOT_BOOK, SLOT_BOOK + 1, false))
                        return ItemStack.EMPTY
                } else {
                    if (!moveItemStackTo(stack, 0, 16, false))
                        return ItemStack.EMPTY
                }
            }
            // From book slot → back to inventory
            index == SLOT_BOOK -> {
                if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, LAST_PLAYER_SLOT, true))
                    return ItemStack.EMPTY
            }
            // From parameter slots → back to inventory
            else -> {
                if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, LAST_PLAYER_SLOT, true))
                    return ItemStack.EMPTY
            }
        }

        if (stack.isEmpty) slot.set(ItemStack.EMPTY)
        else slot.setChanged()

        if (stack.count == returnStack.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        return returnStack
    }

    override fun stillValid(player: Player) = true
}
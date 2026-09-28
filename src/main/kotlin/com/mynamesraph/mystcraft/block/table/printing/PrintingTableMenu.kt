package com.mynamesraph.mystcraft.ui.menu

import com.mojang.datafixers.util.Pair
import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.block.printing.PrintingTableBlockEntity
import com.mynamesraph.mystcraft.registry.MystcraftMenus
import com.mynamesraph.mystcraft.registry.MystcraftTags
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler
import com.mynamesraph.mystcraft.item.BiomeEncyclopediaItem


class PrintingTableMenu(
    containerID: Int,
    private val inventory: Inventory,
    val pos: BlockPos,
    private val container: IItemHandler
) : AbstractContainerMenu(MystcraftMenus.PRINTING_TABLE_MENU.get(), containerID) {

    companion object {
        // Slot pixel positions match the anvil layout concept:
        // book on left, emerald center, output on right
        const val BOOK_SLOT_X    = 27
        const val BOOK_SLOT_Y    = 47
        const val EMERALD_SLOT_X = 76
        const val EMERALD_SLOT_Y = 47
        const val OUTPUT_SLOT_X  = 134
        const val OUTPUT_SLOT_Y  = 47

        val MISSING_BOOK_TX = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID, "item/empty_slot_book"
        )
        val MISSING_EMERALD_TX = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID, "item/emerald_greyedout"
        )
    }

    // Tracks which level instance owns the BE so onOutputTaken can reach it
    private val level = inventory.player.level()

    init {
        // Slot 0 — book input
        addSlot(object : SlotItemHandler(container, 0, BOOK_SLOT_X, BOOK_SLOT_Y) {
            override fun mayPlace(stack: ItemStack) =
                stack.`is`(MystcraftTags.LINKING_BOOK_TAG)
                        || stack.item == Items.WRITTEN_BOOK
                        || stack.item == Items.WRITABLE_BOOK
                        || stack.`is`(MystcraftTags.NOTEBOOK_TAG)

            override fun getNoItemIcon(): Pair<ResourceLocation, ResourceLocation> =
                Pair.of(InventoryMenu.BLOCK_ATLAS, MISSING_BOOK_TX)
        })

        // Slot 1 — emerald for clone, paper for transfer
        addSlot(object : SlotItemHandler(container, 1, EMERALD_SLOT_X, EMERALD_SLOT_Y) {
            override fun mayPlace(stack: ItemStack) =
                stack.item == Items.EMERALD || stack.item == Items.PAPER

            override fun getNoItemIcon(): Pair<ResourceLocation, ResourceLocation> =
                Pair.of(InventoryMenu.BLOCK_ATLAS, MISSING_EMERALD_TX)
        })

        // Slot 2 — output for cloning, destination input for transfer
        addSlot(object : SlotItemHandler(container, 2, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            override fun mayPlace(stack: ItemStack) =
                stack.`is`(MystcraftTags.LINKING_BOOK_TAG)
                        || stack.item is BiomeEncyclopediaItem

            override fun onTake(player: Player, stack: ItemStack) {
                val be = level.getBlockEntity(pos) as? PrintingTableBlockEntity
                val catalyst = be?.container?.getStackInSlot(1) ?: ItemStack.EMPTY
                if (catalyst.item == Items.EMERALD) {
                    be?.onOutputTaken()
                }
                super.onTake(player, stack)
            }

            override fun getNoItemIcon(): Pair<ResourceLocation, ResourceLocation> =
                Pair.of(InventoryMenu.BLOCK_ATLAS, MISSING_BOOK_TX)
        })


        // Player inventory (rows 0–2)
        for (j in 0..2) {
            for (k in 0..8) {
                addSlot(Slot(inventory, k + j * 9 + 9, 8 + k * 18, 84 + j * 18))
            }
        }

        // Hotbar
        for (i in 0..8) {
            addSlot(Slot(inventory, i, 8 + i * 18, 142))
        }
    }

    // Fallback constructor for network deserialisation
    constructor(
        containerID: Int,
        inventory: Inventory,
        buf: FriendlyByteBuf
    ) : this(containerID, inventory, buf.readBlockPos(), ItemStackHandler(3))

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var returnStack = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return returnStack
        if (!slot.hasItem()) return returnStack

        val stack = slot.item
        returnStack = stack.copy()

        when {
            // Taking from output — move to inventory
            index == 2 -> {
                if (!moveItemStackTo(stack, 3, slots.size, true)) return ItemStack.EMPTY
                slot.onQuickCraft(stack, returnStack)
            }
            // Moving from inventory
            index >= 3 -> {
                when {
                    stack.`is`(MystcraftTags.LINKING_BOOK_TAG)
                            || stack.item == Items.WRITTEN_BOOK
                            || stack.item == Items.WRITABLE_BOOK -> {
                        if (!moveItemStackTo(stack, 0, 1, false)) {
                            if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY
                        }
                    }
                    stack.item == Items.EMERALD -> {
                        if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY
                    }
                    stack.item == Items.PAPER -> {
                        if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY
                    }
                    else -> return ItemStack.EMPTY
                }
            }
            // Moving input slots back to inventory
            else -> if (!moveItemStackTo(stack, 3, slots.size, false)) return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.set(ItemStack.EMPTY)
        else slot.setChanged()

        if (stack.count == returnStack.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        return returnStack
    }

    override fun stillValid(player: Player) = true
}
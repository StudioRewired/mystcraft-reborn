package com.mynamesraph.mystcraft.ui.menu

import com.mojang.datafixers.util.Pair
import com.mynamesraph.mystcraft.Mystcraft
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
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler

class WritingDeskMenu(
    containerID:Int,
    inventory: Inventory,
    val pos: BlockPos,
    container:IItemHandler
): AbstractContainerMenu(MystcraftMenus.WRITING_DESK_MENU.get(),containerID) {

    companion object {
        val MISSING_BOOK_TX = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID,
            "item/empty_slot_book"
        )

        val MISSING_NOTEBOOK_TX = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID,
            "item/empty_slot_notebook"
        )
    }

    init {

        // book
        this.addSlot(object: SlotItemHandler(container,0,98,-1) {
            override fun mayPlace(stack: ItemStack): Boolean {
                return stack.`is`(MystcraftTags.LINKING_BOOK_TAG)
            }

            override fun getNoItemIcon(): Pair<ResourceLocation, ResourceLocation>? {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, MISSING_BOOK_TX)
            }
        })

        // notebook
        this.addSlot(object: SlotItemHandler(container,1,-84,-1) {
            override fun mayPlace(stack: ItemStack): Boolean {
                return stack.`is`(MystcraftTags.NOTEBOOK_TAG)
            }

            override fun getNoItemIcon(): Pair<ResourceLocation, ResourceLocation>? {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, MISSING_NOTEBOOK_TX)
            }
        })

        // inventory
        for (j in 0..2) {
            for (k in 0..8) {
                this.addSlot(Slot(inventory, k + j * 9 + 9, 99 + k * 18, j * 18 + 91))
            }
        }

        // hotbar
        for (i in 0..8) {
            this.addSlot(Slot(inventory, i, 99 + i * 18, 149))
        }
    }

    constructor(
        containerId: Int,
        inventory: Inventory,
        buf: FriendlyByteBuf
    ): this(containerId,inventory,buf.readBlockPos(),ItemStackHandler(2))

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }
}
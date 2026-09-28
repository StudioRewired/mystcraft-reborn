package com.mynamesraph.mystcraft.ui.menu

import com.mynamesraph.mystcraft.registry.MystcraftMenus
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack

class LinkingBookMenu(
    containerId: Int,
    val inventory: Inventory,
    val lecternPos: BlockPos,
): AbstractContainerMenu(MystcraftMenus.LINKING_BOOK_MENU.get(), containerId) {

    constructor(
        containerId: Int,
        inventory: Inventory,
        buf: FriendlyByteBuf
    ): this(containerId,inventory,buf.readBlockPos())

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }
}
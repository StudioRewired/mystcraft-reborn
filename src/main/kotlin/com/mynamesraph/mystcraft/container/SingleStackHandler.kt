package com.mynamesraph.mystcraft.container

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.ItemStackHandler

class SingleStackHandler: ItemStackHandler(1) {
    fun extractItem(amount: Int, simulate: Boolean): ItemStack {
        return super.extractItem(0, amount, simulate)
    }

    fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack {
        return super.insertItem(0, stack, simulate)
    }

    fun getItem(): ItemStack {
        return super.getStackInSlot(0)
    }
}
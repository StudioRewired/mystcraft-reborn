package com.mynamesraph.mystcraft.crafting

import com.mynamesraph.mystcraft.crafting.input.PlayerCraftingInput
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.CraftingContainer

object LocationalCraftingHelper {
    fun getPlayerCraftingInput(craftingContainer: CraftingContainer, player: Player) : PlayerCraftingInput {
        return PlayerCraftingInput.of(player,craftingContainer.width,craftingContainer.height,craftingContainer.items)
    }

    fun getPositionedPlayerCraftingInput(craftingContainer: CraftingContainer,player: Player): PlayerCraftingInput.Companion.Positioned {
        return PlayerCraftingInput.ofPositioned(player,craftingContainer.width,craftingContainer.height,craftingContainer.items)
    }
}
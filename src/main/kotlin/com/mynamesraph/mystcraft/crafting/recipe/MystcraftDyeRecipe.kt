package com.mynamesraph.mystcraft.crafting.recipe

import com.mynamesraph.mystcraft.item.MystcraftDyeing
import com.mynamesraph.mystcraft.registry.MystcraftRecipes
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

/** One book OR bag plus one dye, in either crafting grid. No locational crafting side effects. */
class MystcraftDyeRecipe(category: CraftingBookCategory) : CustomRecipe(category) {
    private fun inputs(input: CraftingInput): Pair<ItemStack, DyeItem>? {
        var original: ItemStack? = null
        var dye: DyeItem? = null
        for (i in 0 until input.size()) {
            val stack = input.getItem(i)
            if (stack.isEmpty) continue
            when {
                MystcraftDyeing.supports(stack) && original == null -> original = stack
                stack.item is DyeItem && dye == null -> dye = stack.item as DyeItem
                else -> return null // extra dyes, extra targets and unrelated items are not a match
            }
        }
        return Pair(original ?: return null, dye ?: return null)
    }

    override fun matches(input: CraftingInput, level: Level): Boolean = inputs(input) != null

    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        val (original, dye) = inputs(input) ?: return ItemStack.EMPTY
        return MystcraftDyeing.dyeCopy(original, dye.dyeColor)
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

    override fun getRemainingItems(input: CraftingInput): NonNullList<ItemStack> =
        NonNullList.withSize(input.size(), ItemStack.EMPTY)

    override fun getSerializer(): RecipeSerializer<*> = MystcraftRecipes.DYE_RECIPE_SERIALIZER.get()
}

package com.mynamesraph.mystcraft.crafting.input

import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.StackedContents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeInput
import org.jetbrains.annotations.NotNull
import kotlin.math.max
import kotlin.math.min

/**
 * Should act mostly the same as a CraftingInput except it can also store player information
 */
class PlayerCraftingInput(
    val player: Player?,
    val width: Int,
    val height: Int,
    @NotNull val items: List<ItemStack>
) :RecipeInput {

    companion object {

        @JvmRecord
        data class Positioned(val input: PlayerCraftingInput, val left: Int, val top: Int) {
            companion object {
                val EMPTY: Positioned = Positioned(PlayerCraftingInput.EMPTY, 0, 0)
            }
        }

        val EMPTY: PlayerCraftingInput = PlayerCraftingInput(null, 0,0,emptyList())

        fun ofPositioned(player: Player, width: Int, height: Int, items: List<ItemStack>): Positioned {
            if (width != 0 && height != 0) {
                var i = width - 1
                var j = 0
                var k = height - 1
                var l = 0

                for (i1 in 0 until height) {
                    var flag = true

                    for (j1 in 0 until width) {
                        val itemStack = items[j1 + i1 * width]
                        if (!itemStack.isEmpty) {
                            i = min(i.toDouble(), j1.toDouble()).toInt()
                            j = max(j.toDouble(), j1.toDouble()).toInt()
                            flag = false
                        }
                    }

                    if (!flag) {
                        k = min(k.toDouble(), i1.toDouble()).toInt()
                        l = max(l.toDouble(), i1.toDouble()).toInt()
                    }
                }

                val i2 = j - i + 1
                val j2 = l - k + 1
                if (i2 <= 0 || j2 <= 0) {
                    return Positioned.EMPTY
                } else if (i2 == width && j2 == height) {
                    return Positioned(PlayerCraftingInput(player,width, height, items), i, k)
                } else {
                    val list: MutableList<ItemStack> = ArrayList(i2 * j2)

                    for (k2 in 0 until j2) {
                        for (k1 in 0 until i2) {
                            val l1 = k1 + i + (k2 + k) * width
                            list.add(items[l1])
                        }
                    }

                    return Positioned(PlayerCraftingInput(player,i2, j2, list), i, k)
                }
            } else {
                return Positioned.EMPTY
            }
        }

        fun of(player: Player, width: Int, height: Int, items: List<ItemStack>): PlayerCraftingInput {
            return ofPositioned(player,width, height, items).input
        }
    }

    val stackedContents: StackedContents = StackedContents()
    val ingredientCount: Int

    init {
        var count = 0

        for (itemStack in items) {
            if (!itemStack.isEmpty) {
                count++
                this.stackedContents.accountStack(itemStack,1)
            }
        }

        ingredientCount = count
    }

    override fun getItem(index: Int): ItemStack {
        return items[index]
    }

    override fun size(): Int {
        return items.size
    }

    override fun isEmpty(): Boolean {
        return ingredientCount == 0
    }


    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else {
            if (other !is PlayerCraftingInput
            ) false
            else (width == other.width && height == other.height) && this.ingredientCount == other.ingredientCount && ItemStack.listMatches(
                this.items, other.items
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun hashCode(): Int {
        var i = ItemStack.hashStackList(this.items)
        i = 31 * i + this.width
        return 31 * i + this.height
    }

}
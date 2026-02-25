package com.mynamesraph.mystcraft.crafting.recipe

import com.mojang.logging.LogUtils
import com.mynamesraph.mystcraft.component.LocationComponent
import com.mynamesraph.mystcraft.component.LocationDisplayComponent
import com.mynamesraph.mystcraft.component.RotationComponent
import com.mynamesraph.mystcraft.crafting.input.PlayerCraftingInput
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.registry.MystcraftRecipes
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.neoforged.neoforge.common.util.RecipeMatcher
import org.joml.Vector3f
import com.mynamesraph.mystcraft.data.networking.packet.TriggerPreviewCapturePacket
import net.neoforged.neoforge.network.PacketDistributor
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/***
 * Shapeless recipe that sets a LocationComponent and a RotationComponent from the player to the output.
 */
class LocationalShapelessRecipe(
    val result: ItemStack,
    val ingredientsList: MutableList<Ingredient>
): Recipe<PlayerCraftingInput> {

    private val isSimple:Boolean = ingredientsList.stream().allMatch(Ingredient::isSimple)

    private val lazyIngredients: NonNullList<Ingredient> by lazy {
        val list = NonNullList.create<Ingredient>()
        for (ingredient in ingredientsList) {
            list.add(ingredient)
        }
        list
    }

    override fun assemble(input: PlayerCraftingInput, registries: HolderLookup.Provider): ItemStack {
        val itemStack = result.copy()
        val patchedComponents = PatchedDataComponentMap(itemStack.components)

        if (patchedComponents.get(MystcraftComponents.LOCATION.get()) != null) {
            patchedComponents.set(
                MystcraftComponents.LOCATION.get(),
                LocationComponent(
                    input.player!!.level().dimension(),
                    Vector3f(
                        input.player.x.toFloat(),
                        input.player.y.toFloat(),
                        input.player.z.toFloat()
                    )))
        }
        else {
            LogUtils.getLogger().error(
                "Attempted to craft an item that does not have a ${LocationComponent::class.simpleName} with a ${this::class.simpleName}." +
                        "Please add the component as a default component to the item before using this recipe."
            )
        }

        if (patchedComponents.get(MystcraftComponents.ROTATION.get()) != null) {
            patchedComponents.set(
                MystcraftComponents.ROTATION.get(),
                RotationComponent(
                    input.player!!.xRot,
                    input.player.yRot
                )
            )
        }
        else {
            LogUtils.getLogger().error(
                "Attempted to craft an item that does not have a ${RotationComponent::class.simpleName} with a ${this::class.simpleName}." +
                        "Please add the component as a default component to the item before using this recipe."
            )
        }

        if (patchedComponents.get(MystcraftComponents.LOCATION_DISPLAY.get()) != null) {

            var id = ""
            val text = if (input.player!!.level().dimension().location().toLanguageKey().contains("mystcraft_reborn.age_")) {
                id = input.player.level().dimension().location().toLanguageKey().removePrefix("mystcraft_reborn.age_")
                "mystcraft_reborn.age"
            }
            else {
                input.player.level().dimension().location().toLanguageKey()
            }

            patchedComponents.set(
                MystcraftComponents.LOCATION_DISPLAY.get(),
                LocationDisplayComponent(
                    Component.translatable(text,id)
                        .withStyle(Style.EMPTY.withItalic(false).withColor(0xAAAAAA)
                    )
                )
            )
        }
        else {
            LogUtils.getLogger().error(
                "Attempted to craft an item that does not have a ${LocationDisplayComponent::class.simpleName} with a ${this::class.simpleName}." +
                        "Please add the component as a default component to the item before using this recipe."
            )
        }


        itemStack.applyComponentsAndValidate(patchedComponents.asPatch())

        if (input.player != null && input.player.level() is ServerLevel) {
            val serverPlayer = input.player as ServerPlayer
            serverPlayer.server.execute {
                println("Mystcraft DEBUG: Sending TriggerPreviewCapturePacket to ${serverPlayer.name.string}")
                PacketDistributor.sendToPlayer(serverPlayer, TriggerPreviewCapturePacket())
            }
        }


        return itemStack
    }


    override fun getIngredients(): NonNullList<Ingredient>  {
        return lazyIngredients
    }

    override fun matches(input: PlayerCraftingInput, level: Level): Boolean {
        // Prevent crafting more than one linking book at a time
        for (i in 0 until input.size()) {
            if (input.getItem(i).count > 1) return false
        }
        if (input.ingredientCount != this.ingredientsList.size) {
            return false
        } else if (!isSimple) {
            val nonEmptyItems = ArrayList<ItemStack>(input.ingredientCount)
            for (item in input.items) if (!item.isEmpty) nonEmptyItems.add(item)
            return RecipeMatcher.findMatches(nonEmptyItems, this.ingredientsList) != null
        } else {
            return if (input.size() == 1 && this.ingredientsList.size == 1
            ) this.ingredientsList.first().test(input.getItem(0))
            else input.stackedContents.canCraft(this, null)
        }
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean {
        return width * height >= this.ingredientsList.size
    }

    override fun getResultItem(registries: HolderLookup.Provider): ItemStack {
        return result
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return MystcraftRecipes.LOCATIONAL_SHAPELESS_RECIPE_SERIALIZER.get()
    }

    override fun getType(): RecipeType<*> {
        return MystcraftRecipes.LOCATIONAL_RECIPE_TYPE.get()
    }

    override fun isSpecial(): Boolean {
        return true
    }

    override fun getRemainingItems(input: PlayerCraftingInput): NonNullList<ItemStack> {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY)
    }
}

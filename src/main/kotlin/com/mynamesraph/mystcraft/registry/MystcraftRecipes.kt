package com.mynamesraph.mystcraft.registry

import com.mynamesraph.mystcraft.Mystcraft.Companion.MOD_ID
import com.mynamesraph.mystcraft.crafting.recipe.MystcraftDyeRecipe
import com.mynamesraph.mystcraft.crafting.recipe.LocationalShapelessRecipe
import com.mynamesraph.mystcraft.crafting.serializer.LocationalShapelessRecipeSerializer
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object MystcraftRecipes {
    private val RECIPE_TYPES: DeferredRegister<RecipeType<*>> = DeferredRegister.create(
        Registries.RECIPE_TYPE,
        MOD_ID
    )
    private val RECIPE_SERIALIZERS: DeferredRegister<RecipeSerializer<*>> = DeferredRegister.create(
        Registries.RECIPE_SERIALIZER,
        MOD_ID
    )

    val LOCATIONAL_RECIPE_TYPE: Supplier<RecipeType<LocationalShapelessRecipe>> = RECIPE_TYPES.register(
        "locational_shapeless_crafting",
        Supplier {
            RecipeType.simple(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "locational_shapeless_crafting")
            )
        }
    )

    val LOCATIONAL_SHAPELESS_RECIPE_SERIALIZER: Supplier<RecipeSerializer<LocationalShapelessRecipe>> = RECIPE_SERIALIZERS.register(
        "locational_shapeless_crafting",
        ::LocationalShapelessRecipeSerializer
    )

    // CustomRecipe uses vanilla RecipeType.CRAFTING, so both the 2x2 and 3x3 grids handle it.
    val DYE_RECIPE_SERIALIZER: Supplier<RecipeSerializer<MystcraftDyeRecipe>> = RECIPE_SERIALIZERS.register(
        "dye_book_or_bag",
        Supplier { SimpleCraftingRecipeSerializer(::MystcraftDyeRecipe) }
    )

    fun register(eventBus: IEventBus) {
        RECIPE_TYPES.register(eventBus)
        RECIPE_SERIALIZERS.register(eventBus)
    }
}
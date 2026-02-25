package com.mynamesraph.mystcraft.crafting.serializer

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.mynamesraph.mystcraft.crafting.recipe.LocationalShapelessRecipe
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer

class LocationalShapelessRecipeSerializer: RecipeSerializer<LocationalShapelessRecipe> {

    companion object {
        private val CODEC: MapCodec<LocationalShapelessRecipe> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ItemStack.CODEC.fieldOf("result").forGetter(LocationalShapelessRecipe::result),
                Ingredient.LIST_CODEC_NONEMPTY.fieldOf("ingredients").forGetter(LocationalShapelessRecipe::ingredientsList)
            ).apply(instance,::LocationalShapelessRecipe)
        }

        private val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, LocationalShapelessRecipe> = StreamCodec.composite(
            ItemStack.STREAM_CODEC, LocationalShapelessRecipe::result,
            ByteBufCodecs.fromCodec(Ingredient.LIST_CODEC_NONEMPTY),  LocationalShapelessRecipe::ingredientsList,
            ::LocationalShapelessRecipe
        )
    }

    override fun codec(): MapCodec<LocationalShapelessRecipe> {
        return CODEC
    }

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, LocationalShapelessRecipe> {
        return STREAM_CODEC
    }

}
package com.mynamesraph.mystcraft.mixin;

import com.mynamesraph.mystcraft.crafting.LocationalCraftingHelper;
import com.mynamesraph.mystcraft.crafting.input.PlayerCraftingInput;
import com.mynamesraph.mystcraft.crafting.recipe.LocationalShapelessRecipe;
import com.mynamesraph.mystcraft.registry.MystcraftRecipes;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin{

    @Shadow @Final private CraftingContainer craftSlots;

    public CraftingMenuMixin() {}

    @Inject(at = @At("HEAD"), method = "slotChangedCraftingGrid",cancellable = true)
    private static void slotChangedCraftingGrid(
            AbstractContainerMenu menu,
            Level level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci
    ) {
        if (!level.isClientSide) {
            PlayerCraftingInput input = LocationalCraftingHelper.INSTANCE.getPlayerCraftingInput(craftSlots,player);
            ServerPlayer serverplayer = (ServerPlayer)player;
            ItemStack itemstack = ItemStack.EMPTY;
            RecipeManager recipes = level.getRecipeManager();

            Optional<RecipeHolder<LocationalShapelessRecipe>> optional = recipes.getRecipeFor(
                    MystcraftRecipes.INSTANCE.getLOCATIONAL_RECIPE_TYPE().get(),
                    input,
                    level
            );

            if (optional.isPresent()) {
                RecipeHolder<LocationalShapelessRecipe> recipeHolder = optional.get();
                LocationalShapelessRecipe craftingRecipe = recipeHolder.value();

                if (resultSlots.setRecipeUsed(level,serverplayer,recipeHolder)) {
                    ItemStack itemStack1 = craftingRecipe.assemble(input,level.registryAccess());
                    if (itemStack1.isItemEnabled(level.enabledFeatures())) {
                        itemstack = itemStack1;
                    }
                }

                resultSlots.setItem(0,itemstack);
                menu.setRemoteSlot(0,itemstack);
                serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, itemstack));
                ci.cancel();
            }

        }
    }
}

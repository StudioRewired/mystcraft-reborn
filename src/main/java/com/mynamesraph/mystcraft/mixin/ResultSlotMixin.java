package com.mynamesraph.mystcraft.mixin;

import com.mynamesraph.mystcraft.component.LocationComponent;
import com.mynamesraph.mystcraft.crafting.LocationalCraftingHelper;
import com.mynamesraph.mystcraft.crafting.input.PlayerCraftingInput;
import com.mynamesraph.mystcraft.registry.MystcraftComponents;
import com.mynamesraph.mystcraft.registry.MystcraftRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Shadow protected abstract void checkTakeAchievements(ItemStack stack);

    @Shadow @Final private CraftingContainer craftSlots;

    @Shadow @Final private Player player;

    ResultSlotMixin() {}

    @Inject(at= @At("HEAD"), method="onTake", cancellable = true)
    public void onTake(Player player, ItemStack stack, CallbackInfo ci) {
        @Nullable LocationComponent location = stack.get(MystcraftComponents.INSTANCE.getLOCATION());

        if (location != null) {
            this.checkTakeAchievements(stack);
            PlayerCraftingInput.Companion.Positioned craftinginput$positioned =
                    LocationalCraftingHelper.INSTANCE.getPositionedPlayerCraftingInput(craftSlots,player);
            PlayerCraftingInput craftinginput = craftinginput$positioned.input();
            int i = craftinginput$positioned.left();
            int j = craftinginput$positioned.top();
            net.neoforged.neoforge.common.CommonHooks.setCraftingPlayer(player);
            NonNullList<ItemStack> nonnulllist = player.level().getRecipeManager().getRemainingItemsFor(MystcraftRecipes.INSTANCE.getLOCATIONAL_RECIPE_TYPE().get(), craftinginput, player.level());
            net.neoforged.neoforge.common.CommonHooks.setCraftingPlayer(null);

            for (int k = 0; k < craftinginput.getHeight(); k++) {
                for (int l = 0; l < craftinginput.getWidth(); l++) {
                    int i1 = l + i + (k + j) * this.craftSlots.getWidth();
                    ItemStack itemstack = this.craftSlots.getItem(i1);
                    ItemStack itemstack1 = nonnulllist.get(l + k * craftinginput.getWidth());
                    if (!itemstack.isEmpty()) {
                        this.craftSlots.removeItem(i1, 1);
                        itemstack = this.craftSlots.getItem(i1);
                    }

                    if (!itemstack1.isEmpty()) {
                        if (itemstack.isEmpty()) {
                            this.craftSlots.setItem(i1, itemstack1);
                        } else if (ItemStack.isSameItemSameComponents(itemstack, itemstack1)) {
                            itemstack1.grow(itemstack.getCount());
                            this.craftSlots.setItem(i1, itemstack1);
                        } else if (!this.player.getInventory().add(itemstack1)) {
                            this.player.drop(itemstack1, false);
                        }
                    }
                }
            }
            ci.cancel();
        }

    }
}

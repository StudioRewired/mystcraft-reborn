package com.mynamesraph.mystcraft.mixin;

import com.mynamesraph.mystcraft.data.saved.SpongeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks water (and only water) from spreading into any cell registered as
 * protected by a classic sponge. Injected at HEAD of canSpreadTo so the
 * registry lookup is the first thing that happens — if protected, vanilla's
 * spread logic never runs for that position.
 *
 * canSpreadTo is called both by spreadTo (general flow) and by getNewLiquid
 * (waterlog evaluation), so this single hook covers both the "water flows
 * into the cube" and "waterloggable block tries to fill itself" cases.
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Inject(method = "canSpreadTo", at = @At("HEAD"), cancellable = true)
    private void mystcraft_reborn$blockSpongeProtectedSpread(
            BlockGetter level,
            BlockPos fromPos,
            BlockState fromBlockState,
            Direction direction,
            BlockPos toPos,
            BlockState toBlockState,
            FluidState toFluidState,
            Fluid fluid,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Only block water — let lava and modded fluids spread normally.
        if (fluid != Fluids.WATER && fluid != Fluids.FLOWING_WATER) return;

        // canSpreadTo gets called on both sides; only the server has the
        // SavedData and is the source of truth.
        if (!(level instanceof ServerLevel serverLevel)) return;

        SpongeRegistry registry = serverLevel.getServer().overworld().getDataStorage()
                .computeIfAbsent(SpongeRegistry.FACTORY, SpongeRegistry.FILE_NAME);

        if (registry.isProtected(toPos)) {
            cir.setReturnValue(false);
        }
    }
}
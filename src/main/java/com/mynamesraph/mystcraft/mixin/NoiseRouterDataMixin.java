package com.mynamesraph.mystcraft.mixin;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseRouterData.class)
public interface NoiseRouterDataMixin {

    @Accessor("SHIFT_X")
    static ResourceKey<DensityFunction> getSHIFT_X() {
        throw new AssertionError();
    }

    @Accessor("SHIFT_Z")
    static ResourceKey<DensityFunction> getSHIFT_Z() {
        throw new AssertionError();
    }

    @Accessor("DEPTH_LARGE")
    static ResourceKey<DensityFunction> getDEPTH_LARGE() {
        throw new AssertionError();
    }

    @Accessor("DEPTH_AMPLIFIED")
    static ResourceKey<DensityFunction> getDEPTH_AMPLIFIED() {
        throw new AssertionError();
    }

    @Accessor("FACTOR_LARGE")
    static ResourceKey<DensityFunction> getFACTOR_LARGE() {
        throw new AssertionError();
    }

    @Accessor("FACTOR_AMPLIFIED")
    static ResourceKey<DensityFunction> getFACTOR_AMPLIFIED() {
        throw new AssertionError();
    }

    @Accessor("SLOPED_CHEESE")
    static ResourceKey<DensityFunction> getSLOPED_CHEESE() {
        throw new AssertionError();
    }

    @Accessor("SLOPED_CHEESE_LARGE")
    static ResourceKey<DensityFunction> getSLOPED_CHEESE_LARGE() {
        throw new AssertionError();
    }

    @Accessor("SLOPED_CHEESE_AMPLIFIED")
    static ResourceKey<DensityFunction> getSLOPED_CHEESE_AMPLIFIED() {
        throw new AssertionError();
    }

    @Accessor("ENTRANCES")
    static ResourceKey<DensityFunction> getENTRANCES() {
        throw new AssertionError();
    }

    @Accessor("NOODLE")
    static ResourceKey<DensityFunction> getNOODLE() {
        throw new AssertionError();
    }

    @Accessor("Y")
    static ResourceKey<DensityFunction> getY() {
        throw new AssertionError();
    }


    @Invoker("overworld")
    static NoiseRouter invokeOverworld(
            HolderGetter<DensityFunction> densityFunctions,
            HolderGetter<NormalNoise.NoiseParameters> noiseParameters,
            boolean large,
            boolean amplified
    ) {
        throw new AssertionError();
    }

    @Invoker("none")
    static NoiseRouter invokeNone() {
        throw new AssertionError();
    }

    @Invoker("getFunction")
    static DensityFunction invokeGetFunction(
            HolderGetter<DensityFunction> densityFunctions,
            ResourceKey<DensityFunction> key
    ) {
        throw new AssertionError();
    }

    @Invoker("slideOverworld")
    static DensityFunction invokeSlideOverworld(boolean amplified, DensityFunction densityFunction) {
        throw new AssertionError();
    }

    @Invoker("noiseGradientDensity")
    static DensityFunction invokeNoiseGradientDensity(DensityFunction minFunction,DensityFunction maxFunction) {
        throw new AssertionError();
    }

    @Invoker("postProcess")
    static DensityFunction invokePostProcess(DensityFunction densityFunction) {
        throw new  AssertionError();
    }

    @Invoker("underground")
    static DensityFunction invokeUnderground(
            HolderGetter<DensityFunction> densityFunctions,
            HolderGetter<NormalNoise.NoiseParameters> noiseParameters,
            DensityFunction p_256658_
    ) {
        throw new AssertionError();
    }

    @Invoker("yLimitedInterpolatable")
    static DensityFunction invokeYLimitedInterpolatable(
            DensityFunction input,
            DensityFunction whenInRange,
            int minY,
            int maxY,
            int whenOutOfRange
    ) {
        throw new AssertionError();
    }

}

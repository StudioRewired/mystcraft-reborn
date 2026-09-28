package com.mynamesraph.mystcraft.data.datagen.datapack

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.registry.MystcraftTrims
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider
import java.util.concurrent.CompletableFuture

class MystcraftDatapackProvider(
    output: PackOutput,
    registries: CompletableFuture<HolderLookup.Provider>
) : DatapackBuiltinEntriesProvider(output, registries, BUILDER, setOf(Mystcraft.MOD_ID)) {

    companion object {
        val BUILDER: RegistrySetBuilder = RegistrySetBuilder().add(Registries.TRIM_MATERIAL,MystcraftTrims::bootstrap)
    }
}
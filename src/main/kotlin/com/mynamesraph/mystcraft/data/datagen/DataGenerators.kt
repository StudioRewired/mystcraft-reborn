package com.mynamesraph.mystcraft.data.datagen

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.data.datagen.block.MystcraftBlockLootTableProvider
import com.mynamesraph.mystcraft.data.datagen.block.MystcraftBlockStateProvider
import com.mynamesraph.mystcraft.data.datagen.block.MystcraftBlockTagProvider
import com.mynamesraph.mystcraft.data.datagen.datapack.MystcraftDatapackProvider
import com.mynamesraph.mystcraft.data.datagen.item.MystcraftItemModelProvider
import com.mynamesraph.mystcraft.data.datagen.item.MystcraftItemTagProvider
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.data.event.GatherDataEvent
import java.util.*

@EventBusSubscriber(modid = Mystcraft.MOD_ID,bus= EventBusSubscriber.Bus.MOD)
object DataGenerators {

    @SubscribeEvent
    fun gatherData(event:GatherDataEvent) {
        val generator = event.generator
        val packOutput= generator.packOutput
        val existingFileHelper = event.existingFileHelper
        val lookupProvider = event.lookupProvider

        generator.addProvider(
            event.includeServer(),
            LootTableProvider(
                packOutput,
                Collections.emptySet(),
                listOf(
                    LootTableProvider.SubProviderEntry(
                        ::MystcraftBlockLootTableProvider,
                        LootContextParamSets.BLOCK
                    )
                ),
                lookupProvider
            )
        )

        val blockTagProvider = MystcraftBlockTagProvider(packOutput,lookupProvider,existingFileHelper)
        generator.addProvider(event.includeServer(),blockTagProvider)
        generator.addProvider(event.includeServer(), MystcraftItemTagProvider(
            packOutput,
            lookupProvider,
            blockTagProvider.contentsGetter(),
            existingFileHelper
        ))

        generator.addProvider(event.includeClient(), MystcraftBlockStateProvider(packOutput,existingFileHelper))
        generator.addProvider(event.includeClient(), MystcraftItemModelProvider(packOutput,existingFileHelper))

        generator.addProvider(event.includeServer(), MystcraftDatapackProvider(packOutput,lookupProvider))
    }
}
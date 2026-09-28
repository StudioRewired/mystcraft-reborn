package com.mynamesraph.mystcraft.worldgen

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.data.saved.NoMobDimensions
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.saveddata.SavedData.Factory
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent

@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object MobSpawnSuppressor {

    @SubscribeEvent
    fun onMobFinalizeSpawn(event: FinalizeSpawnEvent) {
        val levelAccessor = event.level
        if (levelAccessor !is ServerLevelAccessor) return

        val serverLevel = levelAccessor.level as? ServerLevel ?: return

        val dimKey = serverLevel.dimension()
        if (dimKey.location().namespace != Mystcraft.MOD_ID) return

        val noMobData = serverLevel.server.overworld().dataStorage.computeIfAbsent(
            Factory({ NoMobDimensions() }, { tag, _ -> loadNoMobDimensions(tag) }),
            NoMobDimensions.FILE_NAME
        )

        if (noMobData.contains(dimKey.location())) {
            event.setSpawnCancelled(true)
        }
    }

    private fun loadNoMobDimensions(tag: CompoundTag): NoMobDimensions {
        val data = NoMobDimensions()
        val list = tag.getList("dimensions", Tag.TAG_STRING.toInt())
        for (i in 0 until list.size) {
            val loc = ResourceLocation.tryParse(list.getString(i))
            if (loc != null) data.dimensions.add(loc)
        }
        return data
    }
}
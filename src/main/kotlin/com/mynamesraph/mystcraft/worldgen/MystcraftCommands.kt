package com.mynamesraph.mystcraft.commands

import com.mynamesraph.mystcraft.data.saved.IsolateWorldgenData
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mynamesraph.mystcraft.data.saved.HeadlampLightData
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.registries.Registries
import net.commoble.infiniverse.api.InfiniverseAPI

object MystcraftCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("mystcraft")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("isolateWorldgen")
                        .then(
                            Commands.argument("value", BoolArgumentType.bool())
                                .executes { ctx ->
                                    val value = BoolArgumentType.getBool(ctx, "value")
                                    val server = ctx.source.server
                                    val data = server.overworld().dataStorage
                                        .computeIfAbsent(IsolateWorldgenData.FACTORY, IsolateWorldgenData.FILE_NAME)
                                    data.isolate = value
                                    data.setDirty()
                                    ctx.source.sendSystemMessage(
                                        Component.literal("Mystcraft age worldgen isolation set to: $value")
                                    )
                                    1
                                }
                        )
                        .executes { ctx ->
                            val server = ctx.source.server
                            val data = server.overworld().dataStorage
                                .computeIfAbsent(IsolateWorldgenData.FACTORY, IsolateWorldgenData.FILE_NAME)
                            ctx.source.sendSystemMessage(
                                Component.literal("Mystcraft age worldgen isolation is currently: ${data.isolate}")
                            )
                            1
                        }
                )
                .then(
                    Commands.literal("deleteAge")
                        .then(
                            // Accepts just the age number, e.g. /mystcraft deleteAge 3
                            // which resolves to mystcraft_reborn:age_3
                            Commands.argument("id", StringArgumentType.word())
                                .executes { ctx ->
                                    val id = StringArgumentType.getString(ctx, "id")
                                    val server = ctx.source.server

                                    val location = ResourceLocation.tryParse("mystcraft_reborn:age_$id")
                                        ?: run {
                                            ctx.source.sendSystemMessage(
                                                Component.literal("Invalid age id: $id")
                                            )
                                            return@executes 0
                                        }

                                    val levelKey = ResourceKey.create(Registries.DIMENSION, location)

                                    // Verify the dimension actually exists before attempting removal
                                    if (server.getLevel(levelKey) == null) {
                                        ctx.source.sendSystemMessage(
                                            Component.literal("Age $id does not exist or is not loaded: $location")
                                        )
                                        return@executes 0
                                    }

                                    InfiniverseAPI.get().markDimensionForUnregistration(server, levelKey)

                                    ctx.source.sendSystemMessage(
                                        Component.literal("Age $id ($location) has been marked for deletion. It will be removed on next server save/shutdown.")
                                    )
                                    1
                                }
                        )
                )
                .then(
                    Commands.literal("headlampLevel")
                        .then(
                            Commands.argument("level", IntegerArgumentType.integer(
                                HeadlampLightData.MIN_LIGHT_LEVEL,
                                HeadlampLightData.MAX_LIGHT_LEVEL
                            ))
                                .executes { ctx ->
                                    val level = IntegerArgumentType.getInteger(ctx, "level")
                                    val server = ctx.source.server
                                    val data = server.overworld().dataStorage
                                        .computeIfAbsent(HeadlampLightData.FACTORY, HeadlampLightData.FILE_NAME)
                                    data.lightLevel = level
                                    data.setDirty()
                                    ctx.source.sendSystemMessage(
                                        Component.literal("Headlamp light level set to: $level")
                                    )
                                    1
                                }
                        )
                        .executes { ctx ->
                            val server = ctx.source.server
                            val data = server.overworld().dataStorage
                                .computeIfAbsent(HeadlampLightData.FACTORY, HeadlampLightData.FILE_NAME)
                            ctx.source.sendSystemMessage(
                                Component.literal("Headlamp light level is currently: ${data.lightLevel}")
                            )
                            1
                        }
                )
        )
    }
}
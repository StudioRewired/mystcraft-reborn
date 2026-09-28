package com.mynamesraph.mystcraft.commands

import com.mynamesraph.mystcraft.data.saved.IsolateWorldgenData
import com.mynamesraph.mystcraft.worldgen.AgeImporter
import net.minecraft.core.component.DataComponents
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
import com.mynamesraph.mystcraft.data.saved.SpongeRadiusData

object MystcraftCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("mystcraft")
                .then(
                    Commands.literal("rename")
                        .then(Commands.literal("clear").executes { rename(it.source, null) })
                        .then(Commands.literal("--clear").executes { rename(it.source, null) })
                        .then(
                            Commands.argument("name", StringArgumentType.greedyString())
                                .executes { rename(it.source, StringArgumentType.getString(it, "name")) }
                        )
                )
                .then(
                    Commands.literal("importAge")
                        .requires { it.hasPermission(2) }
                        .then(
                            Commands.argument("folderName", StringArgumentType.word())
                                .executes { AgeImporter.start(it.source, StringArgumentType.getString(it, "folderName")) }
                        )
                )
                .then(
                    Commands.literal("isolateWorldgen")
                        .requires { it.hasPermission(2) }
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
                        .requires { it.hasPermission(2) }
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
                        .requires { it.hasPermission(2) }
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
                .then(
                    Commands.literal("spongeRadius")
                        .requires { it.hasPermission(2) }
                        .then(
                            Commands.argument(
                                "radius",
                                IntegerArgumentType.integer(
                                    SpongeRadiusData.MIN_RADIUS,
                                    SpongeRadiusData.MAX_RADIUS
                                )
                            )
                                .executes { ctx ->
                                    val radius = IntegerArgumentType.getInteger(ctx, "radius")
                                    val server = ctx.source.server
                                    val data = server.overworld().dataStorage
                                        .computeIfAbsent(SpongeRadiusData.FACTORY, SpongeRadiusData.FILE_NAME)
                                    data.radius = radius
                                    ctx.source.sendSystemMessage(
                                        Component.literal("Classic sponge drain radius set to: $radius")
                                    )
                                    1
                                }
                        )
                        .executes { ctx ->
                            val server = ctx.source.server
                            val data = server.overworld().dataStorage
                                .computeIfAbsent(SpongeRadiusData.FACTORY, SpongeRadiusData.FILE_NAME)
                            ctx.source.sendSystemMessage(
                                Component.literal("Classic sponge drain radius is currently: ${data.radius}")
                            )
                            1
                        }
                )

        )
    }

    private fun rename(source: CommandSourceStack, name: String?): Int {
        val player = source.playerOrException
        val stack = player.mainHandItem
        if (stack.isEmpty) {
            source.sendFailure(Component.literal("Hold an item in your main hand before using /mystcraft rename."))
            return 0
        }
        if (name != null && name.isBlank()) {
            source.sendFailure(Component.literal("The name must not be empty. Use /mystcraft rename clear to remove it."))
            return 0
        }
        if (name == null) stack.remove(DataComponents.CUSTOM_NAME)
        else stack.set(DataComponents.CUSTOM_NAME, Component.literal(name))
        player.inventory.setChanged()
        player.containerMenu.broadcastChanges()
        if (player.containerMenu !== player.inventoryMenu) player.inventoryMenu.broadcastChanges()
        source.sendSuccess({
            Component.literal(if (name == null) "Custom name cleared: " else "Renamed held item: ")
                .append(stack.hoverName)
        }, false)
        return 1
    }

}
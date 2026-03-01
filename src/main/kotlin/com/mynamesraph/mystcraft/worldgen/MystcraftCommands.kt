package com.mynamesraph.mystcraft.commands

import com.mynamesraph.mystcraft.data.saved.IsolateWorldgenData
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import com.mojang.brigadier.arguments.BoolArgumentType

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
                        // Also allow querying the current value
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
        )
    }
}
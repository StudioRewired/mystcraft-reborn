package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.data.networking.packet.AgeDeleteConfirmedPacket
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.PacketDistributor

class ConfirmAgeDeleteScreen(
    private val dimensionId: ResourceLocation,
    private val receptaclePos: BlockPos
) : Screen(Component.literal("Delete Age")) {

    override fun init() {
        super.init()

        addRenderableWidget(
            Button.builder(Component.literal("Yes, delete this age")) {
                PacketDistributor.sendToServer(
                    AgeDeleteConfirmedPacket(dimensionId, receptaclePos)
                )
                onClose()
            }
                .pos(width / 2 - 155, height / 2 + 10)
                .size(150, 20)
                .build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("Cancel")) {
                onClose()
            }
                .pos(width / 2 + 5, height / 2 + 10)
                .size(150, 20)
                .build()
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // No background blur — just a dark translucent overlay
        guiGraphics.fill(0, 0, width, height, 0xA0000000.toInt())

        guiGraphics.drawCenteredString(
            font,
            Component.literal("Permanently delete Age: ${dimensionId.path}?"),
            width / 2,
            height / 2 - 20,
            0xFFFFFF
        )
        guiGraphics.drawCenteredString(
            font,
            Component.literal("This cannot be undone. All portals to this age will stop working."),
            width / 2,
            height / 2 - 5,
            0xFF5555
        )

        // Render widgets manually since we're not calling super.render
        for (widget in renderables) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick)
        }
    }

    override fun isPauseScreen() = true
}
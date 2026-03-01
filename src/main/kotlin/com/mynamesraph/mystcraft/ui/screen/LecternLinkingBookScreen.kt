package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.client.LinkingBookPreviewRenderer
import com.mynamesraph.mystcraft.component.LocationComponent
import com.mynamesraph.mystcraft.component.LocationDisplayComponent
import com.mynamesraph.mystcraft.data.networking.packet.LinkingBookLecternTravelPacket
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.ui.drawCenteredStringNoDropShadow
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.neoforged.neoforge.network.PacketDistributor
import net.minecraft.core.BlockPos

class LecternLinkingBookScreen(
    private val lecternPos: BlockPos,
    private val bookStack: ItemStack,
    title: Component
) : Screen(title) {

    private val TEXTURE = ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "textures/gui/book/linking_book.png")
    private val TEXTURE_UNUSABLE = ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "textures/gui/book/linking_book_unusable.png")

    private val previewRenderer = LinkingBookPreviewRenderer()

    private var BACKGROUND_X = 0
    private var BACKGROUND_Y = 0
    private var TITLE_X = 0
    private var TITLE_Y = 0
    private var BUTTON_X = 0
    private var BUTTON_Y = 0
    private var PREVIEW_X = 0
    private var PREVIEW_Y = 0
    private val PREVIEW_WIDTH = 84
    private val PREVIEW_HEIGHT = 60

    override fun init() {
        super.init()

        BACKGROUND_X = width / 2 - 128
        BACKGROUND_Y = height / 2 - 103
        TITLE_X = BACKGROUND_X + 70
        TITLE_Y = BACKGROUND_Y + 20
        BUTTON_X = BACKGROUND_X + 146
        BUTTON_Y = BACKGROUND_Y + 32
        PREVIEW_X = BACKGROUND_X + 143
        PREVIEW_Y = BACKGROUND_Y + 28

        addWidget(
            Button.builder(
                Component.translatableWithFallback("narration.mystcraft_reborn.linking_book_travel", "Travel")
            ) {
                PacketDistributor.sendToServer(LinkingBookLecternTravelPacket(lecternPos))
                onClose()
            }
                .pos(BUTTON_X, BUTTON_Y)
                .size(80, 48)
                .build()
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        val book = bookStack

        // Preview behind book texture
        val previewComponent = book.components.get(MystcraftComponents.PREVIEW_IMAGE.get())
        previewRenderer.render(guiGraphics, previewComponent, PREVIEW_X, PREVIEW_Y, PREVIEW_WIDTH, PREVIEW_HEIGHT)

        // Book texture
        val locationComponent = book.components.get(MystcraftComponents.LOCATION.get())
        val player = Minecraft.getInstance().player
        if (locationComponent is LocationComponent && player != null) {
            if (player.level().dimension() == locationComponent.levelKey) {
                guiGraphics.blit(TEXTURE_UNUSABLE, BACKGROUND_X, BACKGROUND_Y, 0, 0, 256, 181)
            } else {
                guiGraphics.blit(TEXTURE, BACKGROUND_X, BACKGROUND_Y, 0, 0, 256, 181)
            }
        } else {
            guiGraphics.blit(TEXTURE, BACKGROUND_X, BACKGROUND_Y, 0, 0, 256, 181)
        }

        // Title
        val titleComponent = book.components.get(MystcraftComponents.LOCATION_DISPLAY.get())
        if (titleComponent is LocationDisplayComponent) {
            val styledTitle = titleComponent.name.copy().withStyle(
                Style.EMPTY.withColor(0x303030).withUnderlined(true).withBold(true)
            )
            drawCenteredStringNoDropShadow(guiGraphics, font, styledTitle, TITLE_X, TITLE_Y, 0)
        }
    }

    override fun removed() {
        previewRenderer.release()
        super.removed()
    }
}
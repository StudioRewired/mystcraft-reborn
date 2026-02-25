package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.data.networking.packet.LinkingBookLecternTravelPacket
import com.mynamesraph.mystcraft.ui.drawCenteredStringNoDropShadow
import com.mynamesraph.mystcraft.ui.menu.LinkingBookMenu
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.neoforged.neoforge.network.PacketDistributor

class LecternLinkingBookScreen(
    menu: LinkingBookMenu,
    playerInventory: Inventory,
    title: Component,
) : AbstractContainerScreen<LinkingBookMenu>(menu, playerInventory, title) {

    private val TEXTURE: ResourceLocation = ResourceLocation.fromNamespaceAndPath("mystcraft_reborn","textures/gui/book/linking_book.png")
    private val TEXTURE_UNUSABLE: ResourceLocation = ResourceLocation.fromNamespaceAndPath("mystcraft_reborn","textures/gui/book/linking_book_unusable.png")


    private var BACKGROUND_Y:Int = 0
    private var BACKGROUND_X:Int = 0

    private var BUTTON_X = 0
    private var BUTTON_Y = 0

    override fun init() {
        super.init()

        BACKGROUND_X = (Minecraft.getInstance().screen!!.width /2) - 128
        BACKGROUND_Y = (Minecraft.getInstance().screen!!.height /2) - 103

        BUTTON_X = BACKGROUND_X + 146
        BUTTON_Y = BACKGROUND_Y + 32

        addWidget(
            Button.builder(
                Component.translatableWithFallback("mystcraft.linking_book_travel.narration","Travel")
            )
            {
                PacketDistributor.sendToServer(
                    LinkingBookLecternTravelPacket(
                        menu.lecternPos
                    )
                )

                onClose()
            }
                .pos(BUTTON_X,BUTTON_Y)
                .size(80,48)
                .build()
        )
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        var unformattedTitle = this.title.copy()
        unformattedTitle = unformattedTitle.withStyle(Style.EMPTY.withColor(0x303030).withUnderlined(true).withBold(true))

        drawCenteredStringNoDropShadow(guiGraphics,this.font, unformattedTitle, 29, 0, 0)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        guiGraphics.blit(TEXTURE,BACKGROUND_X,BACKGROUND_Y,0,0,256,181)
        //guiGraphics.hLine(-25,100,0,123123123)
    }



}
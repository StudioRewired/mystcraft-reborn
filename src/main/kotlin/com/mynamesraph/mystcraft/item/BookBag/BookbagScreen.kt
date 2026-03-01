package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.ui.menu.BookBagMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class BookBagScreen(
    menu: BookBagMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<BookBagMenu>(menu, playerInventory, title) {

    companion object {
        val TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID, "textures/gui/book_bag.png"
        )
        // Total GUI dimensions — must match texture content area
        const val GUI_WIDTH  = 176
        const val GUI_HEIGHT = 152
    }

    override fun init() {
        imageWidth  = GUI_WIDTH
        imageHeight = GUI_HEIGHT
        super.init()
        // Hide the default title and inventory label — they're baked into the texture
        titleLabelX = -9999
        inventoryLabelX = -9999
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        super.render(graphics, mouseX, mouseY, partialTick)
        renderTooltip(graphics, mouseX, mouseY)
    }
}
package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.ui.menu.PrintingTableMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import com.mynamesraph.mystcraft.item.BiomeEncyclopediaItem
import com.mynamesraph.mystcraft.registry.MystcraftTags
import net.minecraft.world.item.Items

class PrintingTableScreen(
    menu: PrintingTableMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<PrintingTableMenu>(menu, inventory, title) {

    companion object {
        // 176×166 is the standard container texture size
        private val BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID,
            "textures/gui/printing_table/background.png"
        )
        // Arrow sprite — reuse the vanilla anvil arrow or supply your own
        private val ARROW_TX = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID,
            "textures/gui/printing_table/arrow.png"
        )
        // arrow sprite - ineligible variant
        private val ARROW_DENIED_TX = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID,
            "textures/gui/printing_table/arrow_denied.png"
        )
    }

    init {
        imageWidth  = 176
        imageHeight = 166
    }

    override fun init() {
        super.init()
        inventoryLabelY = imageHeight - 94
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = (width  - imageWidth)  / 2
        val y = (height - imageHeight) / 2

        guiGraphics.blit(BG_TEXTURE, x, y, 0f, 0f, imageWidth, imageHeight, 256, 256)

        val slot0 = menu.getSlot(0)
        val slot1 = menu.getSlot(1)
        val slot2 = menu.getSlot(2)

        val canClone = slot0.hasItem() && slot1.hasItem()
                && slot1.item.item == net.minecraft.world.item.Items.EMERALD
                && !slot2.hasItem()

        val canTransfer = slot0.hasItem() && slot1.hasItem()
                && slot1.item.item == net.minecraft.world.item.Items.PAPER
                && slot2.hasItem()

        val canFetchUrl = slot0.hasItem()
                && slot0.item.item == net.minecraft.world.item.Items.WRITTEN_BOOK
                && slot1.hasItem()
                && slot1.item.item == net.minecraft.world.item.Items.PAPER
                && !slot2.hasItem()

        val canMergeEncyclopedia = slot0.hasItem()
                && slot0.item.`is`(MystcraftTags.NOTEBOOK_TAG)
                && slot1.hasItem()
                && slot1.item.item == Items.PAPER
                && slot2.hasItem()
                && slot2.item.item is BiomeEncyclopediaItem

        val arrowTexture = if (canClone || canTransfer || canFetchUrl) ARROW_TX else ARROW_DENIED_TX
        guiGraphics.blit(
            arrowTexture,
            x + 100, y + 48,
            0f, 0f,
            24, 16,
            24, 16
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Title centred at top
        guiGraphics.drawString(
            font,
            title,
            imageWidth / 2 - font.width(title) / 2,
            6,
            0x404040,
            false
        )
        // Player inventory label
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false)
    }
}
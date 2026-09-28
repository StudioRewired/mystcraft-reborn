package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.block.editing.EditingTableBlockEntity
import com.mynamesraph.mystcraft.data.networking.packet.EditingTableConfirmPacket
import com.mynamesraph.mystcraft.ui.menu.EditingTableMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

class EditingTableScreen(
    menu: EditingTableMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<EditingTableMenu>(menu, inventory, title) {

    companion object {
        val BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID, "textures/gui/editingtablebg.png"
        )

        // ── Layer 1 background fill regions ──────────────────────────────────
        // Parameter area (left): x=7..114, y=12..88
        const val PARAM_BG_X1 = 7;   const val PARAM_BG_X2 = 114
        const val PARAM_BG_Y1 = 12;  const val PARAM_BG_Y2 = 88
        const val PARAM_BG_COLOR = 0xFF8B8B8B.toInt()

        // Extras area (right): x=120..168, y=21..88
        const val EXTRAS_BG_X1 = 120; const val EXTRAS_BG_X2 = 168
        const val EXTRAS_BG_Y1 = 21;  const val EXTRAS_BG_Y2 = 88
        const val EXTRAS_BG_COLOR = 0xFFDC5151.toInt()

        // ── Layer 2: parameter bar fill ───────────────────────────────────────
        // 7 bars, each 9px tall, x=7..114, stride 11 starting at y=12
        const val BAR_X1 = 7;   const val BAR_X2 = 114
        const val BAR_HEIGHT = 9
        const val BAR_STRIDE = 11
        const val BAR_Y_START = 12
        const val BAR_FILL_COLOR = 0xFF51DC5E.toInt()

        // Tier-1 items in display order (matches bar rows top to bottom)
        val TIER1_ORDER = listOf(
            Items.DIAMOND,
            Items.GOLD_INGOT,
            Items.COPPER_INGOT,
            Items.IRON_INGOT,
            Items.COAL,
            Items.RAIL,
            Items.TORCH,
        )

        // ── Layer 2: extras fill ──────────────────────────────────────────────
        // 4 extras, each 14px tall, x=120..168, stride 17 starting at y=21
        const val EXTRAS_FILL_X1 = 120; const val EXTRAS_FILL_X2 = 168
        const val EXTRAS_HEIGHT  = 14
        const val EXTRAS_STRIDE  = 17
        const val EXTRAS_Y_START = 21
        const val EXTRAS_FILL_COLOR = 0xFF51DC5E.toInt()

        val TIER2_ORDER = listOf(
            Items.SPAWNER,
            Items.SPONGE,
            Items.ENDER_PEARL,
            Items.GOLDEN_SHOVEL,
        )

        // ── Checkmark button ──────────────────────────────────────────────────
        const val CHECKMARK_X = 151; const val CHECKMARK_Y = 89
        const val CHECKMARK_SIZE = 18
    }

    override fun init() {
        imageWidth = 176
        imageHeight = 220
        super.init()
        titleLabelX = -9999
        inventoryLabelX = -9999
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = leftPos - 1
        val y = topPos - 1

        val be = menu.pos.let {
            minecraft!!.level?.getBlockEntity(it) as? EditingTableBlockEntity
        }

        // ── Layer 1: solid background fills ──────────────────────────────────
        graphics.fill(
            x + PARAM_BG_X1, y + PARAM_BG_Y1,
            x + PARAM_BG_X2, y + PARAM_BG_Y2,
            PARAM_BG_COLOR
        )
        graphics.fill(
            x + EXTRAS_BG_X1, y + EXTRAS_BG_Y1,
            x + EXTRAS_BG_X2, y + EXTRAS_BG_Y2,
            EXTRAS_BG_COLOR
        )

        // ── Layer 2: parameter bar fills ─────────────────────────────────────
        if (be != null) {
            for ((index, item) in TIER1_ORDER.withIndex()) {
                val count = be.countItem(item)  // 0–16
                if (count > 0) {
                    val barY = y + BAR_Y_START + index * BAR_STRIDE
                    val fillWidth = ((BAR_X2 - BAR_X1) * count / 16f).toInt()
                    graphics.fill(
                        x + BAR_X1, barY,
                        x + BAR_X1 + fillWidth, barY + BAR_HEIGHT,
                        BAR_FILL_COLOR
                    )
                }
            }

            for ((index, item) in TIER2_ORDER.withIndex()) {
                if (be.hasItem(item)) {
                    val extraY = y + EXTRAS_Y_START + index * EXTRAS_STRIDE
                    graphics.fill(
                        x + EXTRAS_FILL_X1, extraY,
                        x + EXTRAS_FILL_X2, extraY + EXTRAS_HEIGHT,
                        EXTRAS_FILL_COLOR
                    )
                }
            }
        }

        // ── Layer 3: menu background texture ─────────────────────────────────
        graphics.blit(BG_TEXTURE, x, y, 0, 0, imageWidth, imageHeight)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Checkmark button hit test
        val cx = leftPos + CHECKMARK_X
        val cy = topPos  + CHECKMARK_Y
        if (mouseX >= cx && mouseX < cx + CHECKMARK_SIZE &&
            mouseY >= cy && mouseY < cy + CHECKMARK_SIZE) {
            handleCheckmarkClick()
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun handleCheckmarkClick() {
        val player = minecraft?.player ?: return
        EditingTableConfirmPacket.sendToServer(menu.pos)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        super.render(graphics, mouseX, mouseY, partialTick)
        renderTooltip(graphics, mouseX, mouseY)
    }

}
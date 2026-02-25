package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.component.BiomeSymbolsComponent
import com.mynamesraph.mystcraft.component.LocationDisplayComponent
import com.mynamesraph.mystcraft.data.networking.packet.WritingDeskRenamePacket
import com.mynamesraph.mystcraft.data.networking.packet.WritingDeskSymbolPacket
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.ui.getDisplayCharacterForBiome
import com.mynamesraph.mystcraft.ui.menu.WritingDeskMenu
import com.mynamesraph.mystcraft.ui.widget.TextButton
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.network.PacketDistributor

class WritingDeskScreen(
    menu: WritingDeskMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<WritingDeskMenu>(menu, inventory, title) {

    private lateinit var bookName: EditBox

    private val INVENTORY_TX = ResourceLocation.fromNamespaceAndPath(
        Mystcraft.MOD_ID,
        "textures/gui/writing_desk/inventory.png"
    )
    private var INVENTORY_X = 0
    private var INVENTORY_Y = 0

    // ── Descriptive-book symbol grid ──────────────────────────────────────────
    // Matches the notebook grid's button size and spacing exactly.

    // Only SYMBOL_COLS / SYMBOL_ROWS differ from the notebook side.
    private var symbolRemoveButtons: MutableList<TextButton> = mutableListOf()
    private var symbolButtonTooltips: MutableMap<TextButton, Component> = mutableMapOf()
    private var symbolPrevButton: TextButton? = null
    private var symbolNextButton: TextButton? = null
    private var currentSymbolPage = 0

    private val SYMBOL_COLS = 7         // match notebook column count
    private val SYMBOL_ROWS = 2          // fewer rows than the notebook (10) — adjust freely
    private val SYMBOLS_PER_PAGE get() = SYMBOL_COLS * SYMBOL_ROWS

    // These three must stay identical to the notebook button geometry
    private val BUTTON_SIZE = 18         // notebook cell size
    private val BUTTON_GAP  = 1          // notebook inter-button gap
    private val BUTTON_STEP get() = BUTTON_SIZE + BUTTON_GAP   // 19 px per cell

    // ── Shared texture refs ───────────────────────────────────────────────────
    private val BOOK_NAME_TX = ResourceLocation.fromNamespaceAndPath(
        Mystcraft.MOD_ID, "writing_desk/text_bar"
    )
    private val BOOK_NAME_DISABLED_TX = ResourceLocation.fromNamespaceAndPath(
        Mystcraft.MOD_ID, "writing_desk/text_bar_disabled"
    )
    private val BTN_PREV_TX = ResourceLocation.fromNamespaceAndPath(
        Mystcraft.MOD_ID, "icons/icon_back.png"
    )
    private val BTN_NEXT_TX = ResourceLocation.fromNamespaceAndPath(
        Mystcraft.MOD_ID, "icons/icon_next.png"
    )
    private val NOTEBOOK_BG_TX = ResourceLocation.withDefaultNamespace(
        "textures/gui/book.png"
    )

    private val DNI_FONT = ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "dni")

    // ── Layout coords (set in init) ───────────────────────────────────────────
    private var BOOK_NAME_X = 0
    private var BOOK_NAME_Y = 0
    private var NOTEBOOK_X  = 0
    private var NOTEBOOK_Y  = 0
    private var BUTTONS_X   = 0   // notebook grid origin
    private var BUTTONS_Y   = 0
    private var SYMBOLS_X   = 0   // descriptive-book grid origin
    private var SYMBOLS_Y = 0

    // ── Notebook biome picker state ───────────────────────────────────────────
    private var buttons: MutableList<TextButton> = mutableListOf()
    private var currentPage = 0
    private val NOTEBOOK_SYMBOLS_PER_PAGE = 22   // first 2 slots are nav, so 50 - 2 = 48... keep 16 per page

    // ── Tick-change detection ─────────────────────────────────────────────────
    private var lastTickItem     = ItemStack.EMPTY
    private var lastTickNoteBook = ItemStack.EMPTY

    // ─────────────────────────────────────────────────────────────────────────
    override fun init() {
        super.init()

        val centerX = Minecraft.getInstance().screen!!.width  / 2
        val centerY = Minecraft.getInstance().screen!!.height / 2

        imageWidth  = width  / 2
        imageHeight = height / 2

        inventoryLabelX = 98
        inventoryLabelY = 80

        INVENTORY_X = centerX - 179
        INVENTORY_Y = centerY - 91

        BOOK_NAME_X = centerX + 32
        BOOK_NAME_Y = centerY - 84

        NOTEBOOK_X = INVENTORY_X + 12
        NOTEBOOK_Y = INVENTORY_Y

        BUTTONS_X = NOTEBOOK_X + 35
        BUTTONS_Y = NOTEBOOK_Y + 17

        // Descriptive-book grid starts at the same Y as the notebook grid
        SYMBOLS_X = BOOK_NAME_X - 25
        SYMBOLS_Y = BUTTONS_Y + BUTTON_STEP * 1

        // ── Book name edit box ─────────────────────────────────────────────
        bookName = EditBox(
            font, BOOK_NAME_X + 3, BOOK_NAME_Y + 4, 133, 12,
            Component.translatable("mystcraft_reborn.writing_desk.rename")
        )
        bookName.setCanLoseFocus(true)
        bookName.setTextColor(-1)
        bookName.setTextColorUneditable(-1)
        bookName.setMaxLength(16)
        bookName.setResponder(::onBookNameChanged)
        bookName.isBordered = false
        bookName.value = ""
        bookName.setEditable(menu.getSlot(0).hasItem())
        addWidget(bookName)

        // ── Notebook biome picker buttons (50 total: [0]=prev, [1]=next, [2..]=symbols) ──
        var count = 0
        for (j in 0..9) {
            for (i in 0..4) {
                val str = "I'm button #${count++}"
                buttons.addLast(
                    addRenderableWidget(
                        TextButton(
                            BUTTONS_X + BUTTON_STEP * i,
                            BUTTONS_Y + BUTTON_STEP * j,
                            BUTTON_SIZE, BUTTON_SIZE,
                            Component.literal(i.toString())
                                .withStyle(Style.EMPTY.withFont(DNI_FONT).withColor(0)),
                            Tooltip.create(Component.literal(i.toString())),
                            2
                        ) { println(str) }
                    )
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBg(guiGraphics, partialTick, mouseX, mouseY)
        renderFg(guiGraphics, partialTick, mouseX, mouseY)
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        // Overlay the prev/next icons on the notebook nav buttons
        if (menu.getSlot(1).hasItem()) {
            guiGraphics.blit(BTN_PREV_TX, buttons[0].x, buttons[0].y, 0f, 0f, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE)
            guiGraphics.blit(BTN_NEXT_TX, buttons[1].x, buttons[1].y, 0f, 0f, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE)
        }

        // Overlay the same prev/next icons on the descriptive-book nav buttons
        symbolPrevButton?.let { btn ->
            if (btn.visible) guiGraphics.blit(BTN_PREV_TX, btn.x, btn.y, 0f, 0f, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE)
        }
        symbolNextButton?.let { btn ->
            if (btn.visible) guiGraphics.blit(BTN_NEXT_TX, btn.x, btn.y, 0f, 0f, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE)
        }

        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun containerTick() {
        super.containerTick()
        bookName.setEditable(menu.getSlot(0).hasItem())
        bookName.isVisible = menu.getSlot(0).hasItem()

        if (menu.getSlot(0).hasItem()) {
            val item = menu.getSlot(0).item
            if (!ItemStack.isSameItemSameComponents(lastTickItem, item)) {
                val display = item.components.get(MystcraftComponents.LOCATION_DISPLAY.get())
                if (display is LocationDisplayComponent) {
                    if (!bookName.value.contains(display.name.string)) {
                        bookName.value = display.name.string
                    }
                }
                // Don't reset currentSymbolPage here — a biome being added/removed
                // shouldn't jump the user back to page 0 mid-browsing.
                rebuildSymbolButtons(item)
                lastTickItem = item.copy()
            }
        } else {
            bookName.value = ""
            lastTickItem = ItemStack.EMPTY
            clearSymbolButtons()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private fun clearSymbolButtons() {
        symbolRemoveButtons.forEach { removeWidget(it) }
        symbolRemoveButtons.clear()
        symbolButtonTooltips.clear()
        symbolPrevButton?.let { removeWidget(it) }
        symbolPrevButton = null
        symbolNextButton?.let { removeWidget(it) }
        symbolNextButton = null
    }

    // Rebuilds the descriptive-book symbol grid for the current page.
    // Called from containerTick (item changed) AND directly from nav button lambdas
    // so page turns are immediate without relying on tick-change detection.
    private fun rebuildSymbolButtons(item: ItemStack) {
        clearSymbolButtons()

        if (!item.has(MystcraftComponents.BIOME_SYMBOLS)) return
        val symbols = item.components.get(MystcraftComponents.BIOME_SYMBOLS.get())
                as? BiomeSymbolsComponent ?: return
        val biomes = symbols.biomes
        if (biomes.isEmpty()) return

        val totalPages = (biomes.size + SYMBOLS_PER_PAGE - 1) / SYMBOLS_PER_PAGE
        // Clamp page in case biomes were removed from under us
        if (currentSymbolPage >= totalPages) currentSymbolPage = (totalPages - 1).coerceAtLeast(0)

        val startIndex = currentSymbolPage * SYMBOLS_PER_PAGE
        val pageSlice  = biomes.drop(startIndex).take(SYMBOLS_PER_PAGE)

        // Nav buttons occupy the first two cells of the first row, exactly like
        // the notebook grid — [0] = prev at col 0, [1] = next at col 1
        val prevBtn = addRenderableWidget(
            TextButton(
                SYMBOLS_X,
                SYMBOLS_Y,
                BUTTON_SIZE, BUTTON_SIZE,
                // Text is invisible — the icon_back graphic is blitted over it in render()
                Component.empty(),
                Tooltip.create(Component.literal("Previous Page")),
                2
            ) {
                if (currentSymbolPage > 0) {
                    currentSymbolPage--
                    // Rebuild immediately — don't rely on tick detection
                    rebuildSymbolButtons(menu.getSlot(0).item)
                }
            }
        )
        prevBtn.active  = currentSymbolPage > 0
        prevBtn.visible = totalPages > 1
        symbolPrevButton = prevBtn

        val nextBtn = addRenderableWidget(
            TextButton(
                SYMBOLS_X + BUTTON_STEP,
                SYMBOLS_Y,
                BUTTON_SIZE, BUTTON_SIZE,
                Component.empty(),
                Tooltip.create(Component.literal("Next Page")),
                2
            ) {
                if (currentSymbolPage < totalPages - 1) {
                    currentSymbolPage++
                    rebuildSymbolButtons(menu.getSlot(0).item)
                }
            }
        )
        nextBtn.active  = currentSymbolPage < totalPages - 1
        nextBtn.visible = totalPages > 1
        symbolNextButton = nextBtn

        // Symbol slots start at col 2 of row 0, then wrap normally.
        // This mirrors how the notebook skips its first two button slots for nav.
        for ((slotIndex, biome) in pageSlice.withIndex()) {
            // Offset by 2 to skip the nav cells
            val cellIndex = slotIndex + 2
            val col = cellIndex % SYMBOL_COLS
            val row = cellIndex / SYMBOL_COLS
            val btnX = SYMBOLS_X + col * BUTTON_STEP
            val btnY = SYMBOLS_Y + row * BUTTON_STEP

            val globalBiomeIndex = startIndex + slotIndex
            val tooltipComponent = Component.literal("Click to remove: ")
                .append(
                    Component.translatable("biome.${biome.toLanguageKey()}")
                        .withStyle(Style.EMPTY.withItalic(true))
                )

            val btn = addRenderableWidget(
                TextButton(
                    btnX, btnY,
                    BUTTON_SIZE, BUTTON_SIZE,
                    Component.literal(getDisplayCharacterForBiome(biome))
                        .withStyle(Style.EMPTY.withFont(DNI_FONT).withColor(0)),
                    null,  // <<< tooltip handled manually in renderTooltip() to avoid double-render
                    2
                ) {
                    PacketDistributor.sendToServer(
                        WritingDeskSymbolPacket("REMOVE_BIOME", biomes[globalBiomeIndex], menu.pos)
                    )
                    lastTickItem = ItemStack.EMPTY
                }
            )
            symbolRemoveButtons.add(btn)
            symbolButtonTooltips[btn] = tooltipComponent
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private fun renderFg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        bookName.render(guiGraphics, mouseX, mouseY, partialTick)

        if (menu.getSlot(1).hasItem()) {
            if (!ItemStack.isSameItemSameComponents(lastTickNoteBook, menu.getSlot(1).item)) {
                val item = menu.getSlot(1).item
                val symbols = item.components.get(MystcraftComponents.BIOME_SYMBOLS.get())
                if (symbols is BiomeSymbolsComponent) applyPageToButtons(symbols)
                lastTickNoteBook = item.copy()
            }
        } else {
            buttons.forEach { it.active = false; it.visible = false }
            currentPage = 0
            lastTickNoteBook = ItemStack.EMPTY
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private fun applyPageToButtons(symbols: BiomeSymbolsComponent) {
        val biomesOrdered = symbols.biomes.sorted()
        val totalBiomes = biomesOrdered.size
        val totalPages = (totalBiomes + NOTEBOOK_SYMBOLS_PER_PAGE - 1) / NOTEBOOK_SYMBOLS_PER_PAGE

        val prevButton = buttons[0]
        prevButton.active = currentPage > 0
        prevButton.visible = true
        prevButton.message = Component.empty()  // icon drawn in render()
        prevButton.setTooltip(Tooltip.create(Component.literal("Previous Page")))
        prevButton.onPress = {
            if (currentPage > 0) {
                currentPage--
                applyPageToButtons(symbols)
                lastTickNoteBook = ItemStack.EMPTY
            }
        }

        val nextButton = buttons[1]
        nextButton.active = currentPage < totalPages - 1
        nextButton.visible = true
        nextButton.message = Component.empty()  // icon drawn in render()
        nextButton.setTooltip(Tooltip.create(Component.literal("Next Page")))
        nextButton.onPress = {
            if (currentPage < totalPages - 1) {
                currentPage++
                applyPageToButtons(symbols)
                lastTickNoteBook = ItemStack.EMPTY
            }
        }

        val symbolButtons = buttons.drop(2).withIndex()
        val startIndex = currentPage * NOTEBOOK_SYMBOLS_PER_PAGE

        for (buttonI in symbolButtons) {
            val biomeIndex = startIndex + buttonI.index

            if (biomeIndex < totalBiomes && buttonI.index < NOTEBOOK_SYMBOLS_PER_PAGE) {
                buttonI.value.active = true
                buttonI.value.visible = true
                buttonI.value.setTooltip(
                    Tooltip.create(
                        Component.translatable("biome.${biomesOrdered[biomeIndex].toLanguageKey()}")
                    )
                )
                buttonI.value.message = Component.literal(
                    getDisplayCharacterForBiome(biomesOrdered[biomeIndex])
                ).withStyle(Style.EMPTY.withFont(DNI_FONT).withColor(0))

                val capturedIndex = biomeIndex
                buttonI.value.onPress = {
                    PacketDistributor.sendToServer(
                        WritingDeskSymbolPacket("BIOME", biomesOrdered[capturedIndex], menu.pos)
                    )
                }
            } else {
                buttonI.value.active = false
                buttonI.value.visible = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        guiGraphics.blit(INVENTORY_TX, INVENTORY_X, INVENTORY_Y, 0F, 0F, 358, 181, 512, 512)

        if (menu.getSlot(1).hasItem()) {
            guiGraphics.blit(NOTEBOOK_BG_TX, NOTEBOOK_X, NOTEBOOK_Y, 0, 0, 256, 256)
        }

        if (menu.getSlot(0).hasItem()) {
            guiGraphics.blitSprite(BOOK_NAME_TX,          BOOK_NAME_X, BOOK_NAME_Y, 140, 16)
        } else {
            guiGraphics.blitSprite(BOOK_NAME_DISABLED_TX, BOOK_NAME_X, BOOK_NAME_Y, 140, 16)
        }
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false)
    }

    override fun renderTooltip(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        for (btn in symbolRemoveButtons) {
            if (btn.visible && btn.isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
                symbolButtonTooltips[btn]?.let { guiGraphics.renderTooltip(font, it, mouseX, mouseY) }
                return
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY)
    }

    // ─────────────────────────────────────────────────────────────────────────
    private fun onBookNameChanged(name: String) {
        val slot = menu.getSlot(0)
        if (slot.hasItem() && slot.item.has(MystcraftComponents.LOCATION_DISPLAY)) {
            PacketDistributor.sendToServer(WritingDeskRenamePacket(name, menu.pos))
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) minecraft!!.player!!.closeContainer()
        return if (!bookName.keyPressed(keyCode, scanCode, modifiers) && !bookName.canConsumeInput())
            super.keyPressed(keyCode, scanCode, modifiers) else true
    }
}
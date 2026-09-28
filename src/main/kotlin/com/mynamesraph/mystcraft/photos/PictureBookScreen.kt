package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.client.LinkingBookPreviewRenderer
import com.mynamesraph.mystcraft.data.networking.packet.BookNoteRules
import com.mynamesraph.mystcraft.data.networking.packet.BookNoteUpdatePacket
import com.mynamesraph.mystcraft.item.MystcraftDyeing
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW

class PictureBookScreen(
    title: Component,
    private val hand: InteractionHand,
    private val player: Player
) : Screen(title) {

    private val TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath("mystcraft_reborn", "textures/gui/book/linking_book_unusable.png")

    private val previewRenderer = LinkingBookPreviewRenderer()
    private var fullscreen = false

    private var BACKGROUND_X = 0
    private var BACKGROUND_Y = 0
    private var PREVIEW_X = 0
    private var PREVIEW_Y = 0
    private val PREVIEW_WIDTH = 84
    private val PREVIEW_HEIGHT = 60

    private var backButton: Button? = null
    private lateinit var titleField: EditBox
    private var noteFields: MutableList<EditBox> = mutableListOf()
    private var textLoaded = false
    private var originalTitle = ""
    private var originalNote = ""
    private var workingTitle = ""
    private var workingNote = ""

    override fun init() {
        if (textLoaded) {
            if (::titleField.isInitialized) workingTitle = BookNoteRules.sanitizeTitle(titleField.value)
            if (noteFields.isNotEmpty()) workingNote = BookNoteUi.noteFrom(noteFields)
        }

        super.init()
        BACKGROUND_X = width / 2 - 128
        BACKGROUND_Y = height / 2 - 103
        PREVIEW_X = BACKGROUND_X + 143
        PREVIEW_Y = BACKGROUND_Y + 28

        val book = player.getItemInHand(hand)
        if (!textLoaded) {
            val baseTitle = book.get(DataComponents.CUSTOM_NAME) ?: book.hoverName
            originalTitle = BookNoteRules.sanitizeTitle(baseTitle.string)
            originalNote = BookNoteRules.sanitize(book.get(MystcraftComponents.BOOK_NOTE.get()) ?: "")
            workingTitle = originalTitle
            workingNote = originalNote
            textLoaded = true
        }

        val titleColor = MystcraftDyeing.color(book) ?: 0x303030
        titleField = BookNoteUi.createTitleField(
            font, BACKGROUND_X, BACKGROUND_Y, workingTitle, titleColor
        )
        addRenderableWidget(titleField)

        noteFields = BookNoteUi.createFields(font, BACKGROUND_X, BACKGROUND_Y, workingNote)
        noteFields.forEach { addRenderableWidget(it) }

        backButton = Button.builder(Component.literal("Back")) {
            fullscreen = false
            updateModeVisibility()
        }
            .pos(width / 2 - 40, height - 30)
            .size(80, 20)
            .build()

        addRenderableWidget(backButton!!)
        updateModeVisibility()
        clearBookTextFocus()
    }

    private fun updateModeVisibility() {
        backButton?.visible = fullscreen
        if (::titleField.isInitialized) {
            titleField.visible = !fullscreen
            titleField.active = !fullscreen
            if (fullscreen) titleField.setFocused(false)
        }
        noteFields.forEach {
            it.visible = !fullscreen
            it.active = !fullscreen
            if (fullscreen) it.setFocused(false)
        }
        if (fullscreen) clearFocus()
    }

    private fun focusTitle() {
        noteFields.forEach { it.setFocused(false) }
        titleField.setFocused(true)
        setFocused(titleField)
        BookNoteUi.collapseSelection(titleField)
    }

    private fun focusNoteLine(index: Int) {
        if (noteFields.isEmpty()) return
        titleField.setFocused(false)
        val target = noteFields[index.coerceIn(0, noteFields.lastIndex)]
        noteFields.forEach { it.setFocused(it === target) }
        setFocused(target)
        BookNoteUi.collapseSelection(target)
    }

    private fun focusTextLine(line: Int, atEnd: Boolean) {
        if (line <= 0) {
            focusTitle()
            val cursor = if (atEnd) titleField.value.length else 0
            titleField.setCursorPosition(cursor)
            BookNoteUi.collapseSelection(titleField)
            return
        }

        if (noteFields.isEmpty()) return
        val noteIndex = (line - 1).coerceIn(0, noteFields.lastIndex)
        focusNoteLine(noteIndex)
        val target = noteFields[noteIndex]
        val cursor = if (atEnd) target.value.length else 0
        target.setCursorPosition(cursor)
        BookNoteUi.collapseSelection(target)
    }

    private fun collapseBookTextSelection() {
        if (::titleField.isInitialized) BookNoteUi.collapseSelections(titleField, noteFields)
    }

    private fun clearBookTextFocus() {
        if (!::titleField.isInitialized) return
        titleField.setFocused(false)
        noteFields.forEach { it.setFocused(false) }
        collapseBookTextSelection()
        clearFocus()
    }

    private fun commitBookText() {
        if (!textLoaded || !::titleField.isInitialized) return

        workingTitle = BookNoteRules.sanitizeTitle(titleField.value)
        workingNote = BookNoteUi.noteFrom(noteFields)
        if (workingTitle == originalTitle && workingNote == originalNote) return

        PacketDistributor.sendToServer(BookNoteUpdatePacket(hand, workingTitle, workingNote))
        originalTitle = workingTitle
        originalNote = workingNote
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (fullscreen) return super.mouseClicked(mouseX, mouseY, button)

        if (BookNoteUi.isInsideLeftPage(mouseX, mouseY, BACKGROUND_X, BACKGROUND_Y)) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                collapseBookTextSelection()
                return true
            }

            if (BookNoteUi.isInsideTitle(mouseX, mouseY, BACKGROUND_X, BACKGROUND_Y)) {
                focusTitle()
                titleField.setCursorPosition(titleField.value.length)
                BookNoteUi.collapseSelection(titleField)
                return true
            }

            val relativeY = (mouseY - (BACKGROUND_Y + BookNoteUi.NOTE_Y_OFFSET)).toInt()
            val line = (relativeY / BookNoteUi.NOTE_LINE_HEIGHT)
                .coerceIn(0, BookNoteRules.MAX_LINES - 1)
            focusNoteLine(line)
            return true
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            collapseBookTextSelection()
            return true
        }

        // Any click on the open book outside the left-page editor opens the media fullscreen.
        if (BookNoteUi.isInsideBook(mouseX, mouseY, BACKGROUND_X, BACKGROUND_Y)) {
            commitBookText()
            fullscreen = true
            updateModeVisibility()
            return true
        }

        clearBookTextFocus()
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!fullscreen && keyCode == GLFW.GLFW_KEY_BACKSPACE && ::titleField.isInitialized &&
            BookNoteUi.handleBackspaceOverflow(titleField, noteFields, ::focusTextLine)
        ) {
            return true
        }

        if (!fullscreen && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            if (::titleField.isInitialized && titleField.isFocused) {
                focusNoteLine(0)
                return true
            }

            val focusedLine = noteFields.indexOfFirst { it.isFocused }
            if (focusedLine >= 0) {
                if (focusedLine < noteFields.lastIndex) focusNoteLine(focusedLine + 1)
                return true
            }
        }
        val handled = super.keyPressed(keyCode, scanCode, modifiers)
        collapseBookTextSelection()
        return handled
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (!fullscreen && ::titleField.isInitialized &&
            BookNoteUi.handleTypedOverflow(codePoint, modifiers, titleField, noteFields, ::focusTextLine)
        ) {
            collapseBookTextSelection()
            return true
        }

        val handled = super.charTyped(codePoint, modifiers)
        collapseBookTextSelection()
        return handled
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        collapseBookTextSelection()
        renderBackground(graphics, mouseX, mouseY, partialTick)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun renderBackground(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        val book = player.getItemInHand(hand)
        val preview = book.get(MystcraftComponents.PREVIEW_IMAGE.get())

        if (fullscreen) {
            if (preview != null) {
                val displayW: Int
                val displayH: Int
                if (width.toFloat() / height >= 2f) {
                    displayH = height
                    displayW = height * 2
                } else {
                    displayW = width
                    displayH = width / 2
                }
                val x = (width - displayW) / 2
                val y = (height - displayH) / 2
                previewRenderer.render(guiGraphics, preview, x, y, displayW, displayH)
            }
            return
        }

        if (preview != null) {
            previewRenderer.render(guiGraphics, preview, PREVIEW_X, PREVIEW_Y, PREVIEW_WIDTH, PREVIEW_HEIGHT)
        }
        guiGraphics.blit(TEXTURE, BACKGROUND_X, BACKGROUND_Y, 0, 0, 256, 181)

    }

    override fun removed() {
        commitBookText()
        previewRenderer.release()
        super.removed()
    }

    override fun isPauseScreen() = false
}

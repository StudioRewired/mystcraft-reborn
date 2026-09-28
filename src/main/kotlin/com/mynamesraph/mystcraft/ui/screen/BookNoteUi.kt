package com.mynamesraph.mystcraft.ui.screen

import com.mynamesraph.mystcraft.data.networking.packet.BookNoteRules
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence

/** UI helpers for the editable title + sign-like note area shared by Mystcraft book screens. */
object BookNoteUi {
    const val LEFT_PAGE_X_OFFSET = 13
    const val LEFT_PAGE_Y_OFFSET = 11
    const val LEFT_PAGE_WIDTH = 112
    const val LEFT_PAGE_HEIGHT = 151

    const val TITLE_CENTER_X_OFFSET = 70
    const val TITLE_Y_OFFSET = 18
    const val TITLE_HIT_X_OFFSET = 18
    const val TITLE_HIT_Y_OFFSET = 14
    const val TITLE_HIT_WIDTH = 104
    const val TITLE_HIT_HEIGHT = 19
    private const val TITLE_MAX_WIDTH = 104
    private const val TITLE_MIN_WIDTH = 18
    private const val TITLE_BOX_HEIGHT = 12
    private const val TITLE_SHADOW_BRIGHTNESS_THRESHOLD = 112

    const val NOTE_X_OFFSET = 22
    const val NOTE_Y_OFFSET = 38
    const val NOTE_WIDTH = 94
    const val NOTE_LINE_HEIGHT = 11
    private const val EDIT_BOX_HEIGHT = 10
    private const val INK_COLOR = 0x303030

    fun createTitleField(
        font: Font,
        backgroundX: Int,
        backgroundY: Int,
        initialTitle: String,
        titleColor: Int
    ): EditBox {
        val field = EditBox(
            font,
            backgroundX + TITLE_CENTER_X_OFFSET - TITLE_MIN_WIDTH / 2,
            backgroundY + TITLE_Y_OFFSET,
            TITLE_MIN_WIDTH,
            TITLE_BOX_HEIGHT,
            Component.literal("Book title")
        )

        field.setBordered(false)
        field.setMaxLength(BookNoteRules.MAX_TITLE_LENGTH)
        field.setTextColor(titleColor)
        field.setTextColorUneditable(titleColor)
        field.setTextShadow(needsTitleShadow(titleColor))
        field.setFormatter { text, _ ->
            FormattedCharSequence.forward(
                text,
                Style.EMPTY.withBold(true).withUnderlined(true)
            )
        }
        field.setResponder { value ->
            centerTitleField(field, font, backgroundX, value)
        }
        field.setValue(BookNoteRules.sanitizeTitle(initialTitle))
        field.setCursorPosition(0)
        field.setHighlightPos(0)
        field.setFocused(false)
        centerTitleField(field, font, backgroundX, field.value)
        return field
    }

    private fun centerTitleField(field: EditBox, font: Font, backgroundX: Int, value: String) {
        val desiredWidth = (font.width(value) + 6).coerceIn(TITLE_MIN_WIDTH, TITLE_MAX_WIDTH)
        field.setWidth(desiredWidth)
        field.setX(backgroundX + TITLE_CENTER_X_OFFSET - desiredWidth / 2)
    }

    fun createFields(
        font: Font,
        backgroundX: Int,
        backgroundY: Int,
        initialNote: String
    ): MutableList<EditBox> {
        val lines = BookNoteRules.sanitize(initialNote).split('\n')
        return MutableList(BookNoteRules.MAX_LINES) { index ->
            EditBox(
                font,
                backgroundX + NOTE_X_OFFSET,
                backgroundY + NOTE_Y_OFFSET + index * NOTE_LINE_HEIGHT,
                NOTE_WIDTH,
                EDIT_BOX_HEIGHT,
                Component.literal("Book note line ${index + 1}")
            ).apply {
                setBordered(false)
                setMaxLength(BookNoteRules.MAX_LINE_LENGTH)
                setTextColor(INK_COLOR)
                setTextColorUneditable(INK_COLOR)
                setTextShadow(false)
                setValue(lines.getOrElse(index) { "" })
                setCursorPosition(0)
                setHighlightPos(0)
                setFocused(false)
            }
        }
    }

    /** Keep sign-like book editing caret-only: no persistent multi-character selection. */
    fun collapseSelection(field: EditBox) {
        field.setHighlightPos(field.cursorPosition)
    }

    fun collapseSelections(titleField: EditBox, noteFields: List<EditBox>) {
        collapseSelection(titleField)
        noteFields.forEach(::collapseSelection)
    }

    /** Use Minecraft's normal text shadow for bright dyed title colors. */
    private fun needsTitleShadow(rgb: Int): Boolean {
        val red = (rgb shr 16) and 0xFF
        val green = (rgb shr 8) and 0xFF
        val blue = rgb and 0xFF
        val brightness = (red * 299 + green * 587 + blue * 114) / 1000
        return brightness >= TITLE_SHADOW_BRIGHTNESS_THRESHOLD
    }

    /**
     * Title is logical line 0; note lines are 1..MAX_LINES. This keeps the
     * sign-like fields visually separate while allowing the caret to flow across boundaries.
     */
    fun focusedTextLine(titleField: EditBox, noteFields: List<EditBox>): Int = when {
        titleField.isFocused -> 0
        else -> {
            val noteIndex = noteFields.indexOfFirst { it.isFocused }
            if (noteIndex >= 0) noteIndex + 1 else -1
        }
    }

    fun textFieldAt(titleField: EditBox, noteFields: List<EditBox>, line: Int): EditBox? = when {
        line == 0 -> titleField
        line in 1..noteFields.size -> noteFields[line - 1]
        else -> null
    }

    fun maxLengthForTextLine(line: Int): Int =
        if (line == 0) BookNoteRules.MAX_TITLE_LENGTH else BookNoteRules.MAX_LINE_LENGTH

    /**
     * Backspace at the first character crosses to the end of the previous logical line.
     * The boundary-crossing keypress only moves the caret; a held Backspace then naturally
     * continues deleting on the preceding line on the next repeat.
     */
    fun handleBackspaceOverflow(
        titleField: EditBox,
        noteFields: List<EditBox>,
        focusTextLine: (line: Int, atEnd: Boolean) -> Unit
    ): Boolean {
        val line = focusedTextLine(titleField, noteFields)
        if (line <= 0) return false

        val field = textFieldAt(titleField, noteFields, line) ?: return false
        if (field.cursorPosition != 0) return false

        focusTextLine(line - 1, true)
        return true
    }

    /**
     * When typing at the end of a full line, carry the new character onto the next line.
     * This includes title -> first note line, making the title behave like logical line 0.
     */
    fun handleTypedOverflow(
        codePoint: Char,
        modifiers: Int,
        titleField: EditBox,
        noteFields: List<EditBox>,
        focusTextLine: (line: Int, atEnd: Boolean) -> Unit
    ): Boolean {
        val line = focusedTextLine(titleField, noteFields)
        if (line < 0 || line >= noteFields.size) return false

        val field = textFieldAt(titleField, noteFields, line) ?: return false
        val atEnd = field.cursorPosition >= field.value.length
        val full = field.value.length >= maxLengthForTextLine(line)
        if (!atEnd || !full) return false

        val nextLine = line + 1
        focusTextLine(nextLine, false)
        val nextField = textFieldAt(titleField, noteFields, nextLine) ?: return true
        nextField.charTyped(codePoint, modifiers)
        collapseSelection(nextField)
        return true
    }

    fun noteFrom(fields: List<EditBox>): String = BookNoteRules.sanitize(
        fields.joinToString("\n") { it.value }
    )

    fun isInsideTitle(mouseX: Double, mouseY: Double, backgroundX: Int, backgroundY: Int): Boolean =
        mouseX >= backgroundX + TITLE_HIT_X_OFFSET &&
            mouseX <= backgroundX + TITLE_HIT_X_OFFSET + TITLE_HIT_WIDTH &&
            mouseY >= backgroundY + TITLE_HIT_Y_OFFSET &&
            mouseY <= backgroundY + TITLE_HIT_Y_OFFSET + TITLE_HIT_HEIGHT

    fun isInsideLeftPage(mouseX: Double, mouseY: Double, backgroundX: Int, backgroundY: Int): Boolean =
        mouseX >= backgroundX + LEFT_PAGE_X_OFFSET &&
            mouseX <= backgroundX + LEFT_PAGE_X_OFFSET + LEFT_PAGE_WIDTH &&
            mouseY >= backgroundY + LEFT_PAGE_Y_OFFSET &&
            mouseY <= backgroundY + LEFT_PAGE_Y_OFFSET + LEFT_PAGE_HEIGHT

    fun isInsideBook(mouseX: Double, mouseY: Double, backgroundX: Int, backgroundY: Int): Boolean =
        mouseX >= backgroundX && mouseX <= backgroundX + 256 &&
            mouseY >= backgroundY && mouseY <= backgroundY + 181
}

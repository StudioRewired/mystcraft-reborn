package com.mynamesraph.mystcraft.data.networking.packet

import com.mynamesraph.mystcraft.Mystcraft
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs

/** Shared limits for editable title/note text shown on Mystcraft book left pages. */
object BookNoteRules {
    const val MAX_TITLE_LENGTH = 16
    const val MAX_LINES = 8
    const val MAX_LINE_LENGTH = 18

    fun sanitizeTitle(raw: String): String = raw
        .replace("\r", "")
        .replace("\n", "")
        .filter { ch -> ch >= ' ' && ch != '\u007f' }
        .take(MAX_TITLE_LENGTH)

    fun sanitize(raw: String): String {
        val lines = raw
            .replace("\r", "")
            .split('\n')
            .take(MAX_LINES)
            .map { line ->
                line.filter { ch -> ch >= ' ' && ch != '\u007f' }
                    .take(MAX_LINE_LENGTH)
            }
            .dropLastWhile { it.isEmpty() }

        return lines.joinToString("\n")
    }
}

/** Client -> server update for title + note metadata on a Mystcraft book in either hand. */
data class BookNoteUpdatePacket(
    val interactionHand: InteractionHand,
    val title: String,
    val note: String
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<BookNoteUpdatePacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "book_note_update")
        )

        // Give enumCodec an explicit buffer type, then compose it exactly like the
        // already-working LinkingBookTravelPacket codec. This avoids Kotlin's B inference failure.
        private val INTERACTION_HAND_CODEC: StreamCodec<FriendlyByteBuf, InteractionHand> =
            NeoForgeStreamCodecs.enumCodec(InteractionHand::class.java)

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, BookNoteUpdatePacket> = StreamCodec.composite(
            INTERACTION_HAND_CODEC,
            BookNoteUpdatePacket::interactionHand,
            ByteBufCodecs.STRING_UTF8,
            BookNoteUpdatePacket::title,
            ByteBufCodecs.STRING_UTF8,
            BookNoteUpdatePacket::note,
            { hand, title, note -> BookNoteUpdatePacket(hand, title, note) }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

/** Client -> server update for title + note metadata on a linking book mounted on a lectern. */
data class LecternBookNoteUpdatePacket(
    val pos: BlockPos,
    val title: String,
    val note: String
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<LecternBookNoteUpdatePacket>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "lectern_book_note_update")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, LecternBookNoteUpdatePacket> = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            LecternBookNoteUpdatePacket::pos,
            ByteBufCodecs.STRING_UTF8,
            LecternBookNoteUpdatePacket::title,
            ByteBufCodecs.STRING_UTF8,
            LecternBookNoteUpdatePacket::note,
            { pos, title, note -> LecternBookNoteUpdatePacket(pos, title, note) }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

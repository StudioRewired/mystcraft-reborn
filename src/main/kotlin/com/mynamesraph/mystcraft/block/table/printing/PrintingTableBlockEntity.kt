package com.mynamesraph.mystcraft.block.printing

import com.mynamesraph.mystcraft.registry.MystcraftBlockEntities
import com.mynamesraph.mystcraft.registry.MystcraftTags
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.util.ImageUtils
import com.mojang.logging.LogUtils
import com.mynamesraph.mystcraft.component.BiomeSymbolsComponent
import javax.imageio.ImageIO
import java.net.URI
import java.util.concurrent.Executors
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import com.mynamesraph.mystcraft.registry.MystcraftItems
import com.mynamesraph.mystcraft.item.BiomeEncyclopediaItem

class PrintingTableBlockEntity(
    pos: BlockPos,
    blockState: BlockState
) : BlockEntity(MystcraftBlockEntities.PRINTING_TABLE_BLOCK_ENTITY.get(), pos, blockState) {

    // Prevents updateOutput() from re-entering itself via onContentsChanged
    private var updatingOutput = false
    private var outputIsAutoClone = false
    private var needsReeval = false

    val container = object : ItemStackHandler(3) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
            // Only recompute output when a real input slot changed,
            // never when we ourselves wrote to slot 2
            if (!updatingOutput && slot != 2) {
                updateOutput()
            }
        }

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            return when (slot) {
                0 -> stack.`is`(MystcraftTags.LINKING_BOOK_TAG)
                1 -> stack.item == Items.EMERALD
                2 -> false
                else -> false
            }
        }
    }

    private fun clearSlot2() {
        val stack = container.getStackInSlot(2)
        if (stack.isEmpty) return
        if (outputIsAutoClone) {
            container.setStackInSlot(2, ItemStack.EMPTY)
        } else {
            val l = level ?: return
            val entity = net.minecraft.world.entity.item.ItemEntity(
                l,
                blockPos.x + 0.5,
                blockPos.y + 1.0,
                blockPos.z + 0.5,
                stack.copy()
            )
            entity.setDefaultPickUpDelay()
            l.addFreshEntity(entity)
            container.setStackInSlot(2, ItemStack.EMPTY)
            needsReeval = true  // ← slot 2 is now empty, inputs may match a new recipe
        }
        outputIsAutoClone = false
    }

    fun updateOutput() {
        if (updatingOutput) return
        val source   = container.getStackInSlot(0)
        val catalyst = container.getStackInSlot(1)
        val dest     = container.getStackInSlot(2)

        updatingOutput = true
        try {
            when {
                // Clone mode: book + emerald + empty dest
                !source.isEmpty
                        && catalyst.item == Items.EMERALD
                        && dest.isEmpty -> {
                    container.setStackInSlot(2, source.copy())
                    outputIsAutoClone = true
                }

                // Transfer mode: book + paper + destination book
                !source.isEmpty
                        && catalyst.item == Items.PAPER
                        && !dest.isEmpty
                        && dest.`is`(MystcraftTags.LINKING_BOOK_TAG) -> {
                    val preview = source.components.get(MystcraftComponents.PREVIEW_IMAGE.get())
                    if (preview != null) {
                        dest.set(MystcraftComponents.PREVIEW_IMAGE.get(), preview)
                        catalyst.shrink(1)
                        container.setStackInSlot(1, catalyst)
                    }
                }

                // URL mode: written book + paper + empty dest
                !source.isEmpty
                        && (source.item == Items.WRITTEN_BOOK || source.item == Items.WRITABLE_BOOK)
                        && catalyst.item == Items.PAPER
                        && dest.isEmpty
                        && !fetchInFlight -> {
                    val url = extractUrlFromBook(source)
                    if (url != null) {
                        fetchInFlight = true
                        fetchAndProducePictureBook(url)
                    }
                }

                // Encyclopedia merge: notebook + paper + encyclopedia
                !source.isEmpty
                        && source.`is`(MystcraftTags.NOTEBOOK_TAG)
                        && catalyst.item == Items.PAPER
                        && !dest.isEmpty
                        && dest.item is BiomeEncyclopediaItem -> {
                    val sourceSymbols = source.components.get(MystcraftComponents.BIOME_SYMBOLS.get())
                    if (sourceSymbols is BiomeSymbolsComponent && sourceSymbols.biomes.isNotEmpty()) {
                        val existingSymbols = dest.components.get(MystcraftComponents.BIOME_SYMBOLS.get())
                        val existingBiomes = (existingSymbols as? BiomeSymbolsComponent)?.biomes ?: emptyList()
                        val merged = (existingBiomes + sourceSymbols.biomes).distinct()
                        dest.set(MystcraftComponents.BIOME_SYMBOLS.get(), BiomeSymbolsComponent(merged))
                        container.setStackInSlot(0, ItemStack.EMPTY)
                        catalyst.shrink(1)
                        container.setStackInSlot(1, catalyst)
                    }
                }

                // Inputs changed while a clone was in slot 2 — clear or eject
                else -> {
                    if (!dest.isEmpty) {
                        clearSlot2()
                    }
                }
            }
        } finally {
            updatingOutput = false
        }

        if (needsReeval) {
            needsReeval = false
            updateOutput()  // safe now — updatingOutput is false again
        }

        setChanged()
    }

    fun onOutputTaken() {
        outputIsAutoClone = false
        val emerald = container.getStackInSlot(1)
        if (!emerald.isEmpty && emerald.item == Items.EMERALD) {
            emerald.shrink(1)
            container.setStackInSlot(1, emerald)
        }
        updateOutput()
        setChanged()
    }

    // Reads the first line of the first page of a written book as the URL.
    // Written book pages are stored as JSON text components.
    private fun extractUrlFromBook(stack: ItemStack): String? {
        // Try finalized written book first
        val writtenContent = stack.components.get(
            net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT
        )
        if (writtenContent != null) {
            val pages = writtenContent.pages()
            if (pages.isEmpty()) return null
            return pages[0].raw().string.trim().takeIf { it.startsWith("http") }
        }

        // Fall back to writable book (book & quill)
        val writableContent = stack.components.get(
            net.minecraft.core.component.DataComponents.WRITABLE_BOOK_CONTENT
        )
        if (writableContent != null) {
            val pages = writableContent.pages()
            if (pages.isEmpty()) return null
            return pages[0].raw().trim().takeIf { it.startsWith("http") }
        }

        return null
    }

    private fun fetchAndProducePictureBook(url: String) {
        val level = level ?: run { fetchInFlight = false; return }

        fetchExecutor.submit {
            try {
                val connection = URI(url).toURL().openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout    = 10000

                // Check content-length before downloading
                connection.connect()
                val contentLength = connection.contentLengthLong
                if (contentLength > 25 * 1024 * 1024) { // 25 MB cap
                    LogUtils.getLogger().warn("Mystcraft PrintingTable: URL rejected, content-length $contentLength exceeds 25MB")
                    fetchInFlight = false
                    return@submit
                }

                // Even if content-length is missing/lying, cap the stream read
                val limitedStream = object : java.io.InputStream() {
                    private val inner = connection.getInputStream()
                    private var bytesRead = 0L
                    private val limit = 25 * 1024 * 1024L // 25 MB

                    override fun read(): Int {
                        if (bytesRead >= limit) throw java.io.IOException("Media exceeded 25MB limit")
                        bytesRead++
                        return inner.read()
                    }

                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        if (bytesRead >= limit) throw java.io.IOException("Media exceeded 25MB limit")
                        val allowed = minOf(len.toLong(), limit - bytesRead).toInt()
                        val n = inner.read(b, off, allowed)
                        if (n > 0) bytesRead += n
                        return n
                    }

                    override fun close() = inner.close()
                }

                val image = ImageIO.read(limitedStream)
                if (image == null) {
                    fetchInFlight = false
                    return@submit
                }

                val jpeg = ImageUtils.compressToJpeg(image)

                // Hop back to the server thread to write results
                val serverLevel = level as? net.minecraft.server.level.ServerLevel
                if (serverLevel == null) {
                    fetchInFlight = false
                    return@submit
                }

                serverLevel.server.execute {
                        try {
                            val pictureBook = ItemStack(MystcraftItems.PICTURE_BOOK.get())
                            pictureBook.set(
                                MystcraftComponents.PREVIEW_IMAGE.get(),
                                PreviewImageComponent(listOf(jpeg))
                            )

                            updatingOutput = true
                            try {
                                // Consume source book and paper
                                container.setStackInSlot(0, ItemStack.EMPTY)
                                container.setStackInSlot(1, ItemStack.EMPTY)
                                container.setStackInSlot(2, pictureBook)
                            } finally {
                                updatingOutput = false
                            }
                            setChanged()
                            level.sendBlockUpdated(blockPos, blockState, blockState, 3)
                        } finally {
                            fetchInFlight = false
                        }
                    }
            } catch (e: Exception) {
                LogUtils.getLogger().warn("Mystcraft PrintingTable: Failed to fetch image from $url", e)
                fetchInFlight = false
            }
        }
    }


    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        container.deserializeNBT(registries, tag)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.merge(container.serializeNBT(registries))
    }

    private val fetchExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MystcraftImageFetch").also { it.isDaemon = true }
    }

    // Prevents re-triggering a fetch while one is already in flight
    private var fetchInFlight = false
}
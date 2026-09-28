package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.block.crystal.CrystalColor
import com.mynamesraph.mystcraft.block.receptacle.BookReceptacleBlockEntity
import com.mynamesraph.mystcraft.block.portal.LinkPortalBlock
import com.mynamesraph.mystcraft.block.receptacle.BookReceptacleBlock
import com.mynamesraph.mystcraft.registry.MystcraftBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.fml.ModList
import java.util.LinkedList

class ColoringBookItem(properties: Properties) : Item(properties) {

    companion object {

        val RAINBOW_ORDER: List<DyeColor> = listOf(
            DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME,
            DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE, DyeColor.BLUE,
            DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK, DyeColor.WHITE,
            DyeColor.GRAY, DyeColor.LIGHT_GRAY, DyeColor.BLACK, DyeColor.BROWN
        )

        // Maps each crystal block to its color, and each color to its crystal/receptacle blocks
        private fun crystalColorOf(block: Block): CrystalColor? = when (block) {
            MystcraftBlocks.BLUE_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.BLUE_BOOK_RECEPTACLE.get() -> CrystalColor.BLUE

            MystcraftBlocks.YELLOW_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.YELLOW_BOOK_RECEPTACLE.get() -> CrystalColor.YELLOW

            MystcraftBlocks.GREEN_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.GREEN_BOOK_RECEPTACLE.get() -> CrystalColor.GREEN

            MystcraftBlocks.PINK_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.PINK_BOOK_RECEPTACLE.get() -> CrystalColor.PINK

            MystcraftBlocks.RED_CRYSTAL_BLOCK.get(),
            MystcraftBlocks.RED_BOOK_RECEPTACLE.get() -> CrystalColor.RED

            else -> null
        }

        private fun crystalBlockFor(color: CrystalColor): Block = when (color) {
            CrystalColor.BLUE -> MystcraftBlocks.BLUE_CRYSTAL_BLOCK.get()
            CrystalColor.YELLOW -> MystcraftBlocks.YELLOW_CRYSTAL_BLOCK.get()
            CrystalColor.GREEN -> MystcraftBlocks.GREEN_CRYSTAL_BLOCK.get()
            CrystalColor.PINK -> MystcraftBlocks.PINK_CRYSTAL_BLOCK.get()
            CrystalColor.RED -> MystcraftBlocks.RED_CRYSTAL_BLOCK.get()
        }

        private fun receptacleBlockFor(color: CrystalColor): Block = when (color) {
            CrystalColor.BLUE -> MystcraftBlocks.BLUE_BOOK_RECEPTACLE.get()
            CrystalColor.YELLOW -> MystcraftBlocks.YELLOW_BOOK_RECEPTACLE.get()
            CrystalColor.GREEN -> MystcraftBlocks.GREEN_BOOK_RECEPTACLE.get()
            CrystalColor.PINK -> MystcraftBlocks.PINK_BOOK_RECEPTACLE.get()
            CrystalColor.RED -> MystcraftBlocks.RED_BOOK_RECEPTACLE.get()
        }

        private val validFrameBlocks: Set<Block> by lazy {
            setOf(
                MystcraftBlocks.BLUE_CRYSTAL_BLOCK.get(),
                MystcraftBlocks.BLUE_BOOK_RECEPTACLE.get(),
                MystcraftBlocks.YELLOW_CRYSTAL_BLOCK.get(),
                MystcraftBlocks.YELLOW_BOOK_RECEPTACLE.get(),
                MystcraftBlocks.GREEN_CRYSTAL_BLOCK.get(),
                MystcraftBlocks.GREEN_BOOK_RECEPTACLE.get(),
                MystcraftBlocks.PINK_CRYSTAL_BLOCK.get(),
                MystcraftBlocks.PINK_BOOK_RECEPTACLE.get(),
                MystcraftBlocks.RED_CRYSTAL_BLOCK.get(),
                MystcraftBlocks.RED_BOOK_RECEPTACLE.get()
            )
        }
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val state = level.getBlockState(pos)

        if (level.isClientSide) return InteractionResult.SUCCESS

        val clickedBlock = state.block

        // Handle portal block — cycle portal color only
        if (clickedBlock is LinkPortalBlock) {
            val currentColor = state.getValue(LinkPortalBlock.COLOR)
            val nextColor = RAINBOW_ORDER[(RAINBOW_ORDER.indexOf(currentColor) + 1) % RAINBOW_ORDER.size]
            var newState = state.setValue(LinkPortalBlock.COLOR, nextColor)
            if (ModList.get().isLoaded("past_el_palettes")) {
                newState = newState.setValue(LinkPortalBlock.IS_PASTEL_COLOR, false)
            }
            level.setBlock(pos, newState, Block.UPDATE_ALL)
            return InteractionResult.SUCCESS
        }

        // Handle crystal frame block — cycle frame color
        if (crystalColorOf(clickedBlock) != null) {
            recolorFrame(level, pos)
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    private fun recolorFrame(level: Level, origin: BlockPos) {
        // BFS through connected frame blocks, cap at 128
        val visited = mutableSetOf<BlockPos>()
        val crystalPositions = mutableListOf<BlockPos>()
        var receptaclePos: BlockPos? = null
        var receptacleState: BlockState? = null

        val queue = LinkedList<BlockPos>()
        queue.add(origin)
        visited.add(origin)

        while (queue.isNotEmpty() && visited.size <= 128) {
            val current = queue.poll()
            val block = level.getBlockState(current).block

            when {
                block == MystcraftBlocks.BLUE_BOOK_RECEPTACLE.get() ||
                        block == MystcraftBlocks.YELLOW_BOOK_RECEPTACLE.get() ||
                        block == MystcraftBlocks.GREEN_BOOK_RECEPTACLE.get() ||
                        block == MystcraftBlocks.PINK_BOOK_RECEPTACLE.get() ||
                        block == MystcraftBlocks.RED_BOOK_RECEPTACLE.get() -> {
                    receptaclePos = current
                    receptacleState = level.getBlockState(current)
                }
                else -> crystalPositions.add(current)
            }

            // Explore all 6 neighbours
            for (direction in net.minecraft.core.Direction.entries) {
                val neighbour = current.relative(direction)
                if (neighbour !in visited && validFrameBlocks.contains(level.getBlockState(neighbour).block)) {
                    visited.add(neighbour)
                    queue.add(neighbour)
                }
            }
        }

        // Validate: need at least 5 crystal blocks and exactly 1 receptacle
        if (crystalPositions.size < 5 || receptaclePos == null || receptacleState == null) return

        // Determine current color from origin block and advance to next
        val currentColor = crystalColorOf(level.getBlockState(origin).block) ?: return
        val nextColor = currentColor.next()

        // Save the portal color from any adjacent portal block so we can re-apply it
        val savedPortalState = findAdjacentPortalState(level, receptaclePos)

        val receptacleBE = level.getBlockEntity(receptaclePos)
        val savedBook = if (receptacleBE is BookReceptacleBlockEntity) {
            receptacleBE.removeBook() // empties the BE so onRemove → popBook drops nothing
        } else null

        // Replace crystal blocks
        val newCrystalBlock = crystalBlockFor(nextColor)
        for (crystalPos in crystalPositions) {
            level.setBlock(crystalPos, newCrystalBlock.defaultBlockState(), Block.UPDATE_ALL)
        }

        // Replace receptacle block, preserving FACING and LOCKED state
        val newReceptacleBlock = receptacleBlockFor(nextColor)
        val newReceptacleState = newReceptacleBlock.defaultBlockState()
            .setValue(
                BookReceptacleBlock.FACING,
                receptacleState.getValue(BookReceptacleBlock.FACING))
            .setValue(
                BookReceptacleBlock.LOCKED,
                receptacleState.getValue(BookReceptacleBlock.LOCKED))

        level.setBlock(receptaclePos, newReceptacleState, Block.UPDATE_ALL)

        // Re-insert the book into the new receptacle BE, which triggers tryGeneratePortal()
        if (savedBook != null && !savedBook.isEmpty) {
            val newBE = level.getBlockEntity(receptaclePos)
            if (newBE is BookReceptacleBlockEntity) {
                newBE.insertBook(savedBook)

                // Re-apply the previous portal color if we captured it
                if (savedPortalState != null) {
                    applyPortalColor(level, receptaclePos, savedPortalState)
                }
            }
        }
    }

    private fun findAdjacentPortalState(level: Level, receptaclePos: BlockPos): BlockState? {
        for (direction in net.minecraft.core.Direction.entries) {
            val neighbour = level.getBlockState(receptaclePos.relative(direction))
            if (neighbour.block is LinkPortalBlock) return neighbour
        }
        return null
    }

    private fun applyPortalColor(level: Level, receptaclePos: BlockPos, portalState: BlockState) {
        val color = portalState.getValue(LinkPortalBlock.COLOR)
        val isPastel = if (ModList.get().isLoaded("past_el_palettes"))
            portalState.getValue(LinkPortalBlock.IS_PASTEL_COLOR) else false

        // Walk all portal blocks connected to this receptacle and recolor them
        val visited = mutableSetOf<BlockPos>()
        val queue = LinkedList<BlockPos>()

        for (direction in net.minecraft.core.Direction.entries) {
            val neighbour = receptaclePos.relative(direction)
            if (level.getBlockState(neighbour).block is LinkPortalBlock) {
                queue.add(neighbour)
                visited.add(neighbour)
            }
        }

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            val currentState = level.getBlockState(current)
            if (currentState.block !is LinkPortalBlock) continue

            var newState = currentState.setValue(LinkPortalBlock.COLOR, color)
            if (ModList.get().isLoaded("past_el_palettes")) {
                newState = newState.setValue(LinkPortalBlock.IS_PASTEL_COLOR, isPastel)
            }
            level.setBlock(current, newState, Block.UPDATE_ALL)

            for (direction in net.minecraft.core.Direction.entries) {
                val neighbour = current.relative(direction)
                if (neighbour !in visited && level.getBlockState(neighbour).block is LinkPortalBlock) {
                    visited.add(neighbour)
                    queue.add(neighbour)
                }
            }
        }
    }
}
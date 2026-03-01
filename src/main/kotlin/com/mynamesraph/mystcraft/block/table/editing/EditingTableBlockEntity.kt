package com.mynamesraph.mystcraft.block.editing

import com.mynamesraph.mystcraft.component.WorldgenParametersComponent
import com.mynamesraph.mystcraft.item.DescriptiveBookItem
import com.mynamesraph.mystcraft.registry.MystcraftBlockEntities
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

class EditingTableBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(MystcraftBlockEntities.EDITING_TABLE_BLOCK_ENTITY.get(), pos, state) {

    companion object {
        const val TOTAL_SLOTS    = 17  // 16 free parameter slots + 1 book slot
        const val SLOT_BOOK      = 16  // x=151, y=107 — restricted to DescriptiveBookItem

        const val MAX_PARAM_STACK = 16

        val TIER1_ITEMS = mapOf(
            Items.DIAMOND       to "terrainTurbulence",
            Items.GOLD_INGOT    to "seaLevel",
            Items.IRON_INGOT    to "caveDensity",
            Items.COPPER_INGOT  to "biomeSize",
            Items.COAL          to "verticalRange",
        )

        val TIER2_ITEMS = mapOf(
            Items.SPAWNER        to "noMobs",
            Items.SPONGE         to "noAquifers",
            Items.ENDER_PEARL    to "caveWorld",
            Items.GOLDEN_SHOVEL  to "superFlat",
        )
    }

    val container = object : ItemStackHandler(TOTAL_SLOTS) {
        override fun getSlotLimit(slot: Int) = if (slot == SLOT_BOOK) 1 else MAX_PARAM_STACK

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            return when (slot) {
                SLOT_BOOK -> stack.item is DescriptiveBookItem
                else      -> true
            }
        }

        override fun onContentsChanged(slot: Int) {
            setChanged()
            if (level != null && !level!!.isClientSide) {
                level!!.sendBlockUpdated(blockPos, level!!.getBlockState(blockPos), level!!.getBlockState(blockPos), 3)
            }
        }
    }

    /**
     * Called when the player clicks the checkmark.
     * Builds the component, attaches it to the book, clears all slots, returns the output book.
     */
    fun confirmAndBuild(): ItemStack {
        val book = container.getStackInSlot(SLOT_BOOK)
        if (book.isEmpty) return ItemStack.EMPTY

        val output = book.copy()
        output.set(MystcraftComponents.WORLDGEN_PARAMETERS.get(), buildParams())

        // Consume everything
        for (i in 0 until TOTAL_SLOTS) {
            container.setStackInSlot(i, ItemStack.EMPTY)
        }
        setChanged()
        return output
    }

    fun buildParams(): WorldgenParametersComponent {
        val counts = mutableMapOf<String, Int>()
        for (i in 0 until 16) {
            val stack = container.getStackInSlot(i)
            if (stack.isEmpty) continue
            TIER1_ITEMS[stack.item]?.let { key ->
                counts[key] = (counts.getOrDefault(key, 0) + stack.count).coerceAtMost(16)
            }
            TIER2_ITEMS[stack.item]?.let { key ->
                counts[key] = 1
            }
        }
        return WorldgenParametersComponent(
            terrainTurbulence = counts["terrainTurbulence"] ?: 6,
            seaLevel          = counts["seaLevel"]          ?: 6,
            caveDensity       = counts["caveDensity"]       ?: 6,
            biomeSize         = counts["biomeSize"]         ?: 6,
            verticalRange     = counts["verticalRange"]     ?: 6,
            superFlat         = counts.containsKey("superFlat"),
            noMobs            = counts.containsKey("noMobs"),
            caveWorld         = counts.containsKey("caveWorld"),
            noAquifers        = counts.containsKey("noAquifers"),
        )
    }

    fun countItem(item: Item): Int {
        var total = 0
        for (i in 0 until 16) {
            val stack = container.getStackInSlot(i)
            if (stack.item == item) total += stack.count
        }
        return total.coerceAtMost(16)
    }

    fun hasItem(item: Item): Boolean {
        for (i in 0 until 16) {
            if (container.getStackInSlot(i).item == item) return true
        }
        return false
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("inventory", container.serializeNBT(registries))
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("inventory")) container.deserializeNBT(registries, tag.getCompound("inventory"))
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registries)
    }

    override fun getUpdatePacket(): net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this)
    }
}
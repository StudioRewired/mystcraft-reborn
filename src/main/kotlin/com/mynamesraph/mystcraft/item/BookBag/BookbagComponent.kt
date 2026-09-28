package com.mynamesraph.mystcraft.component

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.ItemStackHandler

data class BookBagComponent(val stacks: List<ItemStack>) {

    companion object {
        const val SLOT_COUNT = 18

        val CODEC: Codec<BookBagComponent> = ItemStack.OPTIONAL_CODEC
            .listOf()
            .xmap(
                { list ->
                    val padded = list.toMutableList()
                    while (padded.size < SLOT_COUNT) padded.add(ItemStack.EMPTY)
                    BookBagComponent(padded)
                },
                { component -> component.stacks }
            )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BookBagComponent> =
            ItemStack.OPTIONAL_STREAM_CODEC
                .apply(ByteBufCodecs.list(SLOT_COUNT))
                .map(
                    { list -> BookBagComponent(list) },
                    { component -> component.stacks }
                )

        fun fromHandler(handler: ItemStackHandler): BookBagComponent =
            BookBagComponent((0 until handler.slots).map { handler.getStackInSlot(it).copy() })

        fun toHandler(component: BookBagComponent): ItemStackHandler {
            val handler = ItemStackHandler(SLOT_COUNT)
            component.stacks.forEachIndexed { i, stack ->
                if (i < SLOT_COUNT) handler.setStackInSlot(i, stack.copy())
            }
            return handler
        }
    }
}
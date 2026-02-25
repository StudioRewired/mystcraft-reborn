package com.mynamesraph.mystcraft.block.writing

import net.minecraft.util.StringRepresentable

enum class WritingDeskSide(private val displayName:String) : StringRepresentable {
    LEFT("left"),RIGHT("right");

    override fun getSerializedName(): String {
        return this.displayName
    }
}
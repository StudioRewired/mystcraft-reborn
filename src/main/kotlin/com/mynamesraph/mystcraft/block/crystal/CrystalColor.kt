package com.mynamesraph.mystcraft.block.crystal

enum class CrystalColor {
    BLUE, YELLOW, GREEN, PINK, RED;

    fun next(): CrystalColor {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}


package com.mynamesraph.mystcraft.client

import com.mynamesraph.mystcraft.component.PreviewImageComponent
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object ItemFramePreviewCache {

    private const val MAX_ENTRIES = 16
    private val cache = object : LinkedHashMap<Int, LinkingBookPreviewRenderer>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Int, LinkingBookPreviewRenderer>): Boolean {
            return if (size > MAX_ENTRIES) {
                eldest.value.release()
                true
            } else false
        }
    }

    fun getRenderer(component: PreviewImageComponent): LinkingBookPreviewRenderer {
        return cache.getOrPut(component.hashCode()) { LinkingBookPreviewRenderer() }
    }

    fun releaseAll() {
        cache.values.forEach { it.release() }
        cache.clear()
    }
}
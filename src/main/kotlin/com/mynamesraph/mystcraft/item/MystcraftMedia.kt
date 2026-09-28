package com.mynamesraph.mystcraft.item

import com.mynamesraph.mystcraft.component.PreviewImageComponent
import com.mynamesraph.mystcraft.component.PreviewTooltipComponent
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import net.minecraft.world.item.ItemStack

/** Shared, side-safe lookup for Mystcraft stacks which actually contain renderable media. */
object MystcraftMedia {
    fun preview(stack: ItemStack): PreviewImageComponent? = when (stack.item) {
        is LinkingBookItem, is PictureBookItem -> stack.get(MystcraftComponents.PREVIEW_IMAGE.get())
        is CameraItem -> stack.get(MystcraftComponents.CAMERA_PHOTO.get())?.let {
            PreviewImageComponent(listOf(it.jpeg))
        }
        else -> null
    }

    fun tooltip(stack: ItemStack): PreviewTooltipComponent? {
        val preview = preview(stack) ?: return null
        if (preview.frames.isEmpty()) return null

        val dimensions = when (stack.item) {
            is CameraItem -> CameraItem.CAPTURE_WIDTH to CameraItem.CAPTURE_HEIGHT
            is PictureBookItem -> if (preview.frames.size > 1) 320 to 112 else 640 to 384
            else -> PreviewImageComponent.CAPTURE_WIDTH to PreviewImageComponent.CAPTURE_HEIGHT
        }
        return PreviewTooltipComponent(preview, dimensions.first, dimensions.second)
    }
}

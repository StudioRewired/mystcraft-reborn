package com.mynamesraph.mystcraft.component

import net.minecraft.world.inventory.tooltip.TooltipComponent

/**
 * Server-safe data passed from an Item tooltip to the client tooltip renderer.
 * The source dimensions preserve the media aspect ratio without decoding JPEGs here.
 */
data class PreviewTooltipComponent(
    val preview: PreviewImageComponent,
    val sourceWidth: Int,
    val sourceHeight: Int
) : TooltipComponent

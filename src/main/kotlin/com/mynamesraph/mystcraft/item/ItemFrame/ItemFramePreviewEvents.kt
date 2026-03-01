package com.mynamesraph.mystcraft.events

import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.client.ItemFramePreviewCache
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import com.mynamesraph.mystcraft.item.CameraItem
import com.mynamesraph.mystcraft.item.LinkingBookItem
import com.mynamesraph.mystcraft.item.PictureBookItem
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent

@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object ItemFramePreviewEvents {

    @SubscribeEvent
    fun onRenderItemInFrame(event: RenderItemInFrameEvent) {
        val stack = event.itemStack
        val item = stack.item

        val preview: PreviewImageComponent? = when {
            item is LinkingBookItem ->
                stack.components.get(MystcraftComponents.PREVIEW_IMAGE.get())
            item is PictureBookItem ->
                stack.components.get(MystcraftComponents.PREVIEW_IMAGE.get())
            item is CameraItem -> {
                val photo = stack.components.get(MystcraftComponents.CAMERA_PHOTO.get())
                    ?: return
                PreviewImageComponent(listOf(photo.jpeg))
            }
            else -> return
        }

        val nonNullPreview = preview ?: return

        val renderer = ItemFramePreviewCache.getRenderer(nonNullPreview)
        renderer.ensureUploadedPublic(nonNullPreview)
        val location = renderer.getCurrentFrameLocation() ?: return

        event.isCanceled = true

        val poseStack = event.poseStack
        poseStack.pushPose()
        poseStack.translate(0.0, 0.0, -0.03)

        // Render exactly like a map — using RenderType.text which is what
        // MapRenderer uses internally, but via the GuiGraphics blit path
        // so we never need to cast the MultiBufferSource
        val matrix = poseStack.last().pose()
        val buffer = event.multiBufferSource.getBuffer(
            net.minecraft.client.renderer.RenderType.text(location)
        )
        val light = event.packedLight

        buffer.addVertex(matrix, -0.45f, 0.45f, 0f).setUv(0f, 0f).setLight(light).setColor(255, 255, 255, 255)
        buffer.addVertex(matrix, 0.45f, 0.45f, 0f).setUv(1f, 0f).setLight(light).setColor(255, 255, 255, 255)
        buffer.addVertex(matrix, 0.45f, -0.45f, 0f).setUv(1f, 1f).setLight(light).setColor(255, 255, 255, 255)
        buffer.addVertex(matrix, -0.45f, -0.45f, 0f).setUv(0f, 1f).setLight(light).setColor(255, 255, 255, 255)

        poseStack.popPose()

        // No endBatch() call at all — let the rendering pipeline flush naturally
    }
}
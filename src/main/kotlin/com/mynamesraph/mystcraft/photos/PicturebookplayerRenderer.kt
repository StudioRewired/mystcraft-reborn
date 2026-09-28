package com.mynamesraph.mystcraft.client

import com.mynamesraph.mystcraft.block.mediaplayer.PictureBookPlayerBlockEntity
import com.mynamesraph.mystcraft.registry.MystcraftComponents
import com.mynamesraph.mystcraft.component.PreviewImageComponent
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.Direction
import org.joml.Matrix4f

class PictureBookPlayerRenderer(
    ctx: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<PictureBookPlayerBlockEntity> {

    companion object {
        val ERROR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "mystcraft_reborn", "textures/block/bookplayer_nosignal.png"
        )
        val BACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "mystcraft_reborn", "textures/block/bookplayer_back.png"
        )
    }

    private val renderers = HashMap<Long, LinkingBookPreviewRenderer>()

    override fun getRenderBoundingBox(blockEntity: PictureBookPlayerBlockEntity): net.minecraft.world.phys.AABB {
        val range = maxOf(blockEntity.displayWidth, blockEntity.displayHeight).toDouble() +
                maxOf(Math.abs(blockEntity.horizontalOffset), Math.abs(blockEntity.verticalOffset)).toDouble()
        val pos = blockEntity.blockPos
        return net.minecraft.world.phys.AABB(
            pos.x - range, pos.y - range, pos.z - range,
            pos.x + range + 1.0, pos.y + range + 1.0, pos.z + range + 1.0
        )
    }

    override fun render(
        be: PictureBookPlayerBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (!be.hasBook) return

        val facing = be.blockState.getValue(BlockStateProperties.FACING)

        val preview: PreviewImageComponent? =
            be.book.components.get(MystcraftComponents.PREVIEW_IMAGE.get())

        val w = be.displayWidth.toFloat()
        val h = be.displayHeight.toFloat()
        val hOff = be.horizontalOffset.toFloat()
        val vOff = be.verticalOffset.toFloat()

        poseStack.pushPose()
        poseStack.translate(0.5, 0.5, 0.5)

        // Rotate to face the correct direction, then nudge just outside the block face.
        // For UP/DOWN, "vertical offset" moves along the forward axis (N/S) and
        // "horizontal offset" moves along the side axis (E/W), matching player intuition.
        when (facing) {
            Direction.NORTH -> {
                poseStack.translate(0.0, 0.0, -0.51)
            }
            Direction.SOUTH -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f))
                poseStack.translate(0.0, 0.0, -0.51)
            }
            Direction.EAST -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270f))
                poseStack.translate(0.0, 0.0, -0.51)
            }
            Direction.WEST -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f))
                poseStack.translate(0.0, 0.0, -0.51)
            }
            Direction.UP -> {
                // Rotate so the projection faces upward; screen "up" = world North
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f))
                poseStack.translate(0.0, 0.0, -0.51)
            }
            Direction.DOWN -> {
                // Rotate so the projection faces downward; screen "up" = world South
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(270f))
                poseStack.translate(0.0, 0.0, -0.51)
            }
            else -> poseStack.translate(0.0, 0.0, -0.51)
        }

        // Apply user offsets in local screen-plane space
        poseStack.translate(hOff.toDouble(), vOff.toDouble(), 0.0)
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(be.rotation))

        val light = if (be.backlightOn) 0xF000F0 else packedLight
        val halfW = w / 2f
        val halfH = h / 2f

        // ── Front face ──────────────────────────────────────────────────────────
        val frontMatrix: Matrix4f = poseStack.last().pose()

        if (preview != null) {
            val key = be.blockPos.asLong()
            val renderer = renderers.getOrPut(key) { LinkingBookPreviewRenderer() }
            renderer.ensureUploadedPublic(preview)
            val location = renderer.getCurrentFrameLocation()

            if (location != null) {
                val buffer = bufferSource.getBuffer(RenderType.text(location))
                buffer.addVertex(frontMatrix, -halfW,  halfH, 0f).setUv(1f, 0f).setLight(light).setColor(255, 255, 255, 255)
                buffer.addVertex(frontMatrix,  halfW,  halfH, 0f).setUv(0f, 0f).setLight(light).setColor(255, 255, 255, 255)
                buffer.addVertex(frontMatrix,  halfW, -halfH, 0f).setUv(0f, 1f).setLight(light).setColor(255, 255, 255, 255)
                buffer.addVertex(frontMatrix, -halfW, -halfH, 0f).setUv(1f, 1f).setLight(light).setColor(255, 255, 255, 255)
            }
        } else {
            val buffer = bufferSource.getBuffer(RenderType.text(ERROR_TEXTURE))
            buffer.addVertex(frontMatrix, -0.5f,  0.5f, 0f).setUv(1f, 0f).setLight(light).setColor(255, 255, 255, 255)
            buffer.addVertex(frontMatrix,  0.5f,  0.5f, 0f).setUv(0f, 0f).setLight(light).setColor(255, 255, 255, 255)
            buffer.addVertex(frontMatrix,  0.5f, -0.5f, 0f).setUv(0f, 1f).setLight(light).setColor(255, 255, 255, 255)
            buffer.addVertex(frontMatrix, -0.5f, -0.5f, 0f).setUv(1f, 1f).setLight(light).setColor(255, 255, 255, 255)
        }

        // ── Back face ────────────────────────────────────────────────────────────
        poseStack.pushPose()
        poseStack.translate(0.0, 0.0, 0.02)
        val backMatrix: Matrix4f = poseStack.last().pose()

        val backBuffer = bufferSource.getBuffer(RenderType.text(BACK_TEXTURE))
        backBuffer.addVertex(backMatrix,  halfW,  halfH, 0f).setUv(0f, 0f ).setLight(packedLight).setColor(200, 200, 200, 255)
        backBuffer.addVertex(backMatrix, -halfW,  halfH, 0f).setUv(w,   0f ).setLight(packedLight).setColor(200, 200, 200, 255)
        backBuffer.addVertex(backMatrix, -halfW, -halfH, 0f).setUv(w,   h  ).setLight(packedLight).setColor(200, 200, 200, 255)
        backBuffer.addVertex(backMatrix,  halfW, -halfH, 0f).setUv(0f,  h  ).setLight(packedLight).setColor(200, 200, 200, 255)

        poseStack.popPose()
        poseStack.popPose()
    }
}
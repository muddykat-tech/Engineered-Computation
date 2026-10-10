package com.muddykat.engineeredcomputation.client.guide;

import java.util.List;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;

final class StructurePage implements GuidePage
{
	private static final int TITLE_HEIGHT = 11;
	private static final int COUNT_HEIGHT = 18;
	private static final float START_YAW = -30;
	private static final float START_PITCH = 25;

	private final Component title;
	private final Block casing;
	private final EraDriveGeometry geometry;
	private final List<FormattedCharSequence> caption;
	private final int viewHeight;
	private float yaw = START_YAW;
	private float pitch = START_PITCH;

	StructurePage(EraDrive drive, List<FormattedCharSequence> caption)
	{
		this(ECMultiblocks.ERA_DRIVES.get(drive).partBlock().getName(), ECContent.casing(drive.era()).get(),
				drive.geometry(), caption);
	}

	StructurePage(Component title, Block casing, EraDriveGeometry geometry, List<FormattedCharSequence> caption)
	{
		this.title = title;
		this.casing = casing;
		this.geometry = geometry;
		this.caption = caption;
		this.viewHeight = HEIGHT-TITLE_HEIGHT-COUNT_HEIGHT-caption.size()*TextPage.LINE_HEIGHT;
	}

	@Override
	public void render(GuideScreen screen, PoseStack pose, int x, int y, int mouseX, int mouseY, float partialTick)
	{
		screen.drawTitle(pose, title, x, y);
		ECGui.enableScissor(x, y+TITLE_HEIGHT, x+WIDTH, y+TITLE_HEIGHT+viewHeight);
		renderStructure(pose, x+WIDTH/2, y+TITLE_HEIGHT+viewHeight/2);
		ECGui.disableScissor();

		int lineY = y+TITLE_HEIGHT+viewHeight;
		ItemStack casingItem = new ItemStack(casing);
		screen.renderStack(pose, casingItem, x, lineY, mouseX, mouseY);
		screen.drawFitted(pose, new TranslatableComponent("guide.engineered_computation.structure.casings",
				geometry.shapes().size(), casingItem.getHoverName()), x+18, lineY+4, WIDTH-18, screen.textColour());
		lineY += COUNT_HEIGHT;
		for(FormattedCharSequence line : caption)
		{
			screen.font().draw(pose, line, x, lineY, screen.textColour());
			lineY += TextPage.LINE_HEIGHT;
		}
	}

	private void renderStructure(PoseStack pose, int centreX, int centreY)
	{
		BlockPos size = geometry.size();
		float diagonal = Mth.sqrt(size.getX()*size.getX()+size.getY()*size.getY()+size.getZ()*size.getZ());
		float scale = Math.min(viewHeight, WIDTH)*0.95F/diagonal;
		BlockState casingState = casing.defaultBlockState();
		BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
		float pulse = 0.5F+0.5F*Mth.sin(Util.getMillis()/250F);

		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		pose.pushPose();
		pose.translate(centreX, centreY, 150);
		pose.scale(scale, -scale, scale);
		pose.mulPose(Vector3f.XP.rotationDegrees(pitch));
		pose.mulPose(Vector3f.YP.rotationDegrees(yaw));
		pose.translate(-size.getX()/2F, -size.getY()/2F, -size.getZ()/2F);
		Lighting.setupFor3DItems();
		for(BlockPos pos : geometry.shapes().keySet())
		{
			pose.pushPose();
			pose.translate(pos.getX(), pos.getY(), pos.getZ());
			blocks.renderSingleBlock(casingState, pose, buffers, LightTexture.FULL_BRIGHT,
					OverlayTexture.NO_OVERLAY);
			pose.popPose();
		}

		buffers.endBatch();

		BlockPos trigger = geometry.trigger();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		BufferBuilder quads = Tesselator.getInstance().getBuilder();
		quads.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		PoseStack.Pose face = pose.last();
		float z = trigger.getZ()+1.02F;
		float x0 = trigger.getX();
		float y0 = trigger.getY();
		int fillAlpha = 70+Math.round(90*pulse);
		quad(quads, face, x0, y0, x0+1, y0+1, z, 255, 220, 40, fillAlpha);
		float border = 0.1F;
		quad(quads, face, x0, y0, x0+1, y0+border, z+0.001F, 255, 200, 0, 255);
		quad(quads, face, x0, y0+1-border, x0+1, y0+1, z+0.001F, 255, 200, 0, 255);
		quad(quads, face, x0, y0, x0+border, y0+1, z+0.001F, 255, 200, 0, 255);
		quad(quads, face, x0+1-border, y0, x0+1, y0+1, z+0.001F, 255, 200, 0, 255);
		Tesselator.getInstance().end();
		RenderSystem.enableCull();

		pose.pushPose();
		pose.translate(trigger.getX()+0.5F, trigger.getY()+0.55F+0.1F*pulse, trigger.getZ()+1.6F);
		pose.scale(1.6F, 1.6F, 1.6F);
		Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ECContent.ASSEMBLY_WRENCH.get()),
				ItemTransforms.TransformType.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose,
				buffers, 0);
		pose.popPose();

		buffers.endBatch();
		pose.popPose();
	}

	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
			float z, int red, int green, int blue, int alpha)
	{
		consumer.vertex(pose.pose(), x0, y0, z).color(red, green, blue, alpha).endVertex();
		consumer.vertex(pose.pose(), x1, y0, z).color(red, green, blue, alpha).endVertex();
		consumer.vertex(pose.pose(), x1, y1, z).color(red, green, blue, alpha).endVertex();
		consumer.vertex(pose.pose(), x0, y1, z).color(red, green, blue, alpha).endVertex();
	}

	@Override
	public void mouseDragged(double dragX, double dragY)
	{
		yaw += (float)dragX*1.5F;
		pitch = Mth.clamp(pitch+(float)dragY*1.5F, -10, 80);
	}
}

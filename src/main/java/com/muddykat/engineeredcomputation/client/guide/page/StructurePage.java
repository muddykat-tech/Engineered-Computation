package com.muddykat.engineeredcomputation.client.guide.page;

import java.util.List;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.client.guide.GuideScreen;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;

// This will be improved later on
public final class StructurePage implements GuidePage
{
	private static final int TITLE_HEIGHT = 11;
	private static final int COUNT_HEIGHT = 18;
	private static final float START_YAW = -30;
	private static final float START_PITCH = 25;
	private static final float WRENCH_SIZE = 1.6F;
	private static final float WRENCH_OUT = 1.6F;

	private static final float WRENCH_REACH = WRENCH_SIZE/2+0.05F;

	private final Component title;
	private final Block casing;
	private final EraDriveGeometry geometry;
	private final List<FormattedCharSequence> caption;
	private final int viewHeight;
	private float yaw = START_YAW;
	private float pitch = START_PITCH;

	public StructurePage(EraDrive drive, List<FormattedCharSequence> caption)
	{
		this(ECMultiblocks.ERA_DRIVES.get(drive).partBlock().getName(), ECContent.casing(drive.era()).get(),
				drive.geometry(), caption);
	}

	public StructurePage(Component title, Block casing, EraDriveGeometry geometry, List<FormattedCharSequence> caption)
	{
		this.title = title;
		this.casing = casing;
		this.geometry = geometry;
		this.caption = caption;
		this.viewHeight = HEIGHT-TITLE_HEIGHT-COUNT_HEIGHT-caption.size()*TextPage.LINE_HEIGHT;
	}

	@Override
	public void render(GuideScreen screen, GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick)
	{
		screen.drawTitle(graphics, title, x, y);
		graphics.enableScissor(x, y+TITLE_HEIGHT, x+WIDTH, y+TITLE_HEIGHT+viewHeight);
		renderStructure(graphics, x+WIDTH/2, y+TITLE_HEIGHT+viewHeight/2);
		graphics.disableScissor();

		int lineY = y+TITLE_HEIGHT+viewHeight;
		ItemStack casingItem = new ItemStack(casing);
		screen.renderStack(graphics, casingItem, x, lineY, mouseX, mouseY);
		screen.drawFitted(graphics, Component.translatable("guide.engineered_computation.structure.casings",
				geometry.shapes().size(), casingItem.getHoverName()), x+18, lineY+4, WIDTH-18, screen.textColour());
		lineY += COUNT_HEIGHT;
		for(FormattedCharSequence line : caption)
		{
			graphics.drawString(screen.font(), line, x, lineY, screen.textColour(), false);
			lineY += TextPage.LINE_HEIGHT;
		}
	}

	private void renderStructure(GuiGraphics graphics, int centreX, int centreY)
	{
		// Fits the diagonal of the box round the structure and wrench, ensures that it accounts for view rotation
		BlockPos size = geometry.size();
		BlockPos trigger = geometry.trigger();
		float wrenchX = trigger.getX()+0.5F, wrenchY = trigger.getY()+0.6F, wrenchZ = trigger.getZ()+WRENCH_OUT;
		float minX = Math.min(0, wrenchX-WRENCH_SIZE/2), maxX = Math.max(size.getX(), wrenchX+WRENCH_SIZE/2);
		float minY = Math.min(0, wrenchY-WRENCH_REACH), maxY = Math.max(size.getY(), wrenchY+WRENCH_REACH);
		float minZ = 0, maxZ = Math.max(size.getZ(), wrenchZ);
		float diagonal = Mth.sqrt(Mth.square(maxX-minX)+Mth.square(maxY-minY)+Mth.square(maxZ-minZ));
		float scale = Math.min(viewHeight, WIDTH)*0.95F/diagonal;
		BlockState casingState = casing.defaultBlockState();
		BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
		float pulse = 0.5F+0.5F*Mth.sin(Util.getMillis()/250F);

		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(centreX, centreY, 150);
		pose.scale(scale, -scale, scale);
		pose.mulPose(Axis.XP.rotationDegrees(pitch));
		pose.mulPose(Axis.YP.rotationDegrees(yaw));
		pose.translate(-(minX+maxX)/2, -(minY+maxY)/2, -(minZ+maxZ)/2);
		Lighting.setupFor3DItems();
		for(BlockPos pos : geometry.shapes().keySet())
		{
			pose.pushPose();
			pose.translate(pos.getX(), pos.getY(), pos.getZ());
			blocks.renderSingleBlock(casingState, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT,
					OverlayTexture.NO_OVERLAY);
			pose.popPose();
		}

		VertexConsumer quads = graphics.bufferSource().getBuffer(RenderType.debugQuads());
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

		pose.pushPose();
		pose.translate(wrenchX, wrenchY-0.05F+0.1F*pulse, wrenchZ);
		pose.scale(WRENCH_SIZE, WRENCH_SIZE, WRENCH_SIZE);
		Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ECContent.ASSEMBLY_WRENCH.get()),
				ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose,
				graphics.bufferSource(), Minecraft.getInstance().level, 0);
		pose.popPose();

		graphics.flush();
		pose.popPose();
	}

	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
			float z, int red, int green, int blue, int alpha)
	{
		consumer.addVertex(pose, x0, y0, z).setColor(red, green, blue, alpha);
		consumer.addVertex(pose, x1, y0, z).setColor(red, green, blue, alpha);
		consumer.addVertex(pose, x1, y1, z).setColor(red, green, blue, alpha);
		consumer.addVertex(pose, x0, y1, z).setColor(red, green, blue, alpha);
	}

	@Override
	public void mouseDragged(double dragX, double dragY)
	{
		yaw += (float)dragX*1.5F;
		pitch = Mth.clamp(pitch+(float)dragY*1.5F, -10, 80);
	}
}

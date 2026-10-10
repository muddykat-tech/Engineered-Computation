package com.muddykat.engineeredcomputation.client.render;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;

final class DiskPackAnimation
{
	record Unit(ResourceLocation pack, ResourceLocation lid, float hingeY, float hingeZ, float lidMidZ, float spindleX,
			float spindleZ)
	{
		static Unit fromExport(String pack, String lid, float hingeY, float lidFrontZ, float lidBackZ, float centreX,
				float centreZ)
		{
			return new Unit(DriveRenderer.id("block/"+pack), DriveRenderer.id("block/"+lid), hingeY/16,
					1-lidFrontZ/16, 1-(lidFrontZ+lidBackZ)/32, 1-centreX/16, 1-centreZ/16);
		}
	}

	static final Unit MAINFRAME = Unit.fromExport("era4_disk_mainframe_pack", "era4_disk_mainframe_lid", 18, 3.5F, 5, 21,
			-5.5F);
	static final Unit BANK = Unit.fromExport("era4_disk_bank_pack", "era4_disk_bank_lid", 18, 6.5F, 8, 2, -2.5F);

	private static final float SPIN_DEGREES = 18;
	private static final float SPIN_UP_TICKS = 30;

	private static final float PACK_TRAVEL = 9/16F;
	private static final float LID_SHUT_DEGREES = 90;
	private static final float LID_LIFT = 0.1F/16;

	private static final float GLASS_ALPHA = 0.7F;
	private static final int GLASS_TINT_INDEX = 1;

	private static final float DURATION = 40;
	private static final float PACK_SHARE = 0.6F;

	private final Map<MultiblockPartBlockEntity, Progress> progress = new WeakHashMap<>();

	private static final class Progress
	{
		float value;
		double lastTime;
		float spin;
		float angle;
	}

	private static DriveRenderer.QuadStyle lidQuadStyle(BakedQuad quad, Unit unit)
	{
		if(quad.getTintIndex()!=GLASS_TINT_INDEX)
			return DriveRenderer.QuadStyle.SOLID;
		float z = Float.intBitsToFloat(quad.getVertices()[DriveRenderer.VERTEX_POSITION+2]);
		boolean outward = (z-unit.lidMidZ())*quad.getDirection().getStepZ()>0;
		return outward? DriveRenderer.QuadStyle.SEE_THROUGH: DriveRenderer.QuadStyle.HIDDEN;
	}

	static List<ResourceLocation> models()
	{
		return List.of(MAINFRAME.pack(), MAINFRAME.lid(), BANK.pack(), BANK.lid());
	}

	void render(DriveRenderer renderer, MultiblockPartBlockEntity part, Unit unit, boolean loaded, boolean networked,
			float partialTick, PoseStack pose, MultiBufferSource buffers, int packedOverlay)
	{
		double now = part.getLevel().getGameTime()+partialTick;
		Progress state = progress.computeIfAbsent(part, p -> {
			Progress initial = new Progress();
			initial.value = loaded? 1: 0;
			initial.lastTime = now;
			return initial;
		});
		float elapsed = (float)Mth.clamp(now-state.lastTime, 0, DURATION);
		float step = elapsed/DURATION;
		state.lastTime = now;
		boolean spinning = loaded&&networked&&state.value>=1;
		state.spin = spinning? Math.min(1, state.spin+elapsed/SPIN_UP_TICKS): Math.max(0, state.spin-elapsed/SPIN_UP_TICKS);
		state.angle = (state.angle+state.spin*SPIN_DEGREES*elapsed)%360;
		if(!loaded&&state.spin>0)
			step = 0;
		state.value = loaded? Math.min(1, state.value+step): Math.max(0, state.value-step);

		float packDown = DriveRenderer.ease(state.value/PACK_SHARE);
		float lidShut = DriveRenderer.ease((state.value-PACK_SHARE)/(1-PACK_SHARE));

		if(loaded||state.value>0)
			renderer.draw(part, unit.pack(), DriveRenderer.transform(local -> {
				local.translate(0, -PACK_TRAVEL*packDown, 0);
				local.translate(unit.spindleX(), 0, unit.spindleZ());
				local.mulPose(Vector3f.YP.rotationDegrees(state.angle));
				local.translate(-unit.spindleX(), 0, -unit.spindleZ());
			}), pose, buffers, packedOverlay);
		Matrix4f lid = DriveRenderer.transform(local -> {
			local.translate(0, unit.hingeY()+LID_LIFT*lidShut, unit.hingeZ());
			local.mulPose(Vector3f.XP.rotationDegrees(LID_SHUT_DEGREES*lidShut));
			local.translate(0, -unit.hingeY(), -unit.hingeZ());
		});
		renderer.draw(part, unit.lid(), lid, pose, buffers, packedOverlay, quad -> lidQuadStyle(quad, unit), GLASS_ALPHA);
	}
}

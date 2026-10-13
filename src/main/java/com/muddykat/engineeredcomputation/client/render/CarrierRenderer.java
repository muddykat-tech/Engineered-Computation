package com.muddykat.engineeredcomputation.client.render;

import java.util.Collection;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.muddykat.engineeredcomputation.client.render.drive.DriveRenderer;
import com.muddykat.engineeredcomputation.pneumatic.carrier.Carrier;
import com.muddykat.engineeredcomputation.pneumatic.carrier.CarrierFlights;
import com.muddykat.engineeredcomputation.pneumatic.tube.CoveredTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.tube.PneumaticTubeBlock;

// This will likely be switched up later on
public final class CarrierRenderer
{
	public static final ResourceLocation MODEL = DriveRenderer.id("block/tube_carrier");
	private static final double RANGE = 64;

	private CarrierRenderer()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, CarrierRenderer::render);
		NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> CarrierFlights.clear());
		NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.Clone.class, event -> CarrierFlights.clear());
	}

	private static boolean isWindowed(ClientLevel level, BlockPos pos)
	{
		BlockState state = level.getBlockState(pos);

		return state.getBlock() instanceof PneumaticTubeBlock&&state.getValue(PneumaticTubeBlock.WINDOWED)
				&&!(state.getBlock() instanceof CoveredTubeBlock covered&&!covered.isClear());
	}

	private static void render(RenderLevelStageEvent event)
	{
		Collection<CarrierFlights.Flight> flights = CarrierFlights.all();
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;

		if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES||flights.isEmpty()||level==null)
			return;

		double time = level.getGameTime()+event.getPartialTick().getGameTimeDeltaPartialTick(false);
		Vec3 camera = event.getCamera().getPosition();
		PoseStack pose = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
		BakedModel model = mc.getModelManager().getModel(ModelResourceLocation.standalone(MODEL));
		List<BakedQuad> quads = DriveRenderer.allQuads(model);

		for(CarrierFlights.Flight flight : flights)
		{
			BlockPos[] path = flight.path();
			if(path.length<2)
				continue;
			double progress = Math.clamp((time-flight.launched())/Carrier.TICKS_PER_BLOCK, 0, path.length-1);
			int step = Math.min((int)progress, path.length-2);
			Vec3 from = Vec3.atCenterOf(path[step]);
			Vec3 to = Vec3.atCenterOf(path[step+1]);
			Vec3 at = from.lerp(to, progress-step);
			if(at.distanceToSqr(camera)>RANGE*RANGE||!isWindowed(level, BlockPos.containing(at)))
				continue;
			Direction heading = Direction.getNearest(to.x-from.x, to.y-from.y, to.z-from.z);
			pose.pushPose();
			pose.translate(at.x-camera.x, at.y-camera.y, at.z-camera.z);
			if(heading.getAxis()==Direction.Axis.X)
				pose.mulPose(Axis.YP.rotationDegrees(90));
			else if(heading.getAxis()==Direction.Axis.Y)
				pose.mulPose(Axis.XP.rotationDegrees(90));
			pose.translate(-0.5, -0.5, -0.5);
			int light = LevelRenderer.getLightColor(level, BlockPos.containing(at));
			for(BakedQuad quad : quads)
				consumer.putBulkData(pose.last(), quad, 1, 1, 1, 1, light, OverlayTexture.NO_OVERLAY);
			pose.popPose();
		}
		buffers.endBatch(Sheets.cutoutBlockSheet());
	}
}

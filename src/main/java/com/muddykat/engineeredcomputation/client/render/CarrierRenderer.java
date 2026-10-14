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
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
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

	private static boolean isWindowed(BlockGetter level, BlockPos pos)
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
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());

		for(CarrierFlights.Flight flight : flights)
		{
			BlockPos[] path = flight.path();
			if(path.length<2)
				continue;
			double travelled = (time-flight.launched())/Carrier.TICKS_PER_BLOCK;
			if(at(path, travelled).distanceToSqr(camera)<=RANGE*RANGE)
				draw(level, path, travelled, camera, event.getPoseStack(), consumer);
		}
		buffers.endBatch(Sheets.cutoutBlockSheet());
	}

	/** Where a carrier is after travelling the given number of blocks along its path. */
	public static Vec3 at(BlockPos[] path, double travelled)
	{
		double progress = Math.clamp(travelled, 0, path.length-1);
		int step = Math.min((int)progress, path.length-2);
		return Vec3.atCenterOf(path[step]).lerp(Vec3.atCenterOf(path[step+1]), progress-step);
	}

	/** Draws a carrier relative to origin, if the tube it's in has a window to see it through. */
	public static void draw(BlockAndTintGetter level, BlockPos[] path, double travelled, Vec3 origin, PoseStack pose,
			VertexConsumer consumer)
	{
		Vec3 at = at(path, travelled);
		BlockPos in = BlockPos.containing(at);
		if(!isWindowed(level, in))
			return;
		int step = Math.min((int)Math.clamp(travelled, 0, path.length-1), path.length-2);
		BlockPos from = path[step], to = path[step+1];
		Direction heading = Direction.getNearest(to.getX()-from.getX(), to.getY()-from.getY(), to.getZ()-from.getZ());
		List<BakedQuad> quads = DriveRenderer.allQuads(Minecraft.getInstance().getModelManager()
				.getModel(ModelResourceLocation.standalone(MODEL)));
		pose.pushPose();
		pose.translate(at.x-origin.x, at.y-origin.y, at.z-origin.z);
		if(heading.getAxis()==Direction.Axis.X)
			pose.mulPose(Axis.YP.rotationDegrees(90));
		else if(heading.getAxis()==Direction.Axis.Y)
			pose.mulPose(Axis.XP.rotationDegrees(90));
		pose.translate(-0.5, -0.5, -0.5);
		int light = LevelRenderer.getLightColor(level, in);
		for(BakedQuad quad : quads)
			consumer.putBulkData(pose.last(), quad, 1, 1, 1, 1, light, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}
}

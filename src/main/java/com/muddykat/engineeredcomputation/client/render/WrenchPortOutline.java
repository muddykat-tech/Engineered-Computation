package com.muddykat.engineeredcomputation.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.muddykat.engineeredcomputation.connector.ConnectorPorts;
import com.muddykat.engineeredcomputation.item.AssemblyWrenchItem;

public final class WrenchPortOutline
{
	private WrenchPortOutline()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(RenderHighlightEvent.Block.class, WrenchPortOutline::render);
	}

	private static void render(RenderHighlightEvent.Block event)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if(minecraft.level==null||minecraft.player==null
				||!(minecraft.player.getMainHandItem().getItem() instanceof AssemblyWrenchItem))
			return;
		BlockPos pos = event.getTarget().getBlockPos();
		BlockState state = minecraft.level.getBlockState(pos);
		Vec3 local = event.getTarget().getLocation().subtract(Vec3.atLowerCornerOf(pos));
		Direction side = ConnectorPorts.portAt(minecraft.level, pos, state, local);
		if(side==null)
			return;
		event.setCanceled(true);
		Vec3 camera = event.getCamera().getPosition();
		AABB port = ConnectorPorts.portShape(minecraft.level, pos, state, side).bounds()
				.move(pos.getX()-camera.x, pos.getY()-camera.y, pos.getZ()-camera.z).inflate(0.002);
		PoseStack pose = event.getPoseStack();
		LevelRenderer.renderLineBox(pose, event.getMultiBufferSource().getBuffer(RenderType.lines()), port,
				1.0F, 0.82F, 0.29F, 0.9F);
	}
}

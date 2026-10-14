package com.muddykat.engineeredcomputation.compat.ponder;

import java.util.ArrayList;
import java.util.List;

import net.createmod.ponder.api.element.PonderSceneElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.PonderElementBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import com.muddykat.engineeredcomputation.client.render.CarrierRenderer;
import com.muddykat.engineeredcomputation.pneumatic.carrier.Carrier;

/**
 * A carrier flying along a tube path in a scene, as the world's carriers are only known from the server. Like those,
 * it's only seen through windowed tubes.
 */
public final class CarrierElement extends PonderElementBase implements PonderSceneElement
{
	private final BlockPos[] path;
	private int ticks;

	private CarrierElement(BlockPos[] path)
	{
		this.path = path;
	}

	/**
	 * Sends carriers through the corners given, along straight runs between them, spaced as a station sends them, and
	 * idles until the last one arrives.
	 */
	public static void send(SceneBuilder scene, int carriers, int spacing, BlockPos... corners)
	{
		BlockPos[] path = along(corners);
		for(int carrier = 0; carrier<carriers; carrier++)
		{
			scene.addInstruction(ponder -> ponder.addElement(new CarrierElement(path)));
			if(carrier<carriers-1)
				scene.idle(spacing);
		}
		scene.idle((path.length-1)*Carrier.TICKS_PER_BLOCK);
	}

	private static BlockPos[] along(BlockPos... corners)
	{
		List<BlockPos> path = new ArrayList<>();
		path.add(corners[0]);
		for(int corner = 1; corner<corners.length; corner++)
		{
			BlockPos from = corners[corner-1], to = corners[corner];
			Direction step = Direction.getNearest(to.getX()-from.getX(), to.getY()-from.getY(), to.getZ()-from.getZ());
			for(BlockPos at = from; !at.equals(to); )
			{
				at = at.relative(step);
				path.add(at);
			}
		}
		return path.toArray(BlockPos[]::new);
	}

	@Override
	public void tick(PonderScene scene)
	{
		ticks++;
		if(ticks>(path.length-1)*Carrier.TICKS_PER_BLOCK)
			setVisible(false);
	}

	@Override
	public void renderFirst(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt)
	{
	}

	@Override
	public void renderLayer(PonderLevel world, MultiBufferSource buffer, RenderType type, GuiGraphics graphics,
			float pt)
	{
	}

	@Override
	public void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt)
	{
		CarrierRenderer.draw(world, path, (ticks+pt)/Carrier.TICKS_PER_BLOCK, Vec3.ZERO, graphics.pose(),
				buffer.getBuffer(Sheets.cutoutBlockSheet()));
	}
}

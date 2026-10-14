package com.muddykat.engineeredcomputation.client.render.drive;

import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

import com.muddykat.engineeredcomputation.client.render.AnimationClock;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;

final class TapeReels
{
	private static final float[] CENTRE_X = {-6.5F, 6.5F};
	private static final float CENTRE_Y = 62;
	private static final int HALF = 6;
	private static final float FLANGE = 6;
	private static final float RIM = 5;
	private static final float HUB = 1.6F;
	private static final float SPINDLE = 0.8F;
	private static final float SPOKE = 0.6F;
	private static final float FULL = RIM;
	private static final float BACK_Z = -12.05F;
	private static final float TAPE_Z = -12.5F;
	private static final float FRONT_Z = -13;
	private static final float HUB_Z = -13.1F;
	private static final int SPOKES = 3;

	private static final float PASS_TICKS = 4*60*20;
	private static final float TAPE_SPEED = 0.3F;
	private static final float SPIN_UP_TICKS = 15;
	private static final float SLIDE_TICKS = 20;
	private static final float SLIDE_OUT = 14;

	private static final int[] TIER_COLOURS = {0x4678CD, 0x46A55A, 0xE19632, 0x962832};
	private static final int BACK_COLOUR = 0x3C3E42;
	private static final int TAPE_COLOUR = 0x603A24;
	private static final int TAPE_EDGE_COLOUR = 0x4A2C1B;
	private static final int HUB_COLOUR = 0xC8C8CD;
	private static final int SPINDLE_COLOUR = 0x19191C;
	private static final int NONE = -1;

	private final Map<MultiblockPartBlockEntity, State> states = new WeakHashMap<>();

	private static final class State
	{
		boolean present;
		double changedAt = Double.NEGATIVE_INFINITY;
		double lastTime;
		int tier;
		float wound;
		int direction = 1;
		float spin;
		final float[] angles = new float[2];
	}

	void render(MultiblockPartBlockEntity part, boolean networked, float partialTick, PoseStack pose,
			MultiBufferSource buffers, int packedOverlay)
	{
		boolean present = (part.getClientSyncData().getInt(EraDriveState.FILLED_SLOTS)&1)!=0;
		int tierBits = part.getClientSyncData().getInt(EraDriveState.CELL_TIERS)&0xF;
		double now = AnimationClock.now(part.getLevel(), partialTick);
		State state = states.computeIfAbsent(part, p -> {
			State initial = new State();
			initial.present = present;
			initial.lastTime = now;
			return initial;
		});
		if(tierBits>0)
			state.tier = Math.min(TIER_COLOURS.length, tierBits)-1;
		if(present!=state.present)
		{
			state.present = present;
			state.changedAt = now;
			if(present)
			{
				state.wound = 0;
				state.direction = 1;
			}
		}
		float dt = (float)Mth.clamp(now-state.lastTime, 0, 5);
		state.lastTime = now;

		float sliding = DriveRenderer.ease((float)((now-state.changedAt)/SLIDE_TICKS));
		if(!present&&sliding>=1)
			return;
		float out = present? 1-sliding: sliding;

		boolean running = present&&out==0&&networked;
		state.spin = running? Math.min(1, state.spin+dt/SPIN_UP_TICKS): Math.max(0, state.spin-dt/SPIN_UP_TICKS);
		float[] radii = {radius(1-state.wound), radius(state.wound)};
		if(state.spin>0)
		{
			state.wound = Mth.clamp(state.wound+state.direction*state.spin*dt/PASS_TICKS, 0, 1);
			if(state.wound>=1||state.wound<=0)
				state.direction = state.wound>=1? -1: 1;
			for(int reel = 0; reel<2; reel++)
				state.angles[reel] += state.direction*state.spin*TAPE_SPEED/Math.max(HUB, radii[reel])*dt;
		}

		TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
				.apply(DriveRenderer.id("block/indicator_light"));
		VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
		pose.pushPose();
		DriveRenderer.rotate(pose, part.getFacing());
		pose.translate(0, 0, out*SLIDE_OUT/16);
		for(int reel = 0; reel<2; reel++)
		{
			int light = DriveRenderer.lightAt(part, point(CENTRE_X[reel], CENTRE_Y, FRONT_Z));
			Painter paint = new Painter(buffer, pose.last(), white, light, packedOverlay, CENTRE_X[reel]);
			drawReel(paint, radii[reel], state.angles[reel], TIER_COLOURS[state.tier]);
		}
		pose.popPose();
	}

	private static float radius(float share)
	{
		return Mth.sqrt(HUB*HUB+share*(FULL*FULL-HUB*HUB));
	}

	private static float offset(int i)
	{
		return i-HALF+0.5F;
	}

	private static float distance(int column, int row)
	{
		return Mth.sqrt(offset(column)*offset(column)+offset(row)*offset(row));
	}

	private static void drawReel(Painter paint, float tape, float angle, int holder)
	{
		int[][] back = new int[2*HALF][2*HALF], wound = new int[2*HALF][2*HALF], front = new int[2*HALF][2*HALF],
				hub = new int[2*HALF][2*HALF];
		int spoke = scale(holder, 0.9F);
		for(int row = 0; row<2*HALF; row++)
			for(int column = 0; column<2*HALF; column++)
			{
				float r = distance(column, row);
				back[row][column] = r<=FLANGE? BACK_COLOUR: NONE;
				wound[row][column] = r>HUB&&r<=tape? r>tape-1? TAPE_EDGE_COLOUR: TAPE_COLOUR: NONE;
				front[row][column] = r>RIM&&r<=FLANGE? holder: r>HUB&&r<=RIM&&onSpoke(column, row, angle)? spoke: NONE;
				hub[row][column] = r<=SPINDLE? SPINDLE_COLOUR: r<=HUB? HUB_COLOUR: NONE;
			}
		paint.layer(back, BACK_Z);
		paint.layer(wound, TAPE_Z);
		paint.layer(front, FRONT_Z);
		paint.layer(hub, HUB_Z);
		paint.edge(back, BACK_Z, FRONT_Z, scale(holder, 0.75F));
	}

	private static boolean onSpoke(int column, int row, float angle)
	{
		float x = offset(column), y = offset(row);
		for(int spoke = 0; spoke<SPOKES; spoke++)
		{
			float along = angle+spoke*Mth.TWO_PI/SPOKES;
			float cos = Mth.cos(along), sin = Mth.sin(along);
			if(x*cos+y*sin>0&&Math.abs(y*cos-x*sin)<SPOKE)
				return true;
		}
		return false;
	}

	private static int scale(int colour, float by)
	{
		return (int)((colour>>16&0xFF)*by)<<16|(int)((colour>>8&0xFF)*by)<<8|(int)((colour&0xFF)*by);
	}

	private static Vector3f point(float x, float y, float z)
	{
		return new Vector3f(1-x/16, y/16, 1-z/16);
	}

	private record Painter(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite white, int light, int overlay,
			float centreX)
	{
		void layer(int[][] pixels, float z)
		{
			for(int row = 0; row<pixels.length; row++)
				for(int column = 0; column<pixels[row].length; )
				{
					int colour = pixels[row][column], end = column+1;
					while(end<pixels[row].length&&pixels[row][end]==colour)
						end++;
					if(colour!=NONE)
						quad(at(column, row, z), at(end, row, z), at(end, row+1, z), at(column, row+1, z), colour, 0, 0,
								-1);
					column = end;
				}
		}

		void edge(int[][] pixels, float back, float front, int colour)
		{
			int size = pixels.length;
			for(int row = 0; row<size; row++)
				for(int column = 0; column<size; column++)
				{
					if(pixels[row][column]==NONE)
						continue;
					if(column==0||pixels[row][column-1]==NONE)
						quad(at(column, row, back), at(column, row+1, back), at(column, row+1, front),
								at(column, row, front), colour, -1, 0, 0);
					if(column==size-1||pixels[row][column+1]==NONE)
						quad(at(column+1, row, back), at(column+1, row+1, back), at(column+1, row+1, front),
								at(column+1, row, front), colour, 1, 0, 0);
					if(row==0||pixels[row-1][column]==NONE)
						quad(at(column, row, back), at(column+1, row, back), at(column+1, row, front),
								at(column, row, front), colour, 0, -1, 0);
					if(row==size-1||pixels[row+1][column]==NONE)
						quad(at(column, row+1, back), at(column+1, row+1, back), at(column+1, row+1, front),
								at(column, row+1, front), colour, 0, 1, 0);
				}
		}

		private Vector3f at(int column, int row, float z)
		{
			return point(centreX-HALF+column, CENTRE_Y-HALF+row, z);
		}

		private void quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d, int colour, float nx, float ny, float nz)
		{
			vertex(a, colour, -nx, ny, -nz);
			vertex(b, colour, -nx, ny, -nz);
			vertex(c, colour, -nx, ny, -nz);
			vertex(d, colour, -nx, ny, -nz);
			vertex(d, colour, nx, -ny, nz);
			vertex(c, colour, nx, -ny, nz);
			vertex(b, colour, nx, -ny, nz);
			vertex(a, colour, nx, -ny, nz);
		}

		private void vertex(Vector3f point, int colour, float nx, float ny, float nz)
		{
			buffer.addVertex(pose, point.x(), point.y(), point.z())
					.setColor(colour>>16&0xFF, colour>>8&0xFF, colour&0xFF, 255)
					.setUv(white.getU(0.5F), white.getV(0.5F))
					.setOverlay(overlay)
					.setLight(light)
					.setNormal(pose, nx, ny, nz);
		}
	}
}

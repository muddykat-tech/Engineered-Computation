package com.muddykat.engineeredcomputation.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraftforge.client.model.IQuadTransformer;

final class QuadSplitter
{
	private static final float EPSILON = 1e-4f;
	private static final float MIN_AREA = 1e-7f;

	private QuadSplitter()
	{
	}

	static Map<Vec3i, List<BakedQuad>> split(List<BakedQuad> quads, Set<Vec3i> parts, Transformation rotation)
	{
		Transformation toCorner = rotation.blockCenterToCorner();
		Map<Vec3i, List<BakedQuad>> result = new HashMap<>();
		for(BakedQuad quad : quads)
		{
			List<Vertex> vertices = read(quad);
			Vector3f faceNormal = faceNormal(vertices);
			float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
			float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
			for(Vertex v : vertices)
				for(int axis = 0; axis<3; axis++)
				{
					min[axis] = Math.min(min[axis], v.pos[axis]);
					max[axis] = Math.max(max[axis], v.pos[axis]);
				}

			for(int x = Mth.floor(min[0]-EPSILON); x<=Mth.floor(max[0]+EPSILON); x++)
				for(int y = Mth.floor(min[1]-EPSILON); y<=Mth.floor(max[1]+EPSILON); y++)
					for(int z = Mth.floor(min[2]-EPSILON); z<=Mth.floor(max[2]+EPSILON); z++)
					{
						Vec3i cell = new Vec3i(x, y, z);
						List<Vertex> piece = clipToCell(vertices, cell);
						if(piece.size()<3||area(piece)<MIN_AREA||!ownsBoundaryFace(piece, faceNormal, cell))
							continue;
						Vec3i target = parts.contains(cell)? cell: nearest(parts, cell);
						List<BakedQuad> out = result.computeIfAbsent(target, k -> new ArrayList<>());
						emitFan(piece, target, quad, faceNormal, toCorner, out);
					}
		}
		return result;
	}

	private static List<Vertex> clipToCell(List<Vertex> polygon, Vec3i cell)
	{
		int[] lo = {cell.getX(), cell.getY(), cell.getZ()};
		List<Vertex> out = polygon;
		for(int axis = 0; axis<3&&out.size()>=3; axis++)
		{
			out = clipPlane(out, axis, lo[axis], true);
			if(out.size()>=3)
				out = clipPlane(out, axis, lo[axis]+1, false);
		}
		return out;
	}

	private static List<Vertex> clipPlane(List<Vertex> in, int axis, float bound, boolean keepAbove)
	{
		List<Vertex> out = new ArrayList<>(in.size()+2);
		for(int i = 0; i<in.size(); i++)
		{
			Vertex current = in.get(i);
			Vertex next = in.get((i+1)%in.size());
			float dCurrent = keepAbove? current.pos[axis]-bound: bound-current.pos[axis];
			float dNext = keepAbove? next.pos[axis]-bound: bound-next.pos[axis];
			boolean currentInside = dCurrent>=-EPSILON;
			boolean nextInside = dNext>=-EPSILON;
			if(currentInside)
				out.add(current);
			if(currentInside!=nextInside)
				out.add(Vertex.lerp(current, next, dCurrent/(dCurrent-dNext)));
		}
		return out;
	}

	private static boolean ownsBoundaryFace(List<Vertex> piece, Vector3f normal, Vec3i cell)
	{
		int[] lo = {cell.getX(), cell.getY(), cell.getZ()};
		for(int axis = 0; axis<3; axis++)
		{
			if(allNear(piece, axis, lo[axis]))
				return normal.get(axis)<0;
			if(allNear(piece, axis, lo[axis]+1))
				return normal.get(axis)>0;
		}
		return true;
	}

	private static boolean allNear(List<Vertex> piece, int axis, float value)
	{
		for(Vertex v : piece)
			if(Math.abs(v.pos[axis]-value)>EPSILON)
				return false;
		return true;
	}

	private static Vec3i nearest(Set<Vec3i> parts, Vec3i cell)
	{
		Vec3i best = Vec3i.ZERO;
		double bestDistance = Double.MAX_VALUE;
		for(Vec3i part : parts)
		{
			double distance = part.distSqr(cell);
			if(distance<bestDistance)
			{
				bestDistance = distance;
				best = part;
			}
		}
		return best;
	}

	private static float area(List<Vertex> polygon)
	{
		Vector3f sum = new Vector3f();
		Vector3f a = new Vector3f();
		Vector3f b = new Vector3f();
		Vertex origin = polygon.get(0);
		for(int i = 1; i+1<polygon.size(); i++)
		{
			polygon.get(i).offsetFrom(origin, a);
			polygon.get(i+1).offsetFrom(origin, b);
			sum.add(a.cross(b));
		}
		return sum.length()/2;
	}

	private static Vector3f faceNormal(List<Vertex> quad)
	{
		Vector3f diagonalA = new Vector3f();
		Vector3f diagonalB = new Vector3f();
		quad.get(2).offsetFrom(quad.get(0), diagonalA);
		quad.get(3).offsetFrom(quad.get(1), diagonalB);
		Vector3f normal = diagonalA.cross(diagonalB);
		if(normal.lengthSquared()<1e-12f)
			normal.set(quad.get(0).normal);
		return normal.normalize();
	}

	private static void emitFan(
			List<Vertex> polygon, Vec3i offset, BakedQuad template, Vector3f faceNormal, Transformation toCorner,
			List<BakedQuad> out)
	{
		Vector3f rotatedNormal = new Vector3f(faceNormal);
		toCorner.transformNormal(rotatedNormal);
		Direction direction = Direction.getNearest(rotatedNormal.x(), rotatedNormal.y(), rotatedNormal.z());

		Vertex first = polygon.get(0);
		int i = 1;
		while(i<polygon.size()-1)
		{
			Vertex b = polygon.get(i);
			Vertex c = polygon.get(i+1);
			Vertex d = i+2<polygon.size()? polygon.get(i+2): c;
			out.add(bake(new Vertex[]{first, b, c, d}, offset, template, direction, toCorner));
			i += 2;
		}
	}

	private static BakedQuad bake(
			Vertex[] vertices, Vec3i offset, BakedQuad template, Direction direction, Transformation toCorner)
	{
		int[] data = new int[4*IQuadTransformer.STRIDE];
		for(int i = 0; i<4; i++)
		{
			Vertex v = vertices[i];
			int base = i*IQuadTransformer.STRIDE;

			Vector4f pos = new Vector4f(v.pos[0]-offset.getX(), v.pos[1]-offset.getY(), v.pos[2]-offset.getZ(), 1);
			toCorner.transformPosition(pos);
			data[base+IQuadTransformer.POSITION] = Float.floatToRawIntBits(snap(pos.x()/pos.w()));
			data[base+IQuadTransformer.POSITION+1] = Float.floatToRawIntBits(snap(pos.y()/pos.w()));
			data[base+IQuadTransformer.POSITION+2] = Float.floatToRawIntBits(snap(pos.z()/pos.w()));

			data[base+IQuadTransformer.COLOR] = v.color;
			data[base+IQuadTransformer.UV0] = Float.floatToRawIntBits(v.u);
			data[base+IQuadTransformer.UV0+1] = Float.floatToRawIntBits(v.v);
			data[base+IQuadTransformer.UV2] = v.light;

			Vector3f normal = new Vector3f(v.normal);
			toCorner.transformNormal(normal);
			data[base+IQuadTransformer.NORMAL] = packNormal(normal);
		}
		return new BakedQuad(data, template.getTintIndex(), direction, template.getSprite(), template.isShade(),
				template.hasAmbientOcclusion());
	}

	private static float snap(float value)
	{
		float rounded = Math.round(value);
		return Math.abs(value-rounded)<EPSILON? rounded: value;
	}

	private static int packNormal(Vector3f normal)
	{
		return ((byte)Math.round(normal.x()*127)&0xFF)
				|(((byte)Math.round(normal.y()*127)&0xFF)<<8)
				|(((byte)Math.round(normal.z()*127)&0xFF)<<16);
	}

	private static List<Vertex> read(BakedQuad quad)
	{
		int[] data = quad.getVertices();
		List<Vertex> vertices = new ArrayList<>(4);
		for(int i = 0; i<4; i++)
		{
			int base = i*IQuadTransformer.STRIDE;
			int packedNormal = data[base+IQuadTransformer.NORMAL];
			vertices.add(new Vertex(
					new float[]{
							Float.intBitsToFloat(data[base+IQuadTransformer.POSITION]),
							Float.intBitsToFloat(data[base+IQuadTransformer.POSITION+1]),
							Float.intBitsToFloat(data[base+IQuadTransformer.POSITION+2])
					},
					data[base+IQuadTransformer.COLOR],
					Float.intBitsToFloat(data[base+IQuadTransformer.UV0]),
					Float.intBitsToFloat(data[base+IQuadTransformer.UV0+1]),
					data[base+IQuadTransformer.UV2],
					new Vector3f((byte)packedNormal/127f, (byte)(packedNormal>>8)/127f, (byte)(packedNormal>>16)/127f)
			));
		}
		return vertices;
	}

	private record Vertex(float[] pos, int color, float u, float v, int light, Vector3f normal)
	{
		static Vertex lerp(Vertex a, Vertex b, float t)
		{
			float[] pos = new float[3];
			for(int axis = 0; axis<3; axis++)
				pos[axis] = Mth.lerp(t, a.pos[axis], b.pos[axis]);
			Vector3f normal = new Vector3f(a.normal).lerp(b.normal, t);
			if(normal.lengthSquared()>0)
				normal.normalize();
			return new Vertex(pos, lerpColor(a.color, b.color, t), Mth.lerp(t, a.u, b.u), Mth.lerp(t, a.v, b.v),
					a.light, normal);
		}

		private static int lerpColor(int a, int b, float t)
		{
			int out = 0;
			for(int shift = 0; shift<32; shift += 8)
			{
				int channel = Math.round(Mth.lerp(t, (a>>>shift)&0xFF, (b>>>shift)&0xFF));
				out |= (channel&0xFF)<<shift;
			}
			return out;
		}

		void offsetFrom(Vertex origin, Vector3f out)
		{
			out.set(pos[0]-origin.pos[0], pos[1]-origin.pos[1], pos[2]-origin.pos[2]);
		}
	}
}

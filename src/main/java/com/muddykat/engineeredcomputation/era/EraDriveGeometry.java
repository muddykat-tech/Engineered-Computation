package com.muddykat.engineeredcomputation.era;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public record EraDriveGeometry(BlockPos size, BlockPos master, BlockPos trigger, Map<BlockPos, VoxelShape> shapes,
		List<AABB> terminalZones, List<AABB> storageZones, List<AABB> bayZones, List<NetworkPort> ports)
{
	private static final double ZONE_TOLERANCE = 1/64.0;

	public VoxelShape shapeAt(BlockPos posInMultiblock)
	{
		return shapes.getOrDefault(posInMultiblock, Shapes.block());
	}

	public boolean isTerminal(Vec3 point)
	{
		return inAny(terminalZones, point);
	}

	public boolean isStorage(Vec3 point)
	{
		return inAny(storageZones, point);
	}

	public int bayAt(Vec3 point)
	{
		for(int slot = 0; slot<bayZones.size(); slot++)
			if(bayZones.get(slot).inflate(ZONE_TOLERANCE).contains(point))
				return slot;
		return -1;
	}

	public int bayAt(Vec3 eye, Vec3 hit, IntPredicate filled)
	{
		int nearest = -1;
		double nearestDistance = Double.MAX_VALUE;
		for(int slot = 0; slot<bayZones.size(); slot++)
		{
			if(!filled.test(slot))
				continue;
			Optional<Vec3> entry = bayZones.get(slot).inflate(ZONE_TOLERANCE).clip(eye, hit);
			if(entry.isPresent()&&entry.get().distanceToSqr(eye)<nearestDistance)
			{
				nearest = slot;
				nearestDistance = entry.get().distanceToSqr(eye);
			}
		}
		return nearest>=0? nearest: bayAt(hit);
	}

	public boolean hasStorageAt(BlockPos posInMultiblock)
	{
		AABB block = new AABB(posInMultiblock).deflate(ZONE_TOLERANCE);
		for(AABB zone : storageZones)
			if(zone.intersects(block))
				return true;
		return false;
	}

	public boolean connectsOn(BlockPos posInMultiblock, Direction templateSide)
	{
		if(ports.isEmpty())
			return true;
		for(NetworkPort port : ports)
			if(port.pos().equals(posInMultiblock)&&port.side()==templateSide)
				return true;
		return false;
	}

	private static boolean inAny(List<AABB> zones, Vec3 point)
	{
		for(AABB zone : zones)
			if(zone.inflate(ZONE_TOLERANCE).contains(point))
				return true;
		return false;
	}
}

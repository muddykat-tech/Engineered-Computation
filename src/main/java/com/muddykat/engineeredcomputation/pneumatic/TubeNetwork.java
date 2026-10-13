package com.muddykat.engineeredcomputation.pneumatic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

import com.muddykat.engineeredcomputation.registration.ECCapabilities;

public final class TubeNetwork
{
	public static final int MAX_BLOCKS = 4096;

	private final Set<BlockPos> tubes;
	private final Set<BlockPos> ends;
	private final Map<BlockPos, List<BlockPos>> links;
	private final boolean tooLarge;

	private TubeNetwork(Set<BlockPos> tubes, Set<BlockPos> ends, Map<BlockPos, List<BlockPos>> links, boolean tooLarge)
	{
		this.tubes = tubes;
		this.ends = ends;
		this.links = links;
		this.tooLarge = tooLarge;
	}

	private static final Map<Level, Map<BlockPos, TubeNetwork>> KNOWN = new WeakHashMap<>();

	public static TubeNetwork of(Level level, BlockPos pos)
	{
		Map<BlockPos, TubeNetwork> known = KNOWN.computeIfAbsent(level, l -> new HashMap<>());
		TubeNetwork network = known.get(pos);
		if(network==null)
		{
			network = scan(level, pos);
			if(!network.tooLarge)
			{
				for(BlockPos block : network.tubes)
					known.put(block, network);
				for(BlockPos block : network.ends)
					known.put(block, network);
			}
			known.put(pos.immutable(), network);
		}
		return network;
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, event -> {
			if(event.getLevel() instanceof Level level)
				changed(level);
		});
		NeoForge.EVENT_BUS.addListener(ChunkEvent.Unload.class, event -> {
			if(event.getLevel() instanceof Level level)
				changed(level);
		});
	}

	public static void changed(Level level)
	{
		Map<BlockPos, TubeNetwork> known = KNOWN.get(level);
		if(known!=null)
			known.clear();
	}

	public static TubeNetwork scan(Level level, BlockPos start)
	{
		Set<BlockPos> tubes = new HashSet<>();
		Set<BlockPos> ends = new HashSet<>();
		Map<BlockPos, List<BlockPos>> links = new HashMap<>();
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		visited.add(start.immutable());
		queue.add(start.immutable());
		boolean tooLarge = false;
		while(!queue.isEmpty())
		{
			if(visited.size()>MAX_BLOCKS)
			{
				tooLarge = true;
				break;
			}
			BlockPos pos = queue.poll();
			TubeMember member = level.getCapability(ECCapabilities.TUBE_MEMBER, pos, null);
			if(member==null&&!pos.equals(start))
				continue;
			if(member!=null)
				(member.carries()? tubes: ends).add(pos);
			for(Direction direction : Direction.values())
			{
				BlockPos next = pos.relative(direction);
				if(!level.isLoaded(next))
					continue;
				if(member!=null&&level.getCapability(ECCapabilities.TUBE_MEMBER, pos, direction)==null)
					continue;
				if(level.getCapability(ECCapabilities.TUBE_MEMBER, next, direction.getOpposite())==null)
					continue;
				links.computeIfAbsent(pos, p -> new ArrayList<>()).add(next);
				links.computeIfAbsent(next, p -> new ArrayList<>()).add(pos);
				if(visited.add(next))
					queue.add(next);
			}
		}
		return new TubeNetwork(Collections.unmodifiableSet(tubes), Collections.unmodifiableSet(ends), links, tooLarge);
	}

	public Set<BlockPos> getTubes()
	{
		return tubes;
	}

	public Set<BlockPos> getEnds()
	{
		return ends;
	}

	public boolean isTooLarge()
	{
		return tooLarge;
	}

	/** Maps each tube or end to the one it's first reached from, nearest first; ends aren't passed through. */
	public Map<BlockPos, BlockPos> tree(BlockPos from)
	{
		Map<BlockPos, BlockPos> cameFrom = new LinkedHashMap<>();
		if(tooLarge||!ends.contains(from))
			return cameFrom;
		Queue<BlockPos> queue = new ArrayDeque<>();
		cameFrom.put(from, from);
		queue.add(from);
		while(!queue.isEmpty())
		{
			BlockPos pos = queue.poll();
			if(!pos.equals(from)&&!tubes.contains(pos))
				continue;
			for(BlockPos next : links.getOrDefault(pos, List.of()))
			{
				if(cameFrom.containsKey(next)||!tubes.contains(next)&&!tubes.contains(pos))
					continue;
				cameFrom.put(next, pos);
				queue.add(next);
			}
		}
		return cameFrom;
	}

	public List<BlockPos> route(BlockPos from, BlockPos to)
	{
		if(tooLarge||!ends.contains(from)||!ends.contains(to)||from.equals(to))
			return List.of();
		Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		cameFrom.put(from, from);
		queue.add(from);
		while(!queue.isEmpty())
		{
			BlockPos pos = queue.poll();
			for(BlockPos next : links.getOrDefault(pos, List.of()))
			{
				if(cameFrom.containsKey(next))
					continue;
				if(next.equals(to)&&tubes.contains(pos))
				{
					List<BlockPos> path = new ArrayList<>();
					path.add(to);
					for(BlockPos step = pos; !step.equals(from); step = cameFrom.get(step))
						path.add(step);
					path.add(from);
					Collections.reverse(path);
					return path;
				}
				if(!tubes.contains(next))
					continue;
				cameFrom.put(next, pos);
				queue.add(next);
			}
		}
		return List.of();
	}
}

package com.muddykat.engineeredcomputation.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

import com.muddykat.engineeredcomputation.crafting.CraftingComputerState;
import com.muddykat.engineeredcomputation.crafting.DataProcessorState;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.storage.CellInventory;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;
import com.muddykat.engineeredcomputation.storage.StorageHost;

public final class StorageNetwork
{
	public static final int MAX_BLOCKS = 4096;

	private final List<StorageHost> hosts;
	@Nullable
	private final ComputerEra processor;
	private final Set<ComputerEra> eras;
	private final Set<ComputerEra> conflictingEras;
	private final boolean tooLarge;
	private final Set<BlockPos> blocks;
	private final List<CraftingComputerState> craftingComputers;
	private final List<DataProcessorState> dataProcessors;

	private StorageNetwork(List<StorageHost> hosts, Set<ComputerEra> eras, Set<ComputerEra> conflictingEras,
			boolean tooLarge, Set<BlockPos> blocks, List<CraftingComputerState> craftingComputers,
			List<DataProcessorState> dataProcessors)
	{
		this.hosts = hosts;
		this.processor = hosts.stream().filter(StorageHost::isProcessor).map(StorageHost::getEra)
				.max(Comparator.naturalOrder()).orElse(null);
		this.eras = eras;
		this.conflictingEras = conflictingEras;
		this.tooLarge = tooLarge;
		this.blocks = blocks;
		this.craftingComputers = craftingComputers;
		this.dataProcessors = dataProcessors;
	}

	private static final Map<Level, Map<BlockPos, StorageNetwork>> KNOWN = new WeakHashMap<>();

	public static StorageNetwork of(Level level, BlockPos pos)
	{
		Map<BlockPos, StorageNetwork> known = KNOWN.computeIfAbsent(level, l -> new HashMap<>());
		StorageNetwork network = known.get(pos);
		if(network==null)
		{
			network = scan(level, pos);
			if(!network.tooLarge)
				for(BlockPos block : network.blocks)
					known.put(block, network);
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
		Map<BlockPos, StorageNetwork> known = KNOWN.get(level);
		if(known!=null)
			known.clear();
	}

	public static StorageNetwork scan(Level level, BlockPos start)
	{
		Set<StorageHost> hosts = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<ComputerEra> eras = EnumSet.noneOf(ComputerEra.class);
		List<StorageHost> ordered = new ArrayList<>();
		Set<CraftingComputerState> computers = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<DataProcessorState> processors = Collections.newSetFromMap(new IdentityHashMap<>());
		Map<BlockPos, ComputerEra> eraAt = new HashMap<>();
		Set<BlockPos> bridges = new HashSet<>();
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
			NetworkMember member = level.getCapability(ECCapabilities.NETWORK_MEMBER, pos, null);
			if(member==null&&!pos.equals(start))
				continue;
			if(member!=null&&member.getStorage()!=null&&hosts.add(member.getStorage()))
				ordered.add(member.getStorage());
			if(member!=null&&member.getEra()!=null)
			{
				eras.add(member.getEra());
				eraAt.put(pos, member.getEra());
			}
			if(member!=null&&member.isBridge())
				bridges.add(pos);
			if(member!=null&&member.getCraftingComputer()!=null)
				computers.add(member.getCraftingComputer());
			if(member!=null&&member.getDataProcessor()!=null)
				processors.add(member.getDataProcessor());
			for(Direction direction : Direction.values())
			{
				BlockPos next = pos.relative(direction);
				if(!level.isLoaded(next))
					continue;
				if(member!=null&&level.getCapability(ECCapabilities.NETWORK_MEMBER, pos, direction)==null)
					continue;
				if(level.getCapability(ECCapabilities.NETWORK_MEMBER, next, direction.getOpposite())==null)
					continue;
				links.computeIfAbsent(pos, p -> new ArrayList<>()).add(next);
				links.computeIfAbsent(next, p -> new ArrayList<>()).add(pos);
				if(visited.add(next))
					queue.add(next);
			}
		}
		Set<ComputerEra> conflicting = eras.size()<=1? Set.of(): conflicting(eraAt, bridges, links);
		return new StorageNetwork(List.copyOf(ordered), Collections.unmodifiableSet(eras), conflicting, tooLarge,
				Collections.unmodifiableSet(visited), List.copyOf(computers), List.copyOf(processors));
	}

	private static Set<ComputerEra> conflicting(Map<BlockPos, ComputerEra> eraAt, Set<BlockPos> bridges,
			Map<BlockPos, List<BlockPos>> links)
	{
		Set<ComputerEra> conflicting = EnumSet.noneOf(ComputerEra.class);
		Set<BlockPos> seen = new HashSet<>(bridges);
		for(BlockPos from : eraAt.keySet())
		{
			if(!seen.add(from))
				continue;
			Set<ComputerEra> sectionEras = EnumSet.noneOf(ComputerEra.class);
			Queue<BlockPos> queue = new ArrayDeque<>(List.of(from));
			while(!queue.isEmpty())
			{
				BlockPos pos = queue.poll();
				if(eraAt.containsKey(pos))
					sectionEras.add(eraAt.get(pos));
				for(BlockPos next : links.getOrDefault(pos, List.of()))
					if(seen.add(next))
						queue.add(next);
			}
			if(sectionEras.size()>1)
				conflicting.addAll(sectionEras);
		}
		return Collections.unmodifiableSet(conflicting);
	}

	public boolean isTooLarge()
	{
		return tooLarge;
	}

	public List<CraftingComputerState> getCraftingComputers()
	{
		return craftingComputers;
	}

	public List<DataProcessorState> getDataProcessors()
	{
		return dataProcessors;
	}

	public List<StorageHost> getHosts()
	{
		return hosts;
	}

	public boolean isWorking(StorageHost host)
	{
		return !host.needsProcessor()||processor!=null&&processor.compareTo(host.getEra())>=0;
	}

	public Set<ComputerEra> getErasWithoutProcessor()
	{
		Set<ComputerEra> without = EnumSet.noneOf(ComputerEra.class);
		for(StorageHost host : hosts)
			if(!isWorking(host))
				without.add(host.getEra());
		return without;
	}

	public Set<ComputerEra> getEras()
	{
		return eras;
	}

	public Set<ComputerEra> getConflictingEras()
	{
		return conflictingEras;
	}

	public NetworkStorage getStorage()
	{
		List<CellInventory> cells = new ArrayList<>();
		for(StorageHost host : hosts)
			if(isWorking(host))
				cells.addAll(host.getCells());
		return new NetworkStorage(cells);
	}

	public NetworkStorage getStorage(ComputerEra era)
	{
		List<CellInventory> cells = new ArrayList<>();
		for(StorageHost host : hosts)
			if(host.getEra()==era&&isWorking(host))
				cells.addAll(host.getCells());
		return new NetworkStorage(cells);
	}
}

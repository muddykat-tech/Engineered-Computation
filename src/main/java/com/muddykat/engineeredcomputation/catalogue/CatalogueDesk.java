package com.muddykat.engineeredcomputation.catalogue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.pneumatic.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.TubeCarriers;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.storage.CellContents;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.TerminalStats;

public final class CatalogueDesk
{
	public static final int REFRESH_TICKS = 10;

	private final Level level;
	private final BlockPos teletype;
	private final Supplier<BlockPos> chosenOutlet;
	private View view = View.EMPTY;
	private int countdown;

	public record Source(TubeStationBlockEntity station, CellContents tally)
	{
	}

	public record View(@Nullable BlockPos home, @Nullable BlockPos outlet, Map<ItemKey, Long> contents,
			Set<ItemKey> unreachable, List<Source> sources, TerminalStats stats, List<BlockPos> outlets)
	{
		static final View EMPTY = new View(null, null, Map.of(), Set.of(), List.of(), TerminalStats.EMPTY, List.of());
	}

	public CatalogueDesk(Level level, BlockPos teletype, Supplier<BlockPos> chosenOutlet)
	{
		this.level = level;
		this.teletype = teletype.immutable();
		this.chosenOutlet = chosenOutlet;
	}

	public CatalogueDesk(Level level, BlockPos teletype)
	{
		this(level, teletype, () -> null);
	}

	@Nullable
	public static BlockPos home(Level level, BlockPos pos)
	{
		for(Direction direction : Direction.values())
			if(level.getBlockEntity(pos.relative(direction)) instanceof ReceivingStationBlockEntity)
				return pos.relative(direction);
		return null;
	}

	public View view()
	{
		if(countdown--<=0)
		{
			view = look();
			countdown = REFRESH_TICKS;
		}
		return view;
	}

	public List<BlockPos> outlets()
	{
		BlockPos home = home(level, teletype);
		if(home==null)
			return List.of();
		List<BlockPos> others = new ArrayList<>();
		for(BlockPos end : TubeNetwork.of(level, home).getEnds())
			if(!end.equals(home)&&level.getBlockEntity(end) instanceof ReceivingStationBlockEntity)
				others.add(end);
		others.sort(Comparator.comparing((BlockPos pos) -> label(pos).getString()).thenComparing(BlockPos::asLong));
		List<BlockPos> outlets = new ArrayList<>();
		outlets.add(home);
		outlets.addAll(others);
		return outlets;
	}

	public Component label(BlockPos receiver)
	{
		return level.getBlockEntity(receiver) instanceof ReceivingStationBlockEntity station? station.label()
				: new TextComponent("");
	}

	public int request(ItemKey key, int amount)
	{
		View current = look();
		if(current.outlet()==null||amount<=0)
			return 0;
		List<Source> holding = new ArrayList<>();
		for(Source source : current.sources())
			if(amountOf(source.tally(), key)>0)
				holding.add(source);
		holding.sort(Comparator.comparingLong((Source source) -> -amountOf(source.tally(), key)));
		int asked = 0;
		for(Source source : holding)
		{
			int part = (int)Math.min(amount-asked, amountOf(source.tally(), key));
			if(part>0&&source.station().request(key, part, current.outlet()))
				asked += part;
			if(asked>=amount)
				break;
		}
		countdown = 0;
		return asked;
	}

	public ItemStack deposit(ItemStack items)
	{
		BlockPos home = home(level, teletype);
		if(home==null||items.isEmpty()||!(level instanceof ServerLevel serverLevel))
			return items;
		TubeNetwork network = TubeNetwork.of(level, home);
		ItemKey key = ItemKey.of(items);
		List<Source> holding = new ArrayList<>();
		List<Source> taking = new ArrayList<>();
		for(CatalogueCard card : cards(network).values())
		{
			TubeStationBlockEntity station = Catalogue.station(level, network, card);
			if(station==null)
				continue;
			if(amountOf(card.tally(), key)>0)
				holding.add(new Source(station, card.tally()));
			else if(station.takesNewItems())
				taking.add(new Source(station, card.tally()));
		}
		holding.sort(Comparator.comparingLong((Source source) -> -amountOf(source.tally(), key)));
		holding.addAll(taking);
		ItemStack left = items.copy();
		for(Source source : holding)
		{
			List<BlockPos> route = network.route(home, source.station().getBlockPos());
			int room = route.isEmpty()? 0: source.station().room(left);
			while(room>0&&!left.isEmpty())
			{
				ItemStack carried = left.split(Math.min(room, left.getMaxStackSize()));
				TubeCarriers.of(serverLevel).launch(serverLevel, carried, route);
				room -= carried.getCount();
			}
			if(left.isEmpty())
				break;
		}
		countdown = 0;
		return left;
	}

	private View look()
	{
		BlockPos home = home(level, teletype);
		if(home==null)
			return new View(null, null, Map.of(), Set.of(), List.of(),
					TerminalStats.ofCatalogue(0, 0, 0, 0, 0, false, true, false), List.of());
		TubeNetwork network = TubeNetwork.of(level, home);
		if(network.isTooLarge())
			return new View(home, home, Map.of(), Set.of(), List.of(),
					TerminalStats.ofCatalogue(0, 0, 0, 0, 0, true, false, false), List.of(home));
		BlockPos chosen = chosenOutlet.get();
		BlockPos outlet = chosen!=null&&network.getEnds().contains(chosen)
				&&level.getBlockEntity(chosen) instanceof ReceivingStationBlockEntity? chosen: home;

		List<EraDriveState> catalogues = Catalogue.on(level, network);
		int capacity = 0;
		int filed = 0;
		for(EraDriveState catalogue : catalogues)
		{
			capacity += Catalogue.capacity(catalogue);
			filed += Catalogue.cards(catalogue).size();
		}

		Map<ItemKey, Long> contents = new HashMap<>();
		Set<ItemKey> reachable = new HashSet<>();
		List<Source> sources = new ArrayList<>();
		boolean coming = level instanceof ServerLevel serverLevel&&TubeCarriers.of(serverLevel).getCarriers().stream()
				.anyMatch(carrier -> carrier.getDestination().equals(outlet));
		for(CatalogueCard card : cards(network).values())
		{
			for(CellContents.Entry entry : card.tally().entries())
				contents.merge(entry.key(), entry.count(), Long::sum);
			TubeStationBlockEntity station = Catalogue.station(level, network, card);
			if(station==null||network.route(card.station().pos(), outlet).isEmpty())
				continue;
			sources.add(new Source(station, card.tally()));
			card.tally().entries().forEach(entry -> reachable.add(entry.key()));
			coming |= station.hasRequestsFor(outlet);
		}
		Set<ItemKey> unreachable = new HashSet<>(contents.keySet());
		unreachable.removeAll(reachable);
		return new View(home, outlet, contents, unreachable, sources, TerminalStats.ofCatalogue(filed, capacity,
				contents.size(), catalogues.size(), sources.size(), false, false, coming), outlets());
	}

	private Map<GlobalPos, CatalogueCard> cards(TubeNetwork network)
	{
		Map<GlobalPos, CatalogueCard> byStation = new LinkedHashMap<>();
		for(EraDriveState catalogue : Catalogue.on(level, network))
			for(CatalogueCard card : Catalogue.cards(catalogue))
				byStation.merge(card.station(), card, (a, b) -> b.talliedAt()>a.talliedAt()? b: a);
		return byStation;
	}

	private static long amountOf(CellContents tally, ItemKey key)
	{
		for(CellContents.Entry entry : tally.entries())
			if(entry.key().equals(key))
				return entry.count();
		return 0;
	}
}

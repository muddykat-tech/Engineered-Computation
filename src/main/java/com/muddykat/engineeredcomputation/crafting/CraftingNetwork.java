package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

public final class CraftingNetwork
{
	private CraftingNetwork()
	{
	}

	public static List<ProvidedPattern> patterns(Level level, StorageNetwork network)
	{
		List<ProvidedPattern> patterns = new ArrayList<>();
		for(CraftingComputerState computer : network.getCraftingComputers())
			for(Pattern pattern : computer.getPatterns())
				patterns.add(ProvidedPattern.direct(pattern));
		for(DataProcessorState processor : network.getDataProcessors())
			patterns.addAll(processor.provided());
		return patterns;
	}

	public static Set<ItemKey> craftables(Level level, StorageNetwork network)
	{
		Set<ItemKey> craftables = new LinkedHashSet<>();
		if(network.getCraftingComputers().isEmpty())
			return craftables;
		for(ProvidedPattern provided : patterns(level, network))
			if(!provided.pattern().hasTags())
				craftables.addAll(provided.pattern().outputTotals().keySet());
		return craftables;
	}

	public static List<PatternOffer> offers(Level level, StorageNetwork network)
	{
		List<PatternOffer> offers = new ArrayList<>();
		if(network.getCraftingComputers().isEmpty())
			return offers;
		for(ProvidedPattern provided : patterns(level, network))
			if(provided.pattern().hasTags()&&provided.processor()!=null)
				offers.add(new PatternOffer(provided.processor(), provided.slot(), provided.pattern()));
		return offers;
	}

	public static CraftingPlan plan(Level level, StorageNetwork network, NetworkStorage storage, ItemKey target,
			long amount)
	{
		return CraftingPlanner.plan(target, amount, stock(storage), patterns(level, network));
	}

	public static Optional<CraftingPlan> planOffer(Level level, StorageNetwork network, NetworkStorage storage,
			PatternOffer.Choice choice, long amount)
	{
		PatternOffer offer = choice.offer();
		DataProcessorState processor = DataProcessorState.at(level, offer.processor());
		if(processor==null||!network.getDataProcessors().contains(processor)
				||!offer.pattern().equals(processor.getPatterns().get(offer.slot())))
			return Optional.empty();
		ProvidedPattern provided = ProvidedPattern.abstractAt(offer.pattern(), offer.processor(), offer.slot())
				.choosing(choice.picks());
		return Optional.of(CraftingPlanner.planOffer(provided, amount, stock(storage), patterns(level, network)));
	}

	private static Map<ItemKey, Long> stock(NetworkStorage storage)
	{
		Map<ItemKey, Long> stock = new HashMap<>();
		storage.collect(stock);
		return stock;
	}

	public static Optional<StatusCode> order(Level level, StorageNetwork network, NetworkStorage storage, ItemKey target,
			long amount)
	{
		return place(network, storage, () -> plan(level, network, storage, target, amount));
	}

	public static Optional<StatusCode> orderOffer(Level level, StorageNetwork network, NetworkStorage storage,
			PatternOffer.Choice choice, long amount)
	{
		return place(network, storage, () -> planOffer(level, network, storage, choice, amount).orElse(null));
	}

	private static Optional<StatusCode> place(StorageNetwork network, NetworkStorage storage,
			Supplier<CraftingPlan> planner)
	{
		if(network.getCraftingComputers().isEmpty())
			return Optional.of(StatusCode.FAILED_DEPENDENCY);
		CraftingComputerState computer = network.getCraftingComputers().stream().filter(CraftingComputerState::hasRoom)
				.min(Comparator.comparingInt(c -> c.getOrders().size())).orElse(null);
		if(computer==null)
			return Optional.of(StatusCode.INSUFFICIENT_STORAGE);
		CraftingPlan plan = planner.get();
		if(plan==null||!plan.isPossible())
			return Optional.of(StatusCode.NOT_FOUND);
		return computer.start(plan, storage)? Optional.empty(): Optional.of(StatusCode.NOT_FOUND);
	}

	public static boolean isProcessing(StorageNetwork network)
	{
		return network.getCraftingComputers().stream().anyMatch(computer -> !computer.getOrders().isEmpty());
	}

	public static List<OrderSummary> orders(StorageNetwork network)
	{
		List<OrderSummary> orders = new ArrayList<>();
		for(CraftingComputerState computer : network.getCraftingComputers())
			if(computer.getMasterPos()!=null)
				for(CraftingOrder order : computer.getOrders())
					orders.add(OrderSummary.of(computer.getMasterPos(), order));
		return orders;
	}

	public static boolean cancel(StorageNetwork network, BlockPos computerPos, int id)
	{
		for(CraftingComputerState computer : network.getCraftingComputers())
			if(computerPos.equals(computer.getMasterPos()))
				return computer.cancel(id);
		return false;
	}
}

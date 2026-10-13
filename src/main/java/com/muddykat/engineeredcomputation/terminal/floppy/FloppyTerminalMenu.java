package com.muddykat.engineeredcomputation.terminal.floppy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.crafting.CraftingNetwork;
import com.muddykat.engineeredcomputation.crafting.CraftingPlan;
import com.muddykat.engineeredcomputation.crafting.computer.CraftingComputerState;
import com.muddykat.engineeredcomputation.crafting.order.OrderPlanPayload;
import com.muddykat.engineeredcomputation.crafting.order.OrderSummary;
import com.muddykat.engineeredcomputation.crafting.order.TerminalCraftingPayload;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternOffer;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;
import com.muddykat.engineeredcomputation.terminal.TerminalFeatures;
import com.muddykat.engineeredcomputation.terminal.crafting.CraftingGrid;
import com.muddykat.engineeredcomputation.terminal.crafting.VideoTerminalMenu;

public class FloppyTerminalMenu extends VideoTerminalMenu
{
	private static final int CRAFTING_INTERVAL = 10;

	private List<ItemKey> sentCraftables = List.of();
	private List<PatternOffer> sentOffers = List.of();
	private List<OrderSummary> sentOrders = List.of();
	private int craftingCountdown;

	private Set<ItemKey> clientCraftables = Set.of();
	private List<PatternOffer> clientOffers = List.of();
	private List<OrderSummary> clientOrders = List.of();
	@Nullable
	private OrderPlanPayload clientPlan;

	public FloppyTerminalMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf)
	{
		this(id, playerInventory, buf.readBlockPos(), null);
	}

	public FloppyTerminalMenu(int id, Inventory playerInventory, BlockPos terminalPos, @Nullable CraftingGrid grid)
	{
		super(ECMenuTypes.FLOPPY_TERMINAL.get(), id, playerInventory, terminalPos, grid);
	}

	@Override
	public TerminalFeatures getFeatures()
	{
		return TerminalFeatures.ORDERING;
	}

	@Override
	protected Snapshot snapshot(ServerPlayer serverPlayer)
	{
		Snapshot snapshot = super.snapshot(serverPlayer);
		sendCrafting(serverPlayer);
		return snapshot;
	}

	private void sendCrafting(ServerPlayer serverPlayer)
	{
		if(craftingCountdown-->0)
			return;
		craftingCountdown = CRAFTING_INTERVAL;
		List<ItemKey> craftables = List.copyOf(CraftingNetwork.craftables(player.level(), network));
		List<PatternOffer> offers = CraftingNetwork.offers(player.level(), network);
		List<OrderSummary> orders = CraftingNetwork.orders(network);
		if(craftables.equals(sentCraftables)&&offers.equals(sentOffers)&&orders.equals(sentOrders))
			return;
		sentCraftables = craftables;
		sentOffers = offers;
		sentOrders = orders;
		PacketDistributor.sendToPlayer(serverPlayer, new TerminalCraftingPayload(containerId,
				craftables.subList(0, Math.min(craftables.size(), TerminalCraftingPayload.MAX)),
				offers.subList(0, Math.min(offers.size(), TerminalCraftingPayload.MAX)),
				orders.subList(0, Math.min(orders.size(), TerminalCraftingPayload.MAX))));
	}

	public void handleOrderRequest(ItemKey target, long amount, boolean place, Optional<PatternOffer.Choice> offer)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return;
		target = ItemKey.resolve(ItemKey.resolve(target, sentKeys(), player.registryAccess()), sentCraftables,
				player.registryAccess());
		NetworkStorage storage = storage();
		Level level = player.level();
		CraftingPlan plan = offer.isPresent()? CraftingNetwork.planOffer(level, network, storage, offer.get(), amount)
				.orElse(null): CraftingNetwork.plan(level, network, storage, target, amount);
		if(plan==null)
		{
			PacketDistributor.sendToPlayer(serverPlayer, new OrderPlanPayload(containerId, target, amount, List.of(),
					StatusCode.NOT_FOUND, false));
			return;
		}
		StatusCode status;
		if(network.getCraftingComputers().isEmpty())
			status = StatusCode.FAILED_DEPENDENCY;
		else if(network.getCraftingComputers().stream().noneMatch(CraftingComputerState::hasRoom))
			status = StatusCode.INSUFFICIENT_STORAGE;
		else
			status = plan.isPossible()? StatusCode.OK: StatusCode.NOT_FOUND;
		boolean placed = place&&status==StatusCode.OK&&(offer.isPresent()
				? CraftingNetwork.orderOffer(level, network, storage, offer.get(), amount)
				: CraftingNetwork.order(level, network, storage, target, amount)).isEmpty();
		if(placed)
			craftingCountdown = 0;
		PacketDistributor.sendToPlayer(serverPlayer, new OrderPlanPayload(containerId, plan.target(), plan.amount(),
				planLines(plan), status, placed));
	}

	private static List<OrderPlanPayload.Line> planLines(CraftingPlan plan)
	{
		Set<ItemKey> items = new LinkedHashSet<>();
		items.add(plan.target());
		items.addAll(plan.missing().keySet());
		items.addAll(plan.toCraft().keySet());
		items.addAll(plan.fromStorage().keySet());
		List<OrderPlanPayload.Line> lines = new ArrayList<>();
		for(ItemKey item : items)
		{
			if(lines.size()>=OrderPlanPayload.MAX_LINES)
				break;
			lines.add(new OrderPlanPayload.Line(item, plan.fromStorage().getOrDefault(item, 0L),
					plan.toCraft().getOrDefault(item, 0L), plan.missing().getOrDefault(item, 0L)));
		}
		return lines;
	}

	public void cancelOrder(BlockPos computer, int id)
	{
		storage();
		if(CraftingNetwork.cancel(network, computer, id))
			craftingCountdown = 0;
	}

	public void applyCrafting(TerminalCraftingPayload payload)
	{
		clientCraftables = new LinkedHashSet<>(payload.craftables());
		clientOffers = payload.offers();
		clientOrders = payload.orders();
		clientChanged();
	}

	public void applyPlan(OrderPlanPayload payload)
	{
		clientPlan = payload;
	}

	public Set<ItemKey> getClientCraftables()
	{
		return Collections.unmodifiableSet(clientCraftables);
	}

	public List<PatternOffer> getClientOffers()
	{
		return clientOffers;
	}

	public List<OrderSummary> getClientOrders()
	{
		return clientOrders;
	}

	@Nullable
	public OrderPlanPayload getClientPlan()
	{
		return clientPlan;
	}

	public void clearClientPlan()
	{
		clientPlan = null;
	}
}

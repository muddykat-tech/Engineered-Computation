package com.muddykat.engineeredcomputation.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;

import com.muddykat.engineeredcomputation.catalogue.CatalogueDesk;
import com.muddykat.engineeredcomputation.catalogue.RequestSlip;
import com.muddykat.engineeredcomputation.crafting.CraftingComputerState;
import com.muddykat.engineeredcomputation.crafting.CraftingNetwork;
import com.muddykat.engineeredcomputation.crafting.CraftingPlan;
import com.muddykat.engineeredcomputation.crafting.OrderPlanPayload;
import com.muddykat.engineeredcomputation.crafting.OrderSummary;
import com.muddykat.engineeredcomputation.crafting.PatternOffer;
import com.muddykat.engineeredcomputation.crafting.TerminalCraftingPayload;
import com.muddykat.engineeredcomputation.network.ECPayloads;
import com.muddykat.engineeredcomputation.network.NetworkMemberBlock;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.storage.CellContents;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

public class TerminalMenu extends AbstractContainerMenu
{
	public static final int GRID_X = 7;
	public static final int COLUMNS = 9;
	public static final int CRAFT_X = 29;
	public static final int RESULT_X = 119;
	public static final int LINK_X = 158;
	public static final int CLEAR_X = CRAFT_X-14;

	public record Layout(int gridY, int rows, int craftingY, int inventoryY, int hotbarY, int height)
	{
		public static Layout of(TerminalFeatures features)
		{
			int gridY = features.search()? 33: 17;
			int rows = features.crafting()? 3: features.requests()? 5: 6;
			int gridBottom = gridY+rows*18;
			int craftingY = gridBottom+13;
			int inventoryY = features.crafting()? craftingY+3*18+14: features.requests()? craftingY+18+14
					: gridBottom+22;
			return new Layout(gridY, rows, craftingY, inventoryY, inventoryY+58, inventoryY+58+25);
		}
	}

	private static final int INVENTORY_SLOTS = 36;
	public static final int RESULT_SLOT = INVENTORY_SLOTS;
	public static final int CRAFT_GRID_FIRST = RESULT_SLOT+1;
	public static final int TOGGLE_LINKED = 0;
	public static final int CLEAR_GRID = 1;
	public static final int SEND_SLIP = 2;
	public static final int NEXT_OUTLET = 3;
	public static final int PREVIOUS_OUTLET = 4;
	public static final int SLIP_REMOVE = 16;
	public static final int SLIP_LOWER = SLIP_REMOVE+RequestSlip.SIZE;

	private static final int ENTRIES_PER_PAYLOAD = 256;

	private final BlockPos terminalPos;
	private final Player player;
	private final TerminalFeatures features;
	private final Layout layout;
	@Nullable
	private final CraftingGrid grid;
	@Nullable
	private final GridContainer craftGrid;
	private final ResultContainer craftResult = new ResultContainer();
	private int resultVersion = -1;
	private int quickCraftBudget;

	@Nullable
	private StorageNetwork network;
	@Nullable
	private final CatalogueDesk desk;
	@Nullable
	private final TeletypeBlockEntity teletype;
	private Set<ItemKey> sentUnreachable = Set.of();
	@Nullable
	private TeletypeSlipPayload sentSlip;
	private Map<ItemKey, Long> sentContents = Map.of();
	private TerminalStats sentStats = TerminalStats.EMPTY;
	private boolean sentInitial;
	private List<ItemKey> sentCraftables = List.of();
	private List<PatternOffer> sentOffers = List.of();
	private List<OrderSummary> sentOrders = List.of();
	private int craftingCountdown;
	private static final int CRAFTING_INTERVAL = 10;

	private final Map<ItemKey, Long> clientContents = new HashMap<>();
	private Set<ItemKey> clientCraftables = Set.of();
	private List<PatternOffer> clientOffers = List.of();
	private List<OrderSummary> clientOrders = List.of();
	@Nullable
	private OrderPlanPayload clientPlan;
	private TerminalStats clientStats = TerminalStats.EMPTY;
	private Set<ItemKey> clientUnreachable = Set.of();
	private List<RequestSlip.Line> clientSlip = List.of();
	private Component clientOutlet = new TextComponent("");
	private boolean clientOutletHome = true;
	private List<TeletypeSlipPayload.Outlet> clientOutlets = List.of();
	private int clientVersion;

	public TerminalMenu(int id, Inventory playerInventory, FriendlyByteBuf buf)
	{
		this(id, playerInventory, buf.readBlockPos(),
				new TerminalFeatures(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
	}

	public TerminalMenu(int id, Inventory playerInventory, BlockPos terminalPos)
	{
		this(id, playerInventory, terminalPos, TerminalFeatures.SEARCH);
	}

	public TerminalMenu(int id, Inventory playerInventory, BlockPos terminalPos, TerminalFeatures features)
	{
		this(id, playerInventory, terminalPos, features, null);
	}

	public TerminalMenu(int id, Inventory playerInventory, BlockPos terminalPos, TerminalFeatures features,
			@Nullable CraftingGrid grid)
	{
		super(ECMenuTypes.TERMINAL.get(), id);
		this.terminalPos = terminalPos;
		this.player = playerInventory.player;
		this.features = features;
		this.layout = Layout.of(features);
		this.teletype = features.requests()&&!player.level.isClientSide
				&&player.level.getBlockEntity(terminalPos) instanceof TeletypeBlockEntity entity? entity: null;
		this.desk = features.requests()&&!player.level.isClientSide? new CatalogueDesk(player.level, terminalPos,
				() -> teletype==null? null: teletype.getOutlet()): null;

		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(new Slot(playerInventory, col+row*9+9, GRID_X+1+col*18, layout.inventoryY()+1+row*18));
		for(int col = 0; col<9; col++)
			addSlot(new Slot(playerInventory, col, GRID_X+1+col*18, layout.hotbarY()+1));

		if(features.crafting())
		{
			this.grid = grid!=null? grid: CraftingGrid.detached();
			craftGrid = new GridContainer();
			addSlot(new CraftResultSlot(RESULT_X+5, layout.craftingY()+18+1));
			for(int row = 0; row<3; row++)
				for(int col = 0; col<3; col++)
					addSlot(new GridSlot(col+row*3, CRAFT_X+1+col*18, layout.craftingY()+1+row*18));
			addDataSlot(new DataSlot()
			{
				@Override
				public int get()
				{
					return TerminalMenu.this.grid.isLinked()? 1: 0;
				}

				@Override
				public void set(int value)
				{
					TerminalMenu.this.grid.setLinked(value!=0);
				}
			});
		}
		else
		{
			this.grid = null;
			craftGrid = null;
		}
	}

	public static void open(ServerPlayer player, BlockPos terminalPos, Component title, TerminalFeatures features,
			@Nullable CraftingGrid grid)
	{
		NetworkHooks.openGui(player, new SimpleMenuProvider(
						(id, inventory, p) -> new TerminalMenu(id, inventory, terminalPos, features, grid), title),
				buf ->
				{
					buf.writeBlockPos(terminalPos);
					buf.writeBoolean(features.search());
					buf.writeBoolean(features.crafting());
					buf.writeBoolean(features.orders());
					buf.writeBoolean(features.requests());
				});
	}

	public TerminalFeatures getFeatures()
	{
		return features;
	}

	public Layout getLayout()
	{
		return layout;
	}

	public boolean isLinked()
	{
		return grid!=null&&grid.isLinked();
	}

	public boolean isGridSlot(Slot slot)
	{
		return craftGrid!=null&&slot.container==craftGrid;
	}

	public boolean hasLinkedIngredients()
	{
		Map<ItemKey, Long> needed = new HashMap<>();
		for(ItemStack link : grid.slots())
			if(!link.isEmpty())
				needed.merge(ItemKey.of(link), 1L, Long::sum);
		NetworkStorage storage = player.level.isClientSide? null: storage();
		for(Map.Entry<ItemKey, Long> entry : needed.entrySet())
		{
			long stored = storage==null? clientContents.getOrDefault(entry.getKey(), 0L): storage.getAmount(entry.getKey());
			if(stored<entry.getValue())
				return false;
		}
		return true;
	}

	public long storedOnClient(ItemStack link)
	{
		return clientContents.getOrDefault(ItemKey.of(link), 0L);
	}

	private NetworkStorage storage()
	{
		network = StorageNetwork.of(player.level, terminalPos);
		return network.isTooLarge()||!network.getConflictingEras().isEmpty()? NetworkStorage.EMPTY: network.getStorage();
	}

	@Override
	public void broadcastChanges()
	{
		if(grid!=null&&grid.version()!=resultVersion)
			updateCraftResult();
		super.broadcastChanges();
		if(player instanceof ServerPlayer serverPlayer)
			sendContentChanges(serverPlayer);
	}

	private void sendContentChanges(ServerPlayer serverPlayer)
	{
		Map<ItemKey, Long> contents;
		TerminalStats stats;
		if(desk!=null)
		{
			CatalogueDesk.View view = desk.view();
			contents = view.contents();
			stats = view.stats();
			if(!view.unreachable().equals(sentUnreachable))
			{
				sentUnreachable = view.unreachable();
				ECPayloads.sendToPlayer(serverPlayer,
						new TerminalUnreachablePayload(containerId, List.copyOf(sentUnreachable)));
			}
			List<TeletypeSlipPayload.Outlet> outlets = view.outlets().stream().map(pos -> new TeletypeSlipPayload.Outlet(
					pos, desk.label(pos), pos.equals(view.outlet()))).toList();
			TeletypeSlipPayload slip = new TeletypeSlipPayload(containerId,
					teletype==null? List.of(): List.copyOf(teletype.getSlip().lines()),
					view.outlet()==null? new TextComponent(""): desk.label(view.outlet()),
					view.outlet()==null||view.outlet().equals(view.home()), outlets);
			if(!slip.equals(sentSlip))
			{
				sentSlip = slip;
				ECPayloads.sendToPlayer(serverPlayer, slip);
			}
		}
		else
		{
			NetworkStorage storage = storage();
			contents = new HashMap<>();
			storage.collect(contents);
			stats = TerminalStats.of(storage, network.getConflictingEras(), network.isTooLarge(),
					CraftingNetwork.isProcessing(network), network.getErasWithoutProcessor());
		}
		if(features.orders())
			sendCrafting(serverPlayer);

		List<CellContents.Entry> changes = new ArrayList<>();
		for(Map.Entry<ItemKey, Long> entry : contents.entrySet())
			if(!Objects.equals(sentContents.get(entry.getKey()), entry.getValue()))
				changes.add(new CellContents.Entry(entry.getKey(), entry.getValue()));
		for(ItemKey key : sentContents.keySet())
			if(!contents.containsKey(key))
				changes.add(new CellContents.Entry(key, 0));

		if(sentInitial&&changes.isEmpty()&&stats.equals(sentStats))
			return;
		boolean reset = !sentInitial;
		for(int start = 0; start<Math.max(1, changes.size()); start += ENTRIES_PER_PAYLOAD)
		{
			List<CellContents.Entry> batch = changes.subList(start, Math.min(changes.size(), start+ENTRIES_PER_PAYLOAD));
			ECPayloads.sendToPlayer(serverPlayer,
					new TerminalContentsPayload(containerId, reset&&start==0, batch, stats));
		}
		sentContents = contents;
		sentStats = stats;
		sentInitial = true;
	}

	private void sendCrafting(ServerPlayer serverPlayer)
	{
		if(craftingCountdown-->0)
			return;
		craftingCountdown = CRAFTING_INTERVAL;
		List<ItemKey> craftables = List.copyOf(CraftingNetwork.craftables(player.level, network));
		List<PatternOffer> offers = CraftingNetwork.offers(player.level, network);
		List<OrderSummary> orders = CraftingNetwork.orders(network);
		if(craftables.equals(sentCraftables)&&offers.equals(sentOffers)&&orders.equals(sentOrders))
			return;
		sentCraftables = craftables;
		sentOffers = offers;
		sentOrders = orders;
		ECPayloads.sendToPlayer(serverPlayer, new TerminalCraftingPayload(containerId,
				craftables.subList(0, Math.min(craftables.size(), TerminalCraftingPayload.MAX)),
				offers.subList(0, Math.min(offers.size(), TerminalCraftingPayload.MAX)),
				orders.subList(0, Math.min(orders.size(), TerminalCraftingPayload.MAX))));
	}

	public void handleOrderRequest(ItemKey target, long amount, boolean place, Optional<PatternOffer.Choice> offer)
	{
		if(!features.orders()||!(player instanceof ServerPlayer serverPlayer))
			return;
		NetworkStorage storage = storage();
		Level level = player.level;
		CraftingPlan plan = offer.isPresent()? CraftingNetwork.planOffer(level, network, storage, offer.get(), amount)
				.orElse(null): CraftingNetwork.plan(level, network, storage, target, amount);
		if(plan==null)
		{
			ECPayloads.sendToPlayer(serverPlayer, new OrderPlanPayload(containerId, target, amount, List.of(),
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
		ECPayloads.sendToPlayer(serverPlayer, new OrderPlanPayload(containerId, plan.target(), plan.amount(),
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
		if(!features.orders())
			return;
		storage();
		if(CraftingNetwork.cancel(network, computer, id))
			craftingCountdown = 0;
	}

	void handleClick(@Nullable ItemKey key, TerminalAction action)
	{
		if(desk!=null)
		{
			request(key, action);
			return;
		}
		NetworkStorage storage = storage();
		ItemStack carried = getCarried();
		switch(action)
		{
			case INSERT_ALL, INSERT_ONE ->
			{
				if(carried.isEmpty())
					return;
				long amount = action==TerminalAction.INSERT_ALL? carried.getCount(): 1;
				carried.shrink((int)storage.insert(ItemKey.of(carried), amount, false));
				setCarried(carried);
			}
			case TAKE_STACK, TAKE_HALF ->
			{
				if(!carried.isEmpty()||key==null)
					return;
				int stack = (int)Math.min(key.getMaxStackSize(), storage.getAmount(key));
				int amount = action==TerminalAction.TAKE_STACK? stack: (stack+1)/2;
				int taken = (int)storage.extract(key, amount, false);
				if(taken>0)
					setCarried(key.toStack(taken));
			}
			case QUICK_MOVE ->
			{
				if(key==null)
					return;
				int amount = (int)Math.min(key.getMaxStackSize(), storage.getAmount(key));
				ItemStack moved = key.toStack((int)storage.extract(key, amount, false));
				player.getInventory().add(moved);
				if(!moved.isEmpty())
				{
					moved.shrink((int)storage.insert(key, moved.getCount(), false));
					if(!moved.isEmpty())
						player.drop(moved, false);
				}
			}
		}
		broadcastChanges();
	}

	private void request(@Nullable ItemKey key, TerminalAction action)
	{
		if(action==TerminalAction.INSERT_ALL||action==TerminalAction.INSERT_ONE)
		{
			ItemStack carried = getCarried();
			if(carried.isEmpty())
				return;
			int amount = action==TerminalAction.INSERT_ALL? carried.getCount(): 1;
			ItemStack left = desk.deposit(carried.split(amount));
			carried.grow(left.getCount());
			setCarried(carried.isEmpty()&&!left.isEmpty()? left: carried);
		}
		else if(key!=null&&teletype!=null)
		{
			int tallied = (int)Math.min(Integer.MAX_VALUE, desk.view().contents().getOrDefault(key, 0L));
			teletype.getSlip().add(key, action==TerminalAction.TAKE_HALF? 1: key.getMaxStackSize(), tallied);
		}
		broadcastChanges();
	}

	private boolean clickTeletypeButton(Player player, int id)
	{
		if(id<SEND_SLIP||id>PREVIOUS_OUTLET&&(id<SLIP_REMOVE||id>=SLIP_LOWER+RequestSlip.SIZE))
			return false;
		if(player.level.isClientSide||teletype==null)
			return true;
		if(id==SEND_SLIP)
			sendSlip();
		else if(id==NEXT_OUTLET||id==PREVIOUS_OUTLET)
			cycleOutlet(id==NEXT_OUTLET? 1: -1);
		else if(id<SLIP_LOWER)
			teletype.getSlip().remove(id-SLIP_REMOVE);
		else
			teletype.getSlip().lower(id-SLIP_LOWER, 1);
		return true;
	}

	private void sendSlip()
	{
		if(teletype==null||teletype.getSlip().isEmpty())
			return;
		List<RequestSlip.Line> left = new ArrayList<>();
		for(RequestSlip.Line line : teletype.getSlip().lines())
		{
			int asked = desk.request(line.key(), line.amount());
			if(asked<line.amount())
				left.add(new RequestSlip.Line(line.key(), line.amount()-asked));
		}
		teletype.getSlip().set(left);
		player.level.playSound(null, terminalPos, SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.BLOCKS, 0.7F, 1.3F);
	}

	public void chooseOutlet(BlockPos pos)
	{
		if(teletype==null)
			return;
		List<BlockPos> outlets = desk.outlets();
		if(!outlets.contains(pos))
			return;
		teletype.setOutlet(pos.equals(outlets.get(0))? null: pos);
		desk.view();
	}

	private void cycleOutlet(int step)
	{
		if(teletype==null)
			return;
		List<BlockPos> outlets = desk.outlets();
		if(outlets.isEmpty())
			return;
		BlockPos current = desk.view().outlet();
		int index = Math.max(0, outlets.indexOf(current));
		BlockPos next = outlets.get(Math.floorMod(index+step, outlets.size()));
		teletype.setOutlet(next.equals(outlets.get(0))? null: next);
		desk.view();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(player.level.isClientSide||!slot.hasItem())
			return ItemStack.EMPTY;
		if(desk!=null)
		{
			if(index<INVENTORY_SLOTS)
			{
				slot.set(desk.deposit(slot.getItem()));
				broadcastChanges();
			}
			return ItemStack.EMPTY;
		}
		if(craftGrid!=null&&index==RESULT_SLOT)
			return quickCraft(player, slot);
		ItemStack stack = slot.getItem();
		stack.shrink((int)storage().insert(ItemKey.of(stack), stack.getCount(), false));
		if(index>=CRAFT_GRID_FIRST)
		{
			moveItemStackTo(stack, 0, INVENTORY_SLOTS, false);
			slot.set(stack.isEmpty()? ItemStack.EMPTY: stack);
		}
		else
			slot.setChanged();
		return ItemStack.EMPTY;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player)
	{
		if(isLinked()&&slotId>=CRAFT_GRID_FIRST&&slotId<CRAFT_GRID_FIRST+CraftingGrid.SIZE
				&&(clickType==ClickType.PICKUP||clickType==ClickType.QUICK_MOVE))
		{
			if(!player.level.isClientSide)
				clickLinked(slotId-CRAFT_GRID_FIRST, button, clickType);
			return;
		}
		if(craftGrid!=null&&slotId==RESULT_SLOT&&clickType==ClickType.QUICK_MOVE)
			quickCraftBudget = craftResult.getItem(0).getMaxStackSize();
		super.clicked(slotId, button, clickType, player);
		quickCraftBudget = 0;
	}

	private ItemStack quickCraft(Player player, Slot slot)
	{
		ItemStack result = slot.getItem();
		if(result.getCount()>quickCraftBudget||!slot.mayPickup(player))
			return ItemStack.EMPTY;
		quickCraftBudget -= result.getCount();
		ItemStack crafted = result.copy();
		result.getItem().onCraftedBy(result, player.level, player);
		if(!moveItemStackTo(result, 0, INVENTORY_SLOTS, true))
			return ItemStack.EMPTY;
		slot.onQuickCraft(result, crafted);
		if(result.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		if(result.getCount()==crafted.getCount())
			return ItemStack.EMPTY;
		slot.onTake(player, result);
		player.drop(result, false);
		return crafted;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot)
	{
		return slot.container!=craftResult&&!(isLinked()&&isGridSlot(slot))&&super.canTakeItemForPickAll(stack, slot);
	}

	@Override
	public boolean clickMenuButton(Player player, int id)
	{
		if(features.requests())
			return clickTeletypeButton(player, id);
		if(craftGrid==null)
			return false;
		switch(id)
		{
			case TOGGLE_LINKED ->
			{
				if(!player.level.isClientSide)
				{
					if(isLinked())
						unlink();
					else
						link(player);
				}
			}
			case CLEAR_GRID ->
			{
				if(!player.level.isClientSide)
				{
					if(isLinked())
						grid.slots().replaceAll(link -> ItemStack.EMPTY);
					else
						emptyCraftGrid(player);
					grid.changed();
					updateCraftResult();
				}
			}
			default ->
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public void slotsChanged(Container container)
	{
		if(container==craftGrid)
			updateCraftResult();
		super.slotsChanged(container);
	}

	private void updateCraftResult()
	{
		if(!(player.level instanceof ServerLevel level)||!(player instanceof ServerPlayer serverPlayer))
			return;
		resultVersion = grid.version();
		CraftingContainer input = craftGrid;
		ItemStack result = ItemStack.EMPTY;
		Optional<CraftingRecipe> recipe =
				level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
		if(recipe.isPresent()&&craftResult.setRecipeUsed(level, serverPlayer, recipe.get()))
		{
			result = recipe.get().assemble(input);
		}
		craftResult.setItem(0, result);
		setRemoteSlot(RESULT_SLOT, result);
		serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT,
				result));
	}

	private void clickLinked(int slot, int button, ClickType clickType)
	{
		ItemStack carried = getCarried();
		craftGrid.setItem(slot, clickType==ClickType.PICKUP&&!carried.isEmpty()
				? ItemHandlerHelper.copyStackWithSize(carried, 1): ItemStack.EMPTY);
	}

	private void link(Player player)
	{
		NetworkStorage storage = storage();
		for(int i = 0; i<CraftingGrid.SIZE; i++)
		{
			ItemStack stack = grid.getItem(i);
			if(stack.isEmpty())
				continue;
			grid.slots().set(i, ItemHandlerHelper.copyStackWithSize(stack, 1));
			stack.shrink((int)storage.insert(ItemKey.of(stack), stack.getCount(), false));
			giveBack(player, stack);
		}
		grid.setLinked(true);
		updateCraftResult();
	}

	private void unlink()
	{
		NetworkStorage storage = storage();
		for(int i = 0; i<CraftingGrid.SIZE; i++)
		{
			ItemStack link = grid.getItem(i);
			if(!link.isEmpty())
				grid.slots().set(i, storage.extract(ItemKey.of(link), 1, false)>0
						? ItemHandlerHelper.copyStackWithSize(link, 1): ItemStack.EMPTY);
		}
		grid.setLinked(false);
		updateCraftResult();
	}

	private void takeLinkedIngredients(Player taker)
	{
		NetworkStorage storage = storage();
		CraftingContainer input = craftGrid;
		ForgeHooks.setCraftingPlayer(taker);
		NonNullList<ItemStack> leftovers = taker.level.getRecipeManager()
				.getRemainingItemsFor(RecipeType.CRAFTING, input, taker.level);
		ForgeHooks.setCraftingPlayer(null);
		for(ItemStack link : grid.slots())
			if(!link.isEmpty())
				storage.extract(ItemKey.of(link), 1, false);
		for(ItemStack leftover : leftovers)
			if(!leftover.isEmpty())
			{
				leftover.shrink((int)storage.insert(ItemKey.of(leftover), leftover.getCount(), false));
				giveBack(taker, leftover);
			}
	}

	private void emptyCraftGrid(Player player)
	{
		NetworkStorage storage = storage();
		for(int i = 0; i<CraftingGrid.SIZE; i++)
		{
			ItemStack stack = grid.slots().set(i, ItemStack.EMPTY);
			if(stack.isEmpty())
				continue;
			stack.shrink((int)storage.insert(ItemKey.of(stack), stack.getCount(), false));
			giveBack(player, stack);
		}
	}

	private static void giveBack(Player player, ItemStack stack)
	{
		if(stack.isEmpty())
			return;
		if(!player.isAlive()||player instanceof ServerPlayer serverPlayer&&serverPlayer.hasDisconnected())
			player.drop(stack, false);
		else
			player.getInventory().placeItemBackInInventory(stack);
	}

	private final class GridSlot extends Slot
	{
		GridSlot(int index, int x, int y)
		{
			super(craftGrid, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack)
		{
			return !isLinked();
		}

		@Override
		public boolean mayPickup(Player player)
		{
			return !isLinked();
		}
	}

	private final class CraftResultSlot extends ResultSlot
	{
		CraftResultSlot(int x, int y)
		{
			super(player, craftGrid, craftResult, 0, x, y);
		}

		@Override
		public boolean mayPickup(Player player)
		{
			return !isLinked()||hasLinkedIngredients();
		}

		@Override
		public void onTake(Player taker, ItemStack stack)
		{
			if(!isLinked())
			{
				super.onTake(taker, stack);
				return;
			}
			checkTakeAchievements(stack);
			if(taker.level.isClientSide)
			{
				craftResult.setItem(0, assemble(taker.level));
				return;
			}
			takeLinkedIngredients(taker);
			updateCraftResult();
		}
	}

	private ItemStack assemble(Level level)
	{
		CraftingContainer input = craftGrid;
		return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
				.map(recipe -> recipe.assemble(input))
				.orElse(ItemStack.EMPTY);
	}

	private final class GridContainer extends CraftingContainer
	{
		GridContainer()
		{
			super(TerminalMenu.this, CraftingGrid.WIDTH, CraftingGrid.WIDTH);
		}

		@Override
		public int getWidth()
		{
			return CraftingGrid.WIDTH;
		}

		@Override
		public int getHeight()
		{
			return CraftingGrid.WIDTH;
		}

		@Override
		public int getContainerSize()
		{
			return CraftingGrid.SIZE;
		}

		@Override
		public boolean isEmpty()
		{
			return grid.slots().stream().allMatch(ItemStack::isEmpty);
		}

		@Override
		public ItemStack getItem(int slot)
		{
			return grid.getItem(slot);
		}

		@Override
		public ItemStack removeItem(int slot, int amount)
		{
			ItemStack removed = ContainerHelper.removeItem(grid.slots(), slot, amount);
			if(!removed.isEmpty())
				changed();
			return removed;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot)
		{
			return ContainerHelper.takeItem(grid.slots(), slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack)
		{
			grid.slots().set(slot, stack);
			changed();
		}

		@Override
		public void setChanged()
		{
			grid.changed();
		}

		private void changed()
		{
			grid.changed();
			slotsChanged(this);
		}

		@Override
		public boolean stillValid(Player player)
		{
			return true;
		}

		@Override
		public void clearContent()
		{
			grid.slots().replaceAll(stack -> ItemStack.EMPTY);
			grid.changed();
		}

		@Override
		public void fillStackedContents(StackedContents contents)
		{
			for(ItemStack stack : grid.slots())
				contents.accountSimpleStack(stack);
		}
	}

	@Override
	public boolean stillValid(Player player)
	{
		boolean there = features.requests()?
				player.level.getBlockState(terminalPos).getBlock() instanceof EraTerminalBlock
				: NetworkMemberBlock.at(player.level, terminalPos, null)!=null;
		return there
				&&player.getEyePosition().distanceToSqr(Vec3.atCenterOf(terminalPos))
				<Mth.square(player.getReachDistance()+4.0);
	}

	void applyUpdate(TerminalContentsPayload payload)
	{
		if(payload.reset())
			clientContents.clear();
		for(CellContents.Entry entry : payload.entries())
			if(entry.count()>0)
				clientContents.put(entry.key(), entry.count());
			else
				clientContents.remove(entry.key());
		clientStats = payload.stats();
		clientVersion++;
	}

	void applyUnreachable(TerminalUnreachablePayload payload)
	{
		clientUnreachable = Set.copyOf(payload.unreachable());
		clientVersion++;
	}

	public boolean isUnreachableOnClient(ItemKey key)
	{
		return clientUnreachable.contains(key);
	}

	void applySlip(TeletypeSlipPayload payload)
	{
		clientSlip = payload.lines();
		clientOutlet = payload.outlet();
		clientOutletHome = payload.home();
		clientOutlets = payload.outlets();
	}

	public List<TeletypeSlipPayload.Outlet> getClientOutlets()
	{
		return clientOutlets;
	}

	public BlockPos getTerminalPos()
	{
		return terminalPos;
	}

	public List<RequestSlip.Line> getClientSlip()
	{
		return clientSlip;
	}

	public Component getClientOutlet()
	{
		return clientOutlet;
	}

	public boolean isClientOutletHome()
	{
		return clientOutletHome;
	}

	public void applyCrafting(TerminalCraftingPayload payload)
	{
		clientCraftables = new LinkedHashSet<>(payload.craftables());
		clientOffers = payload.offers();
		clientOrders = payload.orders();
		clientVersion++;
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

	public Map<ItemKey, Long> getClientContents()
	{
		return Collections.unmodifiableMap(clientContents);
	}

	public TerminalStats getClientStats()
	{
		return clientStats;
	}

	public int getClientVersion()
	{
		return clientVersion;
	}
}

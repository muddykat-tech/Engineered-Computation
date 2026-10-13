package com.muddykat.engineeredcomputation.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.crafting.CraftingNetwork;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.power.EnergyAccepterBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECCapabilities;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.terminal.crafting.CraftingGrid;
import com.muddykat.engineeredcomputation.terminal.crafting.VideoTerminalMenu;
import com.muddykat.engineeredcomputation.terminal.floppy.FloppyTerminalMenu;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeMenu;

public abstract class TerminalMenu extends AbstractContainerMenu
{
	public static final int GRID_X = 7;
	public static final int COLUMNS = 9;

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

	protected static final int INVENTORY_SLOTS = 36;
	private static final int ENTRIES_PER_PAYLOAD = 256;

	protected final BlockPos terminalPos;
	protected final Player player;
	private final Layout layout;
	@Nullable
	protected StorageNetwork network;
	private Map<ItemKey, Long> sentContents = Map.of();
	private TerminalStats sentStats = TerminalStats.EMPTY;
	private boolean sentInitial;

	private final Map<ItemKey, Long> clientContents = new HashMap<>();
	private TerminalStats clientStats = TerminalStats.EMPTY;
	private int clientVersion;

	protected TerminalMenu(MenuType<?> type, int id, Inventory playerInventory, BlockPos terminalPos)
	{
		super(type, id);
		this.terminalPos = terminalPos;
		this.player = playerInventory.player;
		this.layout = Layout.of(getFeatures());
		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(new Slot(playerInventory, col+row*9+9, GRID_X+1+col*18, layout.inventoryY()+1+row*18));
		for(int col = 0; col<9; col++)
			addSlot(new Slot(playerInventory, col, GRID_X+1+col*18, layout.hotbarY()+1));
	}

	public static void open(ServerPlayer player, BlockPos terminalPos, Component title, ComputerEra era,
			@Nullable CraftingGrid grid)
	{
		MenuConstructor menu = switch(era)
		{
			case PUNCHCARD -> (id, inventory, p) -> new TeletypeMenu(id, inventory, terminalPos);
			case DRUM -> (id, inventory, p) -> new TypewriterMenu(id, inventory, terminalPos);
			case TAPE -> (id, inventory, p) -> new UniscopeMenu(id, inventory, terminalPos);
			case DISK -> (id, inventory, p) -> new VideoTerminalMenu(id, inventory, terminalPos, grid);
			case FLOPPY -> (id, inventory, p) -> new FloppyTerminalMenu(id, inventory, terminalPos, grid);
		};
		player.openMenu(new SimpleMenuProvider(menu, title), buf -> buf.writeBlockPos(terminalPos));
	}

	public abstract TerminalFeatures getFeatures();

	public Layout getLayout()
	{
		return layout;
	}

	public BlockPos getTerminalPos()
	{
		return terminalPos;
	}

	protected NetworkStorage storage()
	{
		network = StorageNetwork.of(player.level(), terminalPos);
		return network.getUsableStorage(player.level());
	}

	@Override
	public void broadcastChanges()
	{
		super.broadcastChanges();
		if(player instanceof ServerPlayer serverPlayer)
			sendContentChanges(serverPlayer);
	}

	public record Snapshot(Map<ItemKey, Long> contents, TerminalStats stats)
	{
	}

	protected Snapshot snapshot(ServerPlayer serverPlayer)
	{
		NetworkStorage storage = storage();
		Map<ItemKey, Long> contents = new HashMap<>();
		storage.collect(contents);
		List<EnergyAccepterBlockEntity> accepters = EnergyAccepterBlockEntity.accepters(player.level(), network);
		TerminalStats stats = TerminalStats.of(storage, network.getConflictingEras(), network.isTooLarge(),
				CraftingNetwork.isProcessing(network), network.getErasWithoutProcessor())
				.withPower(ECConfig.powerEnabled(), network.isPowered(player.level()),
						network.getPowerCost(player.level()), EnergyAccepterBlockEntity.stored(accepters),
						(long)accepters.size()*EnergyAccepterBlockEntity.capacity());
		return new Snapshot(contents, stats);
	}

	private void sendContentChanges(ServerPlayer serverPlayer)
	{
		Snapshot snapshot = snapshot(serverPlayer);
		Map<ItemKey, Long> contents = snapshot.contents();
		List<CellContents.Entry> changes = new ArrayList<>();
		for(Map.Entry<ItemKey, Long> entry : contents.entrySet())
			if(!Objects.equals(sentContents.get(entry.getKey()), entry.getValue()))
				changes.add(new CellContents.Entry(entry.getKey(), entry.getValue()));
		for(ItemKey key : sentContents.keySet())
			if(!contents.containsKey(key))
				changes.add(new CellContents.Entry(key, 0));

		if(sentInitial&&changes.isEmpty()&&snapshot.stats().equals(sentStats))
			return;
		boolean reset = !sentInitial;
		for(int start = 0; start<Math.max(1, changes.size()); start += ENTRIES_PER_PAYLOAD)
		{
			List<CellContents.Entry> batch = changes.subList(start, Math.min(changes.size(), start+ENTRIES_PER_PAYLOAD));
			PacketDistributor.sendToPlayer(serverPlayer,
					new TerminalContentsPayload(containerId, reset&&start==0, batch, snapshot.stats()));
		}
		sentContents = contents;
		sentStats = snapshot.stats();
		sentInitial = true;
	}

	protected Set<ItemKey> sentKeys()
	{
		return sentContents.keySet();
	}

	public void handleClick(@Nullable ItemKey key, TerminalAction action)
	{
		click(key==null? null: ItemKey.resolve(key, sentKeys(), player.registryAccess()), action);
		broadcastChanges();
	}

	protected void click(@Nullable ItemKey key, TerminalAction action)
	{
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
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(player.level().isClientSide||!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		stack.shrink((int)storage().insert(ItemKey.of(stack), stack.getCount(), false));
		slot.setChanged();
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.level().getCapability(ECCapabilities.NETWORK_MEMBER, terminalPos, null)!=null
				&&player.canInteractWithBlock(terminalPos, 4.0);
	}

	public void applyUpdate(TerminalContentsPayload payload)
	{
		if(payload.reset())
			clientContents.clear();
		for(CellContents.Entry entry : payload.entries())
			if(entry.count()>0)
				clientContents.put(entry.key(), entry.count());
			else
				clientContents.remove(entry.key());
		clientStats = payload.stats();
		clientChanged();
	}

	protected void clientChanged()
	{
		clientVersion++;
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

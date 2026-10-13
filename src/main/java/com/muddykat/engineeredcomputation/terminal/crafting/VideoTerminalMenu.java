package com.muddykat.engineeredcomputation.terminal.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;
import com.muddykat.engineeredcomputation.terminal.TerminalFeatures;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public class VideoTerminalMenu extends TerminalMenu
{
	public static final int CRAFT_X = 29;
	public static final int RESULT_X = 119;
	public static final int LINK_X = 158;
	public static final int CLEAR_X = CRAFT_X-14;
	public static final int RESULT_SLOT = INVENTORY_SLOTS;
	public static final int CRAFT_GRID_FIRST = RESULT_SLOT+1;
	public static final int TOGGLE_LINKED = 0;
	public static final int CLEAR_GRID = 1;

	private final CraftingGrid grid;
	private final GridContainer craftGrid = new GridContainer();
	private final ResultContainer craftResult = new ResultContainer();
	private int resultVersion = -1;
	private int quickCraftBudget;

	public VideoTerminalMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf)
	{
		this(id, playerInventory, buf.readBlockPos(), null);
	}

	public VideoTerminalMenu(int id, Inventory playerInventory, BlockPos terminalPos, @Nullable CraftingGrid grid)
	{
		this(ECMenuTypes.VIDEO_TERMINAL.get(), id, playerInventory, terminalPos, grid);
	}

	protected VideoTerminalMenu(MenuType<?> type, int id, Inventory playerInventory, BlockPos terminalPos,
			@Nullable CraftingGrid grid)
	{
		super(type, id, playerInventory, terminalPos);
		this.grid = grid!=null? grid: CraftingGrid.detached();
		int craftingY = getLayout().craftingY();
		addSlot(new CraftResultSlot(RESULT_X+5, craftingY+18+1));
		for(int row = 0; row<3; row++)
			for(int col = 0; col<3; col++)
				addSlot(new GridSlot(col+row*3, CRAFT_X+1+col*18, craftingY+1+row*18));
		addDataSlot(new DataSlot()
		{
			@Override
			public int get()
			{
				return VideoTerminalMenu.this.grid.isLinked()? 1: 0;
			}

			@Override
			public void set(int value)
			{
				VideoTerminalMenu.this.grid.setLinked(value!=0);
			}
		});
	}

	@Override
	public TerminalFeatures getFeatures()
	{
		return TerminalFeatures.CRAFTING;
	}

	public boolean isLinked()
	{
		return grid.isLinked();
	}

	public boolean isGridSlot(Slot slot)
	{
		return slot.container==craftGrid;
	}

	public boolean hasLinkedIngredients()
	{
		Map<ItemKey, Long> needed = new HashMap<>();
		for(ItemStack link : grid.slots())
			if(!link.isEmpty())
				needed.merge(ItemKey.of(link), 1L, Long::sum);
		NetworkStorage storage = player.level().isClientSide? null: storage();
		for(Map.Entry<ItemKey, Long> entry : needed.entrySet())
		{
			long stored = storage==null? getClientContents().getOrDefault(entry.getKey(), 0L)
					: storage.getAmount(entry.getKey());
			if(stored<entry.getValue())
				return false;
		}
		return true;
	}

	public long storedOnClient(ItemStack link)
	{
		return getClientContents().getOrDefault(ItemKey.of(link), 0L);
	}

	@Override
	public void broadcastChanges()
	{
		if(grid.version()!=resultVersion)
			updateCraftResult();
		super.broadcastChanges();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(player.level().isClientSide||!slot.hasItem())
			return ItemStack.EMPTY;
		if(index==RESULT_SLOT)
			return quickCraft(player, slot);
		if(index<CRAFT_GRID_FIRST)
			return super.quickMoveStack(player, index);
		ItemStack stack = slot.getItem();
		stack.shrink((int)storage().insert(ItemKey.of(stack), stack.getCount(), false));
		moveItemStackTo(stack, 0, INVENTORY_SLOTS, false);
		slot.set(stack.isEmpty()? ItemStack.EMPTY: stack);
		return ItemStack.EMPTY;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player)
	{
		if(isLinked()&&slotId>=CRAFT_GRID_FIRST&&slotId<CRAFT_GRID_FIRST+CraftingGrid.SIZE
				&&(clickType==ClickType.PICKUP||clickType==ClickType.QUICK_MOVE))
		{
			if(!player.level().isClientSide)
				clickLinked(slotId-CRAFT_GRID_FIRST, clickType);
			return;
		}
		if(slotId==RESULT_SLOT&&clickType==ClickType.QUICK_MOVE)
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
		result.getItem().onCraftedBy(result, player.level(), player);
		if(!moveItemStackTo(result, 0, INVENTORY_SLOTS, true))
			return ItemStack.EMPTY;
		slot.onQuickCraft(result, crafted);
		if(result.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY, crafted);
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
		switch(id)
		{
			case TOGGLE_LINKED ->
			{
				if(!player.level().isClientSide)
				{
					if(isLinked())
						unlink();
					else
						link(player);
				}
			}
			case CLEAR_GRID ->
			{
				if(!player.level().isClientSide)
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
		if(!(player.level() instanceof ServerLevel level)||!(player instanceof ServerPlayer serverPlayer))
			return;
		resultVersion = grid.version();
		CraftingInput input = craftGrid.asCraftInput();
		ItemStack result = ItemStack.EMPTY;
		Optional<RecipeHolder<CraftingRecipe>> recipe =
				level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
		if(recipe.isPresent()&&craftResult.setRecipeUsed(level, serverPlayer, recipe.get()))
		{
			ItemStack assembled = recipe.get().value().assemble(input, level.registryAccess());
			if(assembled.isItemEnabled(level.enabledFeatures()))
				result = assembled;
		}
		craftResult.setItem(0, result);
		setRemoteSlot(RESULT_SLOT, result);
		serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT,
				result));
	}

	private void clickLinked(int slot, ClickType clickType)
	{
		ItemStack carried = getCarried();
		craftGrid.setItem(slot, clickType==ClickType.PICKUP&&!carried.isEmpty()? carried.copyWithCount(1): ItemStack.EMPTY);
	}

	private void link(Player player)
	{
		NetworkStorage storage = storage();
		for(int i = 0; i<CraftingGrid.SIZE; i++)
		{
			ItemStack stack = grid.getItem(i);
			if(stack.isEmpty())
				continue;
			grid.slots().set(i, stack.copyWithCount(1));
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
				grid.slots().set(i, storage.extract(ItemKey.of(link), 1, false)>0? link.copyWithCount(1): ItemStack.EMPTY);
		}
		grid.setLinked(false);
		updateCraftResult();
	}

	private void takeLinkedIngredients(Player taker)
	{
		NetworkStorage storage = storage();
		CraftingInput input = craftGrid.asCraftInput();
		CommonHooks.setCraftingPlayer(taker);
		NonNullList<ItemStack> leftovers = taker.level().getRecipeManager()
				.getRemainingItemsFor(RecipeType.CRAFTING, input, taker.level());
		CommonHooks.setCraftingPlayer(null);
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

	public void fillGrid(List<List<ItemStack>> options, boolean max)
	{
		if(player.level().isClientSide||options.size()>CraftingGrid.SIZE)
			return;
		NetworkStorage storage = storage();
		boolean linked = isLinked();
		if(!linked)
			emptyCraftGrid(player);
		List<ItemStack> chosen = new ArrayList<>(CraftingGrid.SIZE);
		for(int i = 0; i<CraftingGrid.SIZE; i++)
			chosen.add(i<options.size()? mostAvailable(options.get(i), storage, !linked): ItemStack.EMPTY);
		int sets = linked? 1: max? availableSets(chosen, storage): 1;
		for(int i = 0; i<CraftingGrid.SIZE; i++)
		{
			ItemStack wanted = chosen.get(i);
			grid.slots().set(i, wanted.isEmpty()? ItemStack.EMPTY: linked? wanted.copyWithCount(1):
					take(wanted, sets, storage));
		}
		grid.changed();
		updateCraftResult();
	}

	private long available(ItemStack stack, NetworkStorage storage, boolean withInventory)
	{
		long count = storage.getAmount(ItemKey.of(stack));
		if(withInventory)
			for(ItemStack held : player.getInventory().items)
				if(ItemStack.isSameItemSameComponents(held, stack))
					count += held.getCount();
		return count;
	}

	private ItemStack mostAvailable(List<ItemStack> options, NetworkStorage storage, boolean withInventory)
	{
		ItemStack best = ItemStack.EMPTY;
		long bestCount = -1;
		for(ItemStack option : options)
		{
			long count = option.isEmpty()? -1: available(option, storage, withInventory);
			if(count>bestCount)
			{
				best = option;
				bestCount = count;
			}
		}
		return best;
	}

	private int availableSets(List<ItemStack> chosen, NetworkStorage storage)
	{
		Map<ItemKey, Integer> uses = new HashMap<>();
		Map<ItemKey, ItemStack> stacks = new HashMap<>();
		int sets = Integer.MAX_VALUE;
		for(ItemStack stack : chosen)
			if(!stack.isEmpty())
			{
				uses.merge(ItemKey.of(stack), 1, Integer::sum);
				stacks.putIfAbsent(ItemKey.of(stack), stack);
				sets = Math.min(sets, stack.getMaxStackSize());
			}
		for(Map.Entry<ItemKey, Integer> use : uses.entrySet())
			sets = (int)Math.min(sets, available(stacks.get(use.getKey()), storage, true)/use.getValue());
		return Math.max(1, sets);
	}

	private ItemStack take(ItemStack wanted, int count, NetworkStorage storage)
	{
		ItemKey key = ItemKey.of(wanted);
		int taken = (int)storage.extract(key, count, false);
		for(ItemStack held : player.getInventory().items)
		{
			if(taken>=count)
				break;
			if(ItemStack.isSameItemSameComponents(held, wanted))
			{
				int moved = Math.min(count-taken, held.getCount());
				held.shrink(moved);
				taken += moved;
			}
		}
		return taken==0? ItemStack.EMPTY: key.toStack(taken);
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

	private ItemStack assemble(Level level)
	{
		CraftingInput input = craftGrid.asCraftInput();
		return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
				.map(recipe -> recipe.value().assemble(input, level.registryAccess()))
				.orElse(ItemStack.EMPTY);
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
			if(taker.level().isClientSide)
			{
				craftResult.setItem(0, assemble(taker.level()));
				return;
			}
			takeLinkedIngredients(taker);
			updateCraftResult();
		}
	}

	private final class GridContainer implements CraftingContainer
	{
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
		public List<ItemStack> getItems()
		{
			return List.copyOf(grid.slots());
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
}

package com.muddykat.engineeredcomputation.adapter;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import com.muddykat.engineeredcomputation.network.NetworkCableBlock;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

public class DataAdapterBlockEntity extends BlockEntity implements MenuProvider
{
	/** Synced to the menu: status, items per move, ticks between, on/off, block list. */
	static final int DATA_COUNT = 5;

	private final SimpleContainer filter = new SimpleContainer(1)
	{
		@Override
		public void setChanged()
		{
			super.setChanged();
			DataAdapterBlockEntity.this.setChanged();
		}
	};
	private final AdapterSpeed speed;
	private int items;
	private int ticks;
	private int cooldown;
	private boolean enabled = true;
	/** Whether the filter lists what not to move (a block list) rather than what to move (an allow list). */
	private boolean blocking;
	private AdapterStatus status = AdapterStatus.SERVICE_UNAVAILABLE;

	private final ContainerData data = new ContainerData()
	{
		@Override
		public int get(int index)
		{
			return switch(index)
			{
				case 0 -> status.ordinal();
				case 1 -> items;
				case 2 -> ticks;
				case 3 -> enabled? 1: 0;
				default -> blocking? 1: 0;
			};
		}

		@Override
		public void set(int index, int value)
		{
		}

		@Override
		public int getCount()
		{
			return DATA_COUNT;
		}
	};

	public DataAdapterBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECAdapters.BLOCK_ENTITY.get(), pos, state);
		this.speed = speedOf(state);
		this.items = speed.items();
		this.ticks = speed.ticks();
	}

	static AdapterSpeed speedOf(BlockState state)
	{
		return AdapterSpeed.of(((DataAdapterBlock)state.getBlock()).getEra()).orElseThrow();
	}

	private DataAdapterBlock block()
	{
		return (DataAdapterBlock)getBlockState().getBlock();
	}

	public ItemStack getFilter()
	{
		return filter.getItem(0);
	}

	public void setFilter(ItemStack stack)
	{
		filter.setItem(0, stack);
	}

	SimpleContainer filterContainer()
	{
		return filter;
	}

	public int getItems()
	{
		return items;
	}

	public int getTicks()
	{
		return ticks;
	}

	public void setSpeed(int items, int ticks)
	{
		this.items = speed.clampItems(items);
		this.ticks = speed.clampTicks(ticks);
		cooldown = Math.min(cooldown, this.ticks);
		setChanged();
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
		cooldown = 0;
		setChanged();
	}

	public boolean isBlocking()
	{
		return blocking;
	}

	public void setBlocking(boolean blocking)
	{
		this.blocking = blocking;
		cooldown = 0;
		setChanged();
	}

	public AdapterStatus getStatus()
	{
		return status;
	}

	ContainerData data()
	{
		return data;
	}

	void tickServer()
	{
		if(--cooldown>0)
			return;
		cooldown = ticks;
		status = enabled? transfer(): AdapterStatus.OFF;
		boolean active = status==AdapterStatus.OK;
		if(getBlockState().getValue(DataAdapterBlock.ACTIVE)!=active)
			level.setBlock(worldPosition, getBlockState().setValue(DataAdapterBlock.ACTIVE, active), 3);
	}

	public AdapterStatus transfer()
	{
		Direction facing = getBlockState().getValue(DataAdapterBlock.FACING);
		if(!(level.getBlockState(worldPosition.relative(facing.getOpposite())).getBlock() instanceof NetworkCableBlock))
			return AdapterStatus.SERVICE_UNAVAILABLE;
		IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(facing),
				facing.getOpposite());
		if(target==null)
			return AdapterStatus.SERVICE_UNAVAILABLE;
		StorageNetwork network = StorageNetwork.of(level, worldPosition);
		if(network.isTooLarge())
			return AdapterStatus.CONTENT_TOO_LARGE;
		if(!network.getConflictingEras().isEmpty())
			return AdapterStatus.CONFLICT;
		NetworkStorage storage = network.getStorage();
		if(storage.getCells().isEmpty())
			return network.getErasWithoutProcessor().isEmpty()? AdapterStatus.SERVICE_UNAVAILABLE:
					AdapterStatus.NO_PROCESSOR;
		if(AdapterFilter.isProgram(getFilter())&&!AdapterFilter.takesPrograms(block().getEra()))
			return AdapterStatus.NOT_FOUND;
		AdapterFilter match = new AdapterFilter(getFilter(), blocking);
		return block().getKind()==AdapterKind.EXPORTER? export(storage, target, match): importFrom(target, storage, match);
	}

	private AdapterStatus export(NetworkStorage storage, IItemHandler target, AdapterFilter match)
	{
		Map<ItemKey, Long> stored = new HashMap<>();
		storage.collect(stored);
		int budget = items;
		boolean found = false;
		for(Map.Entry<ItemKey, Long> entry : stored.entrySet())
		{
			ItemKey key = entry.getKey();
			if(budget==0||!match.allows(key.displayStack()))
				continue;
			found = true;
			int want = (int)Math.min(budget, entry.getValue());
			while(want>0)
			{
				int chunk = Math.min(want, key.getMaxStackSize());
				int fits = chunk-ItemHandlerHelper.insertItemStacked(target, key.toStack(chunk), true).getCount();
				int taken = fits==0? 0: (int)storage.extract(key, fits, false);
				if(taken==0)
					break;
				ItemStack left = ItemHandlerHelper.insertItemStacked(target, key.toStack(taken), false);
				if(!left.isEmpty())
					storage.insert(key, left.getCount(), false);
				int moved = taken-left.getCount();
				budget -= moved;
				want -= moved;
				if(moved<chunk)
					break;
			}
		}
		if(budget<items)
			return AdapterStatus.OK;
		return found? AdapterStatus.INSUFFICIENT_STORAGE: AdapterStatus.NOT_FOUND;
	}

	private AdapterStatus importFrom(IItemHandler source, NetworkStorage storage, AdapterFilter match)
	{
		boolean everything = match.isEmpty();
		int budget = items;
		boolean found = false;
		for(int slot = 0; slot<source.getSlots()&&budget>0; slot++)
		{
			ItemStack stack = source.getStackInSlot(slot);
			if(stack.isEmpty()||!everything&&!match.allows(stack))
				continue;
			ItemStack offered = source.extractItem(slot, Math.min(budget, stack.getCount()), true);
			if(offered.isEmpty())
				continue;
			found = true;
			ItemKey key = ItemKey.of(offered);
			int fits = (int)storage.insert(key, offered.getCount(), true);
			if(fits==0)
				continue;
			ItemStack taken = source.extractItem(slot, fits, false);
			int stored = (int)storage.insert(key, taken.getCount(), false);
			if(stored<taken.getCount())
				source.insertItem(slot, key.toStack(taken.getCount()-stored), false);
			budget -= stored;
		}
		if(budget<items)
			return AdapterStatus.OK;
		return found? AdapterStatus.INSUFFICIENT_STORAGE: AdapterStatus.NOT_FOUND;
	}

	@Override
	public Component getDisplayName()
	{
		return getBlockState().getBlock().getName();
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
	{
		return new DataAdapterMenu(id, inventory, this);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		if(!getFilter().isEmpty())
			tag.put("filter", getFilter().save(registries));
		tag.putInt("items", items);
		tag.putInt("ticks", ticks);
		tag.putBoolean("enabled", enabled);
		tag.putBoolean("blocking", blocking);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		filter.setItem(0, tag.contains("filter")? ItemStack.parseOptional(registries, tag.getCompound("filter"))
				: ItemStack.EMPTY);
		setSpeed(tag.contains("items")? tag.getInt("items"): speed.items(),
				tag.contains("ticks")? tag.getInt("ticks"): speed.ticks());
		enabled = !tag.contains("enabled")||tag.getBoolean("enabled");
		blocking = tag.getBoolean("blocking");
	}
}

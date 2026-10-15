package com.muddykat.engineeredcomputation.remote;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.catalogue.CatalogueDesk;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.station.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECRemote;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;

/**
 * Reports what it faces to the data network, and gets it for the network's orders: an inventory, which it takes from
 * straight away like an importer, or through a Receiving Station, what the Tabulator Machines of that Pneumatic
 * Network have tallied, which it requests to that station and collects as it lands.
 */
public class StorageAnalyzerBlockEntity extends BlockEntity
{
	private static final int LOOK_TICKS = 20;
	private static final int BUSY_TICKS = 20;

	/** One inventory's contents, keyed by where the inventory is so the network counts each one once. */
	public record Place(Map<ItemKey, Long> contents, boolean pneumatic)
	{
	}

	/** When crafting plans take from what it reports. Requests from a terminal are always allowed. */
	public enum Priority
	{
		BEFORE_CRAFTING,
		AFTER_CRAFTING,
		REQUESTS_ONLY;

		public Priority next()
		{
			return values()[(ordinal()+1)%values().length];
		}
	}

	private final NetworkMember member = new NetworkMember()
	{
		@Nullable
		@Override
		public StorageAnalyzerBlockEntity getAnalyzer()
		{
			return StorageAnalyzerBlockEntity.this;
		}

		@Override
		public int powerCost(Level level, BlockPos pos)
		{
			return ECConfig.cost(ECRemote.ANALYZER_COST);
		}
	};
	private Priority priority = Priority.BEFORE_CRAFTING;
	/** Items still coming for cancelled orders, which go into the network when they land. */
	private final Map<ItemKey, Long> strays = new LinkedHashMap<>();
	private Map<BlockPos, Place> places = Map.of();
	private int countdown;
	private long busyUntil;

	public StorageAnalyzerBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECRemote.ANALYZER_ENTITY.get(), pos, state);
	}

	@Nullable
	public static StorageAnalyzerBlockEntity at(Level level, BlockPos pos)
	{
		return level.isLoaded(pos)&&level.getBlockEntity(pos) instanceof StorageAnalyzerBlockEntity analyzer
				? analyzer: null;
	}

	public NetworkMember member()
	{
		return member;
	}

	public Priority getPriority()
	{
		return priority;
	}

	public void setPriority(Priority priority)
	{
		this.priority = priority;
		setChanged();
	}

	/** As of the last look, at most a second ago. */
	public Map<BlockPos, Place> places()
	{
		return places;
	}

	private Direction facing()
	{
		return getBlockState().getValue(RemoteLinkBlock.FACING);
	}

	@Nullable
	public ReceivingStationBlockEntity station()
	{
		return level!=null&&level.getBlockEntity(worldPosition.relative(facing()))
				instanceof ReceivingStationBlockEntity station? station: null;
	}

	@Nullable
	private IItemHandler inventory()
	{
		BlockPos target = worldPosition.relative(facing());
		return level==null||!AutotubeBlock.isInventory(level, target, facing())? null
				: level.getCapability(Capabilities.ItemHandler.BLOCK, target, facing().getOpposite());
	}

	/** Whether it faces something it can get items from. */
	public boolean canRequest()
	{
		return station()!=null||inventory()!=null;
	}

	/**
	 * How many it can get: what an inventory holds now, or what it got the Pneumatic Network to send, which can be
	 * fewer than asked for.
	 */
	public int request(ItemKey key, int amount)
	{
		if(amount<=0)
			return 0;
		busyUntil = level.getGameTime()+BUSY_TICKS;
		if(station()!=null)
		{
			int asked = new CatalogueDesk(level, worldPosition).request(key, amount);
			if(asked>0)
				countdown = 0;
			return asked;
		}
		IItemHandler inventory = inventory();
		long held = 0;
		for(int slot = 0; inventory!=null&&slot<inventory.getSlots(); slot++)
			if(key.matches(inventory.getStackInSlot(slot)))
				held += inventory.getStackInSlot(slot).getCount();
		return (int)Math.min(amount, held);
	}

	/** Takes up to max of an item from the inventory, or from what has landed in the Receiving Station. */
	public long collect(ItemKey key, long max)
	{
		if(max<=0)
			return 0;
		busyUntil = level.getGameTime()+BUSY_TICKS;
		ReceivingStationBlockEntity station = station();
		long taken = 0;
		if(station!=null)
		{
			SimpleContainer tray = station.getTray();
			for(int slot = 0; slot<tray.getContainerSize()&&taken<max; slot++)
			{
				ItemStack stack = tray.getItem(slot);
				if(!stack.isEmpty()&&key.matches(stack))
					taken += tray.removeItem(slot, (int)Math.min(max-taken, stack.getCount())).getCount();
			}
			return taken;
		}
		IItemHandler inventory = inventory();
		for(int slot = 0; inventory!=null&&slot<inventory.getSlots()&&taken<max; slot++)
			if(key.matches(inventory.getStackInSlot(slot)))
				taken += inventory.extractItem(slot, (int)Math.min(max-taken, key.getMaxStackSize()), false).getCount();
		if(taken>0)
			countdown = 0;
		return taken;
	}

	/**
	 * Takes over items still coming through the Pneumatic Network for a cancelled order, so they're stored when they
	 * land; an inventory's items were never moved, so there's nothing to take over.
	 */
	public void adopt(ItemKey key, long count)
	{
		if(count<=0||station()==null)
			return;
		strays.merge(key, count, Long::sum);
		setChanged();
	}

	public boolean isBusy()
	{
		return !strays.isEmpty()||level!=null&&level.getGameTime()<busyUntil;
	}

	void tickServer()
	{
		if(!strays.isEmpty())
			storeStrays();
		if(countdown-->0)
			return;
		countdown = LOOK_TICKS;
		look();
	}

	public void look()
	{
		places = Map.copyOf(lookAt(facing()));
		boolean active = !places.isEmpty()||isBusy();
		if(getBlockState().getValue(RemoteLinkBlock.ACTIVE)!=active)
			level.setBlock(worldPosition, getBlockState().setValue(RemoteLinkBlock.ACTIVE, active), 3);
	}

	private Map<BlockPos, Place> lookAt(Direction facing)
	{
		Map<BlockPos, Place> seen = new HashMap<>();
		if(station()!=null)
		{
			for(CatalogueDesk.Source source : new CatalogueDesk(level, worldPosition).view().sources())
			{
				Map<ItemKey, Long> contents = new HashMap<>();
				for(CellContents.Entry entry : source.tally().entries())
					contents.merge(entry.key(), entry.count(), Long::sum);
				if(!contents.isEmpty())
					seen.put(source.station().inventoryPos(), new Place(Map.copyOf(contents), true));
			}
			return seen;
		}
		IItemHandler inventory = inventory();
		Map<ItemKey, Long> contents = new HashMap<>();
		for(int slot = 0; inventory!=null&&slot<inventory.getSlots(); slot++)
		{
			ItemStack stack = inventory.getStackInSlot(slot);
			if(!stack.isEmpty())
				contents.merge(ItemKey.of(stack), (long)stack.getCount(), Long::sum);
		}
		if(!contents.isEmpty())
			seen.put(worldPosition.relative(facing), new Place(Map.copyOf(contents), false));
		return seen;
	}

	private void storeStrays()
	{
		StorageNetwork network = StorageNetwork.of(level, worldPosition);
		if(!network.isPowered(level)||station()==null)
			return;
		NetworkStorage storage = network.getUsableStorage(level);
		for(Map.Entry<ItemKey, Long> stray : List.copyOf(strays.entrySet()))
		{
			ItemKey key = stray.getKey();
			long taken = collect(key, storage.insert(key, stray.getValue(), true));
			if(taken<=0)
				continue;
			long stored = storage.insert(key, taken, false);
			if(stored<taken)
				station().getTray().addItem(key.toStack((int)(taken-stored)));
			long left = stray.getValue()-taken;
			if(left>0)
				strays.put(key, left);
			else
				strays.remove(key);
			setChanged();
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.putString("priority", priority.name());
		ListTag list = new ListTag();
		strays.forEach((key, count) -> {
			CompoundTag entry = new CompoundTag();
			entry.put("item", ItemKey.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), key)
					.getOrThrow());
			entry.putLong("count", count);
			list.add(entry);
		});
		if(!list.isEmpty())
			tag.put("strays", list);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		try
		{
			priority = Priority.valueOf(tag.getString("priority"));
		}
		catch(IllegalArgumentException e)
		{
			priority = Priority.BEFORE_CRAFTING;
		}
		strays.clear();
		for(Tag entry : tag.getList("strays", Tag.TAG_COMPOUND))
		{
			CompoundTag compound = (CompoundTag)entry;
			ItemKey.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), compound.get("item"))
					.result().ifPresent(key -> strays.put(key, compound.getLong("count")));
		}
	}
}

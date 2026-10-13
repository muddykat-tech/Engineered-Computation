package com.muddykat.engineeredcomputation.pneumatic.station;

import java.util.ArrayDeque;
import java.util.Deque;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.pneumatic.carrier.TubeCarriers;
import com.muddykat.engineeredcomputation.pneumatic.pressurizer.Pressure;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;

public class TubeStationBlockEntity extends BlockEntity
{

	public static final int MAX_NAME = 32;

	private final Deque<Request> requests = new ArrayDeque<>();
	private int cooldown;
	private String name = "";
	private boolean takesNewItems;
	private final SimpleContainer filterCard = new SimpleContainer(1)
	{
		@Override
		public void setChanged()
		{
			super.setChanged();
			TubeStationBlockEntity.this.setChanged();
		}
	};
	private boolean blocking;
	private boolean insertOnly;

	public TubeStationBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECPneumatics.TUBE_STATION_ENTITY.get(), pos, state);
	}

	public boolean request(ItemKey key, int amount, BlockPos receiver)
	{
		if(level==null||amount<=0||isInsertOnly()||!(level.getBlockEntity(receiver) instanceof ReceivingStationBlockEntity)
				||TubeNetwork.of(level, worldPosition).route(worldPosition, receiver).isEmpty())
			return false;
		requests.add(new Request(key, amount, receiver.immutable()));
		setChanged();
		return true;
	}

	public int getPendingRequests()
	{
		return requests.size();
	}

	public boolean hasRequestsFor(BlockPos receiver)
	{
		return requests.stream().anyMatch(request -> request.receiver.equals(receiver));
	}

	public CellContents tally()
	{
		IItemHandler source = getSource();
		if(source==null)
			return CellContents.EMPTY;
		Map<ItemKey, Long> amounts = new LinkedHashMap<>();
		for(int slot = 0; slot<source.getSlots(); slot++)
		{
			ItemStack stack = source.getStackInSlot(slot);
			if(!stack.isEmpty())
				amounts.merge(ItemKey.of(stack), (long)stack.getCount(), Long::sum);
		}
		return CellContents.of(amounts);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name.strip().substring(0, Math.min(MAX_NAME, name.strip().length()));
		setChanged();
	}

	public boolean takesNewItems()
	{
		return takesNewItems;
	}

	public void setTakesNewItems(boolean takesNewItems)
	{
		this.takesNewItems = takesNewItems;
		setChanged();
	}

	public boolean filters()
	{
		return getBlockState().getBlock() instanceof TubeStationBlock block&&block.filters();
	}

	public SimpleContainer filterContainer()
	{
		return filterCard;
	}

	public ItemStack getFilterCard()
	{
		return filterCard.getItem(0);
	}

	public void setFilterCard(ItemStack card)
	{
		filterCard.setItem(0, card);
	}

	public boolean isBlocking()
	{
		return blocking;
	}

	public boolean isInsertOnly()
	{
		return insertOnly&&filters();
	}

	public void setInsertOnly(boolean insertOnly)
	{
		this.insertOnly = insertOnly;
		setChanged();
	}

	public void setBlocking(boolean blocking)
	{
		this.blocking = blocking;
		setChanged();
	}

	@Nullable
	public StationFilter filter()
	{
		return filters()? StationFilter.of(getFilterCard(), blocking): null;
	}

	public Component label()
	{
		return name.isEmpty()? inventoryName(): Component.literal(name);
	}

	public Component inventoryName()
	{
		if(level==null)
			return Component.empty();
		Direction facing = getBlockState().getValue(TubeStationBlock.FACING);
		BlockPos front = worldPosition.relative(facing);
		if(level.getBlockEntity(front) instanceof Nameable named&&named.hasCustomName())
			return named.getDisplayName();
		return level.getBlockState(front).getBlock().getName();
	}

	public ItemStack accept(ItemStack items)
	{
		IItemHandler source = getSource();
		StationFilter filter = filter();
		if(source==null||filter!=null&&!filter.allows(items))
			return items;
		return ItemHandlerHelper.insertItemStacked(source, items.copy(), false);
	}

	public int room(ItemStack items)
	{
		IItemHandler source = getSource();
		return source==null? 0: items.getCount()-ItemHandlerHelper.insertItemStacked(source, items.copy(), true).getCount();
	}

	@Nullable
	public IItemHandler getSource()
	{
		if(level==null)
			return null;
		Direction facing = getBlockState().getValue(TubeStationBlock.FACING);
		return level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(facing), facing.getOpposite());
	}

	void tickServer()
	{
		if(cooldown>0)
		{
			cooldown--;
			return;
		}
		Request request = requests.peek();
		if(request==null||!(level instanceof ServerLevel serverLevel))
			return;
		TubeNetwork network = TubeNetwork.of(level, worldPosition);
		List<BlockPos> route = network.route(worldPosition, request.receiver);
		boolean high = Pressure.high(level, network);
		ItemStack items = route.isEmpty()? ItemStack.EMPTY: take(request.key, Math.min(request.remaining,
				Pressure.carrierItems(high, request.key.getMaxStackSize())));
		if(items.isEmpty())
		{
			requests.poll();
			setChanged();
			return;
		}
		if(high)
			Pressure.draw(level, network);
		TubeCarriers.of(serverLevel).launch(serverLevel, items, route);
		request.remaining -= items.getCount();
		if(request.remaining<=0)
			requests.poll();
		cooldown = Pressure.carrierTicks(high);
		setChanged();
	}

	private ItemStack take(ItemKey key, int amount)
	{
		IItemHandler source = getSource();
		ItemStack taken = ItemStack.EMPTY;
		if(source==null)
			return taken;
		for(int slot = 0; slot<source.getSlots()&&taken.getCount()<amount; slot++)
		{
			if(!key.matches(source.getStackInSlot(slot)))
				continue;
			ItemStack got = source.extractItem(slot, amount-taken.getCount(), false);
			if(taken.isEmpty())
				taken = got;
			else
				taken.grow(got.getCount());
		}
		return taken;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		ListTag list = new ListTag();
		for(Request request : requests)
		{
			CompoundTag entry = new CompoundTag();
			entry.put("item", ItemKey.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
					request.key).getOrThrow());
			entry.putInt("remaining", request.remaining);
			entry.putLong("receiver", request.receiver.asLong());
			list.add(entry);
		}
		tag.put("requests", list);
		tag.putInt("cooldown", cooldown);
		if(!name.isEmpty())
			tag.putString("name", name);
		tag.putBoolean("takes_new_items", takesNewItems);
		if(!getFilterCard().isEmpty())
			tag.put("filter_card", getFilterCard().save(registries));
		tag.putBoolean("blocking", blocking);
		tag.putBoolean("insert_only", insertOnly);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		requests.clear();
		for(Tag item : tag.getList("requests", Tag.TAG_COMPOUND))
		{
			CompoundTag entry = (CompoundTag)item;
			ItemKey.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), entry.get("item")).result()
					.ifPresent(key -> requests.add(new Request(key, entry.getInt("remaining"),
							BlockPos.of(entry.getLong("receiver")))));
		}
		cooldown = tag.getInt("cooldown");
		name = tag.getString("name");
		takesNewItems = tag.getBoolean("takes_new_items");
		filterCard.setItem(0, tag.contains("filter_card")? ItemStack.parseOptional(registries,
				tag.getCompound("filter_card")): ItemStack.EMPTY);
		blocking = tag.getBoolean("blocking");
		insertOnly = tag.getBoolean("insert_only");
	}

	private static final class Request
	{
		final ItemKey key;
		int remaining;
		final BlockPos receiver;

		Request(ItemKey key, int remaining, BlockPos receiver)
		{
			this.key = key;
			this.remaining = remaining;
			this.receiver = receiver;
		}
	}
}

package com.muddykat.engineeredcomputation.pneumatic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.storage.CellContents;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public class TubeStationBlockEntity extends BlockEntity
{
	public static final int DISPATCH_TICKS = 10;

	public static final int MAX_NAME = 32;

	private final Deque<Request> requests = new ArrayDeque<>();
	private int cooldown;
	private String name = "";
	private boolean takesNewItems;

	public TubeStationBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECPneumatics.TUBE_STATION_ENTITY.get(), pos, state);
	}

	public boolean request(ItemKey key, int amount, BlockPos receiver)
	{
		if(level==null||amount<=0||!(level.getBlockEntity(receiver) instanceof ReceivingStationBlockEntity)
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
		return source==null? items: ItemHandlerHelper.insertItemStacked(source, items.copy(), false);
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
		BlockEntity target = level.getBlockEntity(worldPosition.relative(facing));
		return target==null? null: target.getCapability(ForgeCapabilities.ITEM_HANDLER,
				facing.getOpposite()).orElse(null);
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
		List<BlockPos> route = TubeNetwork.of(level, worldPosition).route(worldPosition, request.receiver);
		ItemStack items = route.isEmpty()? ItemStack.EMPTY: take(request.key, Math.min(request.remaining,
				request.key.getMaxStackSize()));
		if(items.isEmpty())
		{
			requests.poll();
			setChanged();
			return;
		}
		TubeCarriers.of(serverLevel).launch(serverLevel, items, route);
		request.remaining -= items.getCount();
		if(request.remaining<=0)
			requests.poll();
		cooldown = DISPATCH_TICKS;
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
	protected void saveAdditional(CompoundTag tag)
	{
		super.saveAdditional(tag);
		ListTag list = new ListTag();
		for(Request request : requests)
		{
			CompoundTag entry = new CompoundTag();
			entry.put("item", ItemKey.CODEC.encodeStart(NbtOps.INSTANCE,
					request.key).getOrThrow(false, error -> {}));
			entry.putInt("remaining", request.remaining);
			entry.putLong("receiver", request.receiver.asLong());
			list.add(entry);
		}
		tag.put("requests", list);
		tag.putInt("cooldown", cooldown);
		if(!name.isEmpty())
			tag.putString("name", name);
		tag.putBoolean("takes_new_items", takesNewItems);
	}

	@Override
	public void load(CompoundTag tag)
	{
		super.load(tag);
		requests.clear();
		for(Tag item : tag.getList("requests", Tag.TAG_COMPOUND))
		{
			CompoundTag entry = (CompoundTag)item;
			ItemKey.CODEC.parse(NbtOps.INSTANCE, entry.get("item")).result()
					.ifPresent(key -> requests.add(new Request(key, entry.getInt("remaining"),
							BlockPos.of(entry.getLong("receiver")))));
		}
		cooldown = tag.getInt("cooldown");
		name = tag.getString("name");
		takesNewItems = tag.getBoolean("takes_new_items");
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

package com.muddykat.engineeredcomputation.pneumatic.autotube;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.pneumatic.carrier.TubeCarriers;
import com.muddykat.engineeredcomputation.pneumatic.station.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.station.StationFilter;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECCapabilities;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public class AutotubeBlockEntity extends BlockEntity
{
	public static final int MAX_AMOUNT = 64;
	public static final int WATCH_TICKS = 4;

	private int amount = 1;
	private boolean split;
	@Nullable
	private BlockPos lastSent;
	private int lastDistance;
	private int watchCountdown;
	private int cut;
	@Nullable
	private Long seen;

	public AutotubeBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECPneumatics.AUTOTUBE_ENTITY.get(), pos, state);
	}

	public boolean isCut(Direction side)
	{
		return (cut&1<<side.ordinal())!=0;
	}

	public void setCut(Direction side, boolean cut)
	{
		this.cut = cut? this.cut|1<<side.ordinal(): this.cut&~(1<<side.ordinal());
		setChanged();
		if(level!=null&&!level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public boolean splits()
	{
		return split;
	}

	public void setSplit(boolean split)
	{
		this.split = split;
		setChanged();
	}

	public int getAmount()
	{
		return amount;
	}

	public void setAmount(int amount)
	{
		this.amount = Mth.clamp(amount, 1, MAX_AMOUNT);
		setChanged();
	}

	@Nullable
	public IItemHandler getInventory()
	{
		if(level==null)
			return null;
		Direction facing = getBlockState().getValue(AutotubeBlock.FACING);
		return level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(facing), facing.getOpposite());
	}

	public ItemStack accept(ItemStack items)
	{
		IItemHandler inventory = getInventory();
		return inventory==null? items: ItemHandlerHelper.insertItemStacked(inventory, items.copy(), false);
	}

	public int room(ItemStack items)
	{
		return room(getInventory(), items);
	}

	/** Takes a fresh look after firing, so its own sending doesn't count as a change. */
	void watch()
	{
		if(watchCountdown-->0)
			return;
		watchCountdown = WATCH_TICKS;
		IItemHandler inventory = getInventory();
		if(inventory==null)
		{
			seen = null;
			return;
		}
		long now = contents(inventory);
		if(seen!=null&&seen!=now&&fire())
			now = contents(inventory);
		seen = now;
	}

	private static long contents(IItemHandler inventory)
	{
		long hash = inventory.getSlots();
		for(int slot = 0; slot<inventory.getSlots(); slot++)
		{
			ItemStack stack = inventory.getStackInSlot(slot);
			hash = hash*31+(stack.isEmpty()? 0: ItemStack.hashItemAndComponents(stack)*65+stack.getCount());
		}
		return hash;
	}

	public boolean fire()
	{
		IItemHandler inventory = getInventory();
		if(!(level instanceof ServerLevel serverLevel)||inventory==null)
			return false;
		TubeNetwork network = TubeNetwork.of(level, worldPosition);
		// Neighbours first, because the tree is ordered nearest first.
		Map<BlockPos, BlockPos> tree = new LinkedHashMap<>();
		tree.put(worldPosition, worldPosition);
		Set<BlockPos> beside = beside();
		beside.forEach(pos -> tree.put(pos, worldPosition));
		network.tree(worldPosition).forEach(tree::putIfAbsent);
		Set<ItemKey> tried = new HashSet<>();
		for(int slot = 0; slot<inventory.getSlots(); slot++)
		{
			ItemStack stack = inventory.getStackInSlot(slot);
			if(stack.isEmpty())
				continue;
			ItemKey key = ItemKey.of(stack);
			if(!tried.add(key))
				continue;
			int total = split? Math.min(amount, held(inventory, key)): Math.min(amount, stack.getMaxStackSize());
			Places places = places(network, tree, beside, stack.copyWithCount(total));
			if(places==null)
				continue;
			List<Send> sends = split? split(tree, places, total): List.of(next(tree, places, total));
			boolean sent = false;
			for(Send send : sends)
			{
				ItemStack taken = take(inventory, key, send.count());
				if(taken.isEmpty())
					break;
				BlockPos to = send.path().get(send.path().size()-1);
				if(beside.contains(to))
					pushBeside(inventory, to, taken);
				else
					while(!taken.isEmpty())
						TubeCarriers.of(serverLevel).launch(serverLevel, taken.split(taken.getMaxStackSize()), send.path());
				sent = true;
			}
			if(!sent)
				continue;
			if(!split)
			{
				lastSent = sends.get(0).path().get(sends.get(0).path().size()-1);
				lastDistance = sends.get(0).path().size()-1;
			}
			setChanged();
			return true;
		}
		return false;
	}

	@Nullable
	private Places places(TubeNetwork network, Map<BlockPos, BlockPos> tree, Set<BlockPos> beside, ItemStack items)
	{
		List<BlockPos> asking = new ArrayList<>();
		List<BlockPos> taking = new ArrayList<>();
		List<BlockPos> others = new ArrayList<>();
		Map<BlockPos, Integer> rooms = new HashMap<>();
		for(BlockPos end : tree.keySet())
		{
			if(end.equals(worldPosition)||network.getTubes().contains(end))
				continue;
			BlockEntity at = level.getBlockEntity(end);
			if(at instanceof TubeStationBlockEntity station)
			{
				StationFilter filter = station.filter();
				int room = filter==null||filter.allows(items)? station.room(items): 0;
				if(room<=0)
					continue;
				if(filter!=null&&!filter.blocking())
					asking.add(end);
				else if(filter!=null||station.takesNewItems()||holds(station, items))
					taking.add(end);
				else
					continue;
				rooms.put(end, room);
				continue;
			}
			int room = at instanceof ReceivingStationBlockEntity receiver? receiver.room(items)
					: at instanceof AutotubeBlockEntity autotube? autotube.room(items)
					: beside.contains(end)? room(besideInventory(end), items): 0;
			if(room>0)
			{
				others.add(end);
				rooms.put(end, room);
			}
		}
		List<BlockPos> places = !asking.isEmpty()? asking: !taking.isEmpty()? taking: others;
		return places.isEmpty()? null: new Places(places, rooms);
	}

	private Send next(Map<BlockPos, BlockPos> tree, Places places, int count)
	{
		BlockPos next = next(tree, places.places());
		return new Send(pathTo(tree, next), Math.min(count, places.rooms().get(next)));
	}

	private List<Send> split(Map<BlockPos, BlockPos> tree, Places places, int total)
	{
		List<BlockPos> nearest = places.places().subList(0, Math.min(places.places().size(), total));
		List<Send> sends = new ArrayList<>();
		for(int index = 0; index<nearest.size(); index++)
		{
			BlockPos place = nearest.get(index);
			int share = Math.min(total/nearest.size()+(index<total%nearest.size()? 1: 0), places.rooms().get(place));
			if(share>0)
				sends.add(new Send(pathTo(tree, place), share));
		}
		return sends;
	}

	private static int held(IItemHandler inventory, ItemKey key)
	{
		int held = 0;
		for(int slot = 0; slot<inventory.getSlots(); slot++)
			if(key.matches(inventory.getStackInSlot(slot)))
				held += inventory.getStackInSlot(slot).getCount();
		return held;
	}

	private BlockPos next(Map<BlockPos, BlockPos> tree, List<BlockPos> places)
	{
		int last = lastSent==null? -1: places.indexOf(lastSent);
		if(last>=0)
			return places.get((last+1)%places.size());
		if(lastSent!=null)
			for(BlockPos place : places)
				if(pathTo(tree, place).size()-1>lastDistance)
					return place;
		return places.get(0);
	}

	private Set<BlockPos> beside()
	{
		Set<BlockPos> beside = new LinkedHashSet<>();
		Direction facing = getBlockState().getValue(AutotubeBlock.FACING);
		for(Direction side : Direction.values())
		{
			BlockPos neighbour = worldPosition.relative(side);
			if(side==facing||isCut(side)||!level.isLoaded(neighbour))
				continue;
			if(level.getCapability(ECCapabilities.TUBE_MEMBER, neighbour, side.getOpposite())!=null)
			{
				if(level.getBlockEntity(neighbour) instanceof AutotubeBlockEntity)
					beside.add(neighbour);
			}
			else if(AutotubeBlock.isInventory(level, neighbour, side))
				beside.add(neighbour);
		}
		return beside;
	}

	@Nullable
	private IItemHandler besideInventory(BlockPos neighbour)
	{
		for(Direction side : Direction.values())
			if(worldPosition.relative(side).equals(neighbour))
				return level.getCapability(Capabilities.ItemHandler.BLOCK, neighbour, side.getOpposite());
		return null;
	}

	private static int room(@Nullable IItemHandler inventory, ItemStack items)
	{
		return inventory==null? 0
				: items.getCount()-ItemHandlerHelper.insertItemStacked(inventory, items.copy(), true).getCount();
	}

	private void pushBeside(IItemHandler from, BlockPos to, ItemStack items)
	{
		ItemStack left = level.getBlockEntity(to) instanceof AutotubeBlockEntity autotube? autotube.accept(items)
				: besideInventory(to)==null? items: ItemHandlerHelper.insertItemStacked(besideInventory(to), items, false);
		if(!left.isEmpty())
			left = ItemHandlerHelper.insertItemStacked(from, left, false);
		if(!left.isEmpty())
			Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5,
					worldPosition.getZ()+0.5, left);
		level.playSound(null, worldPosition, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.6F, 1.1F);
	}

	private static boolean holds(TubeStationBlockEntity station, ItemStack items)
	{
		IItemHandler source = station.getSource();
		if(source==null)
			return false;
		for(int slot = 0; slot<source.getSlots(); slot++)
			if(ItemStack.isSameItemSameComponents(source.getStackInSlot(slot), items))
				return true;
		return false;
	}

	private List<BlockPos> pathTo(Map<BlockPos, BlockPos> tree, BlockPos end)
	{
		List<BlockPos> path = new ArrayList<>();
		for(BlockPos pos = end; !pos.equals(worldPosition); pos = tree.get(pos))
			path.add(pos);
		path.add(worldPosition);
		Collections.reverse(path);
		return path;
	}

	private static ItemStack take(IItemHandler inventory, ItemKey key, int amount)
	{
		ItemStack taken = ItemStack.EMPTY;
		for(int slot = 0; slot<inventory.getSlots()&&taken.getCount()<amount; slot++)
		{
			if(!key.matches(inventory.getStackInSlot(slot)))
				continue;
			ItemStack got = inventory.extractItem(slot, amount-taken.getCount(), false);
			if(taken.isEmpty())
				taken = got;
			else
				taken.grow(got.getCount());
		}
		return taken;
	}

	/** The client needs the disabled ports to work out its shape as its neighbours change. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		CompoundTag tag = super.getUpdateTag(registries);
		tag.putInt("cut", cut);
		return tag;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}

	private record Places(List<BlockPos> places, Map<BlockPos, Integer> rooms)
	{
	}

	private record Send(List<BlockPos> path, int count)
	{
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.putInt("amount", amount);
		tag.putInt("cut", cut);
		tag.putBoolean("split", split);
		if(lastSent!=null)
		{
			tag.putLong("last_sent", lastSent.asLong());
			tag.putInt("last_distance", lastDistance);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		amount = tag.contains("amount")? Mth.clamp(tag.getInt("amount"), 1, MAX_AMOUNT): 1;
		cut = tag.getInt("cut");
		split = tag.getBoolean("split");
		lastSent = tag.contains("last_sent")? BlockPos.of(tag.getLong("last_sent")): null;
		lastDistance = tag.getInt("last_distance");
	}
}

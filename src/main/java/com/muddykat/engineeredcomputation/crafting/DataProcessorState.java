package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import com.muddykat.engineeredcomputation.multiblock.core.MultiblockContext;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockState;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockStateHost;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

public class DataProcessorState implements MultiblockState, NetworkMember
{
	public static final int PATTERN_SLOTS = 9;
	public static final int ITEMS_PER_TICK = 64;
	public static final BlockPos BOTTOM = BlockPos.ZERO;
	public static final BlockPos TOP = new BlockPos(0, 2, 0);
	public static final String FILLED = "filled";

	private final MultiblockStateHost host;
	private final ItemStackHandler patterns;
	private final Map<ItemKey, Long> arrived = new LinkedHashMap<>();
	@Nullable
	private Level level;
	@Nullable
	private BlockPos masterPos;
	@Nullable
	private BlockPos topPos;
	private Direction front = Direction.SOUTH;
	private long allowanceTick = -1;
	private int allowance;
	private long processingUntil;

	public DataProcessorState(MultiblockStateHost host)
	{
		this.host = host;
		this.patterns = createPatternSlots(() -> {
			host.setChanged();
			host.syncToClient();
		});
	}

	public static ItemStackHandler createPatternSlots(Runnable onChanged)
	{
		return new ItemStackHandler(PATTERN_SLOTS)
		{
			@Override
			public boolean isItemValid(int slot, ItemStack stack)
			{
				return stack.getItem() instanceof PatternFloppyItem
						&&PatternFloppyItem.pattern(stack).mode()==Pattern.Mode.ABSTRACT;
			}

			@Override
			public int getSlotLimit(int slot)
			{
				return 1;
			}

			@Override
			protected void onContentsChanged(int slot)
			{
				onChanged.run();
			}
		};
	}

	@Nullable
	public static DataProcessorState at(Level level, @Nullable BlockPos pos)
	{
		return pos!=null&&level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part
				&&part.getState() instanceof DataProcessorState processor? processor: null;
	}

	public ItemStackHandler getPatternSlots()
	{
		return patterns;
	}

	public Map<Integer, Pattern> getPatterns()
	{
		Map<Integer, Pattern> held = new LinkedHashMap<>();
		for(int slot = 0; slot<patterns.getSlots(); slot++)
			if(PatternFloppyItem.holds(patterns.getStackInSlot(slot), Pattern.Mode.ABSTRACT))
				held.put(slot, PatternFloppyItem.pattern(patterns.getStackInSlot(slot)));
		return held;
	}

	public boolean runs(Pattern pattern)
	{
		return getPatterns().containsValue(pattern);
	}

	@Nullable
	public BlockPos getMasterPos()
	{
		located();
		return masterPos;
	}

	private boolean located()
	{
		if(level==null)
		{
			MultiblockContext ctx = host.context();
			if(ctx!=null)
				locate(ctx);
		}
		return level!=null;
	}

	@Nullable
	@Override
	public DataProcessorState getDataProcessor()
	{
		return this;
	}

	@Nullable
	public IItemHandler target()
	{
		return !located()? null: handlerFacing(topPos);
	}

	@Nullable
	private IItemHandler source()
	{
		return !located()? null: handlerFacing(masterPos);
	}

	@Nullable
	private IItemHandler handlerFacing(BlockPos pos)
	{
		BlockEntity facing = level.getBlockEntity(pos.relative(front));
		return facing==null? null: facing.getCapability(ForgeCapabilities.ITEM_HANDLER,
				front.getOpposite()).orElse(null);
	}

	public int takeMove(int atLeast)
	{
		refill();
		if(allowance<=0)
			return 0;
		int moved = Math.max(allowance, atLeast);
		allowance = 0;
		return moved;
	}

	private void refill()
	{
		long now = located()? level.getGameTime(): 0;
		if(now!=allowanceTick)
		{
			allowanceTick = now;
			allowance = ITEMS_PER_TICK;
		}
	}

	public void markProcessing()
	{
		if(level!=null)
			processingUntil = level.getGameTime()+2;
	}

	public Map<ItemKey, Long> take(Predicate<ItemKey> matches, long max)
	{
		Map<ItemKey, Long> taken = new LinkedHashMap<>();
		for(Map.Entry<ItemKey, Long> entry : List.copyOf(arrived.entrySet()))
		{
			if(max<=0)
				break;
			if(!matches.test(entry.getKey()))
				continue;
			long count = Math.min(max, entry.getValue());
			taken.put(entry.getKey(), count);
			max -= count;
			if(count==entry.getValue())
				arrived.remove(entry.getKey());
			else
				arrived.put(entry.getKey(), entry.getValue()-count);
		}
		if(!taken.isEmpty())
			host.setChanged();
		return taken;
	}

	long room(ItemKey key)
	{
		if(!located())
			return 0;
		long[] waiting = {0};
		for(CraftingComputerState computer : StorageNetwork.of(level, masterPos).getCraftingComputers())
			for(CraftingOrder order : computer.getOrders())
				order.expectFrom(masterPos, (matches, count) -> {
					if(matches.test(key))
						waiting[0] += count;
				});
		return Math.max(0, waiting[0]-arrived.getOrDefault(key, 0L));
	}

	private long receive(ItemKey key, long count, boolean simulate)
	{
		long taken = Math.min(count, room(key));
		if(taken>0&&!simulate)
		{
			arrived.merge(key, taken, Long::sum);
			host.setChanged();
		}
		return taken;
	}

	public StatusCode status()
	{
		if(!located())
			return StatusCode.SERVICE_UNAVAILABLE;
		if(StorageNetwork.of(level, masterPos).getCraftingComputers().isEmpty())
			return StatusCode.FAILED_DEPENDENCY;
		if(target()==null||source()==null)
			return StatusCode.SERVICE_UNAVAILABLE;
		return level.getGameTime()<processingUntil? StatusCode.PROCESSING: StatusCode.OK;
	}

	private void locate(MultiblockContext ctx)
	{
		level = ctx.level();
		masterPos = ctx.masterPos();
		topPos = ctx.toAbsolute(TOP);
		front = MultiblockOrientation.rotationFor(ctx.facing()).rotate(Direction.SOUTH);
	}

	@Override
	public void writeSyncData(CompoundTag tag)
	{
		int filled = 0;
		for(int slot = 0; slot<patterns.getSlots(); slot++)
			if(!patterns.getStackInSlot(slot).isEmpty())
				filled |= 1<<slot;
		tag.putInt(FILLED, filled);
	}

	@Override
	public void tick(MultiblockContext ctx)
	{
		locate(ctx);
		IItemHandler source = source();
		if(source==null)
			return;
		int left = ITEMS_PER_TICK;
		for(int slot = 0; slot<source.getSlots()&&left>0; slot++)
		{
			ItemStack stack = source.getStackInSlot(slot);
			if(stack.isEmpty())
				continue;
			long room = room(ItemKey.of(stack));
			if(room<=0)
				continue;
			ItemStack pulled = source.extractItem(slot, (int)Math.min(Math.min(room, left), stack.getCount()), false);
			if(!pulled.isEmpty())
			{
				arrived.merge(ItemKey.of(pulled), (long)pulled.getCount(), Long::sum);
				left -= pulled.getCount();
				host.setChanged();
			}
		}
	}

	@Nullable
	@Override
	public IItemHandler getItemHandler(BlockPos posInMultiblock, @Nullable Direction side)
	{
		return posInMultiblock.equals(BOTTOM)? input: null;
	}

	private final IItemHandler input = new IItemHandler()
	{
		@Override
		public int getSlots()
		{
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot)
		{
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
		{
			if(stack.isEmpty())
				return stack;
			long taken = receive(ItemKey.of(stack), stack.getCount(), simulate);
			return taken==0? stack: stack.copyWithCount(stack.getCount()-(int)taken);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate)
		{
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot)
		{
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack)
		{
			return true;
		}
	};

	@Override
	public InteractionResult use(MultiblockContext ctx, Player player, BlockPos posInMultiblock, Vec3 hit)
	{
		locate(ctx);
		if(player instanceof ServerPlayer serverPlayer)
			NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
							(id, inventory, p) -> new DataProcessorMenu(id, inventory, this, ctx.masterPos()),
							ctx.definition().partBlock().getName()),
					buf -> buf.writeBlockPos(ctx.masterPos()));
		return InteractionResult.CONSUME;
	}

	@Override
	public void onDisassemble(MultiblockContext ctx)
	{
		locate(ctx);
		BlockPos pos = ctx.masterPos();
		for(int slot = 0; slot<patterns.getSlots(); slot++)
		{
			ItemStack stack = patterns.getStackInSlot(slot);
			if(!stack.isEmpty())
			{
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
				patterns.setStackInSlot(slot, ItemStack.EMPTY);
			}
		}
		StorageNetwork network = StorageNetwork.of(level, pos);
		NetworkStorage storage = network.isTooLarge()||!network.getConflictingEras().isEmpty()? NetworkStorage.EMPTY:
				network.getStorage();
		arrived.forEach((key, count) -> {
			long left = count-storage.insert(key, count, false);
			while(left>0)
			{
				int stack = (int)Math.min(left, key.getMaxStackSize());
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), key.toStack(stack));
				left -= stack;
			}
		});
		arrived.clear();
	}

	@Override
	public void save(CompoundTag tag)
	{
		tag.put("patterns", patterns.serializeNBT());
		ListTag list = new ListTag();
		arrived.forEach((key, count) -> {
			CompoundTag entry = new CompoundTag();
			entry.put("item", key.toStack(1).save(new CompoundTag()));
			entry.putLong("count", count);
			list.add(entry);
		});
		tag.put("arrived", list);
	}

	@Override
	public void load(CompoundTag tag)
	{
		if(tag.contains("patterns"))
			patterns.deserializeNBT(tag.getCompound("patterns"));
		arrived.clear();
		for(Tag entry : tag.getList("arrived", Tag.TAG_COMPOUND))
		{
			CompoundTag compound = (CompoundTag)entry;
			ItemStack stack = ItemStack.of(compound.getCompound("item"));
			if(!stack.isEmpty())
				arrived.merge(ItemKey.of(stack), compound.getLong("count"), Long::sum);
		}
	}

	List<ProvidedPattern> provided()
	{
		List<ProvidedPattern> provided = new ArrayList<>();
		if(located())
			getPatterns().forEach((slot, pattern) -> provided.add(ProvidedPattern.abstractAt(pattern, masterPos, slot)));
		return provided;
	}
}

package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import com.muddykat.engineeredcomputation.era.EraMachine;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockContext;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockState;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockStateHost;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.storage.EraDataCell;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

public class CraftingComputerState implements MultiblockState, NetworkMember
{
	public static final int PATTERN_SLOTS = 8;
	public static final int MAX_ORDERS = 8;
	public static final int CRAFTS_PER_TICK = 2;
	public static final String WORKING = "working";
	public static final String FILLED = "filled";

	private final MultiblockStateHost host;
	private final ItemStackHandler patterns;
	private final List<CraftingOrder> orders = new ArrayList<>();
	private int nextId = 1;
	private boolean syncedWorking;
	@Nullable
	private Level level;
	@Nullable
	private BlockPos masterPos;

	public CraftingComputerState(MultiblockStateHost host)
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
						&&PatternFloppyItem.pattern(stack).mode()==Pattern.Mode.DIRECT;
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

	public ItemStackHandler getPatternSlots()
	{
		return patterns;
	}

	public List<Pattern> getPatterns()
	{
		List<Pattern> held = new ArrayList<>();
		for(int slot = 0; slot<patterns.getSlots(); slot++)
			if(PatternFloppyItem.holds(patterns.getStackInSlot(slot), Pattern.Mode.DIRECT))
				held.add(PatternFloppyItem.pattern(patterns.getStackInSlot(slot)));
		return held;
	}

	public List<CraftingOrder> getOrders()
	{
		return Collections.unmodifiableList(orders);
	}

	public boolean hasRoom()
	{
		return orders.size()<MAX_ORDERS;
	}

	@Nullable
	public BlockPos getMasterPos()
	{
		return masterPos;
	}

	@Nullable
	@Override
	public CraftingComputerState getCraftingComputer()
	{
		return this;
	}

	boolean start(CraftingPlan plan, NetworkStorage storage)
	{
		if(!plan.isPossible()||!hasRoom())
			return false;
		Map<ItemKey, Long> taken = new HashMap<>();
		for(Map.Entry<ItemKey, Long> entry : plan.fromStorage().entrySet())
		{
			long got = storage.extract(entry.getKey(), entry.getValue(), false);
			taken.put(entry.getKey(), got);
			if(got<entry.getValue())
			{
				taken.forEach((key, count) -> storage.insert(key, count, false));
				return false;
			}
		}
		orders.add(CraftingOrder.of(nextId++, plan));
		host.setChanged();
		return true;
	}

	public boolean cancel(int id)
	{
		for(CraftingOrder order : orders)
			if(order.id()==id)
			{
				order.cancel();
				return true;
			}
		return false;
	}

	private void giveBack(CraftingOrder order)
	{
		if(level==null||masterPos==null)
			return;
		for(ItemStack left : order.empty(storage()))
			Containers.dropItemStack(level, masterPos.getX()+0.5, masterPos.getY()+1, masterPos.getZ()+0.5, left);
	}

	private NetworkStorage storage()
	{
		StorageNetwork network = StorageNetwork.of(level, masterPos);
		return network.isTooLarge()||!network.getConflictingEras().isEmpty()? NetworkStorage.EMPTY: network.getStorage();
	}

	@Override
	public void tick(MultiblockContext ctx)
	{
		level = ctx.level();
		masterPos = ctx.masterPos();
		if(!orders.isEmpty()&&level instanceof ServerLevel serverLevel)
		{
			for(CraftingOrder order : List.copyOf(orders))
				if(order.isCancelled())
				{
					orders.remove(order);
					giveBack(order);
				}
			NetworkStorage storage = storage();
			int[] budget = {CRAFTS_PER_TICK};
			for(CraftingOrder order : List.copyOf(orders))
				if(order.tick(serverLevel, storage, budget))
					orders.remove(order);
			host.setChanged();
		}
		boolean working = !orders.isEmpty();
		if(working!=syncedWorking)
		{
			syncedWorking = working;
			host.syncToClient();
		}
	}

	@Override
	public void writeSyncData(CompoundTag tag)
	{
		tag.putBoolean(WORKING, !orders.isEmpty());
		int filled = 0;
		for(int slot = 0; slot<patterns.getSlots(); slot++)
			if(!patterns.getStackInSlot(slot).isEmpty())
				filled |= 1<<slot;
		tag.putInt(FILLED, filled);
	}

	public static InteractionResult insertIntoBay(MultiblockPartBlockEntity part, Player player, InteractionHand hand,
			Vec3 click)
	{
		MultiblockPartBlockEntity master = part.getMaster();
		if(master==null||master.getDefinition()!=ECMultiblocks.CRAFTING_COMPUTER)
			return InteractionResult.PASS;
		int slot = EraMachine.CRAFTING_COMPUTER.geometry().bayAt(master.getContext().toTemplatePoint(click));
		if(slot<0)
			return InteractionResult.PASS;
		Level level = master.getLevel();
		if(level==null||level.isClientSide)
			return (master.getClientSyncData().getInt(FILLED)&1<<slot)==0? InteractionResult.SUCCESS: InteractionResult.PASS;
		if(!(master.getState() instanceof CraftingComputerState state)||!state.patterns.getStackInSlot(slot).isEmpty()
				||!state.patterns.isItemValid(slot, player.getItemInHand(hand)))
			return InteractionResult.PASS;
		state.patterns.setStackInSlot(slot, EraDataCell.takeFromHand(player, hand));
		return InteractionResult.CONSUME;
	}

	@Override
	public InteractionResult use(MultiblockContext ctx, Player player, BlockPos posInMultiblock, Vec3 hit)
	{
		level = ctx.level();
		masterPos = ctx.masterPos();
		int bay = EraMachine.CRAFTING_COMPUTER.geometry().bayAt(hit);
		if(player.isSecondaryUseActive()&&bay>=0&&!patterns.getStackInSlot(bay).isEmpty())
		{
			player.setItemInHand(InteractionHand.MAIN_HAND, patterns.extractItem(bay, 1, false));
			return InteractionResult.CONSUME;
		}
		if(player instanceof ServerPlayer serverPlayer)
			NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
							(id, inventory, p) -> new CraftingComputerMenu(id, inventory, this, ctx.masterPos()),
							ctx.definition().partBlock().getName()),
					buf -> buf.writeBlockPos(ctx.masterPos()));
		return InteractionResult.CONSUME;
	}

	@Override
	public void onDisassemble(MultiblockContext ctx)
	{
		level = ctx.level();
		masterPos = ctx.masterPos();
		for(CraftingOrder order : List.copyOf(orders))
			giveBack(order);
		orders.clear();
		for(int slot = 0; slot<patterns.getSlots(); slot++)
		{
			ItemStack stack = patterns.getStackInSlot(slot);
			if(!stack.isEmpty())
			{
				Containers.dropItemStack(level, masterPos.getX(), masterPos.getY(), masterPos.getZ(), stack.copy());
				patterns.setStackInSlot(slot, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public void save(CompoundTag tag)
	{
		tag.put("patterns", patterns.serializeNBT());
		ListTag list = new ListTag();
		for(CraftingOrder order : orders)
			list.add(order.save());
		tag.put("orders", list);
		tag.putInt("next_id", nextId);
	}

	@Override
	public void load(CompoundTag tag)
	{
		if(tag.contains("patterns"))
			patterns.deserializeNBT(tag.getCompound("patterns"));
		orders.clear();
		for(Tag entry : tag.getList("orders", Tag.TAG_COMPOUND))
			orders.add(CraftingOrder.load((CompoundTag)entry));
		nextId = Math.max(1, tag.getInt("next_id"));
	}
}

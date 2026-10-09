package com.muddykat.engineeredcomputation.crafting;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class CraftingComputerMenu extends AbstractContainerMenu
{
	public static final int LEFT_X = 8;
	public static final int RIGHT_X = 150;
	public static final int BAYS_Y = 18;
	public static final int BAY_STEP = 20;
	public static final int COLUMN_BAYS = 4;
	public static final int DISPLAY_X = 30;
	public static final int DISPLAY_Y = 17;
	public static final int DISPLAY_WIDTH = 116;
	public static final int DISPLAY_HEIGHT = 79;
	public static final int INVENTORY_Y = 112;
	private static final double EXTRA_REACH = 4.0;

	private final BlockPos pos;
	@Nullable
	private final CraftingComputerState computer;
	private final ContainerData data;
	private final Player player;
	private List<OrderView> sentOrders;
	private int countdown;
	private static final int INTERVAL = 5;
	private List<OrderView> clientOrders = List.of();

	public CraftingComputerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf)
	{
		this(id, inventory, null, CraftingComputerState.createPatternSlots(() -> {}), buf.readBlockPos(),
				new SimpleContainerData(1));
	}

	CraftingComputerMenu(int id, Inventory inventory, CraftingComputerState computer, BlockPos pos)
	{
		this(id, inventory, computer, computer.getPatternSlots(), pos, new ContainerData()
		{
			@Override
			public int get(int index)
			{
				return computer.getOrders().size();
			}

			@Override
			public void set(int index, int value)
			{
			}

			@Override
			public int getCount()
			{
				return 1;
			}
		});
	}

	private CraftingComputerMenu(int id, Inventory inventory, @Nullable CraftingComputerState computer,
			IItemHandler bays, BlockPos pos, ContainerData data)
	{
		super(ECMenuTypes.CRAFTING_COMPUTER.get(), id);
		this.pos = pos;
		this.computer = computer;
		this.data = data;
		this.player = inventory.player;
		for(int i = 0; i<CraftingComputerState.PATTERN_SLOTS; i++)
			addSlot(new SlotItemHandler(bays, i, (i<COLUMN_BAYS? LEFT_X: RIGHT_X)+1, BAYS_Y+(i%COLUMN_BAYS)*BAY_STEP+1));
		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(new Slot(inventory, col+row*9+9, 8+col*18, INVENTORY_Y+row*18));
		for(int col = 0; col<9; col++)
			addSlot(new Slot(inventory, col, 8+col*18, INVENTORY_Y+58));
		addDataSlots(data);
	}

	public int orders()
	{
		return data.get(0);
	}

	public List<OrderView> getClientOrders()
	{
		return clientOrders;
	}

	void applyOrders(List<OrderView> orders)
	{
		clientOrders = orders;
	}

	@Override
	public void broadcastChanges()
	{
		super.broadcastChanges();
		if(computer==null||!(player instanceof ServerPlayer serverPlayer)||countdown-->0)
			return;
		countdown = INTERVAL;
		List<OrderView> orders = computer.getOrders().stream().map(CraftingOrder::view).toList();
		if(orders.equals(sentOrders))
			return;
		sentOrders = orders;
		PacketDistributor.sendToPlayer(serverPlayer, new CraftingComputerPayload(containerId, orders));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		int bays = CraftingComputerState.PATTERN_SLOTS;
		boolean moved = index<bays? moveItemStackTo(stack, bays, slots.size(), true): moveItemStackTo(stack, 0, bays, false);
		if(!moved)
			return ItemStack.EMPTY;
		slot.setChanged();
		return original;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.canInteractWithBlock(pos, EXTRA_REACH)
				&&(computer==null||player.level().getBlockEntity(pos)!=null);
	}
}

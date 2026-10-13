package com.muddykat.engineeredcomputation.pneumatic.autotube;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

/** A button click's id is the amount to set (1-64), or {@link #TOGGLE_SPLIT}. */
public class AutotubeMenu extends AbstractContainerMenu
{
	public static final int TOGGLE_SPLIT = 100;
	private static final int DATA = 2;

	private final BlockPos pos;
	@Nullable
	private final AutotubeBlockEntity autotube;
	private final ContainerData data;

	public AutotubeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf)
	{
		this(id, buf.readBlockPos(), null, new SimpleContainerData(DATA));
	}

	public AutotubeMenu(int id, BlockPos pos, @Nullable AutotubeBlockEntity autotube, ContainerData data)
	{
		super(ECMenuTypes.AUTOTUBE.get(), id);
		this.pos = pos;
		this.autotube = autotube;
		this.data = data;
		addDataSlots(data);
	}

	public static void open(ServerPlayer player, AutotubeBlockEntity autotube)
	{
		ContainerData data = new ContainerData()
		{
			@Override
			public int get(int index)
			{
				return index==0? autotube.getAmount(): autotube.splits()? 1: 0;
			}

			@Override
			public void set(int index, int value)
			{
			}

			@Override
			public int getCount()
			{
				return DATA;
			}
		};
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new AutotubeMenu(id, autotube.getBlockPos(),
				autotube, data), autotube.getBlockState().getBlock().getName()),
				buf -> buf.writeBlockPos(autotube.getBlockPos()));
	}

	public int getAmount()
	{
		return Math.max(1, data.get(0));
	}

	public boolean splits()
	{
		return data.get(1)!=0;
	}

	@Override
	public boolean clickMenuButton(Player player, int id)
	{
		if(id==TOGGLE_SPLIT)
		{
			if(autotube!=null)
				autotube.setSplit(!autotube.splits());
			return true;
		}
		if(id<1||id>AutotubeBlockEntity.MAX_AMOUNT)
			return false;
		if(autotube!=null)
			autotube.setAmount(id);
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.level().getBlockEntity(pos) instanceof AutotubeBlockEntity&&player.canInteractWithBlock(pos, 4.0);
	}
}

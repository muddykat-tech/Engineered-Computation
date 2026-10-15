package com.muddykat.engineeredcomputation.remote;

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

public class StorageAnalyzerMenu extends AbstractContainerMenu
{
	public static final int CYCLE_PRIORITY = 0;
	public static final int NOTHING = 0;
	public static final int INVENTORY = 1;
	public static final int PNEUMATIC = 2;
	private static final int DATA = 2;

	private final BlockPos pos;
	@Nullable
	private final StorageAnalyzerBlockEntity analyzer;
	private final ContainerData data;

	public StorageAnalyzerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf)
	{
		this(id, buf.readBlockPos(), null, new SimpleContainerData(DATA));
	}

	public StorageAnalyzerMenu(int id, BlockPos pos, @Nullable StorageAnalyzerBlockEntity analyzer,
			ContainerData data)
	{
		super(ECMenuTypes.STORAGE_ANALYZER.get(), id);
		this.pos = pos;
		this.analyzer = analyzer;
		this.data = data;
		addDataSlots(data);
	}

	public static void open(ServerPlayer player, StorageAnalyzerBlockEntity analyzer)
	{
		ContainerData data = new ContainerData()
		{
			@Override
			public int get(int index)
			{
				return index==0? analyzer.getPriority().ordinal()
						: analyzer.station()!=null? PNEUMATIC: analyzer.canRequest()? INVENTORY: NOTHING;
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
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new StorageAnalyzerMenu(id,
				analyzer.getBlockPos(), analyzer, data), analyzer.getBlockState().getBlock().getName()),
				buf -> buf.writeBlockPos(analyzer.getBlockPos()));
	}

	public StorageAnalyzerBlockEntity.Priority getPriority()
	{
		StorageAnalyzerBlockEntity.Priority[] priorities = StorageAnalyzerBlockEntity.Priority.values();
		return priorities[Math.floorMod(data.get(0), priorities.length)];
	}

	/** What it faces: {@link #NOTHING}, {@link #INVENTORY} or a Receiving Station ({@link #PNEUMATIC}). */
	public int facing()
	{
		return data.get(1);
	}

	@Override
	public boolean clickMenuButton(Player player, int id)
	{
		if(id!=CYCLE_PRIORITY)
			return false;
		if(analyzer!=null)
			analyzer.setPriority(analyzer.getPriority().next());
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
		return player.level().getBlockEntity(pos) instanceof StorageAnalyzerBlockEntity
				&&player.canInteractWithBlock(pos, 4.0);
	}
}

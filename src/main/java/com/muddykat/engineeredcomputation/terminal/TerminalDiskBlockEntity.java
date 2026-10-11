package com.muddykat.engineeredcomputation.terminal;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECTerminals;
import com.muddykat.engineeredcomputation.storage.CellInventory;
import com.muddykat.engineeredcomputation.storage.CellStatus;
import com.muddykat.engineeredcomputation.storage.EraDataCell;
import com.muddykat.engineeredcomputation.storage.StorageHost;

public class TerminalDiskBlockEntity extends CraftingTerminalBlockEntity implements NetworkMember
{
	public static final int SLOTS = 2;

	private final ItemStackHandler disks = createDiskSlots(this::slotsChanged);
	private int syncedStatus = -1;
	private final StorageHost host = new StorageHost()
	{
		@Override
		public ComputerEra getEra()
		{
			return ComputerEra.FLOPPY;
		}

		@Override
		public boolean needsProcessor()
		{
			return false;
		}

		@Override
		public List<CellInventory> getCells()
		{
			List<CellInventory> cells = new ArrayList<>();
			for(int slot = 0; slot<SLOTS; slot++)
			{
				CellInventory cell = CellInventory.of(disks.getStackInSlot(slot),
						TerminalDiskBlockEntity.this::diskContentsChanged);
				if(cell!=null)
					cells.add(cell);
			}
			return cells;
		}
	};

	public TerminalDiskBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECTerminals.TERMINAL_DISKS.get(), pos, state);
	}

	public static ItemStackHandler createDiskSlots(Runnable onChanged)
	{
		return new ItemStackHandler(SLOTS)
		{
			@Override
			public boolean isItemValid(int slot, ItemStack stack)
			{
				return EraDataCell.isTerminalDisk(stack);
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

	public ItemStackHandler getDisks()
	{
		return disks;
	}

	@Nullable
	@Override
	public StorageHost getStorage()
	{
		return hasDisk()? host: null;
	}

	@Nullable
	@Override
	public ComputerEra getEra()
	{
		return hasDisk()? ComputerEra.FLOPPY: null;
	}

	@Override
	public int powerCost(Level level, BlockPos pos)
	{
		return ECConfig.cost(ECConfig.terminalKey(ComputerEra.FLOPPY));
	}

	private boolean hasDisk()
	{
		for(int slot = 0; slot<SLOTS; slot++)
			if(!disks.getStackInSlot(slot).isEmpty())
				return true;
		return false;
	}

	public void dropDisks(Level level, BlockPos pos)
	{
		for(int slot = 0; slot<SLOTS; slot++)
		{
			ItemStack disk = disks.getStackInSlot(slot);
			if(!disk.isEmpty())
			{
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), disk.copy());
				disks.setStackInSlot(slot, ItemStack.EMPTY);
			}
		}
	}

	private void diskContentsChanged()
	{
		setChanged();
		if(CellStatus.pack(disks)!=syncedStatus)
			disksChanged();
	}

	private void slotsChanged()
	{
		disksChanged();
		if(level!=null&&!level.isClientSide)
			StorageNetwork.changed(level);
	}

	private void disksChanged()
	{
		setChanged();
		if(level!=null&&!level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.put("disks", disks.serializeNBT(registries));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		if(tag.contains("disks"))
			disks.deserializeNBT(registries, tag.getCompound("disks"));
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		syncedStatus = CellStatus.pack(disks);
		return saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}
}

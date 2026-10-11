package com.muddykat.engineeredcomputation.multiblock.era;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.catalogue.CardDeckItem;
import com.muddykat.engineeredcomputation.catalogue.Catalogue;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockContext;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockState;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockStateHost;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.storage.Archivist;
import com.muddykat.engineeredcomputation.storage.CellInventory;
import com.muddykat.engineeredcomputation.storage.CellStatus;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.EraDataCell;
import com.muddykat.engineeredcomputation.storage.StorageHost;
import com.muddykat.engineeredcomputation.storage.StorageRole;
import com.muddykat.engineeredcomputation.terminal.CraftingGrid;
import com.muddykat.engineeredcomputation.terminal.TerminalFeatures;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public class EraDriveState implements MultiblockState, NetworkMember, StorageHost
{
	public static final String FILLED_SLOTS = "filled_slots";
	public static final String CELL_STATUS = "cell_status";
	public static final String CELL_TIERS = "cell_tiers";

	public static final int STORAGE_BAY_SLOTS = 9;

	private final EraDrive drive;
	private final ComputerEra era;
	private final MultiblockStateHost host;
	private final ItemStackHandler cellSlots;
	@Nullable
	private final SimpleContainer storageBay;
	@Nullable
	private final CraftingGrid craftingGrid;
	private int syncedStatus = -1;
	private int archiveCountdown;

	public static final int ARCHIVE_TICKS = 10;
	public static final int ARCHIVE_ITEMS = 128;

	public EraDriveState(EraDrive drive, MultiblockStateHost host)
	{
		this.drive = drive;
		this.era = drive.era();
		this.host = host;
		this.cellSlots = createCellSlots(drive, host::syncToClient);
		this.storageBay = drive.geometry().storageZones().isEmpty()? null: new SimpleContainer(STORAGE_BAY_SLOTS)
		{
			@Override
			public void setChanged()
			{
				super.setChanged();
				host.setChanged();
			}
		};
		this.craftingGrid = !drive.geometry().terminalZones().isEmpty()&&TerminalFeatures.of(era).crafting()
				? new CraftingGrid(host::setChanged): null;
	}

	static ItemStackHandler createCellSlots(EraDrive drive, Runnable onChanged)
	{
		ComputerEra era = drive.era();
		return new ItemStackHandler(drive.slots())
		{
			@Override
			public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag)
			{
				ItemStackHandler saved = new ItemStackHandler();
				saved.deserializeNBT(registries, tag);
				for(int i = 0; i<getSlots(); i++)
					stacks.set(i, i<saved.getSlots()? saved.getStackInSlot(i): ItemStack.EMPTY);
				for(int i = getSlots(); i<saved.getSlots(); i++)
					if(!saved.getStackInSlot(i).isEmpty())
						EngineeredComputation.LOGGER.warn("Dropped {} from a {} drive slot that no longer exists",
								saved.getStackInSlot(i), drive.getSerializedName());
				onLoad();
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack)
			{
				return fitsBay(stack, era);
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

	public static boolean fitsBay(ItemStack stack, ComputerEra era)
	{
		return era==ComputerEra.PUNCHCARD? stack.getItem() instanceof CardDeckItem: EraDataCell.isCellOfEra(stack, era);
	}

	public EraDrive getDrive()
	{
		return drive;
	}

	@Override
	public int powerCost(Level level, BlockPos pos)
	{
		return ECConfig.cost(ECConfig.costKey(drive));
	}

	@Override
	public boolean chargedPerBlock()
	{
		return false;
	}

	@Override
	public ComputerEra getEra()
	{
		return era;
	}

	@Nullable
	@Override
	public StorageHost getStorage()
	{
		return era==ComputerEra.PUNCHCARD? null: this;
	}

	@Override
	public boolean isProcessor()
	{
		return drive.isProcessor();
	}

	@Override
	public List<CellInventory> getCells()
	{
		List<CellInventory> cells = new ArrayList<>(cellSlots.getSlots());
		for(int i = 0; i<cellSlots.getSlots(); i++)
		{
			CellInventory cell = CellInventory.of(cellSlots.getStackInSlot(i), this::cellContentsChanged);
			if(cell!=null)
				cells.add(cell);
		}
		return cells;
	}

	private void cellContentsChanged()
	{
		host.setChanged();
		if(CellStatus.pack(cellSlots)!=syncedStatus)
			host.syncToClient();
	}

	public ItemStackHandler getCellSlots()
	{
		return cellSlots;
	}

	@Nullable
	public Container getStorageBay()
	{
		return storageBay;
	}

	@Override
	public InteractionResult use(MultiblockContext ctx, Player player, BlockPos posInMultiblock, Vec3 hit)
	{
		int bay = drive.geometry().bayAt(ctx.toTemplatePoint(player.getEyePosition()), hit,
				slot -> !cellSlots.getStackInSlot(slot).isEmpty());
		if(player.isSecondaryUseActive()&&bay>=0&&!cellSlots.getStackInSlot(bay).isEmpty())
		{
			player.setItemInHand(InteractionHand.MAIN_HAND, cellSlots.extractItem(bay, 1, false));
			return InteractionResult.CONSUME;
		}
		if(player instanceof ServerPlayer serverPlayer)
		{
			if(drive.geometry().isTerminal(hit))
			{
				TerminalMenu.open(serverPlayer, ctx.toAbsolute(posInMultiblock),
						ctx.definition().partBlock().getName(), TerminalFeatures.of(era), craftingGrid);
				return InteractionResult.CONSUME;
			}
			BlockPos masterPos = ctx.masterPos();
			if(storageBay!=null&&drive.geometry().isStorage(hit))
			{
				serverPlayer.openMenu(new SimpleMenuProvider(
						(id, inventory, p) -> new DispenserMenu(id, inventory, storageBay)
						{
							@Override
							public boolean stillValid(Player player)
							{
								return EraDriveMenu.isDriveInReach(player, masterPos);
							}
						},
						Component.translatable("container.engineered_computation.storage_bay")));
				return InteractionResult.CONSUME;
			}
			EraDriveMenu.openDrive(serverPlayer, drive, cellSlots, masterPos, ctx.definition().partBlock().getName());
		}
		return InteractionResult.CONSUME;
	}

	public static InteractionResult insertIntoBay(
			MultiblockPartBlockEntity part, Player player, InteractionHand hand, Vec3 click)
	{
		MultiblockPartBlockEntity master = part.getMaster();
		EraDrive drive = master==null? null: ECMultiblocks.driveOf(master.getDefinition());
		if(drive==null||!fitsBay(player.getItemInHand(hand), drive.era()))
			return InteractionResult.PASS;
		Level level = master.getLevel();
		if(level==null)
			return InteractionResult.PASS;
		EraDriveState state = null;
		IntPredicate filled;
		if(level.isClientSide)
		{
			int synced = master.getClientSyncData().getInt(FILLED_SLOTS);
			filled = slot -> (synced&1<<slot)!=0;
		}
		else if(master.getState() instanceof EraDriveState driveState)
		{
			state = driveState;
			filled = slot -> !driveState.cellSlots.getStackInSlot(slot).isEmpty();
		}
		else
			return InteractionResult.PASS;
		MultiblockContext ctx = master.getContext();
		int slot = drive.geometry().bayAt(ctx.toTemplatePoint(player.getEyePosition()), ctx.toTemplatePoint(click),
				filled);
		if(slot<0||filled.test(slot))
			return InteractionResult.PASS;
		if(level.isClientSide)
			return InteractionResult.SUCCESS;
		state.cellSlots.setStackInSlot(slot, EraDataCell.takeFromHand(player, hand));
		return InteractionResult.CONSUME;
	}

	@Override
	public void tick(MultiblockContext ctx)
	{
		if(era==ComputerEra.PUNCHCARD)
		{
			if(--archiveCountdown>0)
				return;
			archiveCountdown = Catalogue.RETALLY_TICKS;
			if(Catalogue.retally(ctx.level(), ctx.masterPos(), cellSlots))
				host.setChanged();
			return;
		}
		if(StorageRole.of(era)!=StorageRole.BULK||!ECConfig.tapeArchiving()||--archiveCountdown>0)
			return;
		archiveCountdown = ARCHIVE_TICKS;
		List<CellInventory> reels = getCells();
		if(reels.isEmpty())
			return;
		StorageNetwork network = StorageNetwork.of(ctx.level(), ctx.masterPos());
		if(network.isTooLarge()||!network.getConflictingEras().isEmpty()||!network.isPowered(ctx.level()))
			return;
		Archivist.run(network.getStorage(), reels, ARCHIVE_ITEMS);
	}

	@Override
	public void onDisassemble(MultiblockContext ctx)
	{
		BlockPos masterPos = ctx.masterPos();
		for(int i = 0; i<cellSlots.getSlots(); i++)
		{
			ItemStack stack = cellSlots.getStackInSlot(i);
			if(!stack.isEmpty())
			{
				Containers.dropItemStack(ctx.level(), masterPos.getX(), masterPos.getY(), masterPos.getZ(), stack.copy());
				cellSlots.setStackInSlot(i, ItemStack.EMPTY);
			}
		}
		if(storageBay!=null)
			Containers.dropContents(ctx.level(), masterPos, storageBay);
		if(craftingGrid!=null)
			craftingGrid.dropContents(ctx.level(), masterPos);
	}

	@Nullable
	@Override
	public IItemHandler getItemHandler(BlockPos posInMultiblock, @Nullable Direction side)
	{
		if(storageBay!=null&&drive.geometry().hasStorageAt(posInMultiblock))
			return new InvWrapper(storageBay);
		return cellSlots;
	}

	@Override
	public void writeSyncData(CompoundTag tag)
	{
		int filled = 0;
		for(int i = 0; i<cellSlots.getSlots(); i++)
			if(!cellSlots.getStackInSlot(i).isEmpty())
				filled |= 1<<i;
		tag.putInt(FILLED_SLOTS, filled);
		int tiers = 0;
		for(int i = 0; i<cellSlots.getSlots(); i++)
			tiers |= (tierOf(cellSlots.getStackInSlot(i))+1)<<(4*i);
		tag.putInt(CELL_TIERS, tiers);
		syncedStatus = CellStatus.pack(cellSlots);
		tag.putInt(CELL_STATUS, syncedStatus);
	}

	private int tierOf(ItemStack stack)
	{
		if(era==ComputerEra.PUNCHCARD)
			return ECCatalogue.tierOf(stack.getItem());
		List<? extends Supplier<EraDataCell>> tiers = ECDataCells.cellsOf(era);
		for(int tier = 0; tier<tiers.size(); tier++)
			if(stack.is(tiers.get(tier).get()))
				return tier;
		return -1;
	}

	@Override
	public void save(CompoundTag tag, HolderLookup.Provider registries)
	{
		tag.put("cells", cellSlots.serializeNBT(registries));
		if(storageBay!=null)
			tag.put("storage_bay", ContainerHelper.saveAllItems(new CompoundTag(), storageBay.getItems(), registries));
		if(craftingGrid!=null)
			tag.put("crafting_grid", craftingGrid.save(registries));
	}

	@Override
	public void load(CompoundTag tag, HolderLookup.Provider registries)
	{
		if(tag.contains("cells"))
			cellSlots.deserializeNBT(registries, tag.getCompound("cells"));
		if(storageBay!=null&&tag.contains("storage_bay"))
			ContainerHelper.loadAllItems(tag.getCompound("storage_bay"), storageBay.getItems(), registries);
		if(craftingGrid!=null&&tag.contains("crafting_grid"))
			craftingGrid.load(tag.getCompound("crafting_grid"), registries);
	}
}

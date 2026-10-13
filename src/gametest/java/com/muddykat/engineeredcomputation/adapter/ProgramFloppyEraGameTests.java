package com.muddykat.engineeredcomputation.adapter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

@TestCategory("adapters")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class ProgramFloppyEraGameTests
{
	private ProgramFloppyEraGameTests()
	{
	}

	private static DataAdapterBlockEntity exporter(GameTestHelper helper, ComputerEra era)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		BlockPos port = StorageGameTests.atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		level.setBlockAndUpdate(port, ECCables.block().get().defaultBlockState());
		BlockPos adapterPos = port.above();
		level.setBlockAndUpdate(adapterPos, ECAdapters.of(era).get(AdapterKind.EXPORTER).get().defaultBlockState()
				.setValue(DataAdapterBlock.FACING, Direction.UP));
		level.setBlockAndUpdate(adapterPos.above(), Blocks.CHEST.defaultBlockState());
		return (DataAdapterBlockEntity)level.getBlockEntity(adapterPos);
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void lowerErasRefuseProgramFloppies(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = exporter(helper, ComputerEra.DRUM);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		DataAdapterMenu menu = (DataAdapterMenu)adapter.createMenu(1, player.getInventory(), player);
		StorageGameTests.check(helper, !menu.takesPrograms(), "drum era should not take programs");
		menu.setCarried(new ItemStack(ECAdapters.PROGRAM_FLOPPY.get()));
		menu.clicked(DataAdapterMenu.FILTER_SLOT, 0, ClickType.PICKUP, player);
		StorageGameTests.check(helper, menu.getFilter().isEmpty(), "filter should be empty");
		StorageGameTests.check(helper, menu.getCarried().is(ECAdapters.PROGRAM_FLOPPY.get()), "program floppy should still be carried");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyEraTakesProgramFloppies(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = exporter(helper, ComputerEra.FLOPPY);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		DataAdapterMenu menu = (DataAdapterMenu)adapter.createMenu(1, player.getInventory(), player);
		StorageGameTests.check(helper, menu.takesPrograms(), "floppy era should take programs");
		menu.setCarried(new ItemStack(ECAdapters.PROGRAM_FLOPPY.get()));
		menu.clicked(DataAdapterMenu.FILTER_SLOT, 0, ClickType.PICKUP, player);
		StorageGameTests.check(helper, menu.getFilter().is(ECAdapters.PROGRAM_FLOPPY.get()), "filter should have program floppy");
		StorageGameTests.check(helper, menu.getCarried().isEmpty(), "program floppy should be in filter");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aProgramLeftInALowerEraDoesNothing(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = exporter(helper, ComputerEra.DRUM);
		StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage().insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 10, false);
		adapter.setFilter(new ItemStack(ECAdapters.PROGRAM_FLOPPY.get()));
		StorageGameTests.check(helper, adapter.transfer()==AdapterStatus.NOT_FOUND, "transfer should not find anything");
		helper.succeed();
	}
}

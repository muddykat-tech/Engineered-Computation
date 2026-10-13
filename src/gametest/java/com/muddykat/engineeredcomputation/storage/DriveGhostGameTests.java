package com.muddykat.engineeredcomputation.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveMenu;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECDataCells;

@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class DriveGhostGameTests
{
	private DriveGhostGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void eachDriveSlotGhostsWhatItTakes(GameTestHelper helper)
	{
		for(EraDrive drive : EraDrive.values())
		{
			EraDriveMenu menu = menu(helper, drive);
			StorageGameTests.check(helper, menu.getCellSlotCount()==drive.slots(),
					"Drive "+drive.name()+" has wrong slot count");
			for(int i = 0; i<drive.slots(); i++)
			{
				Item expected = drive.isCompactSlot(i)? compact(): drive.era()==ComputerEra.PUNCHCARD
						? ECCatalogue.decks().get(0).get(): firstCell(drive.era());
				StorageGameTests.check(helper, menu.getGhost(i).is(expected),
						"Drive "+drive.name()+" slot "+i+" has wrong ghost item");
			}
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyProcessorGhostsAFloppyThenTwoSmallFloppies(GameTestHelper helper)
	{
		EraDriveMenu menu = menu(helper, EraDrive.FLOPPY_PROCESSOR);
		StorageGameTests.check(helper, menu.getGhost(0).is(firstCell(ComputerEra.FLOPPY)),
				"First slot should ghost a floppy");
		StorageGameTests.check(helper, menu.getGhost(1).is(compact()), "Second slot should ghost a 3.5-inch floppy");
		StorageGameTests.check(helper, menu.getGhost(2).is(compact()), "Third slot should ghost a 3.5-inch floppy");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void compactSlotsNameThreeAndAHalfInchFloppies(GameTestHelper helper)
	{
		EraDriveMenu menu = menu(helper, EraDrive.FLOPPY_PROCESSOR);
		StorageGameTests.check(helper, !name(menu.getAcceptedCells(0)).equals(name(menu.getAcceptedCells(1))),
				"Top slot and small slots should have different names");
		StorageGameTests.check(helper, name(menu.getAcceptedCells(1)).equals(name(menu.getAcceptedCells(2))),
				"Small slots should have the same name");

		EraDriveMenu bay = menu(helper, EraDrive.COMPACT_FLOPPY_BAY);
		StorageGameTests.check(helper, name(bay.getAcceptedCells(0)).equals(name(menu.getAcceptedCells(1))),
				"Bay slots should have the same name as processor small slots");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aGhostIsAFreshStack(GameTestHelper helper)
	{
		EraDriveMenu menu = menu(helper, EraDrive.FLOPPY_BANK);
		ItemStack ghost = menu.getGhost(0);
		StorageGameTests.check(helper, !ghost.isEmpty(), "Ghost should not be empty");
		StorageGameTests.check(helper, ghost.getCount()==1, "Ghost should have count 1");
		helper.succeed();
	}

	private static EraDriveMenu menu(GameTestHelper helper, EraDrive drive)
	{
		return EraDriveMenu.forDrive(1, FakePlayerFactory.getMinecraft(helper.getLevel()).getInventory(), drive,
				new ItemStackHandler(drive.slots()), BlockPos.ZERO);
	}

	private static Item compact()
	{
		return ECDataCells.terminalDisks().get(0).get();
	}

	private static Item firstCell(ComputerEra era)
	{
		return ECDataCells.cellsOf(era).get(0).get();
	}

	private static String name(Component component)
	{
		return component.getString();
	}
}

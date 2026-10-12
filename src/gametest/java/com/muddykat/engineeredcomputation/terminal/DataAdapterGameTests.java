package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.AdapterStatus;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlock;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlockEntity;
import com.muddykat.engineeredcomputation.adapter.Program;
import com.muddykat.engineeredcomputation.adapter.ProgramEntry;
import com.muddykat.engineeredcomputation.adapter.ProgramFloppyItem;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

@TestCategory("adapters")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class DataAdapterGameTests
{
	private DataAdapterGameTests()
	{
	}

	private static DataAdapterBlockEntity setUp(GameTestHelper helper, ComputerEra era, AdapterKind kind, boolean cable)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		BlockPos port = StorageGameTests.atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		if(cable)
			level.setBlockAndUpdate(port, ECCables.block().get().defaultBlockState());
		BlockPos adapterPos = port.above();
		level.setBlockAndUpdate(adapterPos, ECAdapters.of(era).get(kind).get().defaultBlockState()
				.setValue(DataAdapterBlock.FACING, Direction.UP));
		level.setBlockAndUpdate(adapterPos.above(), Blocks.CHEST.defaultBlockState());
		return (DataAdapterBlockEntity)level.getBlockEntity(adapterPos);
	}

	private static int inChest(GameTestHelper helper, DataAdapterBlockEntity adapter, Item item)
	{
		ChestBlockEntity chest = (ChestBlockEntity)helper.getLevel().getBlockEntity(adapter.getBlockPos().above());
		int count = 0;
		for(int i = 0; i<chest.getContainerSize(); i++)
			if(chest.getItem(i).is(item))
				count += chest.getItem(i).getCount();
		return count;
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void exporterMovesOnlyItsFilterAtItsEraSpeed(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.DRUM, AdapterKind.EXPORTER, true);
		NetworkStorage network = StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage();
		network.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 3, false);
		network.insert(ItemKey.of(new ItemStack(Items.DIRT)), 5, false);
		adapter.setFilter(new ItemStack(Items.COBBLESTONE));
		AdapterStatus status = adapter.transfer();
		StorageGameTests.check(helper, status==AdapterStatus.OK, "First transfer should succeed");
		StorageGameTests.check(helper, inChest(helper, adapter, Items.COBBLESTONE)==1, "Chest should hold 1 cobblestone");
		status = adapter.transfer();
		StorageGameTests.check(helper, status==AdapterStatus.OK, "Second transfer should succeed");
		status = adapter.transfer();
		StorageGameTests.check(helper, status==AdapterStatus.OK, "Third transfer should succeed");
		StorageGameTests.check(helper, inChest(helper, adapter, Items.COBBLESTONE)==3, "Chest should hold 3 cobblestone");
		StorageGameTests.check(helper, network.getAmount(ItemKey.of(new ItemStack(Items.DIRT)))==5L, "Network should still have 5 dirt");
		status = adapter.transfer();
		StorageGameTests.check(helper, status==AdapterStatus.NOT_FOUND, "Fourth transfer should fail with NOT_FOUND");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void emptyExporterFindsNothing(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.DRUM, AdapterKind.EXPORTER, true);
		NetworkStorage network = StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage();
		network.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 4, false);
		AdapterStatus status = adapter.transfer();
		StorageGameTests.check(helper, status==AdapterStatus.NOT_FOUND, "Transfer should fail with NOT_FOUND");
		StorageGameTests.check(helper, inChest(helper, adapter, Items.COBBLESTONE)==0, "Chest should hold no cobblestone");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void emptyImporterTakesEverything(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.FLOPPY, AdapterKind.IMPORTER, true);
		ChestBlockEntity chest = (ChestBlockEntity)helper.getLevel().getBlockEntity(adapter.getBlockPos().above());
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
		chest.setItem(1, new ItemStack(Items.DIRT, 3));
		AdapterStatus status = adapter.transfer();
		StorageGameTests.check(helper, status==AdapterStatus.OK, "Transfer should succeed");
		StorageGameTests.check(helper, chest.getItem(0).isEmpty(), "Chest should hold no cobblestone");
		StorageGameTests.check(helper, chest.getItem(1).isEmpty(), "Chest should hold no dirt");
		NetworkStorage network = StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage();
		StorageGameTests.check(helper, network.getAmount(ItemKey.of(new ItemStack(Items.COBBLESTONE)))==20L, "Network should hold 20 cobblestone");
		StorageGameTests.check(helper, network.getAmount(ItemKey.of(new ItemStack(Items.DIRT)))==3L, "Network should hold 3 dirt");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void noCableMeansServiceUnavailable(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.DRUM, AdapterKind.EXPORTER, false);
		adapter.setFilter(new ItemStack(Items.COBBLESTONE));
		AdapterStatus status = adapter.transfer();
		StorageGameTests.check(helper, status==AdapterStatus.SERVICE_UNAVAILABLE, "Transfer should fail with SERVICE_UNAVAILABLE");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void fullChestIsInsufficientStorage(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.FLOPPY, AdapterKind.EXPORTER, true);
		NetworkStorage network = StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage();
		network.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 10, false);
		ChestBlockEntity chest = (ChestBlockEntity)helper.getLevel().getBlockEntity(adapter.getBlockPos().above());
		for(int i = 0; i<27; i++)
			chest.setItem(i, new ItemStack(Items.DIRT, 64));
		adapter.setFilter(new ItemStack(Items.COBBLESTONE));
		AdapterStatus status = adapter.transfer();
		StorageGameTests.check(helper, status==AdapterStatus.INSUFFICIENT_STORAGE, "Transfer should fail with INSUFFICIENT_STORAGE");
		StorageGameTests.check(helper, network.getAmount(ItemKey.of(new ItemStack(Items.COBBLESTONE)))==10L, "Network should still hold 10 cobblestone");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void programFloppyFiltersByTag(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.FLOPPY, AdapterKind.EXPORTER, true);
		NetworkStorage network = StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage();
		network.insert(ItemKey.of(new ItemStack(Items.IRON_INGOT)), 5, false);
		network.insert(ItemKey.of(new ItemStack(Items.GOLD_INGOT)), 5, false);
		network.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 5, false);
		ItemStack floppy = new ItemStack(ECAdapters.PROGRAM_FLOPPY.get());
		ProgramFloppyItem.setProgram(floppy, Program.EMPTY.with(0,
				ProgramEntry.of(TagKey.create(Registries.ITEM, ResourceLocation.parse("forge:ingots")))));
		adapter.setFilter(floppy);
		AdapterStatus status = adapter.transfer();
		StorageGameTests.check(helper, status==AdapterStatus.OK, "Transfer should succeed");
		StorageGameTests.check(helper, inChest(helper, adapter, Items.IRON_INGOT)==5, "Chest should hold 5 iron ingots");
		StorageGameTests.check(helper, inChest(helper, adapter, Items.GOLD_INGOT)==5, "Chest should hold 5 gold ingots");
		StorageGameTests.check(helper, inChest(helper, adapter, Items.COBBLESTONE)==0, "Chest should hold no cobblestone");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void blockListExporterSendsEverythingButItsFilter(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.FLOPPY, AdapterKind.EXPORTER, true);
		NetworkStorage network = StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage();
		network.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 5, false);
		network.insert(ItemKey.of(new ItemStack(Items.DIRT)), 5, false);
		adapter.setFilter(new ItemStack(Items.COBBLESTONE));
		StorageGameTests.check(helper, !adapter.isBlocking(), "an adapter should start with an allow list");
		adapter.setBlocking(true);
		AdapterStatus last = drain(adapter);
		StorageGameTests.check(helper, inChest(helper, adapter, Items.DIRT)==5, "Chest should hold all 5 dirt");
		StorageGameTests.check(helper, inChest(helper, adapter, Items.COBBLESTONE)==0,
				"the blocked cobblestone was exported");
		StorageGameTests.check(helper, last==AdapterStatus.NOT_FOUND,
				"with only blocked items left the exporter should find nothing, not "+last);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void emptyBlockListExporterSendsEverything(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.FLOPPY, AdapterKind.EXPORTER, true);
		NetworkStorage network = StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage();
		network.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 3, false);
		network.insert(ItemKey.of(new ItemStack(Items.DIRT)), 2, false);
		adapter.setBlocking(true);
		drain(adapter);
		StorageGameTests.check(helper, inChest(helper, adapter, Items.COBBLESTONE)==3
						&&inChest(helper, adapter, Items.DIRT)==2,
				"an empty block list blocks nothing, so everything should be exported");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void blockListImporterLeavesItsFilter(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.FLOPPY, AdapterKind.IMPORTER, true);
		ChestBlockEntity chest = (ChestBlockEntity)helper.getLevel().getBlockEntity(adapter.getBlockPos().above());
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
		chest.setItem(1, new ItemStack(Items.DIRT, 3));
		adapter.setFilter(new ItemStack(Items.DIRT));
		adapter.setBlocking(true);
		drain(adapter);
		NetworkStorage network = StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage();
		StorageGameTests.check(helper, network.getAmount(ItemKey.of(new ItemStack(Items.COBBLESTONE)))==20L,
				"Network should hold all 20 cobblestone");
		StorageGameTests.check(helper, inChest(helper, adapter, Items.DIRT)==3, "the blocked dirt was imported");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void blockListProgramFloppyBlocksItsEntries(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.FLOPPY, AdapterKind.EXPORTER, true);
		NetworkStorage network = StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage();
		network.insert(ItemKey.of(new ItemStack(Items.IRON_INGOT)), 5, false);
		network.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 5, false);
		ItemStack floppy = new ItemStack(ECAdapters.PROGRAM_FLOPPY.get());
		ProgramFloppyItem.setProgram(floppy, Program.EMPTY.with(0,
				ProgramEntry.of(TagKey.create(Registries.ITEM, ResourceLocation.parse("forge:ingots")))));
		adapter.setFilter(floppy);
		adapter.setBlocking(true);
		drain(adapter);
		StorageGameTests.check(helper, inChest(helper, adapter, Items.COBBLESTONE)==5,
				"Chest should hold 5 cobblestone");
		StorageGameTests.check(helper, inChest(helper, adapter, Items.IRON_INGOT)==0,
				"the program's blocked ingots were exported");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theBlockListIsSaved(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.DRUM, AdapterKind.EXPORTER, true);
		adapter.setBlocking(true);
		DataAdapterBlockEntity loaded = new DataAdapterBlockEntity(adapter.getBlockPos(), adapter.getBlockState());
		loaded.load(adapter.saveWithoutMetadata());
		StorageGameTests.check(helper, loaded.isBlocking(), "the block list wasn't saved");
		DataAdapterBlockEntity old = new DataAdapterBlockEntity(adapter.getBlockPos(), adapter.getBlockState());
		var tag = adapter.saveWithoutMetadata();
		tag.remove("blocking");
		old.load(tag);
		StorageGameTests.check(helper, !old.isBlocking(),
				"an adapter saved before the setting existed isn't an allow list");
		helper.succeed();
	}

	/** Transfers until the adapter stops moving anything (or 64 times); the last status. */
	private static AdapterStatus drain(DataAdapterBlockEntity adapter)
	{
		AdapterStatus status = AdapterStatus.OK;
		for(int i = 0; i<64&&status==AdapterStatus.OK; i++)
			status = adapter.transfer();
		return status;
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void switchedOffMovesNothing(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.DRUM, AdapterKind.EXPORTER, true);
		StorageNetwork.scan(helper.getLevel(), adapter.getBlockPos().below()).getStorage()
				.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 10, false);
		adapter.setFilter(new ItemStack(Items.COBBLESTONE));
		StorageGameTests.check(helper, adapter.isEnabled(), "an adapter should start switched on");
		adapter.setEnabled(false);
		helper.runAfterDelay(40, () -> {
			StorageGameTests.check(helper, inChest(helper, adapter, Items.COBBLESTONE)==0,
					"a switched-off exporter moved items");
			StorageGameTests.check(helper, adapter.getStatus()==AdapterStatus.OFF, "a switched-off adapter isn't OFF");
			adapter.setEnabled(true);
			helper.runAfterDelay(20, () -> {
				StorageGameTests.check(helper, inChest(helper, adapter, Items.COBBLESTONE)>0,
						"switched back on, the exporter didn't move anything");
				helper.succeed();
			});
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void speedStaysWithinTheEra(GameTestHelper helper)
	{
		DataAdapterBlockEntity adapter = setUp(helper, ComputerEra.TAPE, AdapterKind.EXPORTER, true);
		adapter.setSpeed(100, 1);
		StorageGameTests.check(helper, adapter.getItems()==8, "Items should be limited to 8 for TAPE");
		StorageGameTests.check(helper, adapter.getTicks()==5, "Ticks should be limited to 5 for TAPE");
		adapter.setSpeed(0, 500);
		StorageGameTests.check(helper, adapter.getItems()==1, "Items should be limited to 1");
		StorageGameTests.check(helper, adapter.getTicks()==200, "Ticks should be limited to 200");
		helper.succeed();
	}
}

package com.muddykat.engineeredcomputation.terminal;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.AdapterStatus;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlock;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlockEntity;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.power.EnergyAccepterBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECPower;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.ItemKey;

/**
 * Networks paying RF for their machines through Energy Accepters. Power is a global setting, so these run in a batch of
 * their own with it switched on, and it's switched back off after.
 */
@TestCategory("power")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class PowerGameTests
{
	private static final String BATCH = "power";

	private PowerGameTests()
	{
	}

	@BeforeBatch(batch = BATCH)
	public static void switchPowerOn(ServerLevel level)
	{
		ECConfig.POWER.set(true);
	}

	@AfterBatch(batch = BATCH)
	public static void switchPowerOff(ServerLevel level)
	{
		ECConfig.POWER.set(false);
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void powerIsOffByDefault(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		StorageGameTests.check(helper, !ECConfig.powerEnabled(), "power should be off by default");
		StorageGameTests.check(helper, setup.network().isPowered(helper.getLevel()),
				"with power off, a network without an accepter should run");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, batch = BATCH)
	public static void aNetworkWithoutAnAccepterStops(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		ServerLevel level = helper.getLevel();
		StorageGameTests.check(helper, !setup.network().isPowered(level), "a network with no accepter ran");
		StorageGameTests.check(helper, setup.network().getUsableStorage(level).getCells().isEmpty(),
				"an unpaid network's storage could still be used");
		StorageGameTests.check(helper, setup.exporter().transfer()==AdapterStatus.PAYMENT_REQUIRED,
				"an unpaid exporter should show 402, not "+setup.exporter().transfer());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, batch = BATCH)
	public static void eachMachineCostsOnce(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		long expected = ECConfig.cost(ECConfig.costKey(EraDrive.DRUM_COMPUTER))
				+ECConfig.cost(ECConfig.adapterKey(ComputerEra.DRUM, AdapterKind.EXPORTER));
		long cost = setup.network().getPowerCost(helper.getLevel());
		StorageGameTests.check(helper, cost==expected,
				"a Drum Computer (many blocks, charged once) and an exporter should cost "+expected+", not "+cost);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, batch = BATCH)
	public static void anAccepterPaysForItsNetwork(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		ServerLevel level = helper.getLevel();
		EnergyAccepterBlockEntity accepter = accepter(helper, setup, 1);
		accepter.setEnergy(50_000);
		long cost = setup.network().getPowerCost(level);
		helper.runAfterDelay(10, () -> {
			StorageGameTests.check(helper, setup.network().isPowered(level), "a fed accepter didn't power the network");
			StorageGameTests.check(helper, accepter.getEnergy()<50_000&&(50_000-accepter.getEnergy())%cost==0,
					"the accepter should be drawn in whole ticks of "+cost+" RF, holds "+accepter.getEnergy());
			StorageGameTests.check(helper, setup.exporter().transfer()==AdapterStatus.OK,
					"a paid exporter should move items, not "+setup.exporter().transfer());
			helper.succeed();
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE, batch = BATCH)
	public static void tooLittleRfStopsTheNetworkWithoutDrawingIt(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		ServerLevel level = helper.getLevel();
		EnergyAccepterBlockEntity accepter = accepter(helper, setup, 1);
		int tooLittle = (int)setup.network().getPowerCost(level)-1;
		accepter.setEnergy(tooLittle);
		helper.runAfterDelay(10, () -> {
			StorageGameTests.check(helper, !setup.network().isPowered(level), "a network short of RF ran");
			StorageGameTests.check(helper, accepter.getEnergy()==tooLittle, "RF was drawn without paying");
			helper.succeed();
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE, batch = BATCH)
	public static void acceptersShareTheirRf(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		ServerLevel level = helper.getLevel();
		EnergyAccepterBlockEntity first = accepter(helper, setup, 1);
		EnergyAccepterBlockEntity second = accepter(helper, setup, 2);
		long cost = setup.network().getPowerCost(level);
		int half = (int)cost/2+1;
		first.setEnergy(half);
		second.setEnergy(half);
		helper.runAfterDelay(1, () -> {
			StorageGameTests.check(helper, setup.network().getAccepters().size()==2, "the network doesn't see both");
			StorageGameTests.check(helper, setup.network().isPowered(level),
					"two accepters holding enough between them didn't pay");
			StorageGameTests.check(helper, first.getEnergy()+second.getEnergy()==2L*half-cost,
					"they should pay "+cost+" between them, left "+first.getEnergy()+" and "+second.getEnergy());
			helper.succeed();
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE, batch = BATCH)
	public static void anAccepterTakesRfOnAnySide(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(StorageGameTests.ORIGIN);
		level.setBlockAndUpdate(pos, ECPower.BLOCK.get().defaultBlockState());
		for(Direction side : Direction.values())
		{
			IEnergyStorage storage = energy(level, pos, side);
			StorageGameTests.check(helper, storage!=null&&storage.receiveEnergy(100, false)==100,
					"it didn't take RF on its "+side+" side");
		}
		IEnergyStorage storage = energy(level, pos, null);
		storage.receiveEnergy(Integer.MAX_VALUE, false);
		StorageGameTests.check(helper, storage.getEnergyStored()==ECConfig.accepterCapacity(),
				"it should fill to its capacity, holds "+storage.getEnergyStored());
		StorageGameTests.check(helper, storage.extractEnergy(100, false)==0, "RF could be taken back out");
		helper.succeed();
	}

	@Nullable
	private static IEnergyStorage energy(ServerLevel level, BlockPos pos, @Nullable Direction side)
	{
		BlockEntity entity = level.getBlockEntity(pos);
		return entity==null? null: entity.getCapability(CapabilityEnergy.ENERGY, side).resolve().orElse(null);
	}

	private record Setup(BlockPos cable, DataAdapterBlockEntity exporter, ServerLevel level)
	{
		StorageNetwork network()
		{
			return StorageNetwork.of(level, cable);
		}
	}

	/** A Drum Computer holding cobblestone, a cable at its port, and an exporter on the cable into a chest. */
	private static Setup setup(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		BlockPos cable = StorageGameTests.atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		level.setBlockAndUpdate(cable, ECCables.block().get().defaultBlockState());
		BlockPos exporterPos = cable.above();
		level.setBlockAndUpdate(exporterPos, ECAdapters.of(ComputerEra.DRUM).get(AdapterKind.EXPORTER).get()
				.defaultBlockState().setValue(DataAdapterBlock.FACING, Direction.UP));
		level.setBlockAndUpdate(exporterPos.above(), Blocks.CHEST.defaultBlockState());
		DataAdapterBlockEntity exporter = (DataAdapterBlockEntity)level.getBlockEntity(exporterPos);
		exporter.setFilter(new ItemStack(Items.COBBLESTONE));
		StorageNetwork.of(level, cable).getStorage().insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64, false);
		return new Setup(cable, exporter, level);
	}

	/** The {@code nth} Energy Accepter, in the nth free spot beside the cable. */
	private static EnergyAccepterBlockEntity accepter(GameTestHelper helper, Setup setup, int nth)
	{
		ServerLevel level = helper.getLevel();
		int found = 0;
		for(Direction side : Direction.Plane.HORIZONTAL)
		{
			BlockPos pos = setup.cable().relative(side);
			if(!level.getBlockState(pos).isAir()||++found<nth)
				continue;
			level.setBlockAndUpdate(pos, ECPower.BLOCK.get().defaultBlockState());
			return (EnergyAccepterBlockEntity)level.getBlockEntity(pos);
		}
		throw new IllegalStateException("no room beside the cable for accepter "+nth);
	}
}

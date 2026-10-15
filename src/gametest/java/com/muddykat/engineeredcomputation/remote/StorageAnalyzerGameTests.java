package com.muddykat.engineeredcomputation.remote;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.catalogue.CatalogueGameTests;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.network.cable.NetworkCableBlock;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.registration.ECRemote;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

@TestCategory("remote")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class StorageAnalyzerGameTests
{
	private static final ItemKey DIAMOND = ItemKey.of(new ItemStack(Items.DIAMOND));
	private static final ItemKey IRON = ItemKey.of(new ItemStack(Items.IRON_INGOT));

	private StorageAnalyzerGameTests()
	{
	}

	/** The cable goes first, as an analyzer only joins the network through a cable already at its back. */
	public static StorageAnalyzerBlockEntity analyzer(ServerLevel level, BlockPos pos, Direction facing)
	{
		level.setBlockAndUpdate(pos.relative(facing.getOpposite()), ECCables.block().get().defaultBlockState());
		level.setBlockAndUpdate(pos, ECRemote.ANALYZER.get().defaultBlockState().setValue(RemoteLinkBlock.FACING, facing));
		StorageAnalyzerBlockEntity analyzer = (StorageAnalyzerBlockEntity)level.getBlockEntity(pos);
		analyzer.look();
		return analyzer;
	}

	private static Map<ItemKey, RemoteStock.Stock> stock(ServerLevel level, BlockPos onNetwork)
	{
		List<RemoteStock.Stock> stock = RemoteStock.of(level, StorageNetwork.scan(level, onNetwork));
		return stock.stream().collect(Collectors.toMap(RemoteStock.Stock::key, s -> s));
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anAnalyzerReportsTheInventoryItFaces(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos chest = helper.absolutePos(new BlockPos(2, 1, 2));
		level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
		ChestBlockEntity chestEntity = (ChestBlockEntity)level.getBlockEntity(chest);
		chestEntity.setItem(0, new ItemStack(Items.DIAMOND, 12));
		chestEntity.setItem(1, new ItemStack(Items.IRON_INGOT, 5));
		chestEntity.setItem(2, new ItemStack(Items.DIAMOND, 3));
		StorageAnalyzerBlockEntity analyzer = analyzer(level, chest.east(), Direction.WEST);

		StorageGameTests.check(helper, StorageNetwork.scan(level, analyzer.getBlockPos()).getAnalyzers().contains(analyzer),
				"the analyzer should be on its cable's network");
		Map<ItemKey, RemoteStock.Stock> stock = stock(level, analyzer.getBlockPos());
		StorageGameTests.check(helper, stock.get(DIAMOND).equals(new RemoteStock.Stock(DIAMOND, 15, 0))
				&&stock.get(IRON).equals(new RemoteStock.Stock(IRON, 5, 0)),
				"the network should see the chest's 15 diamonds and 5 iron, got "+stock);
		StorageGameTests.check(helper, chestEntity.countItem(Items.DIAMOND)==15, "nothing should be taken");
		StorageGameTests.check(helper, level.getBlockState(analyzer.getBlockPos()).getValue(RemoteLinkBlock.ACTIVE),
				"an analyzer with something to report should light up");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aCablePlacedAfterwardsJoinsTheAnalyzer(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
		level.setBlockAndUpdate(pos, ECRemote.ANALYZER.get().defaultBlockState().setValue(RemoteLinkBlock.FACING,
				Direction.WEST));
		BlockPos cable = pos.east();
		level.setBlockAndUpdate(cable, Block.updateFromNeighbourShapes(ECCables.block().get().defaultBlockState(),
				level, cable));
		StorageGameTests.check(helper, level.getBlockState(cable).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.WEST)),
				"a cable placed at the analyzer's back should join it");
		StorageGameTests.check(helper, StorageNetwork.scan(level, cable).getAnalyzers().size()==1,
				"the analyzer should be on that cable's network");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anAnalyzerIgnoresMachinesAndEmptySpace(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos station = helper.absolutePos(new BlockPos(2, 1, 2));
		level.setBlockAndUpdate(station, ECPneumatics.TUBE_STATION.get().defaultBlockState());
		StorageAnalyzerBlockEntity atMachine = analyzer(level, station.east(), Direction.WEST);
		StorageAnalyzerBlockEntity atNothing = analyzer(level, station.above(2).east(), Direction.WEST);
		StorageGameTests.check(helper, atMachine.places().isEmpty()&&atNothing.places().isEmpty(),
				"only plain inventories and Receiving Stations should be reported");
		StorageGameTests.check(helper, !level.getBlockState(atMachine.getBlockPos()).getValue(RemoteLinkBlock.ACTIVE),
				"an analyzer with nothing to report stays dark");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void twoAnalyzersOnOneInventoryCountItOnce(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos chest = helper.absolutePos(new BlockPos(3, 1, 2));
		level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
		((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 7));
		for(int x = 1; x<=5; x++)
			level.setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 1, 3)), ECCables.block().get().defaultBlockState());
		analyzer(level, chest.west(), Direction.EAST);
		StorageAnalyzerBlockEntity east = analyzer(level, chest.east(), Direction.WEST);
		level.setBlockAndUpdate(chest.west(2).south(), ECCables.block().get().defaultBlockState());
		level.setBlockAndUpdate(chest.east(2).south(), ECCables.block().get().defaultBlockState());

		StorageGameTests.check(helper, StorageNetwork.scan(level, east.getBlockPos()).getAnalyzers().size()==2,
				"both analyzers should be on one network");
		Map<ItemKey, RemoteStock.Stock> stock = stock(level, east.getBlockPos());
		StorageGameTests.check(helper, stock.get(DIAMOND).total()==7,
				"the chest's 7 diamonds should count once, got "+stock);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 200)
	public static void anAnalyzerOnAReceivingStationReportsItsPneumaticNetwork(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		CatalogueGameTests.Setup setup = CatalogueGameTests.setup(helper, 40);
		setup.file(setup.card(level).withTally(setup.stationEntity(level).tally(), level.getGameTime()));
		Direction side = freeSide(level, setup.receiver());
		StorageAnalyzerBlockEntity analyzer = analyzer(level, setup.receiver().relative(side), side.getOpposite());

		StorageAnalyzerBlockEntity.Place place = analyzer.places().get(setup.chest());
		StorageGameTests.check(helper, place!=null&&place.pneumatic()&&place.contents().get(DIAMOND)==40,
				"the station's chest should be reported as part of the Pneumatic Network, got "+analyzer.places());
		RemoteStock.Stock stock = stock(level, analyzer.getBlockPos()).get(DIAMOND);
		StorageGameTests.check(helper, stock!=null&&stock.pneumatic()==40&&stock.stored()==0,
				"the network should see 40 diamonds in the Pneumatic Network, got "+stock);
		StorageGameTests.check(helper, setup.chestEntity(level).countItem(Items.DIAMOND)==40,
				"nothing should be requested");
		helper.succeed();
	}

	public static Direction freeSide(ServerLevel level, BlockPos pos)
	{
		for(Direction side : Direction.Plane.HORIZONTAL)
			if(level.getBlockState(pos.relative(side)).isAir()&&level.getBlockState(pos.relative(side, 2)).isAir())
				return side;
		throw new IllegalStateException("no room beside "+pos);
	}
}

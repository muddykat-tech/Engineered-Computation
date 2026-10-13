package com.muddykat.engineeredcomputation.pneumatic;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.connector.ConnectorBlockEntity;
import com.muddykat.engineeredcomputation.connector.ConnectorPorts;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.StorageBridgeBlock;
import com.muddykat.engineeredcomputation.network.cable.NetworkCableBlock;
import com.muddykat.engineeredcomputation.pneumatic.carrier.Carrier;
import com.muddykat.engineeredcomputation.pneumatic.carrier.TubeCarriers;
import com.muddykat.engineeredcomputation.pneumatic.pressurizer.PressurizerBlock;
import com.muddykat.engineeredcomputation.pneumatic.pressurizer.PressurizerBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.station.ReceivingStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.station.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.tube.CoveredTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.tube.PneumaticTubeBlock;
import com.muddykat.engineeredcomputation.registration.ECBridges;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECCapabilities;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.storage.ItemKey;

@TestCategory("pneumatic")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class PneumaticGameTests
{
	private static final String TEMPLATE = "gametest/empty_8x8x8";
	private static final ItemKey COBBLESTONE = ItemKey.of(new ItemStack(Items.COBBLESTONE));

	private PneumaticGameTests()
	{
	}

	private record Line(BlockPos chest, BlockPos station, BlockPos receiver)
	{
		ChestBlockEntity chestEntity(ServerLevel level)
		{
			return (ChestBlockEntity)level.getBlockEntity(chest);
		}

		TubeStationBlockEntity stationEntity(ServerLevel level)
		{
			return (TubeStationBlockEntity)level.getBlockEntity(station);
		}

		ReceivingStationBlockEntity receiverEntity(ServerLevel level)
		{
			return (ReceivingStationBlockEntity)level.getBlockEntity(receiver);
		}
	}

	private static Line line(GameTestHelper helper, int cobblestone)
	{
		Line line = lowPressureLine(helper, cobblestone);
		BlockPos pressurizer = place(helper, 4, 1, 2, ECPneumatics.PRESSURIZER.get().defaultBlockState()
				.setValue(PressurizerBlock.FACING, Direction.SOUTH));
		((PressurizerBlockEntity)helper.getLevel().getBlockEntity(pressurizer)).fuelContainer()
				.setItem(0, new ItemStack(Items.COAL, 64));
		return line;
	}

	private static Line lowPressureLine(GameTestHelper helper, int cobblestone)
	{
		BlockPos chest = place(helper, 1, 1, 1, Blocks.CHEST.defaultBlockState());
		BlockPos station = place(helper, 2, 1, 1, station(Direction.WEST));
		for(int x = 3; x<=5; x++)
			place(helper, x, 1, 1, tube());
		BlockPos receiver = place(helper, 6, 1, 1, receiver());
		ChestBlockEntity entity = (ChestBlockEntity)helper.getLevel().getBlockEntity(chest);
		for(int slot = 0; cobblestone>0; slot++, cobblestone -= 64)
			entity.setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(64, cobblestone)));
		return new Line(chest, station, receiver);
	}

	@GameTest(template = TEMPLATE)
	public static void carriersDeliverToTheReceivingStation(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = line(helper, 10);
		check(helper, line.stationEntity(level).request(COBBLESTONE, 10, line.receiver()), "request should be taken");
		helper.runAfterDelay(4, () -> {
			check(helper, line.chestEntity(level).countItem(Items.COBBLESTONE)==0, "chest should have been emptied");
			check(helper, line.receiverEntity(level).getTray().isEmpty(), "carrier shouldn't have landed yet");
			check(helper, carriersTo(level, line.receiver()).size()==1, "one carrier should be in flight");
			helper.succeedWhen(() -> {
				helper.assertTrue(line.receiverEntity(level).getTray().countItem(Items.COBBLESTONE)==10,
						"tray should hold the 10 cobblestone");
				helper.assertTrue(carriersTo(level, line.receiver()).isEmpty(), "carrier should be done");
			});
		});
	}

	@GameTest(template = TEMPLATE)
	public static void moreThanAStackTakesSeveralCarriers(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = line(helper, 100);
		check(helper, line.stationEntity(level).request(COBBLESTONE, 100, line.receiver()), "request should be taken");
		helper.runAfterDelay(3, () -> {
			List<Carrier> flying = carriersTo(level, line.receiver());
			check(helper, flying.size()==1&&flying.get(0).getItems().getCount()==64,
					"a carrier should take one stack at a time");
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(line.receiverEntity(level).getTray().countItem(Items.COBBLESTONE)==100,
					"tray should hold all 100 cobblestone");
			helper.assertTrue(line.stationEntity(level).getPendingRequests()==0, "request should be met");
		});
	}

	@GameTest(template = TEMPLATE)
	public static void lowPressureCarriersAreSmallAndSpacedOut(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = lowPressureLine(helper, 20);
		check(helper, line.stationEntity(level).request(COBBLESTONE, 20, line.receiver()), "request should be taken");
		helper.runAfterDelay(3, () -> {
			List<Carrier> flying = carriersTo(level, line.receiver());
			check(helper, flying.size()==1&&flying.get(0).getItems().getCount()==ECConfig.carrierItems(false),
					"a low pressure carrier should hold "+ECConfig.carrierItems(false));
			helper.runAfterDelay(ECConfig.carrierTicks(false)/2, () -> {
				check(helper, carriersTo(level, line.receiver()).size()==1,
						"the next carrier left before the low pressure wait was up");
				helper.succeedWhen(() -> helper.assertTrue(line.receiverEntity(level).getTray()
						.countItem(Items.COBBLESTONE)==20, "the tray should get all 20 in the end"));
			});
		});
	}

	@GameTest(template = TEMPLATE)
	public static void sentItemsLeaveAsPressureAllows(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = lowPressureLine(helper, 0);
		List<BlockPos> route = TubeNetwork.of(level, line.station()).route(line.station(), line.receiver());
		TubeCarriers.of(level).send(new ItemStack(Items.COBBLESTONE, 20), route);
		helper.runAfterDelay(1, () -> {
			int size = ECConfig.carrierItems(false);
			check(helper, carriersTo(level, line.receiver()).stream().mapToInt(c -> c.getItems().getCount()).sum()==size
					&&TubeCarriers.of(level).waitingAt(line.station())==20-size,
					"the first low pressure carrier should take "+size+" and leave the rest waiting");
			helper.succeedWhen(() -> helper.assertTrue(line.receiverEntity(level).getTray()
					.countItem(Items.COBBLESTONE)==20, "all 20 should arrive in the end"));
		});
	}

	@GameTest(template = TEMPLATE)
	public static void aPressurizerBurnsFuelForHighPressure(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = line(helper, 64);
		PressurizerBlockEntity pressurizer = (PressurizerBlockEntity)level.getBlockEntity(
				helper.absolutePos(new BlockPos(4, 1, 2)));
		check(helper, level.getBlockState(pressurizer.getBlockPos()).getValue(PressurizerBlock.LIT),
				"a fuelled Pressurizer should be lit");
		check(helper, line.stationEntity(level).request(COBBLESTONE, 64, line.receiver()), "request should be taken");
		helper.runAfterDelay(3, () -> {
			List<Carrier> flying = carriersTo(level, line.receiver());
			check(helper, flying.size()==1&&flying.get(0).getItems().getCount()==64,
					"a high pressure carrier should take the whole stack");
			check(helper, pressurizer.fuelContainer().getItem(0).getCount()==63
							&&pressurizer.getCharge()==ECConfig.carriersPerFuel()-1,
					"the carrier should have burned a coal and drawn a charge from it");
			helper.succeed();
		});
	}

	@GameTest(template = TEMPLATE)
	public static void aPressurizerOnlyBurnsCoal(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos at = place(helper, 3, 1, 3, ECPneumatics.PRESSURIZER.get().defaultBlockState());
		PressurizerBlockEntity pressurizer = (PressurizerBlockEntity)level.getBlockEntity(at);
		check(helper, pressurizer.fuelContainer().canPlaceItem(0, new ItemStack(Items.CHARCOAL))
				&&!pressurizer.fuelContainer().canPlaceItem(0, new ItemStack(Items.OAK_LOG)),
				"it should take coal and charcoal, and nothing else");
		check(helper, !pressurizer.hasPressure()&&!pressurizer.draw(), "an empty Pressurizer gave pressure");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void requestsStopWhenTheStockRunsOut(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = line(helper, 5);
		check(helper, line.stationEntity(level).request(COBBLESTONE, 20, line.receiver()), "request should be taken");
		helper.succeedWhen(() -> {
			helper.assertTrue(line.receiverEntity(level).getTray().countItem(Items.COBBLESTONE)==5,
					"tray should hold the 5 there were");
			helper.assertTrue(line.stationEntity(level).getPendingRequests()==0, "request should be dropped");
		});
	}

	@GameTest(template = TEMPLATE)
	public static void requestsWithoutARouteAreRefused(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		place(helper, 1, 1, 1, Blocks.CHEST.defaultBlockState());
		BlockPos station = place(helper, 2, 1, 1, station(Direction.WEST));
		BlockPos touching = place(helper, 3, 1, 1, receiver());
		BlockPos apart = place(helper, 6, 1, 1, receiver());
		TubeStationBlockEntity entity = (TubeStationBlockEntity)level.getBlockEntity(station);
		check(helper, !entity.request(COBBLESTONE, 1, touching), "a receiver with no tube between shouldn't be reached");
		check(helper, !entity.request(COBBLESTONE, 1, apart), "an unconnected receiver shouldn't be reached");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void carriersNeverPassThroughStations(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		place(helper, 1, 1, 1, Blocks.CHEST.defaultBlockState());
		BlockPos first = place(helper, 2, 1, 1, station(Direction.WEST));
		place(helper, 3, 1, 1, tube());
		BlockPos receiver = place(helper, 4, 1, 1, receiver());
		place(helper, 2, 2, 1, tube());
		place(helper, 2, 3, 1, tube());
		BlockPos second = place(helper, 2, 4, 1, station(Direction.UP));
		place(helper, 2, 5, 1, Blocks.CHEST.defaultBlockState());

		TubeNetwork network = TubeNetwork.scan(level, second);
		check(helper, network.getEnds().contains(receiver), "the receiver is on the same network");
		check(helper, network.route(second, receiver).isEmpty(), "no route should run through station 1");
		check(helper, network.route(first, receiver).size()==3, "station 1 should reach the receiver by its tube");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void aBrokenTubeSpillsTheCarrier(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = line(helper, 10);
		check(helper, line.stationEntity(level).request(COBBLESTONE, 10, line.receiver()), "request should be taken");
		helper.runAfterDelay(2, () -> {
			check(helper, carriersTo(level, line.receiver()).size()==1, "carrier should be on its way");
			helper.setBlock(5, 1, 1, Blocks.AIR);
			helper.succeedWhen(() -> {
				helper.assertItemEntityPresent(Items.COBBLESTONE, new BlockPos(5, 1, 1), 2);
				helper.assertTrue(line.receiverEntity(level).getTray().isEmpty(), "nothing should land");
				helper.assertTrue(carriersTo(level, line.receiver()).isEmpty(), "carrier should be gone");
			});
		});
	}

	@GameTest(template = TEMPLATE)
	public static void aFullTrayKeepsTheCarrierWaiting(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = line(helper, 10);
		for(int slot = 0; slot<ReceivingStationBlockEntity.SLOTS; slot++)
			line.receiverEntity(level).getTray().setItem(slot, new ItemStack(Items.DIRT, 64));
		check(helper, line.stationEntity(level).request(COBBLESTONE, 10, line.receiver()), "request should be taken");
		helper.runAfterDelay(30, () -> {
			List<Carrier> waiting = carriersTo(level, line.receiver());
			check(helper, waiting.size()==1&&waiting.get(0).getItems().getCount()==10,
					"carrier should wait at the full tray with all it carries");
			line.receiverEntity(level).getTray().setItem(0, ItemStack.EMPTY);
			helper.succeedWhen(() -> {
				helper.assertTrue(line.receiverEntity(level).getTray().countItem(Items.COBBLESTONE)==10,
						"carrier should land once there's room");
				helper.assertTrue(carriersTo(level, line.receiver()).isEmpty(), "carrier should be done");
			});
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void carriersWaitForAnUnloadedDestination(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		place(helper, 1, 1, 1, Blocks.CHEST.defaultBlockState());
		BlockPos station = place(helper, 2, 1, 1, station(Direction.WEST));
		BlockPos tube = place(helper, 3, 1, 1, tube());
		BlockPos far = station.offset(30000, 0, 0);
		check(helper, !level.isLoaded(far), "the far end must start unloaded for this test to mean anything");
		TubeCarriers.of(level).launch(level, new ItemStack(Items.COBBLESTONE, 7), List.of(station, tube, far));
		helper.runAfterDelay(20, () -> {
			check(helper, carriersTo(level, far).size()==1, "carrier should wait for its destination to load");
			check(helper, !level.isLoaded(far), "waiting shouldn't load the destination");
			ChunkPos chunk = new ChunkPos(far);
			level.setChunkForced(chunk.x, chunk.z, true);
			level.setBlockAndUpdate(far, receiver());
			helper.succeedWhen(() -> {
				helper.assertTrue(level.getBlockEntity(far) instanceof ReceivingStationBlockEntity receiver
						&&receiver.getTray().countItem(Items.COBBLESTONE)==7, "carrier should land once loaded");
				helper.assertTrue(carriersTo(level, far).isEmpty(), "carrier should be done");
				level.removeBlock(far, false);
				level.setChunkForced(chunk.x, chunk.z, false);
			});
		});
	}

	@GameTest(template = TEMPLATE)
	public static void carriersAreSavedWithTheLevel(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		TubeCarriers carriers = new TubeCarriers();
		List<BlockPos> path = List.of(new BlockPos(1, 2, 3), new BlockPos(2, 2, 3), new BlockPos(3, 2, 3));
		carriers.launch(level, new ItemStack(Items.DIAMOND_PICKAXE), path);
		CompoundTag saved = carriers.save(new CompoundTag(), level.registryAccess());
		List<Carrier> loaded = TubeCarriers.load(saved, level.registryAccess()).getCarriers();
		check(helper, loaded.size()==1, "one carrier should come back");
		Carrier carrier = loaded.get(0);
		check(helper, carrier.getPath().equals(path), "route should come back, got "+carrier.getPath());
		check(helper, carrier.getItems().is(Items.DIAMOND_PICKAXE), "items should come back");
		check(helper, carrier.getLaunched()==carriers.getCarriers().get(0).getLaunched(), "launch tick should come back");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void tubesNeverJoinTheDataNetwork(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos tube = place(helper, 3, 1, 3, tube());
		BlockPos station = place(helper, 3, 1, 4, station(Direction.SOUTH));
		BlockPos receiver = place(helper, 3, 2, 3, receiver());
		BlockPos cable = place(helper, 4, 1, 3, ECCables.block().get().defaultBlockState());
		place(helper, 4, 1, 4, ECCables.block().get().defaultBlockState());
		place(helper, 4, 2, 3, ECCables.block().get().defaultBlockState());
		BlockPos bridge = place(helper, 2, 1, 3, ECBridges.block().get().defaultBlockState()
				.setValue(StorageBridgeBlock.FACING, Direction.NORTH));

		for(BlockPos pos : List.of(tube, station, receiver))
			check(helper, level.getCapability(ECCapabilities.NETWORK_MEMBER, pos, null)==null,
					"tube blocks must never be storage network members: "+pos);
		for(BlockPos pos : List.of(cable, bridge))
			check(helper, level.getCapability(ECCapabilities.TUBE_MEMBER, pos, null)==null,
					"data network blocks must never be tube members: "+pos);
		check(helper, !level.getBlockState(cable).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.WEST)),
				"cable shouldn't reach for the tube");
		check(helper, !level.getBlockState(tube).getValue(PneumaticTubeBlock.CONNECTIONS.get(Direction.EAST)),
				"tube shouldn't reach for the cable");
		check(helper, !level.getBlockState(tube).getValue(PneumaticTubeBlock.CONNECTIONS.get(Direction.WEST)),
				"tube shouldn't reach for the bridge");
		check(helper, level.getBlockState(tube).getValue(PneumaticTubeBlock.CONNECTIONS.get(Direction.SOUTH)),
				"tube should reach for the station");
		TubeNetwork network = TubeNetwork.scan(level, tube);
		check(helper, network.getTubes().size()==1&&network.getEnds().size()==2,
				"tube network should be the tube and its two stations, got "+network.getTubes()+" "+network.getEnds());
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void tubesRunStraightOrMeetInAJunction(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos middle = place(helper, 3, 1, 3, tube());
		check(helper, level.getBlockState(middle).getValue(PneumaticTubeBlock.STRAIGHT)==PneumaticTubeBlock.Straight.NONE,
				"a lone tube should be a junction box");
		place(helper, 2, 1, 3, tube());
		place(helper, 4, 1, 3, tube());
		check(helper, level.getBlockState(middle).getValue(PneumaticTubeBlock.STRAIGHT)==PneumaticTubeBlock.Straight.X,
				"a tube joined east and west should run straight along x");
		place(helper, 3, 2, 3, tube());
		check(helper, level.getBlockState(middle).getValue(PneumaticTubeBlock.STRAIGHT)==PneumaticTubeBlock.Straight.NONE,
				"a tube joined three ways should be a junction box");
		helper.setBlock(2, 1, 3, Blocks.AIR);
		helper.setBlock(4, 1, 3, Blocks.AIR);
		place(helper, 3, 0, 3, tube());
		check(helper, level.getBlockState(middle).getValue(PneumaticTubeBlock.STRAIGHT)==PneumaticTubeBlock.Straight.Y,
				"a tube joined up and down should run straight along y");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void shiftRightClickingATubeFitsAWindow(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = place(helper, 3, 1, 3, tube());
		Player player = FakePlayerFactory.getMinecraft(level);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		player.setShiftKeyDown(false);
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		check(helper, !level.getBlockState(pos).getValue(PneumaticTubeBlock.WINDOWED), "a plain click fitted a window");
		player.setShiftKeyDown(true);
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		check(helper, level.getBlockState(pos).getValue(PneumaticTubeBlock.WINDOWED), "shift-right-click should fit a window");
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		check(helper, !level.getBlockState(pos).getValue(PneumaticTubeBlock.WINDOWED), "again should take it out");
		player.setShiftKeyDown(false);
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void anEjectingStationFillsTheInventoryInFrontOfIt(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = place(helper, 3, 1, 3, receiver().setValue(ReceivingStationBlock.FACING, Direction.EAST));
		BlockPos chestPos = place(helper, 4, 1, 3, Blocks.CHEST.defaultBlockState());
		ReceivingStationBlockEntity receiver = (ReceivingStationBlockEntity)level.getBlockEntity(pos);
		receiver.accept(new ItemStack(Items.COBBLESTONE, 20));
		receiver.setEjecting(true);
		helper.succeedWhen(() -> {
			helper.assertTrue(((ChestBlockEntity)level.getBlockEntity(chestPos)).countItem(Items.COBBLESTONE)==20,
					"the cobblestone should go into the chest in front");
			helper.assertTrue(receiver.getTray().isEmpty(), "the tray should be empty");
			helper.assertItemEntityNotPresent(Items.COBBLESTONE, new BlockPos(4, 1, 3), 2);
		});
	}

	@GameTest(template = TEMPLATE)
	public static void aDyeTintsAWindowWithoutBeingUsedUp(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = place(helper, 3, 1, 3, tube().setValue(PneumaticTubeBlock.WINDOWED, true));
		Player player = FakePlayerFactory.getMinecraft(level);
		ItemStack dye = new ItemStack(Items.BLUE_DYE, 3);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		level.getBlockState(pos).useItemOn(dye, level, player, InteractionHand.MAIN_HAND, hit);
		check(helper, PneumaticTubeBlock.tintColour(level.getBlockState(pos))==DyeColor.BLUE, "the window should be blue");
		check(helper, dye.getCount()==3, "the dye shouldn't be used up");
		level.getBlockState(pos).useItemOn(dye, level, player, InteractionHand.MAIN_HAND, hit);
		check(helper, PneumaticTubeBlock.tintColour(level.getBlockState(pos))==null, "the same dye again should clear it");
		BlockPos solid = place(helper, 5, 1, 3, tube());
		level.getBlockState(solid).useItemOn(dye, level, player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(solid), Direction.UP, solid, false));
		check(helper, level.getBlockState(solid).getValue(PneumaticTubeBlock.TINT)==0, "a solid tube was tinted");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void windowedTubesArePlacedTintedByADyeInTheOtherHand(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Player player = FakePlayerFactory.getMinecraft(level);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ECPneumatics.WINDOWED_TUBE_ITEM.get()));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.RED_DYE));
		BlockPos below = place(helper, 3, 1, 3, Blocks.STONE.defaultBlockState());
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false)));
		BlockState placed = level.getBlockState(below.above());
		player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		check(helper, placed.is(ECPneumatics.TUBE.get())&&placed.getValue(PneumaticTubeBlock.WINDOWED),
				"the windowed tube should be placed with a window, got "+placed);
		check(helper, PneumaticTubeBlock.tintColour(placed)==DyeColor.RED, "it should be tinted red");
		check(helper, placed.getBlock().getCloneItemStack(level, below.above(), placed)
				.is(ECPneumatics.WINDOWED_TUBE_ITEM.get()), "picking it should give the windowed tube");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void solidAndWindowedTubesCraftIntoEachOther(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		check(helper, craft(level, new ItemStack(ECPneumatics.TUBE.get())).is(ECPneumatics.WINDOWED_TUBE_ITEM.get()),
				"a tube should craft into a windowed tube");
		check(helper, craft(level, new ItemStack(ECPneumatics.WINDOWED_TUBE_ITEM.get())).is(ECPneumatics.TUBE.get().asItem()),
				"a windowed tube should craft back into a tube");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void aWindowedTubeDropsAWindowedTube(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos windowed = place(helper, 3, 1, 3, tube().setValue(PneumaticTubeBlock.WINDOWED, true));
		BlockPos solid = place(helper, 5, 1, 3, tube());
		level.destroyBlock(windowed, true);
		level.destroyBlock(solid, true);
		helper.assertItemEntityPresent(ECPneumatics.WINDOWED_TUBE_ITEM.get(), new BlockPos(3, 1, 3), 1);
		helper.assertItemEntityPresent(ECPneumatics.TUBE.get().asItem(), new BlockPos(5, 1, 3), 1);
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void aCoveredTubeStillCarries(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = lowPressureLine(helper, 10);
		BlockPos middle = helper.absolutePos(new BlockPos(4, 1, 1));
		level.setBlockAndUpdate(middle, level.getBlockState(middle).setValue(PneumaticTubeBlock.WINDOWED, true)
				.setValue(PneumaticTubeBlock.TINT, PneumaticTubeBlock.tintOf(DyeColor.GREEN)));
		check(helper, CoveredTubeBlock.cover(level, middle, Blocks.STONE.defaultBlockState()), "stone should cover a tube");
		BlockState covered = level.getBlockState(middle);
		check(helper, covered.is(ECPneumatics.COVERED_TUBE.get()), "a stone cover hides the tube, got "+covered);
		check(helper, covered.getValue(PneumaticTubeBlock.WINDOWED)
				&&PneumaticTubeBlock.tintColour(covered)==DyeColor.GREEN, "the covered tube should keep its tinted window");
		check(helper, covered.getValue(PneumaticTubeBlock.STRAIGHT)==PneumaticTubeBlock.Straight.X,
				"the covered tube should keep its connections");
		check(helper, line.stationEntity(level).request(COBBLESTONE, 10, line.receiver()), "request should be taken");
		helper.succeedWhen(() -> helper.assertTrue(line.receiverEntity(level).getTray().countItem(Items.COBBLESTONE)==10,
				"carriers should pass through the covered tube"));
	}

	@GameTest(template = TEMPLATE)
	public static void tubeCoversComeAndGo(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = place(helper, 3, 1, 3, tube().setValue(PneumaticTubeBlock.WINDOWED, true));
		check(helper, !CoveredTubeBlock.cover(level, pos, tube()), "a tube shouldn't cover a tube");
		check(helper, CoveredTubeBlock.cover(level, pos, Blocks.GLASS.defaultBlockState()), "glass should cover a tube");
		check(helper, level.getBlockState(pos).is(ECPneumatics.CLEAR_COVERED_TUBE.get()),
				"a glass cover should show the tube inside");
		check(helper, level.getBlockState(pos).getBlock().getCloneItemStack(level, pos, level.getBlockState(pos))
				.is(Items.GLASS), "picking a covered tube gives its cover");
		ItemStack cover = CoveredTubeBlock.uncover(level, pos);
		check(helper, cover.is(Items.GLASS), "uncovering should give the glass back");
		check(helper, level.getBlockState(pos).is(ECPneumatics.TUBE.get())
				&&level.getBlockState(pos).getValue(PneumaticTubeBlock.WINDOWED), "the windowed tube should be back");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void aPlayerBreakingACoveredTubeBreaksOnlyTheCover(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = place(helper, 3, 1, 3, tube());
		CoveredTubeBlock.cover(level, pos, Blocks.OAK_PLANKS.defaultBlockState());
		Player player = FakePlayerFactory.getMinecraft(level);
		level.getBlockState(pos).onDestroyedByPlayer(level, pos, player, true, level.getFluidState(pos));
		check(helper, level.getBlockState(pos).is(ECPneumatics.TUBE.get()), "the tube should stay");
		helper.assertItemEntityPresent(Items.OAK_PLANKS, new BlockPos(3, 1, 3), 1.5);
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void theWrenchCutsAndReopensATube(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = line(helper, 10);
		BlockPos tube = helper.absolutePos(new BlockPos(4, 1, 1));
		check(helper, ConnectorPorts.wrench(level, tube, new Vec3(14/16.0, 0.5, 0.5)), "wrenching the arm did nothing");
		check(helper, ConnectorBlockEntity.isCut(level, tube, Direction.EAST), "the arm's side should be cut");
		check(helper, !level.getBlockState(tube).getValue(PneumaticTubeBlock.CONNECTIONS.get(Direction.EAST))
				&&!level.getBlockState(tube.east()).getValue(PneumaticTubeBlock.CONNECTIONS.get(Direction.WEST)),
				"the tubes still join across the cut");
		check(helper, !line.stationEntity(level).request(COBBLESTONE, 10, line.receiver()),
				"a request went through the cut");
		check(helper, ConnectorPorts.wrench(level, tube, new Vec3(12.5/16, 0.5, 0.5)), "wrenching the cap did nothing");
		check(helper, !ConnectorBlockEntity.isCut(level, tube, Direction.EAST)
				&&level.getBlockState(tube).getValue(PneumaticTubeBlock.CONNECTIONS.get(Direction.EAST)),
				"the reopened side should join again");
		check(helper, line.stationEntity(level).request(COBBLESTONE, 10, line.receiver()),
				"a request should go through the reopened tube");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void aCutGoesWithItsNeighbour(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		line(helper, 0);
		BlockPos tube = helper.absolutePos(new BlockPos(4, 1, 1));
		ConnectorPorts.wrench(level, tube, new Vec3(14/16.0, 0.5, 0.5));
		level.setBlockAndUpdate(tube.east(), Blocks.AIR.defaultBlockState());
		check(helper, !ConnectorBlockEntity.isCut(level, tube, Direction.EAST),
				"the cut stayed with nothing left to cut off");
		level.setBlockAndUpdate(tube.east(), tube());
		check(helper, level.getBlockState(tube).getValue(PneumaticTubeBlock.CONNECTIONS.get(Direction.EAST)),
				"a tube put back where the cut one was should join");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void aCutOutlastsACover(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = line(helper, 10);
		BlockPos tube = helper.absolutePos(new BlockPos(4, 1, 1));
		ConnectorPorts.wrench(level, tube, new Vec3(14/16.0, 0.5, 0.5));
		check(helper, CoveredTubeBlock.cover(level, tube, Blocks.STONE.defaultBlockState()), "the tube wasn't covered");
		check(helper, ConnectorBlockEntity.isCut(level, tube, Direction.EAST)
				&&level.getCapability(ECCapabilities.TUBE_MEMBER, tube, Direction.EAST)==null,
				"covering the tube lost its cut");
		check(helper, !ConnectorPorts.wrench(level, tube, new Vec3(14/16.0, 0.5, 0.5)),
				"the wrench cut a covered tube rather than leaving it to take the cover off");
		CoveredTubeBlock.uncover(level, tube);
		check(helper, ConnectorBlockEntity.isCut(level, tube, Direction.EAST), "uncovering the tube lost its cut");
		check(helper, !line.stationEntity(level).request(COBBLESTONE, 10, line.receiver()),
				"a request went through the cut");
		helper.succeed();
	}

	private static ItemStack craft(ServerLevel level, ItemStack stack)
	{
		CraftingInput input = CraftingInput.of(1, 1, List.of(stack));
		return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
				.map(recipe -> recipe.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
	}

	private static List<Carrier> carriersTo(ServerLevel level, BlockPos destination)
	{
		return TubeCarriers.of(level).getCarriers().stream().filter(c -> c.getDestination().equals(destination)).toList();
	}

	private static BlockPos place(GameTestHelper helper, int x, int y, int z, BlockState state)
	{
		BlockPos absolute = helper.absolutePos(new BlockPos(x, y, z));
		helper.getLevel().setBlockAndUpdate(absolute, state);
		return absolute;
	}

	private static BlockState tube()
	{
		return ECPneumatics.TUBE.get().defaultBlockState();
	}

	private static BlockState station(Direction facing)
	{
		return ECPneumatics.TUBE_STATION.get().defaultBlockState().setValue(TubeStationBlock.FACING, facing);
	}

	private static BlockState receiver()
	{
		return ECPneumatics.RECEIVING_STATION.get().defaultBlockState();
	}

	private static void check(GameTestHelper helper, boolean condition, String failure)
	{
		if(!condition)
			helper.fail(failure);
	}
}

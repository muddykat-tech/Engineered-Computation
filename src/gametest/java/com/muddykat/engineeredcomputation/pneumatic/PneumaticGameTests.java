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
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.crafting.Pattern;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.NetworkCableBlock;
import com.muddykat.engineeredcomputation.network.NetworkMemberBlock;
import com.muddykat.engineeredcomputation.network.StorageBridgeBlock;
import com.muddykat.engineeredcomputation.registration.ECBridges;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.StorageGameTests;

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
				StorageGameTests.require(line.receiverEntity(level).getTray().countItem(Items.COBBLESTONE)==10,
						"tray should hold the 10 cobblestone");
				StorageGameTests.require(carriersTo(level, line.receiver()).isEmpty(), "carrier should be done");
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
			StorageGameTests.require(line.receiverEntity(level).getTray().countItem(Items.COBBLESTONE)==100,
					"tray should hold all 100 cobblestone");
			StorageGameTests.require(line.stationEntity(level).getPendingRequests()==0, "request should be met");
		});
	}

	@GameTest(template = TEMPLATE)
	public static void requestsStopWhenTheStockRunsOut(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Line line = line(helper, 5);
		check(helper, line.stationEntity(level).request(COBBLESTONE, 20, line.receiver()), "request should be taken");
		helper.succeedWhen(() -> {
			StorageGameTests.require(line.receiverEntity(level).getTray().countItem(Items.COBBLESTONE)==5,
					"tray should hold the 5 there were");
			StorageGameTests.require(line.stationEntity(level).getPendingRequests()==0, "request should be dropped");
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
				StorageGameTests.require(line.receiverEntity(level).getTray().isEmpty(), "nothing should land");
				StorageGameTests.require(carriersTo(level, line.receiver()).isEmpty(), "carrier should be gone");
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
				StorageGameTests.require(line.receiverEntity(level).getTray().countItem(Items.COBBLESTONE)==10,
						"carrier should land once there's room");
				StorageGameTests.require(carriersTo(level, line.receiver()).isEmpty(), "carrier should be done");
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
				StorageGameTests.require(level.getBlockEntity(far) instanceof ReceivingStationBlockEntity receiver
						&&receiver.getTray().countItem(Items.COBBLESTONE)==7, "carrier should land once loaded");
				StorageGameTests.require(carriersTo(level, far).isEmpty(), "carrier should be done");
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
		CompoundTag saved = carriers.save(new CompoundTag());
		List<Carrier> loaded = TubeCarriers.load(saved).getCarriers();
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
			check(helper, NetworkMemberBlock.at(level, pos, null)==null,
					"tube blocks must never be storage network members: "+pos);
		for(BlockPos pos : List.of(cable, bridge))
			check(helper, TubeMemberBlock.at(level, pos, null)==null,
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
		level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit);
		check(helper, !level.getBlockState(pos).getValue(PneumaticTubeBlock.WINDOWED), "a plain click fitted a window");
		player.setShiftKeyDown(true);
		level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit);
		check(helper, level.getBlockState(pos).getValue(PneumaticTubeBlock.WINDOWED), "shift-right-click should fit a window");
		level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit);
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
			StorageGameTests.require(((ChestBlockEntity)level.getBlockEntity(chestPos))
					.countItem(Items.COBBLESTONE)==20,
					"the cobblestone should go into the chest in front");
			StorageGameTests.require(receiver.getTray().isEmpty(), "the tray should be empty");
			helper.assertItemEntityCountIs(Items.COBBLESTONE, new BlockPos(4, 1, 3), 2, 0);
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
		player.setItemInHand(InteractionHand.MAIN_HAND, dye);
		level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit);
		check(helper, PneumaticTubeBlock.tintColour(level.getBlockState(pos))==DyeColor.BLUE, "the window should be blue");
		check(helper, dye.getCount()==3, "the dye shouldn't be used up");
		level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit);
		check(helper, PneumaticTubeBlock.tintColour(level.getBlockState(pos))==null, "the same dye again should clear it");
		BlockPos solid = place(helper, 5, 1, 3, tube());
		level.getBlockState(solid).use(level, player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(solid), Direction.UP, solid, false));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
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
		Line line = line(helper, 10);
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
		helper.succeedWhen(() -> StorageGameTests.require(line.receiverEntity(level).getTray()
				.countItem(Items.COBBLESTONE)==10,
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

	private static ItemStack craft(ServerLevel level, ItemStack stack)
	{
		CraftingContainer input = Pattern.craftingGrid(1, 1, List.of(stack));
		return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
				.map(recipe -> recipe.assemble(input)).orElse(ItemStack.EMPTY);
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

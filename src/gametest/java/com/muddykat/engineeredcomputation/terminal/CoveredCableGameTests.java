package com.muddykat.engineeredcomputation.terminal;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.CoveredCableBlock;
import com.muddykat.engineeredcomputation.network.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.network.NetworkCableBlock;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.storage.ECDataCells;

@TestCategory("network")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CoveredCableGameTests
{
	private CoveredCableGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void coveredCableStillCarriesTheNetwork(GameTestHelper helper)
	{
		BlockPos port = cables(helper);
		StorageGameTests.check(helper, cells(helper, port.above())==1, "Network should have 1 cell before covering");
		ServerLevel level = helper.getLevel();
		boolean covered = CoveredCableBlock.cover(level, port, Blocks.STONE.defaultBlockState());
		StorageGameTests.check(helper, covered, "Covering should succeed");
		StorageGameTests.check(helper, helper.getLevel().getBlockState(port).is(ECCables.covered().get()), "Block should be covered cable");
		CoveredCableBlockEntity entity = (CoveredCableBlockEntity)helper.getLevel().getBlockEntity(port);
		StorageGameTests.check(helper, entity.getCover().equals(Blocks.STONE.defaultBlockState()), "Cover should be stone");
		StorageGameTests.check(helper, cells(helper, port.above())==1, "Network should still have 1 cell after covering");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void seeThroughCoversShowTheCable(GameTestHelper helper)
	{
		BlockPos port = cables(helper);
		ServerLevel level = helper.getLevel();
		boolean covered = CoveredCableBlock.cover(level, port, Blocks.GLASS.defaultBlockState());
		StorageGameTests.check(helper, covered, "Covering with glass should succeed");
		StorageGameTests.check(helper, helper.getLevel().getBlockState(port).is(ECCables.clearCovered().get()), "Block should be clear covered cable");
		StorageGameTests.check(helper, cells(helper, port.above())==1, "Network should still have 1 cell with see-through cover");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void onlyFullPlainBlocksCover(GameTestHelper helper)
	{
		BlockPos port = cables(helper);
		ServerLevel level = helper.getLevel();

		BlockState[] invalidCovers = {Blocks.SAND.defaultBlockState(), Blocks.STONE_SLAB.defaultBlockState(),
				Blocks.CHEST.defaultBlockState(), Blocks.REDSTONE_BLOCK.defaultBlockState()};

		for(BlockState cover : invalidCovers)
		{
			boolean valid = CoveredCableBlock.isValidCover(cover, level, port);
			StorageGameTests.check(helper, !valid, "Invalid cover should not be valid");

			boolean covered = CoveredCableBlock.cover(level, port, cover);
			StorageGameTests.check(helper, !covered, "Covering with invalid cover should fail");
			StorageGameTests.check(helper, helper.getLevel().getBlockState(port).is(ECCables.block().get()), "Block should remain plain cable");
		}

		boolean valid = CoveredCableBlock.isValidCover(Blocks.OAK_LOG.defaultBlockState(), level, port);
		StorageGameTests.check(helper, valid, "Oak log should be a valid cover");

		level.setBlockAndUpdate(port.above(), Blocks.DIRT.defaultBlockState());
		boolean covered = CoveredCableBlock.cover(level, port.above(), Blocks.STONE.defaultBlockState());
		StorageGameTests.check(helper, !covered, "Covering non-cable block should fail");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void uncoverGivesTheCoverBack(GameTestHelper helper)
	{
		BlockPos port = cables(helper);
		ServerLevel level = helper.getLevel();
		CoveredCableBlock.cover(level, port, Blocks.STONE.defaultBlockState());
		ItemStack back = CoveredCableBlock.uncover(level, port);
		StorageGameTests.check(helper, back.is(Items.STONE), "Uncovered item should be stone");
		StorageGameTests.check(helper, back.getCount()==1, "Uncovered item count should be 1");
		StorageGameTests.check(helper, helper.getLevel().getBlockState(port).is(ECCables.block().get()), "Block should become plain cable");
		boolean connected = level.getBlockState(port).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.UP));
		StorageGameTests.check(helper, connected, "Plain cable should still connect to above cable");
		StorageGameTests.check(helper, cells(helper, port.above())==1, "Network should still have 1 cell after uncovering");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void wrenchTakesTheCoverOff(GameTestHelper helper)
	{
		BlockPos port = cables(helper);
		ServerLevel level = helper.getLevel();
		CoveredCableBlock.cover(level, port, Blocks.STONE.defaultBlockState());

		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		wrench(helper, player, port);

		StorageGameTests.check(helper, helper.getLevel().getBlockState(port).is(ECCables.block().get()), "Block should become plain cable");
		StorageGameTests.check(helper, player.getInventory().countItem(Items.STONE)==1, "Player should have stone item");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void otherBreakingDropsCableAndCover(GameTestHelper helper)
	{
		BlockPos port = cables(helper);
		ServerLevel level = helper.getLevel();
		CoveredCableBlock.cover(level, port, Blocks.STONE.defaultBlockState());
		level.destroyBlock(port, true);

		StorageGameTests.check(helper, dropped(level, port, ECCables.item().get())==1, "Should drop cable item");
		StorageGameTests.check(helper, dropped(level, port, Items.STONE)==1, "Should drop cover item");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aPlayerBreakingItBreaksOnlyTheCover(GameTestHelper helper)
	{
		BlockPos port = cables(helper);
		ServerLevel level = helper.getLevel();
		CoveredCableBlock.cover(level, port, Blocks.STONE.defaultBlockState());
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		player.gameMode.destroyBlock(port);
		StorageGameTests.check(helper, level.getBlockState(port).is(ECCables.block().get()),
				"breaking a covered cable didn't leave the plain cable");
		StorageGameTests.check(helper, level.getBlockState(port).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.UP)),
				"the cable left behind lost its connection");
		StorageGameTests.check(helper, dropped(level, port, Items.STONE)==1&&dropped(level, port, ECCables.item().get())==0,
				"breaking a covered cable should drop just its cover");
		StorageGameTests.check(helper, cells(helper, port.above())==1, "the network broke with the cover");
		helper.succeed();
	}

	private static BlockPos cables(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		BlockPos port = StorageGameTests.atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		level.setBlockAndUpdate(port, ECCables.block().get().defaultBlockState());
		level.setBlockAndUpdate(port.above(), ECCables.block().get().defaultBlockState());
		return port;
	}

	private static int cells(GameTestHelper helper, BlockPos pos)
	{
		return StorageNetwork.scan(helper.getLevel(), pos).getStorage().getCells().size();
	}

	private static int dropped(ServerLevel level, BlockPos pos, Item item)
	{
		int count = 0;
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3));
		for(ItemEntity drop : drops)
			if(drop.getItem().is(item))
				count += drop.getItem().getCount();
		return count;
	}

	private static void wrench(GameTestHelper helper, StorageGameTests.MenuRecordingPlayer player, BlockPos pos)
	{
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ECContent.ASSEMBLY_WRENCH.get()));
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		ECContent.ASSEMBLY_WRENCH.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}
}

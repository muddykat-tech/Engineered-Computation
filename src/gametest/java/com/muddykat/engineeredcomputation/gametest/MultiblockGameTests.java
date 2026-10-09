package com.muddykat.engineeredcomputation.gametest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.items.IItemHandler;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockDefinition;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlock;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockStateHost;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.storage.ECDataCells;

@TestCategory("multiblock")
@GameTestHolder(EngineeredComputation.MODID)
public final class MultiblockGameTests
{
	static final String EMPTY_TEMPLATE = EngineeredComputation.MODID+":gametest/empty_8x8x8";
	private static final BlockPos ORIGIN = new BlockPos(3, 1, 3);

	public static final MultiblockStateHost NO_HOST = new MultiblockStateHost()
	{
		@Override
		public void setChanged()
		{
		}

		@Override
		public void syncToClient()
		{
		}
	};

	private MultiblockGameTests()
	{
	}

	@GameTestGenerator
	public static Collection<TestFunction> multiblockTests()
	{
		List<TestFunction> tests = new ArrayList<>();
		for(EraDrive drive : EraDrive.values())
			for(Direction facing : Direction.Plane.HORIZONTAL)
			{
				String name = drive.blockName()+"_"+facing.getName();
				tests.add(new TestFunction("multiblock", "break_part_"+name, EMPTY_TEMPLATE, 40, 0, true,
						helper -> formAndBreak(helper, drive, facing, false)));
				tests.add(new TestFunction("multiblock", "break_master_"+name, EMPTY_TEMPLATE, 40, 0, true,
						helper -> formAndBreak(helper, drive, facing, true)));
			}
		return tests;
	}

	private static void formAndBreak(GameTestHelper helper, EraDrive drive, Direction facing, boolean breakMaster)
	{
		ComputerEra era = drive.era();
		MultiblockDefinition<EraDriveState> definition = ECMultiblocks.ERA_DRIVES.get(drive);
		EraDriveGeometry geometry = drive.geometry();
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(ORIGIN);
		Block casing = ECContent.casing(era).get();
		List<BlockPos> templatePositions = geometry.shapes().keySet().stream()
				.sorted(Comparator.comparingLong(BlockPos::asLong)).toList();

		for(BlockPos pos : templatePositions)
			level.setBlockAndUpdate(world(origin, pos, facing), casing.defaultBlockState());
		BlockPos trigger = world(origin, geometry.trigger(), facing);
		Direction front = facing.getOpposite();

		BlockPos gap = templatePositions.stream().filter(p -> !p.equals(geometry.trigger())).findFirst().orElseThrow();
		level.removeBlock(world(origin, gap, facing), false);
		check(helper, !definition.tryForm(level, trigger, front), "formed with a block missing");
		level.setBlockAndUpdate(world(origin, gap, facing), casing.defaultBlockState());
		for(EraDrive other : otherEra(era).drives())
			check(helper, !ECMultiblocks.ERA_DRIVES.get(other).tryForm(level, trigger, front),
					"another era's drive formed from this era's casing");

		check(helper, definition.tryForm(level, trigger, front), "complete structure didn't form");

		MultiblockPartBlockEntity master = null;
		for(BlockPos pos : templatePositions)
		{
			BlockPos worldPos = world(origin, pos, facing);
			check(helper, level.getBlockState(worldPos).is(definition.partBlock()), "not a part at "+pos);
			check(helper, level.getBlockState(worldPos).getValue(MultiblockPartBlock.FACING)==facing,
					"wrong facing at "+pos);
			if(!(level.getBlockEntity(worldPos) instanceof MultiblockPartBlockEntity part))
			{
				helper.fail("no part block entity at "+pos);
				return;
			}
			check(helper, part.getPosInMultiblock().equals(pos), "part at "+pos+" thinks it's at "+part.getPosInMultiblock());
			check(helper, part.getMaster()!=null&&part.getMasterPos().equals(world(origin, geometry.master(), facing)),
					"part at "+pos+" can't find its master");
			if(part.isMaster())
				master = part;
		}
		if(master==null||!(master.getState() instanceof EraDriveState state))
		{
			helper.fail("no master with an era drive state");
			return;
		}

		BlockPos victim = breakMaster? geometry.master(): templatePositions.stream()
				.filter(p -> !p.equals(geometry.master())).findFirst().orElseThrow();
		BlockPos cellPart = templatePositions.stream().filter(p -> !geometry.hasStorageAt(p)).findFirst().orElseThrow();
		IItemHandler handler = level.getBlockEntity(world(origin, cellPart, facing))
				.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
		check(helper, handler!=null, "no item handler on a part");
		ItemStack cell = firstBayItem(era);
		ItemStack foreignCell = firstBayItem(otherEra(era));
		check(helper, !handler.insertItem(0, foreignCell, false).isEmpty(), "other era's cell accepted");
		check(helper, handler.insertItem(0, cell.copy(), false).isEmpty(), "own era's cell rejected");
		check(helper, handler.getSlots()==drive.slots(), "drive has "+handler.getSlots()+" slots, not "+drive.slots());

		CompoundTag saved = master.saveWithoutMetadata();
		EraDriveState reloaded = new EraDriveState(drive, NO_HOST);
		reloaded.load(saved.getCompound("state"));
		check(helper, ItemStack.isSameItem(reloaded.getCellSlots().getStackInSlot(0), cell), "cell lost on reload");

		BlockPos victimWorld = world(origin, victim, facing);
		level.destroyBlock(victimWorld, true);
		for(BlockPos pos : templatePositions)
		{
			BlockPos worldPos = world(origin, pos, facing);
			if(pos.equals(victim))
				check(helper, level.getBlockState(worldPos).isAir(), "broken part wasn't removed");
			else
				check(helper, level.getBlockState(worldPos).is(casing), "casing not restored at "+pos);
		}
		check(helper, countDropped(level, victimWorld, casing.asItem())==1, "broken part didn't drop its casing");
		check(helper, countDropped(level, victimWorld, cell.getItem())==1, "the drive's cell wasn't dropped");
		helper.succeed();
	}

	static int countDropped(ServerLevel level, BlockPos pos, Item item)
	{
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4),
				entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	static BlockPos world(BlockPos origin, BlockPos templatePos, Direction facing)
	{
		return origin.offset(MultiblockOrientation.toWorldOffset(templatePos, facing));
	}

	private static ItemStack firstBayItem(ComputerEra era)
	{
		return new ItemStack(era==ComputerEra.PUNCHCARD? ECCatalogue.decks().get(0).get()
				: ECDataCells.cellsOf(era).get(0).get());
	}

	private static ComputerEra otherEra(ComputerEra era)
	{
		return ComputerEra.values()[(era.ordinal()+1)%ComputerEra.values().length];
	}

	static void check(GameTestHelper helper, boolean condition, String failure)
	{
		if(!condition)
			helper.fail(failure);
	}
}

package com.muddykat.engineeredcomputation.recipe;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

@TestCategory("recipes")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class BlockDropGameTests
{
	private BlockDropGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void everyBlockWithAnItemHasALootTable(GameTestHelper helper)
	{
		for(Block block : modBlocks())
			if(block.asItem()!=Items.AIR)
				StorageGameTests.check(helper, helper.getLevel().getServer().reloadableRegistries()
						.getLootTable(block.getLootTable())!=LootTable.EMPTY, id(block)+" has an item but no loot table");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void everyBlockNeedingAToolIsMineable(GameTestHelper helper)
	{
		for(Block block : modBlocks())
		{
			BlockState state = block.defaultBlockState();
			if(state.requiresCorrectToolForDrops())
				StorageGameTests.check(helper, mineable(state), id(block)+" needs a tool but no tool mines it");
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void adaptersDropThemselves(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(BlockPos.ZERO);
		for(DeferredItem<BlockItem> item : ECAdapters.items())
		{
			Block block = item.get().getBlock();
			List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), level, pos, null, null,
					new ItemStack(Items.IRON_PICKAXE));
			StorageGameTests.check(helper, drops.size()==1&&drops.get(0).is(item.get())&&drops.get(0).getCount()==1,
					id(block)+" dropped "+drops);
		}
		helper.succeed();
	}

	private static List<Block> modBlocks()
	{
		return BuiltInRegistries.BLOCK.stream()
				.filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(EngineeredComputation.MODID))
				.toList();
	}

	private static String id(Block block)
	{
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}

	private static boolean mineable(BlockState state)
	{
		return state.is(BlockTags.MINEABLE_WITH_PICKAXE)||state.is(BlockTags.MINEABLE_WITH_AXE)
				||state.is(BlockTags.MINEABLE_WITH_SHOVEL)||state.is(BlockTags.MINEABLE_WITH_HOE);
	}
}

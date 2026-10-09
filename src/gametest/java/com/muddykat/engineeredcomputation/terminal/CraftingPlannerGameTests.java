package com.muddykat.engineeredcomputation.terminal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.crafting.CraftingPlan;
import com.muddykat.engineeredcomputation.crafting.CraftingPlanner;
import com.muddykat.engineeredcomputation.crafting.Pattern;
import com.muddykat.engineeredcomputation.crafting.ProvidedPattern;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.storage.ItemKey;

@TestCategory("crafting")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingPlannerGameTests
{
	private CraftingPlannerGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void chainsThroughSubCrafts(GameTestHelper helper)
	{
		ProvidedPattern planks = direct(Items.OAK_LOG, 1, Items.OAK_PLANKS, 4);
		ProvidedPattern sticks = direct(Items.OAK_PLANKS, 2, Items.STICK, 4);
		ProvidedPattern torches = direct2(Items.COAL, Items.STICK, Items.TORCH, 4);

		List<ProvidedPattern> patterns = List.of(planks, sticks, torches);
		Map<ItemKey, Long> stock = stock(Items.OAK_LOG, 1, Items.COAL, 1);
		CraftingPlan plan = CraftingPlanner.plan(key(Items.TORCH), 4, stock, patterns);

		StorageGameTests.check(helper, plan.isPossible(), "Plan should be possible");
		StorageGameTests.check(helper, plan.steps().size()==3, "Should have 3 steps");
		StorageGameTests.check(helper, fromStorage(plan, Items.OAK_LOG)==1, "Should take 1 OAK_LOG from storage");
		StorageGameTests.check(helper, fromStorage(plan, Items.COAL)==1, "Should take 1 COAL from storage");
		StorageGameTests.check(helper, crafted(plan, Items.TORCH)==4, "Should craft 4 TORCH");
		StorageGameTests.check(helper, crafted(plan, Items.STICK)==4, "Should craft 4 STICK");
		StorageGameTests.check(helper, crafted(plan, Items.OAK_PLANKS)==4, "Should craft 4 OAK_PLANKS");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void usesStockBeforeCrafting(GameTestHelper helper)
	{
		ProvidedPattern planks = direct(Items.OAK_LOG, 1, Items.OAK_PLANKS, 4);
		ProvidedPattern sticks = direct(Items.OAK_PLANKS, 2, Items.STICK, 4);
		ProvidedPattern torches = direct2(Items.COAL, Items.STICK, Items.TORCH, 4);

		List<ProvidedPattern> patterns = List.of(planks, sticks, torches);
		Map<ItemKey, Long> stock = stock(Items.COAL, 1, Items.STICK, 3);
		CraftingPlan plan = CraftingPlanner.plan(key(Items.TORCH), 4, stock, patterns);

		StorageGameTests.check(helper, plan.isPossible(), "Plan should be possible");
		StorageGameTests.check(helper, fromStorage(plan, Items.STICK)==1, "Should take 1 STICK from storage");
		StorageGameTests.check(helper, crafted(plan, Items.STICK)==0, "Should craft 0 STICK");
		StorageGameTests.check(helper, crafted(plan, Items.OAK_PLANKS)==0, "Should craft 0 OAK_PLANKS");
		StorageGameTests.check(helper, plan.steps().size()==1, "Should have 1 step");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void targetIsAlwaysCrafted(GameTestHelper helper)
	{
		ProvidedPattern planks = direct(Items.OAK_LOG, 1, Items.OAK_PLANKS, 4);
		ProvidedPattern sticks = direct(Items.OAK_PLANKS, 2, Items.STICK, 4);

		List<ProvidedPattern> patterns = List.of(planks, sticks);
		Map<ItemKey, Long> stock = stock(Items.STICK, 64, Items.OAK_PLANKS, 2);
		CraftingPlan plan = CraftingPlanner.plan(key(Items.STICK), 4, stock, patterns);

		StorageGameTests.check(helper, plan.isPossible(), "Plan should be possible");
		StorageGameTests.check(helper, crafted(plan, Items.STICK)==4, "Should craft 4 STICK");
		StorageGameTests.check(helper, fromStorage(plan, Items.OAK_PLANKS)==2, "Should take 2 OAK_PLANKS from storage");
		StorageGameTests.check(helper, fromStorage(plan, Items.STICK)==0, "Should take 0 STICK from storage");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void reportsWhatsMissing(GameTestHelper helper)
	{
		ProvidedPattern planks = direct(Items.OAK_LOG, 1, Items.OAK_PLANKS, 4);
		ProvidedPattern sticks = direct(Items.OAK_PLANKS, 2, Items.STICK, 4);
		ProvidedPattern torches = direct2(Items.COAL, Items.STICK, Items.TORCH, 4);

		List<ProvidedPattern> patterns = List.of(planks, sticks, torches);
		Map<ItemKey, Long> stock = stock(Items.OAK_LOG, 1);
		CraftingPlan plan = CraftingPlanner.plan(key(Items.TORCH), 4, stock, patterns);

		StorageGameTests.check(helper, !plan.isPossible(), "Plan should not be possible");
		StorageGameTests.check(helper, missing(plan, Items.COAL)==1, "Should be missing 1 COAL");
		StorageGameTests.check(helper, missing(plan, Items.STICK)==0, "Should not be missing STICK (can be crafted)");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void roundsUpToWholeRuns(GameTestHelper helper)
	{
		ProvidedPattern planks = direct(Items.OAK_LOG, 1, Items.OAK_PLANKS, 4);
		ProvidedPattern sticks = direct(Items.OAK_PLANKS, 2, Items.STICK, 4);

		List<ProvidedPattern> patterns = List.of(planks, sticks);
		Map<ItemKey, Long> stock = stock(Items.OAK_LOG, 10);
		CraftingPlan plan = CraftingPlanner.plan(key(Items.STICK), 5, stock, patterns);

		StorageGameTests.check(helper, plan.isPossible(), "Plan should be possible");
		StorageGameTests.check(helper, crafted(plan, Items.STICK)==8, "Should craft 8 STICK (2 runs of 4)");
		StorageGameTests.check(helper, fromStorage(plan, Items.OAK_LOG)==1, "Should take 1 OAK_LOG from storage (2 runs need 4 planks = 1 log run)");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void loopsAreMissingNotEndless(GameTestHelper helper)
	{
		ProvidedPattern toNuggets = direct(Items.IRON_INGOT, 1, Items.IRON_NUGGET, 9);
		ProvidedPattern toIngot = direct(Items.IRON_NUGGET, 9, Items.IRON_INGOT, 1);

		List<ProvidedPattern> patterns = List.of(toNuggets, toIngot);
		Map<ItemKey, Long> stock = new HashMap<>();
		CraftingPlan plan = CraftingPlanner.plan(key(Items.IRON_INGOT), 1, stock, patterns);

		StorageGameTests.check(helper, !plan.isPossible(), "Plan should not be possible");
		StorageGameTests.check(helper, missing(plan, Items.IRON_INGOT)>0, "Should be missing IRON_INGOT");
		helper.succeed();
	}

	private static ItemKey key(Item item)
	{
		return ItemKey.of(new ItemStack(item));
	}

	private static ProvidedPattern direct(Item in, int inCount, Item out, int outCount)
	{
		return ProvidedPattern.direct(new Pattern(Pattern.Mode.DIRECT, List.of(new ItemStack(in, inCount)),
				List.of(new ItemStack(out, outCount))));
	}

	private static ProvidedPattern direct2(Item a, Item b, Item out, int outCount)
	{
		return ProvidedPattern.direct(new Pattern(Pattern.Mode.DIRECT, List.of(new ItemStack(a), new ItemStack(b)),
				List.of(new ItemStack(out, outCount))));
	}

	private static Map<ItemKey, Long> stock(Object... itemsAndAmounts)
	{
		Map<ItemKey, Long> stock = new HashMap<>();
		for(int i = 0; i<itemsAndAmounts.length; i += 2)
			stock.put(key((Item)itemsAndAmounts[i]), ((Number)itemsAndAmounts[i+1]).longValue());
		return stock;
	}

	private static long fromStorage(CraftingPlan plan, Item item)
	{
		return plan.fromStorage().getOrDefault(key(item), 0L);
	}

	private static long crafted(CraftingPlan plan, Item item)
	{
		return plan.toCraft().getOrDefault(key(item), 0L);
	}

	private static long missing(CraftingPlan plan, Item item)
	{
		return plan.missing().getOrDefault(key(item), 0L);
	}
}

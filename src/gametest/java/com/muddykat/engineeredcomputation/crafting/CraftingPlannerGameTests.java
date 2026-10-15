package com.muddykat.engineeredcomputation.crafting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.crafting.pattern.ProvidedPattern;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.remote.RemoteSources;
import com.muddykat.engineeredcomputation.remote.StorageAnalyzerBlockEntity;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

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

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void beforeCraftingRequestsBeforeCrafting(GameTestHelper helper)
	{
		List<ProvidedPattern> patterns = List.of(direct(Items.OAK_PLANKS, 2, Items.STICK, 4),
				direct2(Items.COAL, Items.STICK, Items.TORCH, 4));
		Map<ItemKey, Long> stock = stock(Items.COAL, 1, Items.OAK_PLANKS, 2);
		CraftingPlan plan = CraftingPlanner.plan(key(Items.TORCH), 4, stock, patterns,
				remote(StorageAnalyzerBlockEntity.Priority.BEFORE_CRAFTING, Items.STICK, 8));

		StorageGameTests.check(helper, plan.isPossible(), "the plan should be possible");
		StorageGameTests.check(helper, plan.requested().getOrDefault(key(Items.STICK), 0L)==1,
				"the stick should be requested, got "+plan.requests());
		StorageGameTests.check(helper, crafted(plan, Items.STICK)==0&&fromStorage(plan, Items.OAK_PLANKS)==0,
				"no sticks should be crafted when one can be requested first");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void afterCraftingOnlyRequestsWhatCantBeCrafted(GameTestHelper helper)
	{
		Map<ItemKey, Long> stock = stock(Items.COAL, 1, Items.OAK_PLANKS, 2);
		List<RemoteSources.Source> remote = remote(StorageAnalyzerBlockEntity.Priority.AFTER_CRAFTING, Items.STICK, 8);
		CraftingPlan crafts = CraftingPlanner.plan(key(Items.TORCH), 4, stock, List.of(
				direct(Items.OAK_PLANKS, 2, Items.STICK, 4), direct2(Items.COAL, Items.STICK, Items.TORCH, 4)), remote);
		StorageGameTests.check(helper, crafts.requests().isEmpty()&&crafted(crafts, Items.STICK)==4,
				"sticks with a pattern should be crafted, got "+crafts.requests());

		CraftingPlan requests = CraftingPlanner.plan(key(Items.TORCH), 4, stock, List.of(
				direct2(Items.COAL, Items.STICK, Items.TORCH, 4)), remote);
		StorageGameTests.check(helper, requests.isPossible()&&requests.requested().getOrDefault(key(Items.STICK), 0L)==1,
				"a stick nothing can craft should be requested, got "+requests.requests());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void requestsOnlyIsLeftOutOfPlans(GameTestHelper helper)
	{
		CraftingPlan plan = CraftingPlanner.plan(key(Items.TORCH), 4, stock(Items.COAL, 1), List.of(
				direct2(Items.COAL, Items.STICK, Items.TORCH, 4)),
				remote(StorageAnalyzerBlockEntity.Priority.REQUESTS_ONLY, Items.STICK, 8));
		StorageGameTests.check(helper, !plan.isPossible()&&plan.requests().isEmpty()&&missing(plan, Items.STICK)==1,
				"a requests-only analyzer should be left out of plans");

		CraftingPlan request = CraftingPlanner.planRequest(key(Items.STICK), 5,
				remote(StorageAnalyzerBlockEntity.Priority.REQUESTS_ONLY, Items.STICK, 8));
		StorageGameTests.check(helper, request.isPossible()&&request.requested().get(key(Items.STICK))==5,
				"it should still take explicit requests, got "+request.requests());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theTargetIsNeverRequestedByACraft(GameTestHelper helper)
	{
		CraftingPlan plan = CraftingPlanner.plan(key(Items.STICK), 4, stock(Items.OAK_PLANKS, 2), List.of(
				direct(Items.OAK_PLANKS, 2, Items.STICK, 4)),
				remote(StorageAnalyzerBlockEntity.Priority.BEFORE_CRAFTING, Items.STICK, 64));
		StorageGameTests.check(helper, plan.requests().isEmpty()&&crafted(plan, Items.STICK)==4,
				"ordering a craft should craft it, not fetch it");

		CraftingPlan shortRequest = CraftingPlanner.planRequest(key(Items.STICK), 70,
				remote(StorageAnalyzerBlockEntity.Priority.BEFORE_CRAFTING, Items.STICK, 64));
		StorageGameTests.check(helper, !shortRequest.isPossible()&&missing(shortRequest, Items.STICK)==6,
				"a request for more than can be reached should be missing the rest");
		helper.succeed();
	}

	private static List<RemoteSources.Source> remote(StorageAnalyzerBlockEntity.Priority priority, Item item,
			long count)
	{
		return List.of(new RemoteSources.Source(BlockPos.ZERO, priority, Map.of(key(item), count)));
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

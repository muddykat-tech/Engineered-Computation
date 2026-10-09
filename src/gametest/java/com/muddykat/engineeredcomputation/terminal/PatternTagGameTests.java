package com.muddykat.engineeredcomputation.terminal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
public final class PatternTagGameTests
{
	private PatternTagGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void tagKeepsTheSlotsAmount(GameTestHelper helper)
	{
		Pattern p = abstractOf(List.of(new ItemStack(Items.COBBLESTONE, 4)), List.of())
				.withTag(false, 0, tag("minecraft:logs"));
		StorageGameTests.check(helper, p.input(0).getCount()==4, "Input count should be 4");
		StorageGameTests.check(helper, p.inputTag(0).equals(Optional.of(tag("minecraft:logs"))),
				"Input tag should match");
		StorageGameTests.check(helper, p.hasTags(), "Pattern should have tags");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void changingTheAmountKeepsTheTag(GameTestHelper helper)
	{
		Pattern p = abstractOf(List.of(new ItemStack(Items.COBBLESTONE, 4)), List.of())
				.withTag(false, 0, tag("minecraft:logs"));
		Pattern more = p.withInput(0, p.input(0).copyWithCount(6));
		StorageGameTests.check(helper, more.input(0).getCount()==6, "Input count should be 6");
		StorageGameTests.check(helper, more.inputTag(0).isPresent(), "Input tag should still be present");

		Pattern other = p.withInput(0, new ItemStack(Items.DIRT));
		StorageGameTests.check(helper, other.inputTag(0).isEmpty(), "Input tag should be empty");
		StorageGameTests.check(helper, !other.hasTags(), "Pattern should not have tags");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void directPatternsHaveNoTags(GameTestHelper helper)
	{
		Pattern p = new Pattern(Pattern.Mode.DIRECT, List.of(new ItemStack(Items.OAK_PLANKS)),
				List.of(new ItemStack(Items.STICK)), Map.of(0, tag("minecraft:planks")), Map.of());
		StorageGameTests.check(helper, p.inputTags().isEmpty(), "Input tags should be empty");
		StorageGameTests.check(helper, !p.hasTags(), "Pattern should not have tags");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void picksResolveInputTags(GameTestHelper helper)
	{
		Pattern p = abstractOf(List.of(new ItemStack(Items.COBBLESTONE, 2)), List.of(new ItemStack(Items.GOLD_INGOT)))
				.withTag(false, 0, tag("c:raw_materials"));
		Optional<Pattern> resolved = p.resolve(Map.of(0, Items.RAW_GOLD));
		StorageGameTests.check(helper, resolved.isPresent(), "Resolved pattern should be present");
		StorageGameTests.check(helper, resolved.get().input(0).is(Items.RAW_GOLD), "Input should be RAW_GOLD");
		StorageGameTests.check(helper, resolved.get().input(0).getCount()==2, "Input count should be 2");
		StorageGameTests.check(helper, resolved.get().inputTags().isEmpty(),
				"Resolved pattern should have no input tags");

		StorageGameTests.check(helper, p.resolve(Map.of()).isEmpty(), "Empty picks should result in empty resolution");
		StorageGameTests.check(helper, p.resolve(Map.of(0, Items.DIRT)).isEmpty(),
				"Invalid pick should result in empty resolution");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void outputTagsMatchAnyOfTheirItems(GameTestHelper helper)
	{
		Pattern p = anyOreToAnyIngot();
		StorageGameTests.check(helper, p.outputMatches(0, key(Items.IRON_INGOT)), "Should match iron ingot");
		StorageGameTests.check(helper, p.outputMatches(0, key(Items.COPPER_INGOT)), "Should match copper ingot");
		StorageGameTests.check(helper, !p.outputMatches(0, key(Items.DIRT)), "Should not match dirt");

		Pattern stone = abstractOf(List.of(new ItemStack(Items.COBBLESTONE)), List.of(new ItemStack(Items.STONE)));
		StorageGameTests.check(helper, stone.outputMatches(0, key(Items.STONE)), "Should match stone");
		StorageGameTests.check(helper, !stone.outputMatches(0, key(Items.SMOOTH_STONE)),
				"Should not match smooth stone");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void bigInputTagsWarn(GameTestHelper helper)
	{
		Pattern big = abstractOf(List.of(), List.of()).withTag(false, 0, tag("minecraft:logs"))
				.withTag(true, 0, tag("minecraft:logs"));
		StorageGameTests.check(helper, big.warnsAt(0), "Big input tag should warn");

		Pattern small = abstractOf(List.of(), List.of()).withTag(false, 0, tag("c:raw_materials/iron"));
		StorageGameTests.check(helper, !small.warnsAt(0), "Small input tag should not warn");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void tagsAreSaved(GameTestHelper helper)
	{
		Pattern p = anyOreToAnyIngot();
		var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		var json = Pattern.CODEC.encodeStart(ops, p).getOrThrow();
		Pattern parsed = Pattern.CODEC.parse(ops, json).getOrThrow();
		StorageGameTests.check(helper, parsed.equals(p), "Parsed pattern should equal original");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void plannerNeverChainsTagPatterns(GameTestHelper helper)
	{
		ProvidedPattern ingots = ProvidedPattern.abstractAt(anyOreToAnyIngot(), BlockPos.ZERO, 0);
		ProvidedPattern block = ProvidedPattern.direct(new Pattern(Pattern.Mode.DIRECT, List.of(new ItemStack(
				Items.IRON_INGOT, 9)), List.of(new ItemStack(Items.IRON_BLOCK))));
		CraftingPlan plan = CraftingPlanner.plan(key(Items.IRON_BLOCK), 1, Map.of(key(Items.RAW_IRON), 64L),
				List.of(ingots, block));
		StorageGameTests.check(helper, !plan.isPossible(), "Plan should not be possible");
		StorageGameTests.check(helper, plan.missing().getOrDefault(key(Items.IRON_INGOT), 0L)==9,
				"Missing iron ingots should be 9");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void offersArePlannedWithTheirPicks(GameTestHelper helper)
	{
		ProvidedPattern offer = ProvidedPattern.abstractAt(anyOreToAnyIngot(), BlockPos.ZERO, 0);
		CraftingPlan picked = CraftingPlanner.planOffer(offer.choosing(Map.of(0, Items.RAW_IRON)), 3,
				Map.of(key(Items.RAW_IRON), 5L), List.of());
		StorageGameTests.check(helper, picked.isPossible(), "Plan should be possible");
		StorageGameTests.check(helper, picked.fromStorage().getOrDefault(key(Items.RAW_IRON), 0L)==3,
				"Should use 3 raw iron");
		StorageGameTests.check(helper, picked.steps().size()==1, "Should have one step");
		StorageGameTests.check(helper, picked.steps().get(0).runs()==3, "Step should run 3 times");
		StorageGameTests.check(helper, picked.targetTag().equals(Optional.of(tag("c:ingots"))),
				"Target tag should be c:ingots");

		CraftingPlan unpicked = CraftingPlanner.planOffer(offer, 3, Map.of(key(Items.RAW_IRON), 5L), List.of());
		StorageGameTests.check(helper, !unpicked.isPossible(), "Unpicked plan should not be possible");
		helper.succeed();
	}

	private static TagKey<Item> tag(String id)
	{
		return ItemTags.create(ResourceLocation.parse(id));
	}

	private static ItemKey key(Item item)
	{
		return ItemKey.of(new ItemStack(item));
	}

	private static Pattern abstractOf(List<ItemStack> inputs, List<ItemStack> outputs)
	{
		return new Pattern(Pattern.Mode.ABSTRACT, inputs, outputs);
	}

	private static Pattern anyOreToAnyIngot()
	{
		return abstractOf(List.of(), List.of()).withTag(false, 0, tag("c:raw_materials"))
				.withTag(true, 0, tag("c:ingots"));
	}
}

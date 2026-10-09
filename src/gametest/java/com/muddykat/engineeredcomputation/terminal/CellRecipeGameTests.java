package com.muddykat.engineeredcomputation.terminal;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.catalogue.CardDeck;
import com.muddykat.engineeredcomputation.catalogue.CardDeckItem;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCard;
import com.muddykat.engineeredcomputation.crafting.Pattern;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.storage.CellContents;

@TestCategory({"storage", "recipes"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellRecipeGameTests
{
	private CellRecipeGameTests()
	{
	}

	private static ItemStack craft(ServerLevel level, int width, int height, List<ItemStack> stacks)
	{
		CraftingContainer input = Pattern.craftingGrid(width, height, stacks);
		Optional<CraftingRecipe> recipe =
				level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
		return recipe.isPresent()? recipe.get().assemble(input, level.registryAccess()): ItemStack.EMPTY;
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void decksAreMadeOfPaper(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		List<ItemStack> stacks = List.of(
				new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER),
				new ItemStack(Items.PAPER), new ItemStack(Items.REDSTONE), new ItemStack(Items.PAPER),
				new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER)
		);
		ItemStack result = craft(level, 3, 3, stacks);
		ItemStack expected = new ItemStack(ECCatalogue.decks().get(0).get());
		StorageGameTests.check(helper, !result.isEmpty()&&result.is(expected.getItem()),
				"paper and redstone should craft the smallest deck, got "+result);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void twoEmptyDecksMakeABiggerOne(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		List<ItemStack> stacks = List.of(
				new ItemStack(ECCatalogue.decks().get(0).get()),
				new ItemStack(ECCatalogue.decks().get(0).get())
		);
		ItemStack result = craft(level, 2, 1, stacks);
		ItemStack expected = new ItemStack(ECCatalogue.decks().get(1).get());
		StorageGameTests.check(helper, !result.isEmpty()&&result.is(expected.getItem()),
				"two empty decks should craft the next size, got "+result);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void filledDecksDontCombine(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		ItemStack stack1 = new ItemStack(ECCatalogue.decks().get(0).get());
		ItemStack stack2 = new ItemStack(ECCatalogue.decks().get(0).get());
		CardDeckItem.setDeck(stack1, CardDeck.EMPTY.with(new CatalogueCard(GlobalPos.of(helper.getLevel().dimension(),
				BlockPos.ZERO), Component.literal("Chest"), CellContents.EMPTY, CatalogueCard.NEVER)));
		List<ItemStack> stacks = List.of(stack1, stack2);
		ItemStack result = craft(level, 2, 1, stacks);
		StorageGameTests.check(helper, result.isEmpty(),
				"a deck holding a card and an empty one should not combine, got "+result);
		helper.succeed();
	}
}

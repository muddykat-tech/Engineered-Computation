package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.item.GuideItem;
import com.muddykat.engineeredcomputation.registration.ECContent;

@TestCategory({"guide", "recipes"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class GuideRecipeGameTests
{
	private GuideRecipeGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void guidesAreNeverMadeFromGuides(GameTestHelper helper)
	{
		for(ComputerEra era : ComputerEra.values())
			for(Ingredient ingredient : guideRecipe(helper, era).getIngredients())
				for(ItemStack stack : ingredient.getItems())
					StorageGameTests.check(helper, !(stack.getItem() instanceof GuideItem),
							era.guideName()+" is made from a guide");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void eachGuideUsesThePreviousErasPart(GameTestHelper helper)
	{
		for(int i = 1; i<ComputerEra.values().length; i++)
		{
			ComputerEra era = ComputerEra.values()[i];
			ComputerEra previous = ComputerEra.values()[i-1];
			boolean found = false;
			for(Ingredient ingredient : guideRecipe(helper, era).getIngredients())
				for(ItemStack stack : ingredient.getItems())
					found |= stack.getItem()==ECContent.component(previous).get();
			StorageGameTests.check(helper, found, era.guideName()+" doesn't use the "+previous.componentName());
		}
		helper.succeed();
	}

	private static Recipe<?> guideRecipe(GameTestHelper helper, ComputerEra era)
	{
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, era.guideName());
		RecipeHolder<?> holder = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
		if(holder==null)
			helper.fail(era.guideName()+" has no recipe");
		return holder.value();
	}
}

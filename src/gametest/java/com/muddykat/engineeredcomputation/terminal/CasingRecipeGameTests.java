package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.registration.ECContent;

@TestCategory("recipes")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CasingRecipeGameTests
{
	private CasingRecipeGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void eachCasingIsMadeFromThePreviousErasCasing(GameTestHelper helper)
	{
		for(int i = 1; i<ComputerEra.values().length; i++)
		{
			ComputerEra era = ComputerEra.values()[i];
			ComputerEra previous = ComputerEra.values()[i-1];
			Recipe<?> recipe = casingRecipe(helper, era);
			StorageGameTests.check(helper, uses(recipe, ECContent.casingItem(previous).get())==4,
					era.casingName()+" isn't made from four "+previous.casingName());
			StorageGameTests.check(helper, uses(recipe, ECContent.component(era).get())==1,
					era.casingName()+" isn't made round a "+era.componentName());
			StorageGameTests.check(helper, recipe.getResultItem(helper.getLevel().registryAccess()).getCount()==8,
					era.casingName()+" doesn't make 8");
		}
		helper.succeed();
	}

	private static int uses(Recipe<?> recipe, Item item)
	{
		int count = 0;
		for(Ingredient ingredient : recipe.getIngredients())
			if(ingredient.test(new ItemStack(item)))
				count++;
		return count;
	}

	private static Recipe<?> casingRecipe(GameTestHelper helper, ComputerEra era)
	{
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, era.casingName());
		RecipeHolder<?> holder = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
		if(holder==null)
			helper.fail(era.casingName()+" has no recipe");
		return holder.value();
	}
}

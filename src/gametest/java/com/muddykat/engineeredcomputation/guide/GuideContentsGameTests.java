package com.muddykat.engineeredcomputation.guide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

@TestCategory("guide")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class GuideContentsGameTests
{
	private GuideContentsGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void everyGuideHoldsWhatItsRecipesNeed(GameTestHelper helper)
	{
		RecipeManager recipes = helper.getLevel().getRecipeManager();
		for(ComputerEra era : ComputerEra.values())
		{
			List<ResourceLocation> all = new ArrayList<>(GuideRecipes.shown(era));
			all.addAll(GuideRecipes.prerequisites(GuideRecipes.shown(era), recipes, helper.getLevel().registryAccess()));

			Set<Item> made = new HashSet<>();
			for(ResourceLocation id : all)
			{
				StorageGameTests.check(helper, recipes.byKey(id).isPresent(), era.guideName()+": no recipe "+id);
				made.add(recipe(recipes, id).value().getResultItem(helper.getLevel().registryAccess()).getItem());
			}

			for(ResourceLocation id : all)
				for(Ingredient ingredient : recipe(recipes, id).value().getIngredients())
				{
					ItemStack[] options = ingredient.getItems();
					boolean modOnly = options.length>0;
					boolean found = false;
					for(ItemStack option : options)
					{
						modOnly &= BuiltInRegistries.ITEM.getKey(option.getItem()).getNamespace().equals(EngineeredComputation.MODID);
						found |= made.contains(option.getItem());
					}
					if(modOnly&&!found)
						helper.fail(era.guideName()+": "+id+" needs "+BuiltInRegistries.ITEM.getKey(options[0].getItem())
								+" but the guide has no recipe for it");
				}
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void earlierPartsComeBeforeWhatTheyMake(GameTestHelper helper)
	{
		List<ResourceLocation> earlier = GuideRecipes.prerequisites(GuideRecipes.shown(ComputerEra.FLOPPY),
				helper.getLevel().getRecipeManager(), helper.getLevel().registryAccess());
		String[] paths = {"relay", "vacuum_tube", "transistor", "integrated_circuit"};
		for(String path : paths)
			StorageGameTests.check(helper, earlier.contains(id(path)), "the Palm Pilot is missing the "+path+" recipe");
		for(int i = 0; i+1<paths.length; i++)
			StorageGameTests.check(helper, earlier.indexOf(id(paths[i]))<earlier.indexOf(id(paths[i+1])),
					paths[i]+" doesn't come before "+paths[i+1]);
		helper.succeed();
	}

	private static RecipeHolder<?> recipe(RecipeManager recipes, ResourceLocation id)
	{
		return recipes.byKey(id).orElseThrow();
	}

	private static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, path);
	}
}

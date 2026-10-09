package com.muddykat.engineeredcomputation.client.guide;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

final class RecipePage implements GuidePage
{
	private static final int GRID_Y = 24;
	private static final int ARROW_X = 60;
	private static final int RESULT_X = 84;
	private static final int RESULT_SIZE = 26;
	private static final int CYCLE_MILLIS = 1000;

	private final ResourceLocation recipe;

	RecipePage(ResourceLocation recipe)
	{
		this.recipe = recipe;
	}

	@Nullable
	static Recipe<?> lookUp(ResourceLocation id)
	{
		Level level = Minecraft.getInstance().level;
		return level==null? null: level.getRecipeManager().byKey(id).orElse(null);
	}

	static ItemStack result(ResourceLocation id)
	{
		Recipe<?> recipe = lookUp(id);
		Level level = Minecraft.getInstance().level;
		return recipe==null||level==null? ItemStack.EMPTY: recipe.getResultItem(level.registryAccess());
	}

	@Override
	public void render(GuideScreen screen, GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick)
	{
		Recipe<?> value = lookUp(recipe);
		Level level = Minecraft.getInstance().level;
		if(value==null||level==null)
		{
			graphics.drawString(screen.font(), Component.translatable("guide.engineered_computation.recipe_missing"),
					x, y, screen.textColour(), false);
			return;
		}
		ItemStack result = value.getResultItem(level.registryAccess());

		List<FormattedCharSequence> title = screen.font().split(result.getHoverName().copy()
				.withStyle(ChatFormatting.BOLD), WIDTH);
		for(int i = 0; i<Math.min(2, title.size()); i++)
			graphics.drawString(screen.font(), title.get(i), x+(WIDTH-screen.font().width(title.get(i)))/2,
					y+i*TextPage.LINE_HEIGHT, screen.textColour(), false);

		NonNullList<Ingredient> ingredients = value.getIngredients();
		int width = value instanceof ShapedRecipe shaped? shaped.getWidth(): 3;
		int gridY = y+GRID_Y;
		long cycle = Util.getMillis()/CYCLE_MILLIS;
		for(int row = 0; row<3; row++)
			for(int col = 0; col<3; col++)
			{
				int slotX = x+col*18;
				int slotY = gridY+row*18;
				screen.drawSlot(graphics, slotX, slotY, 18);
				int index = row*width+col;
				if(col<width&&index<ingredients.size())
					screen.renderStack(graphics, pick(ingredients.get(index), cycle), slotX+1, slotY+1, mouseX, mouseY);
			}

		screen.drawArrow(graphics, x+ARROW_X, gridY+27);
		int resultY = gridY+27-RESULT_SIZE/2;
		screen.drawSlot(graphics, x+RESULT_X, resultY, RESULT_SIZE);
		screen.renderStack(graphics, result, x+RESULT_X+5, resultY+5, mouseX, mouseY);

		if(!(value instanceof ShapedRecipe))
			screen.drawFitted(graphics, Component.translatable("guide.engineered_computation.recipe.shapeless"),
					x, gridY+3*18+6, WIDTH, screen.textColour());
	}

	private static ItemStack pick(Ingredient ingredient, long cycle)
	{
		ItemStack[] options = ingredient.getItems();
		return options.length==0? ItemStack.EMPTY: options[Mth.floor(cycle%options.length)];
	}
}

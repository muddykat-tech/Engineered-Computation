package com.muddykat.engineeredcomputation.pneumatic;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class WindowedTubeItem extends BlockItem
{
	public WindowedTubeItem(Block block, Properties properties)
	{
		super(block, properties);
	}

	@Nullable
	@Override
	protected BlockState getPlacementState(BlockPlaceContext context)
	{
		BlockState state = super.getPlacementState(context);
		if(state==null||!state.hasProperty(PneumaticTubeBlock.WINDOWED))
			return state;
		state = state.setValue(PneumaticTubeBlock.WINDOWED, true);
		Player player = context.getPlayer();
		if(player!=null)
		{
			InteractionHand other = context.getHand()==InteractionHand.MAIN_HAND? InteractionHand.OFF_HAND
					: InteractionHand.MAIN_HAND;
			if(player.getItemInHand(other).getItem() instanceof DyeItem dye)
				state = state.setValue(PneumaticTubeBlock.TINT, PneumaticTubeBlock.tintOf(dye.getDyeColor()));
		}
		return state;
	}

	@Override
	public String getDescriptionId()
	{
		return getOrCreateDescriptionId();
	}

	@Override
	public void registerBlocks(Map<Block, Item> blockToItemMap, Item item)
	{
	}
}

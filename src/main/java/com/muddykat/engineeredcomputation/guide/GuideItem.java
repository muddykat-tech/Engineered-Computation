package com.muddykat.engineeredcomputation.guide;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.client.ClientHooks;
import com.muddykat.engineeredcomputation.era.ComputerEra;

public class GuideItem extends Item
{
	private final ComputerEra era;

	public GuideItem(ComputerEra era, Properties properties)
	{
		super(properties);
		this.era = era;
	}

	public ComputerEra getEra()
	{
		return era;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		if(level.isClientSide)
			ClientHooks.openGuide(era);
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag)
	{
		lines.add(Component.translatable("tooltip.engineered_computation.guide.covers", era.displayName())
				.withStyle(ChatFormatting.GRAY));
	}
}

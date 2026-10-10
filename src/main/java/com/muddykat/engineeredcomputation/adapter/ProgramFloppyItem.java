package com.muddykat.engineeredcomputation.adapter;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.registration.ECDataComponents;

public class ProgramFloppyItem extends Item implements ProgrammableItem
{
	public ProgramFloppyItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public int programSize()
	{
		return Program.SIZE;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		if(player instanceof ServerPlayer serverPlayer)
			ProgrammableItem.open(serverPlayer, hand);
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag)
	{
		Program program = stack.getOrDefault(ECDataComponents.PROGRAM.get(), Program.EMPTY);
		lines.add(Component.translatable("tooltip.engineered_computation.program_floppy.era")
				.withStyle(ChatFormatting.DARK_GRAY));
		if(program.isEmpty())
		{
			lines.add(Component.translatable("tooltip.engineered_computation.program_floppy.blank")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		for(ProgramEntry entry : program.entries())
		{
			entry.item().ifPresent(item -> lines.add(Component.literal("• ").append(item.value().getDescription())
					.withStyle(ChatFormatting.GRAY)));
			entry.tag().ifPresent(tag -> lines.add(Component.literal("• #"+tag.location())
					.withStyle(ChatFormatting.DARK_AQUA)));
		}
	}
}

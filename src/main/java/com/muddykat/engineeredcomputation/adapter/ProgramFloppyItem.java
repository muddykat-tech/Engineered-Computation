package com.muddykat.engineeredcomputation.adapter;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class ProgramFloppyItem extends Item
{
	private static final String PROGRAM = "program";

	public static Program program(ItemStack stack)
	{
		CompoundTag tag = stack.getTag();
		return tag==null||!tag.contains(PROGRAM)? Program.EMPTY
				: Program.CODEC.parse(NbtOps.INSTANCE, tag.get(PROGRAM)).result().orElse(Program.EMPTY);
	}

	public static void setProgram(ItemStack stack, Program program)
	{
		Program.CODEC.encodeStart(NbtOps.INSTANCE, program).result()
				.ifPresent(encoded -> stack.getOrCreateTag().put(PROGRAM, encoded));
	}

	public ProgramFloppyItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		if(player instanceof ServerPlayer serverPlayer)
			NetworkHooks.openGui(serverPlayer, new SimpleMenuProvider(
					(id, inventory, p) -> new ProgramFloppyMenu(id, inventory, hand),
					player.getItemInHand(hand).getHoverName()), buf -> buf.writeEnum(hand));
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag)
	{
		Program program = program(stack);
		lines.add(new TranslatableComponent("tooltip.engineered_computation.program_floppy.era")
				.withStyle(ChatFormatting.DARK_GRAY));
		if(program.isEmpty())
		{
			lines.add(new TranslatableComponent("tooltip.engineered_computation.program_floppy.blank")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		for(ProgramEntry entry : program.entries())
		{
			entry.item().ifPresent(item -> lines.add(new TextComponent("• ").append(item.value().getDescription())
					.withStyle(ChatFormatting.GRAY)));
			entry.tag().ifPresent(tag -> lines.add(new TextComponent("• #"+tag.location())
					.withStyle(ChatFormatting.DARK_AQUA)));
		}
	}
}

package com.muddykat.engineeredcomputation.storage;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.terminal.FloppyTerminalBlock;

public class EraDataCell extends Item
{
	public static final int MIN_PRIORITY = -99;
	public static final int MAX_PRIORITY = 99;

	private final ComputerEra era;
	private final int totalBytes;
	private final int bytesPerType;
	private final int totalTypes;
	private final boolean terminalDisk;

	public EraDataCell(Properties properties, ComputerEra era, int totalBytes, int bytesPerType, int totalTypes)
	{
		this(properties, era, totalBytes, bytesPerType, totalTypes, false);
	}

	public EraDataCell(Properties properties, ComputerEra era, int totalBytes, int bytesPerType, int totalTypes,
			boolean terminalDisk)
	{
		super(properties.stacksTo(1));
		this.era = era;
		this.totalBytes = totalBytes;
		this.bytesPerType = bytesPerType;
		this.totalTypes = totalTypes;
		this.terminalDisk = terminalDisk;
	}

	public ComputerEra getEra()
	{
		return era;
	}

	public int getTotalBytes()
	{
		return totalBytes;
	}

	public int getBytesPerType()
	{
		return bytesPerType;
	}

	public int getTotalTypes()
	{
		return totalTypes;
	}

	public boolean isTerminalDisk()
	{
		return terminalDisk;
	}

	public static boolean isCellOfEra(ItemStack stack, ComputerEra era)
	{
		return stack.getItem() instanceof EraDataCell cell&&cell.getEra()==era&&!cell.terminalDisk;
	}

	public static boolean isTerminalDisk(ItemStack stack)
	{
		return stack.getItem() instanceof EraDataCell cell&&cell.terminalDisk;
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Player player = context.getPlayer();
		if(player==null||!player.isSecondaryUseActive())
			return InteractionResult.PASS;
		if(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MultiblockPartBlockEntity part)
			return EraDriveState.insertIntoBay(part, player, context.getHand(), context.getClickLocation());
		return FloppyTerminalBlock.insertIntoBay(context.getLevel(), context.getClickedPos(), player,
				context.getHand(), context.getClickLocation());
	}

	public static ItemStack takeFromHand(Player player, InteractionHand hand)
	{
		ItemStack held = player.getItemInHand(hand);
		ItemStack taken = held.copyWithCount(1);
		held.shrink(1);
		player.setItemInHand(hand, held.isEmpty()? ItemStack.EMPTY: held.copy());
		return taken;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag)
	{
		CellInventory inventory = CellInventory.of(stack, () -> {
		});
		if(inventory==null)
			return;
		lines.add(Component.translatable("tooltip.engineered_computation.cell.bytes",
				ByteSize.format(inventory.getUsedBytes()), ByteSize.format(inventory.getTotalBytes()))
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("tooltip.engineered_computation.cell.types",
				inventory.getContents().types(), inventory.getTotalTypes()).withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("tooltip.engineered_computation.cell.role."
				+inventory.getRole().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_AQUA));
		if(inventory.getPriority()!=0)
			lines.add(Component.translatable("tooltip.engineered_computation.cell.priority",
					String.format(Locale.ROOT, "%+d", inventory.getPriority())).withStyle(ChatFormatting.GOLD));
	}
}

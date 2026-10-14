package com.muddykat.engineeredcomputation.storage.cell;

import java.io.IOException;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.storage.ByteSize;

public final class CellStorageCommand
{
	private static final String KEY = "commands."+EngineeredComputation.MODID+".cells.compact.";
	private static final String CLEANUP = "commands."+EngineeredComputation.MODID+".cells.cleanup.";
	private static final String CONFIRM = "/engineeredcomputation cells cleanup confirm";

	private CellStorageCommand()
	{
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("engineeredcomputation")
				.requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
				.then(Commands.literal("cells")
						.then(Commands.literal("compact").executes(CellStorageCommand::compact))
						.then(Commands.literal("cleanup").executes(CellStorageCommand::warn)
								.then(Commands.literal("confirm").executes(CellStorageCommand::cleanup)))));
	}

	private static int warn(CommandContext<CommandSourceStack> context)
	{
		Component command = Component.literal(CONFIRM).withStyle(style -> style.withColor(ChatFormatting.YELLOW)
				.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, CONFIRM)));
		context.getSource().sendSuccess(() -> Component.translatable(CLEANUP+"warning", command), false);
		return 1;
	}

	private static int cleanup(CommandContext<CommandSourceStack> context)
	{
		CommandSourceStack source = context.getSource();
		CellSweep sweep = CellSweep.start(source.getServer(), message -> source.sendSuccess(() -> message, true));
		if(sweep==null)
		{
			source.sendFailure(Component.translatable(CLEANUP+"running"));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable(CLEANUP+"started"), true);
		return 1;
	}

	private static int compact(CommandContext<CommandSourceStack> context)
	{
		CommandSourceStack source = context.getSource();
		try
		{
			CellStorage.Compaction compaction = CellStorage.get(source.getServer()).compact(ECConfig.sectorSize());
			if(compaction==null)
			{
				source.sendFailure(Component.translatable(KEY+"bulk"));
				return 0;
			}
			if(compaction.sectorBefore()==compaction.sectorAfter())
				source.sendSuccess(() -> Component.translatable(KEY+"done", ByteSize.format(compaction.bytesBefore()),
						ByteSize.format(compaction.bytesAfter())), true);
			else
				source.sendSuccess(() -> Component.translatable(KEY+"resized", ByteSize.format(compaction.bytesBefore()),
						ByteSize.format(compaction.bytesAfter()), compaction.sectorBefore(), compaction.sectorAfter()), true);
			return 1;
		}
		catch(IOException e)
		{
			EngineeredComputation.LOGGER.error("Could not compact cell storage", e);
			source.sendFailure(Component.translatable(KEY+"failed"));
			return 0;
		}
	}
}

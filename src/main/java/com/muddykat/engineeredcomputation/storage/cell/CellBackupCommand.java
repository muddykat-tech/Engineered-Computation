package com.muddykat.engineeredcomputation.storage.cell;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.muddykat.engineeredcomputation.EngineeredComputation;

/** Lists the cells cleanup backed up, a page at a time, and restores them as items. */
public final class CellBackupCommand
{
	static final String COMMAND = "/engineeredcomputation cells ";
	private static final String KEY = "commands."+EngineeredComputation.MODID+".cells.";
	private static final int PAGE_SIZE = 8;
	private static final int HOVER_ITEMS = 10;

	private static final SuggestionProvider<CommandSourceStack> BACKUPS = (context, builder) -> {
		try
		{
			return SharedSuggestionProvider.suggest(CellBackup.names(storage(context)), builder);
		}
		catch(IOException e)
		{
			return Suggestions.empty();
		}
	};

	private static final SuggestionProvider<CommandSourceStack> CELLS = (context, builder) -> {
		try
		{
			Path file = CellBackup.path(storage(context), StringArgumentType.getString(context, "name"));
			if(file==null)
				return Suggestions.empty();
			return SharedSuggestionProvider.suggest(CellBackup.read(file, storage(context).records()).stream()
					.map(entry -> entry.id().toString()), builder);
		}
		catch(IOException|IllegalArgumentException e)
		{
			return Suggestions.empty();
		}
	};

	private CellBackupCommand()
	{
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("engineeredcomputation")
				.requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
				.then(Commands.literal("cells")
						.then(Commands.literal("backups").executes(context -> listBackups(context, 1))
								.then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(
										context -> listBackups(context, IntegerArgumentType.getInteger(context, "page")))))
						.then(Commands.literal("backup")
								.then(Commands.argument("name", StringArgumentType.word()).suggests(BACKUPS)
										.executes(context -> listCells(context, 1))
										.then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(
												context -> listCells(context, IntegerArgumentType.getInteger(context, "page"))))))
						.then(Commands.literal("restore")
								.then(Commands.argument("name", StringArgumentType.word()).suggests(BACKUPS)
										.then(Commands.argument("cell", UuidArgument.uuid()).suggests(CELLS)
												.then(Commands.literal("give")
														.executes(context -> give(context, context.getSource().getPlayerOrException()))
														.then(Commands.argument("player", EntityArgument.player()).executes(
																context -> give(context, EntityArgument.getPlayer(context, "player")))))
												.then(Commands.literal("drop")
														.executes(context -> drop(context, context.getSource().getPosition()))
														.then(Commands.argument("pos", Vec3Argument.vec3()).executes(
																context -> drop(context, Vec3Argument.getVec3(context, "pos"))))))))));
	}

	private static int listBackups(CommandContext<CommandSourceStack> context, int page)
	{
		CommandSourceStack source = context.getSource();
		CellStorage storage = storage(context);
		try
		{
			List<String> names = CellBackup.names(storage);
			if(names.isEmpty())
			{
				source.sendFailure(Component.translatable(KEY+"backups.none"));
				return 0;
			}
			int pages = pages(names.size());
			int shown = Math.min(page, pages);
			source.sendSuccess(() -> Component.translatable(KEY+"backups.header", shown, pages), false);
			for(String name : pageOf(names, shown))
			{
				int cells = CellBackup.read(CellBackup.fileFor(storage, name), storage.records()).size();
				source.sendSuccess(() -> Component.literal("  ")
						.append(link(Component.literal(name), COMMAND+"backup "+name,
								Component.translatable(KEY+"backups.view"), ChatFormatting.AQUA))
						.append(" ")
						.append(Component.translatable(KEY+"backups.entry", cells).withStyle(ChatFormatting.GRAY)), false);
			}
			sendPages(source, COMMAND+"backups ", shown, pages);
			return 1;
		}
		catch(IOException e)
		{
			return failed(source, e);
		}
	}

	private static int listCells(CommandContext<CommandSourceStack> context, int page)
	{
		CommandSourceStack source = context.getSource();
		CellStorage storage = storage(context);
		String name = StringArgumentType.getString(context, "name");
		Path file = CellBackup.path(storage, name);
		if(file==null)
		{
			source.sendFailure(Component.translatable(KEY+"restore.no_backup", name));
			return 0;
		}
		try
		{
			List<CellBackup.Entry> entries = CellBackup.read(file, storage.records());
			int pages = pages(entries.size());
			int shown = Math.min(page, pages);
			source.sendSuccess(() -> Component.translatable(KEY+"backup.header", name, entries.size(), shown, pages),
					false);
			for(CellBackup.Entry entry : pageOf(entries, shown))
			{
				String restore = COMMAND+"restore "+name+" "+entry.id();
				source.sendSuccess(() -> Component.literal("  ")
						.append(Component.literal(shortId(entry.id())).withStyle(style -> style
								.withColor(ChatFormatting.GRAY)
								.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, describe(entry)))))
						.append(" ")
						.append(Component.translatable(KEY+"backup.entry", entry.contents().types(),
								entry.contents().totalCount()).withStyle(style -> style
								.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, describe(entry)))))
						.append(" ")
						.append(link(Component.translatable(KEY+"backup.give"), restore+" give",
								Component.translatable(KEY+"backup.give.hover"), ChatFormatting.GREEN))
						.append(" ")
						.append(link(Component.translatable(KEY+"backup.drop"), restore+" drop",
								Component.translatable(KEY+"backup.drop.hover"), ChatFormatting.GOLD)), false);
			}
			sendPages(source, COMMAND+"backup "+name+" ", shown, pages);
			return 1;
		}
		catch(IOException e)
		{
			return failed(source, e);
		}
	}

	private static int give(CommandContext<CommandSourceStack> context, ServerPlayer player)
			throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		try
		{
			CellRestore.Restored restored = restore(context);
			ItemStack stack = restored.stack();
			Component cell = stack.getHoverName();
			if(!player.getInventory().add(stack)||!stack.isEmpty())
			{
				ItemEntity dropped = player.drop(stack, false);
				if(dropped!=null)
				{
					dropped.setNoPickUpDelay();
					dropped.setTarget(player.getUUID());
				}
			}
			player.containerMenu.broadcastChanges();
			source.sendSuccess(() -> Component.translatable(KEY+"restore.given", cell, shortId(cellId(context)),
					player.getDisplayName()), true);
			reportGuess(source, restored, cell);
			return 1;
		}
		catch(IOException e)
		{
			return failed(source, e);
		}
	}

	private static int drop(CommandContext<CommandSourceStack> context, Vec3 pos) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		try
		{
			CellRestore.Restored restored = restore(context);
			Component cell = restored.stack().getHoverName();
			ServerLevel level = source.getLevel();
			ItemEntity dropped = new ItemEntity(level, pos.x, pos.y, pos.z, restored.stack());
			dropped.setDefaultPickUpDelay();
			level.addFreshEntity(dropped);
			source.sendSuccess(() -> Component.translatable(KEY+"restore.dropped", cell, shortId(cellId(context)),
					String.format(Locale.ROOT, "%.1f", pos.x), String.format(Locale.ROOT, "%.1f", pos.y),
					String.format(Locale.ROOT, "%.1f", pos.z)), true);
			reportGuess(source, restored, cell);
			return 1;
		}
		catch(IOException e)
		{
			return failed(source, e);
		}
	}

	private static CellRestore.Restored restore(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException, IOException
	{
		return CellRestore.restore(storage(context), StringArgumentType.getString(context, "name"), cellId(context));
	}

	private static void reportGuess(CommandSourceStack source, CellRestore.Restored restored, Component cell)
	{
		if(restored.guessedType())
			source.sendSuccess(() -> Component.translatable(KEY+"restore.guessed", cell)
					.withStyle(ChatFormatting.GRAY), false);
	}

	/** The cell's type and what it holds, most plentiful first. */
	private static Component describe(CellBackup.Entry entry)
	{
		MutableComponent text = entry.cell()==null
				? Component.translatable(KEY+"backup.unknown").withStyle(ChatFormatting.GRAY)
				: Component.empty().append(entry.cell().getDescription()).withStyle(ChatFormatting.AQUA);
		List<CellContents.Entry> items = entry.contents().entries().stream()
				.sorted((a, b) -> Long.compare(b.count(), a.count())).toList();
		for(CellContents.Entry item : items.subList(0, Math.min(HOVER_ITEMS, items.size())))
			text.append("\n").append(Component.literal(item.count()+" × ")
					.append(item.key().displayStack().getHoverName()).withStyle(ChatFormatting.WHITE));
		if(items.size()>HOVER_ITEMS)
			text.append("\n").append(Component.translatable(KEY+"backup.more", items.size()-HOVER_ITEMS)
					.withStyle(ChatFormatting.GRAY));
		return text;
	}

	private static void sendPages(CommandSourceStack source, String command, int page, int pages)
	{
		if(pages<=1)
			return;
		MutableComponent line = Component.literal("  ");
		line.append(page>1? link(Component.translatable(KEY+"page.previous"), command+(page-1), null,
				ChatFormatting.YELLOW): Component.translatable(KEY+"page.previous").withStyle(ChatFormatting.DARK_GRAY));
		line.append(" ").append(Component.translatable(KEY+"page", page, pages)).append(" ");
		line.append(page<pages? link(Component.translatable(KEY+"page.next"), command+(page+1), null,
				ChatFormatting.YELLOW): Component.translatable(KEY+"page.next").withStyle(ChatFormatting.DARK_GRAY));
		source.sendSuccess(() -> line, false);
	}

	private static MutableComponent link(MutableComponent text, String command, @Nullable Component hover,
			ChatFormatting colour)
	{
		return text.withStyle(style -> {
			style = style.withColor(colour).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
			return hover==null? style: style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
		});
	}

	private static int pages(int entries)
	{
		return Math.max(1, (entries+PAGE_SIZE-1)/PAGE_SIZE);
	}

	private static <T> List<T> pageOf(List<T> all, int page)
	{
		int from = (page-1)*PAGE_SIZE;
		return all.subList(Math.min(from, all.size()), Math.min(from+PAGE_SIZE, all.size()));
	}

	private static UUID cellId(CommandContext<CommandSourceStack> context)
	{
		return UuidArgument.getUuid(context, "cell");
	}

	static String shortId(UUID id)
	{
		return id.toString().substring(0, 8);
	}

	private static CellStorage storage(CommandContext<CommandSourceStack> context)
	{
		return CellStorage.get(context.getSource().getServer());
	}

	private static int failed(CommandSourceStack source, IOException e)
	{
		EngineeredComputation.LOGGER.error("Could not read or update a cell backup", e);
		source.sendFailure(Component.translatable(KEY+"restore.failed"));
		return 0;
	}
}

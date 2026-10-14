package com.muddykat.engineeredcomputation.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.cell.CellBackup;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.CellInventory;
import com.muddykat.engineeredcomputation.storage.cell.CellRestore;
import com.muddykat.engineeredcomputation.storage.cell.CellStorage;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellRecords;

@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellRestoreGameTests
{
	private CellRestoreGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void backupsRecordTheCellType(GameTestHelper helper) throws Exception
	{
		CellStorage storage = CellStorage.get(helper.getLevel().getServer());
		EraDataCell type = ECDataCells.cellsOf(ComputerEra.DISK).get(1).get();
		UUID id = filledCell(type, 3);
		Path file = backupFile(storage);
		storage.removeWithBackup(Set.of(id), file);
		List<CellBackup.Entry> entries = CellBackup.read(file, records(helper));
		check(helper, entries.size()==1&&entries.getFirst().cell()==type,
				"the backup didn't record the cell type: "+entries);
		Files.delete(file);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void giveRestoresIntoTheInventoryOnce(GameTestHelper helper) throws Exception
	{
		MinecraftServer server = helper.getLevel().getServer();
		CellStorage storage = CellStorage.get(server);
		EraDataCell type = ECDataCells.cellsOf(ComputerEra.DRUM).getLast().get();
		UUID id = filledCell(type, 4);
		CellContents contents = storage.contents(id);
		Path file = backupFile(storage);
		storage.removeWithBackup(Set.of(id), file);

		ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "cell_restore"));
		String command = "engineeredcomputation cells restore "+name(file)+" "+id+" give";
		int result = server.getCommands().getDispatcher().execute(command, player.createCommandSourceStack()
				.withPermission(4));
		check(helper, result==1, "the give command failed");
		ItemStack given = player.getInventory().items.stream().filter(stack -> stack.is(type)).findFirst()
				.orElse(ItemStack.EMPTY);
		check(helper, !given.isEmpty(), "the restored cell isn't in the player's inventory");
		check(helper, !id.equals(given.get(ECDataComponents.CELL_UUID.get())), "the restored cell kept its old id");
		check(helper, CellInventory.peek(given).getContents().equals(contents), "the restored cell lost its contents");
		check(helper, !Files.exists(file), "an emptied backup wasn't deleted");
		check(helper, fails(() -> server.getCommands().getDispatcher().execute(command,
				player.createCommandSourceStack().withPermission(4))), "the same cell was restored twice");
		empty(given);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void dropRestoresIntoTheWorld(GameTestHelper helper) throws Exception
	{
		MinecraftServer server = helper.getLevel().getServer();
		CellStorage storage = CellStorage.get(server);
		EraDataCell type = ECDataCells.cellsOf(ComputerEra.FLOPPY).getFirst().get();
		UUID first = filledCell(type, 2), second = filledCell(type, 5);
		Path file = backupFile(storage);
		storage.removeWithBackup(new LinkedHashSet<>(List.of(first, second)), file);

		Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3)));
		int result = server.getCommands().getDispatcher().execute("engineeredcomputation cells restore "+name(file)
				+" "+second+" drop "+String.format(Locale.ROOT, "%.1f %.1f %.1f", at.x, at.y, at.z),
				server.createCommandSourceStack()
				.withLevel(helper.getLevel()));
		check(helper, result==1, "the drop command failed");
		List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(1),
				entity -> entity.getItem().is(type));
		check(helper, dropped.size()==1&&CellInventory.peek(dropped.getFirst().getItem()).getTypes()==5,
				"the restored cell wasn't dropped where asked");
		List<CellBackup.Entry> left = CellBackup.read(file, records(helper));
		check(helper, left.size()==1&&left.getFirst().id().equals(first), "the backup should only hold the other cell");
		empty(dropped.getFirst().getItem());
		dropped.getFirst().discard();
		Files.delete(file);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void unknownTypeRestoresIntoTheSmallestFittingCell(GameTestHelper helper) throws Exception
	{
		CellStorage storage = CellStorage.get(helper.getLevel().getServer());
		Path file = backupFile(storage);
		UUID id = UUID.randomUUID();
		CellBackup.write(file, List.of(new CellBackup.Entry(id, contents(30), null)), records(helper));
		CellRestore.Restored restored = CellRestore.restore(storage, name(file), id);
		EraDataCell expected = ECDataCells.cellsOf(ComputerEra.DISK).getFirst().get();
		check(helper, restored.guessedType()&&restored.stack().is(expected),
				"30 types should restore into "+expected+", got "+restored.stack().getItem());
		empty(restored.stack());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void backupListsAPageAtATime(GameTestHelper helper) throws Exception
	{
		MinecraftServer server = helper.getLevel().getServer();
		CellStorage storage = CellStorage.get(server);
		Path file = backupFile(storage);
		List<CellBackup.Entry> entries = new ArrayList<>();
		for(int i = 0; i<20; i++)
			entries.add(new CellBackup.Entry(UUID.randomUUID(), contents(1+i%5),
					ECDataCells.cellsOf(ComputerEra.DISK).getFirst().get()));
		CellBackup.write(file, entries, records(helper));
		try
		{
			check(helper, linesOf(server, "engineeredcomputation cells backup "+name(file)).size()==1+8+1,
					"page one should be a header, 8 cells and the page links");
			check(helper, linesOf(server, "engineeredcomputation cells backup "+name(file)+" 3").size()==1+4+1,
					"the last page should hold the 4 cells left");
			check(helper, linesOf(server, "engineeredcomputation cells backup "+name(file)+" 9").size()==1+4+1,
					"a page past the end should show the last page");
			check(helper, linesOf(server, "engineeredcomputation cells backups").stream()
					.anyMatch(line -> line.contains(name(file))), "the backup list doesn't show the backup");
		}
		finally
		{
			Files.delete(file);
		}
		helper.succeed();
	}

	private static UUID filledCell(EraDataCell type, int types)
	{
		ItemStack stack = new ItemStack(type);
		CellInventory cell = CellInventory.of(stack, () -> {
		});
		contents(types).entries().forEach(entry -> cell.insert(entry.key(), entry.count(), false));
		return stack.get(ECDataComponents.CELL_UUID.get());
	}

	private static CellContents contents(int types)
	{
		Item[] items = BuiltInRegistries.ITEM.stream()
				.filter(item -> item!=Items.AIR&&!(item instanceof EraDataCell)).toArray(Item[]::new);
		Map<ItemKey, Long> amounts = new LinkedHashMap<>();
		for(int i = 0; i<types; i++)
			amounts.put(ItemKey.of(new ItemStack(items[i])), 1L+i);
		return CellContents.of(amounts);
	}

	private static void empty(ItemStack stack)
	{
		CellInventory cell = CellInventory.of(stack, () -> {
		});
		cell.getContents().entries().forEach(entry -> cell.extract(entry.key(), entry.count(), false));
	}

	private static Path backupFile(CellStorage storage)
	{
		return storage.directory().resolve("removed-test-"+UUID.randomUUID().toString().substring(0, 8)+".dat");
	}

	private static String name(Path file)
	{
		return file.getFileName().toString().replace(".dat", "");
	}

	private static CellRecords records(GameTestHelper helper)
	{
		return new CellRecords(helper.getLevel().registryAccess());
	}

	private static boolean fails(Command command)
	{
		try
		{
			command.run();
			return false;
		}
		catch(CommandSyntaxException e)
		{
			return true;
		}
	}

	private static List<String> linesOf(MinecraftServer server, String command) throws CommandSyntaxException
	{
		List<String> lines = new ArrayList<>();
		CommandSource recorder = new CommandSource()
		{
			@Override
			public void sendSystemMessage(Component message)
			{
				lines.add(message.getString());
			}

			@Override
			public boolean acceptsSuccess()
			{
				return true;
			}

			@Override
			public boolean acceptsFailure()
			{
				return true;
			}

			@Override
			public boolean shouldInformAdmins()
			{
				return false;
			}
		};
		CommandSourceStack source = new CommandSourceStack(recorder, Vec3.ZERO, Vec2.ZERO, server.overworld(), 4,
				"test", Component.literal("test"), server, null);
		server.getCommands().getDispatcher().execute(command, source);
		return lines;
	}

	private static void check(GameTestHelper helper, boolean condition, String failure)
	{
		StorageGameTests.check(helper, condition, failure);
	}

	private interface Command
	{
		void run() throws CommandSyntaxException;
	}
}

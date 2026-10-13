package com.muddykat.engineeredcomputation.storage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.storage.cell.CellInventory;
import com.muddykat.engineeredcomputation.storage.cell.CellStatus;
import com.muddykat.engineeredcomputation.storage.cell.CellStorage;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;
import com.muddykat.engineeredcomputation.terminal.TerminalStats;

/** Times the stack-component cells against the UUID cells; results go to the log under [cell-stress]. */
@TestCategory({"storage", "stress"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellBackendStressGameTests
{
	private static final int RUNS = 15;
	private static final int[] NETWORK_CELLS = {16, 64, 256};

	private CellBackendStressGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 400)
	public static void fullCellPerEra(GameTestHelper helper)
	{
		RegistryAccess registries = helper.getLevel().registryAccess();
		Item[] items = fillItems();
		for(ComputerEra era : ECDataCells.uuidCells().keySet())
		{
			EraDataCell type = ECDataCells.cellsOf(era).getLast().get();
			Measured component = measureCell(fill(new ItemStack(type), type, items), registries);
			Measured uuid = measureCell(fill(new ItemStack(ECDataCells.uuidCells().get(era).get()), type, items),
					registries);
			StorageGameTests.check(helper, component.contents.equals(uuid.contents),
					era+": the two cells hold different contents after the same fill");
			log("%s top tier, %d types, %d items", era.getSerializedName(), component.contents.size(),
					component.contents.values().stream().mapToLong(Long::longValue).sum());
			component.log("component");
			uuid.log("uuid");
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void networkOfFullCells(GameTestHelper helper)
	{
		RegistryAccess registries = helper.getLevel().registryAccess();
		Item[] items = fillItems();
		ComputerEra era = ComputerEra.DISK;
		EraDataCell type = ECDataCells.cellsOf(era).getLast().get();
		for(int count : NETWORK_CELLS)
		{
			NetworkResult component = measureNetwork(count, () -> new ItemStack(type), type, items, registries,
					helper);
			NetworkResult uuid = measureNetwork(count, () -> new ItemStack(ECDataCells.uuidCells().get(era).get()),
					type, items, registries, helper);
			StorageGameTests.check(helper, component.total==uuid.total,
					count+" cells: networks hold different totals ("+component.total+" vs "+uuid.total+")");
			log("network of %d %s cells, %d types each", count, era.getSerializedName(), type.getTotalTypes());
			component.log("component");
			uuid.log("uuid");
		}
		helper.succeed();
	}

	private static Item[] fillItems()
	{
		return BuiltInRegistries.ITEM.stream()
				.filter(item -> item!=Items.AIR&&item!=Items.COBBLESTONE&&!(item instanceof EraDataCell))
				.toArray(Item[]::new);
	}

	/** Fills every type slot but one, half of the remaining bytes, so inserts of a new type still have room. */
	private static ItemStack fill(ItemStack stack, EraDataCell type, Item[] items)
	{
		CellInventory cell = CellInventory.of(stack, () -> {
		});
		int types = Math.max(1, type.getTotalTypes()-1);
		long itemBytes = type.getTotalBytes()-(long)type.getTotalTypes()*type.getBytesPerType();
		long perType = Math.max(1, itemBytes*CellInventory.ITEMS_PER_BYTE/2/types);
		for(int i = 0; i<types; i++)
			cell.insert(ItemKey.of(new ItemStack(items[i])), perType, false);
		return stack;
	}

	private static Measured measureCell(ItemStack stack, RegistryAccess registries)
	{
		CellInventory cell = CellInventory.of(stack, () -> {
		});
		ItemKey stored = cell.getContents().entries().getFirst().key();
		ItemKey fresh = ItemKey.of(new ItemStack(Items.COBBLESTONE));
		Measured result = new Measured();
		cell.collect(result.contents);
		result.insertExtract = median(() -> {
			cell.insert(stored, 64, false);
			cell.extract(stored, 64, false);
		});
		result.newType = median(() -> {
			cell.insert(fresh, 64, false);
			cell.extract(fresh, 64, false);
		});
		result.simulate = median(() -> cell.insert(fresh, 64, true));
		result.amount = median(() -> cell.getAmount(stored));
		result.collect = median(() -> cell.collect(new HashMap<>()));
		result.status = median(() -> CellStatus.of(stack));
		result.saveNanos = median(() -> saveStack(stack, registries));
		result.syncNanos = median(() -> syncStack(stack, registries));
		result.saveBytes = nbtBytes(saveStack(stack, registries));
		result.syncBytes = syncStack(stack, registries);
		return result;
	}

	private static NetworkResult measureNetwork(int count, Supplier<ItemStack> blank,
			EraDataCell type, Item[] items, RegistryAccess registries, GameTestHelper helper)
	{
		List<ItemStack> stacks = new ArrayList<>();
		List<CellInventory> cells = new ArrayList<>();
		for(int i = 0; i<count; i++)
		{
			ItemStack stack = fill(blank.get(), type, items);
			stacks.add(stack);
			cells.add(CellInventory.of(stack, () -> {
			}));
		}
		NetworkStorage storage = new NetworkStorage(cells);
		ItemKey stored = cells.getFirst().getContents().entries().getFirst().key();
		ItemKey cobble = ItemKey.of(new ItemStack(Items.COBBLESTONE));
		NetworkResult result = new NetworkResult();
		HashMap<ItemKey, Long> all = new HashMap<>();
		storage.collect(all);
		result.total = all.values().stream().mapToLong(Long::longValue).sum();
		result.collect = median(() -> storage.collect(new HashMap<>()));
		result.stats = median(() -> TerminalStats.of(storage, Set.of(), false));
		result.insertExtract = median(() -> {
			storage.insert(stored, 64, false);
			storage.extract(stored, 64, false);
		});
		result.simulate = median(() -> storage.insert(cobble, 64, true));
		result.statuses = median(() -> stacks.forEach(CellStatus::of));
		result.saveNanos = median(() -> stacks.forEach(stack -> saveStack(stack, registries)));
		result.saveBytes = stacks.stream().mapToLong(stack -> nbtBytes(saveStack(stack, registries))).sum();
		result.syncBytes = stacks.stream().mapToLong(stack -> syncStack(stack, registries)).sum();
		CellStorage world = CellStorage.get(helper.getLevel().getServer());
		CompoundTag worldSave = world.save(new CompoundTag(), registries);
		result.worldSaveBytes = nbtBytes(worldSave);
		result.worldCells = world.size();
		for(CellInventory cell : cells)
			for(var entry : cell.getContents().entries())
				cell.extract(entry.key(), Long.MAX_VALUE, false);
		return result;
	}

	private static Tag saveStack(ItemStack stack, RegistryAccess registries)
	{
		return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
	}

	private static long syncStack(ItemStack stack, RegistryAccess registries)
	{
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try
		{
			ItemStack.STREAM_CODEC.encode(buf, stack);
			return buf.readableBytes();
		}
		finally
		{
			buf.release();
		}
	}

	private static long nbtBytes(Tag tag)
	{
		CompoundTag wrapper = new CompoundTag();
		wrapper.put("t", tag);
		try
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			NbtIo.write(wrapper, new DataOutputStream(out));
			return out.size();
		}
		catch(IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private static long median(Runnable run)
	{
		for(int i = 0; i<3; i++)
			run.run();
		long[] times = new long[RUNS];
		for(int i = 0; i<RUNS; i++)
		{
			long start = System.nanoTime();
			run.run();
			times[i] = System.nanoTime()-start;
		}
		Arrays.sort(times);
		return times[RUNS/2];
	}

	private static void log(String format, Object... args)
	{
		EngineeredComputation.LOGGER.info("[cell-stress] "+String.format(Locale.ROOT, format, args));
	}

	private static final class Measured
	{
		final HashMap<ItemKey, Long> contents = new HashMap<>();
		long insertExtract, newType, simulate, amount, collect, status, saveNanos, syncNanos, saveBytes, syncBytes;

		void log(String name)
		{
			CellBackendStressGameTests.log("  %-9s insert+extract %8.4f ms | new type %8.4f ms | simulate %8.4f ms | "
							+"amount %8.4f ms | collect %8.4f ms | status %8.4f ms | stack save %8.4f ms, %6d B | "
							+"stack sync %8.4f ms, %6d B", name, insertExtract/1e6, newType/1e6, simulate/1e6,
					amount/1e6, collect/1e6, status/1e6, saveNanos/1e6, saveBytes, syncNanos/1e6, syncBytes);
		}
	}

	private static final class NetworkResult
	{
		long total, collect, stats, insertExtract, simulate, statuses, saveNanos, saveBytes, syncBytes,
				worldSaveBytes, worldCells;

		void log(String name)
		{
			CellBackendStressGameTests.log("  %-9s collect %8.4f ms | stats %8.4f ms | insert+extract %8.4f ms | "
							+"simulate %8.4f ms | all statuses %8.4f ms | save all stacks %8.4f ms, %8d B | "
							+"sync all stacks %8d B | world cell data %8d B (%d cells)", name, collect/1e6,
					stats/1e6, insertExtract/1e6, simulate/1e6, statuses/1e6, saveNanos/1e6, saveBytes, syncBytes,
					worldSaveBytes, worldCells);
		}
	}
}

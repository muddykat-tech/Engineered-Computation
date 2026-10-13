package com.muddykat.engineeredcomputation.storage.persist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.NetworkPort;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.CellInventory;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;

public final class CellLayout
{
	public static final EraDrive DRIVE = EraDrive.FLOPPY_WORKSTATION;
	private static final Direction FACING = Direction.NORTH;

	public final Map<UUID, CellContents> live = new LinkedHashMap<>();
	public final Map<UUID, String> drive = new HashMap<>();
	public final Map<UUID, String> network = new HashMap<>();
	public final Map<UUID, String> chunk = new HashMap<>();
	private final List<ItemStack> worldCells = new ArrayList<>();
	private final Item[] items;
	private final Random random = new Random(42);

	private CellLayout()
	{
		items = BuiltInRegistries.ITEM.stream()
				.filter(item -> item!=Items.AIR&&!(item instanceof EraDataCell)).toArray(Item[]::new);
	}

	/**
	 * Rows of Floppy Workstations, each row cabled into its own network, every bay holding a filled era 5 UUID cell.
	 * Fits {@code gametest/empty_24x20x24}.
	 */
	public static CellLayout floppyWorld(GameTestHelper helper, int networks, int drivesPerNetwork)
	{
		CellLayout layout = new CellLayout();
		ServerLevel level = helper.getLevel();
		NetworkPort port = DRIVE.geometry().ports().getFirst();
		for(int row = 0; row<networks; row++)
		{
			List<EraDriveState> drives = new ArrayList<>();
			BlockPos firstCable = null, lastCable = null;
			for(int col = 0; col<drivesPerNetwork; col++)
			{
				BlockPos at = new BlockPos(1+4*col, 1, 2+5*row);
				EraDriveState drive = StorageGameTests.formDriveAt(helper, DRIVE, FACING, at);
				drives.add(drive);
				BlockPos origin = helper.absolutePos(at);
				BlockPos cable = StorageGameTests.world(origin, port.pos(), FACING)
						.relative(MultiblockOrientation.rotationFor(FACING).rotate(port.side()));
				if(firstCable==null)
					firstCable = cable;
				else
					BlockPos.betweenClosed(lastCable, cable).forEach(pos ->
							level.setBlockAndUpdate(pos, ECCables.block().get().defaultBlockState()));
				lastCable = cable;
				level.setBlockAndUpdate(cable, ECCables.block().get().defaultBlockState());

				BlockPos master = StorageGameTests.world(origin, DRIVE.geometry().master(), FACING);
				for(int slot = 0; slot<drive.getCellSlots().getSlots(); slot++)
				{
					ItemStack stack = new ItemStack(ECDataCells.uuidCells().get(ComputerEra.FLOPPY).get());
					drive.getCellSlots().setStackInSlot(slot, stack);
					UUID id = layout.fillUuidCell(drive.getCellSlots().getStackInSlot(slot));
					layout.place(id, "drive_"+master.getX()+"_"+master.getY()+"_"+master.getZ(), "network_"+row,
							new ChunkPos(master));
				}
			}
			Set<?> hosts = Set.copyOf(StorageNetwork.scan(level, firstCable).getHosts());
			StorageGameTests.check(helper, hosts.equals(Set.copyOf(drives)),
					"network "+row+" should hold exactly its "+drivesPerNetwork+" workstations, found "+hosts.size());
		}
		return layout;
	}

	/** Same setup as the floppy world without placing blocks: 2 cells per drive, 4 drives per chunk. */
	public static CellLayout synthetic(int cells, int drivesPerNetwork)
	{
		CellLayout layout = new CellLayout();
		int bays = DRIVE.slots();
		for(int i = 0; i<cells; i++)
		{
			UUID id = new UUID(0xEC, i);
			int drive = i/bays;
			layout.live.put(id, layout.randomContents());
			layout.place(id, "drive_"+drive, "network_"+drive/drivesPerNetwork, new ChunkPos(drive/4, 0));
		}
		return layout;
	}

	private UUID fillUuidCell(ItemStack stack)
	{
		CellInventory cell = CellInventory.of(stack, () -> {
		});
		randomContents().entries().forEach(entry -> cell.insert(entry.key(), entry.count(), false));
		UUID id = stack.get(ECDataComponents.CELL_UUID.get());
		live.put(id, cell.getContents());
		worldCells.add(stack);
		return id;
	}

	private CellContents randomContents()
	{
		EraDataCell type = ECDataCells.uuidCells().get(ComputerEra.FLOPPY).get();
		int types = 8+random.nextInt(type.getTotalTypes()-8);
		Map<ItemKey, Long> amounts = new LinkedHashMap<>();
		int start = random.nextInt(items.length-types);
		for(int i = 0; i<types; i++)
			amounts.put(ItemKey.of(new ItemStack(items[start+i])), 1L+random.nextInt(2000));
		return CellContents.of(amounts);
	}

	private void place(UUID id, String driveKey, String networkKey, ChunkPos chunkPos)
	{
		drive.put(id, driveKey);
		network.put(id, networkKey);
		chunk.put(id, "chunk_"+chunkPos.x+"_"+chunkPos.z);
	}

	/** Empties the placed UUID cells so they don't stay in the world's cell storage for later tests. */
	public void emptyWorldCells()
	{
		for(ItemStack stack : worldCells)
		{
			CellInventory cell = CellInventory.of(stack, () -> {
			});
			cell.getContents().entries().forEach(entry -> cell.extract(entry.key(), entry.count(), false));
		}
	}

	public List<UUID> cellsIn(Map<UUID, String> grouping, String group)
	{
		return live.keySet().stream().filter(id -> group.equals(grouping.get(id))).toList();
	}

	/** Changes one count same method that an import or export would. */
	public void touch(UUID id)
	{
		List<CellContents.Entry> entries = new ArrayList<>(live.get(id).entries());
		CellContents.Entry first = entries.getFirst();
		entries.set(0, new CellContents.Entry(first.key(), first.count()%5000+1+random.nextInt(64)));
		live.put(id, new CellContents(entries));
	}

	public void moveTo(UUID id, UUID besides)
	{
		drive.put(id, drive.get(besides));
		network.put(id, network.get(besides));
		chunk.put(id, chunk.get(besides));
	}

	public List<CellSaveStrategy> strategies(Path root, HolderLookup.Provider registries) throws IOException
	{
		return List.of(
				new GroupedFileStrategy("per drive", root.resolve("drive"), registries, drive::get),
				new GroupedFileStrategy("per network", root.resolve("network"), registries, network::get),
				new GroupedFileStrategy("per chunk", root.resolve("chunk"), registries, chunk::get),
				new RegionFileStrategy(root.resolve("indexed_original").resolve("cells.ecc"),
						RegionFileStrategy.Options.NONE, registries),
				new RegionFileStrategy(root.resolve("indexed").resolve("cells.ecc"), registries),
				new GroupedFileStrategy("bulk file", root.resolve("bulk"), registries, id -> "cells"));
	}

	public static Path tempDir() throws IOException
	{
		return Files.createTempDirectory("ec-cell-persist");
	}

	public static void delete(Path root) throws IOException
	{
		try(Stream<Path> walk = Files.walk(root))
		{
			for(Path path : walk.sorted(Comparator.reverseOrder()).toList())
				Files.delete(path);
		}
	}
}

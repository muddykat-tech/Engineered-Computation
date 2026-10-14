package com.muddykat.engineeredcomputation.storage.cell;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;

/**
 * Finds every cell id still referenced anywhere in the world's saves, and removes stored cells that nothing refers to.
 * <p>
 * The world is read from disk on a background thread, since a big world can take longer than the server watchdog
 * allows a tick. Back on the server thread it saves again and rereads only what was written since the sweep began, so
 * a cell moved while the sweep ran is still seen.
 */
public final class CellSweep
{
	private static final int SECTOR = 4096;
	private static final String KEY_PREFIX = "commands."+EngineeredComputation.MODID+".cells.cleanup.";

	@Nullable
	private static CellSweep running;

	private final MinecraftServer server;
	private final Consumer<Component> report;
	private final Path root;
	private final List<Path> dimensions = new ArrayList<>();
	private final String key;
	private final byte[] keyBytes;
	private final Set<UUID> found = ConcurrentHashMap.newKeySet();
	private final List<ChunkRef> retry = new ArrayList<>();
	private final long startMillis;
	private long savedMillis;
	private long backgroundMillis;
	private volatile boolean cancelled;
	private volatile boolean read;
	@Nullable
	private volatile String failure;
	private int chunks;
	private int files;
	@Nullable
	private volatile Result result;

	private CellSweep(MinecraftServer server, Consumer<Component> report)
	{
		this.server = server;
		this.report = report;
		this.root = server.getWorldPath(LevelResource.ROOT);
		for(ServerLevel level : server.getAllLevels())
			dimensions.add(DimensionType.getStorageFolder(level.dimension(), root));
		this.key = ECDataComponents.CELL_UUID.getId().toString();
		this.keyBytes = key.getBytes(StandardCharsets.UTF_8);
		this.startMillis = System.currentTimeMillis();
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> {
			if(running!=null)
				running.cancelled = true;
			running = null;
		});
		// Finished from the tick rather than server.execute, which a busy server can put off for many ticks.
		NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
			CellSweep sweep = running;
			if(sweep!=null&&sweep.read&&sweep.server==event.getServer())
				sweep.finish();
		});
	}

	/** Null if a sweep is already running. Must be called on the server thread. */
	@Nullable
	public static CellSweep start(MinecraftServer server, Consumer<Component> report)
	{
		if(running!=null)
			return null;
		CellSweep sweep = new CellSweep(server, report);
		running = sweep;
		server.saveEverything(true, true, true);
		sweep.savedMillis = System.currentTimeMillis();
		Thread thread = new Thread(() -> {
			long begin = System.currentTimeMillis();
			sweep.scanWorld(0);
			sweep.backgroundMillis = System.currentTimeMillis()-begin;
			sweep.read = true;
		}, "Engineered Computation cell sweep");
		thread.setDaemon(true);
		thread.start();
		return sweep;
	}

	@Nullable
	public Result result()
	{
		return result;
	}

	private void scanWorld(long newerThan)
	{
		for(Path dimension : dimensions)
		{
			scanRegions(dimension.resolve("region"), newerThan);
			scanRegions(dimension.resolve("entities"), newerThan);
			scanDataFiles(dimension.resolve("data"), newerThan);
		}
		scanDataFiles(root.resolve(LevelResource.PLAYER_DATA_DIR.getId()), newerThan);
		scanDataFile(root.resolve(LevelResource.LEVEL_DATA_FILE.getId()), newerThan);
	}

	private void scanRegions(Path dir, long newerThan)
	{
		if(!Files.isDirectory(dir))
			return;
		try(Stream<Path> list = Files.list(dir))
		{
			for(Path file : list.filter(path -> path.getFileName().toString().endsWith(".mca")).toList())
			{
				if(cancelled||failure!=null)
					return;
				scanRegion(file, newerThan);
			}
		}
		catch(IOException e)
		{
			fail("could not list "+root.relativize(dir), e);
		}
	}

	private void scanRegion(Path file, long newerThan)
	{
		try(FileChannel channel = FileChannel.open(file, StandardOpenOption.READ))
		{
			if(channel.size()<2*SECTOR)
				return;
			ByteBuffer header = read(channel, 0, 2*SECTOR);
			for(int index = 0; index<1024; index++)
			{
				int location = header.getInt(index*4);
				long saved = header.getInt(SECTOR+index*4)&0xFFFFFFFFL;
				if(location==0||saved*1000<newerThan)
					continue;
				try
				{
					scanChunk(channel, file, index, location);
				}
				catch(IOException|RuntimeException e)
				{
					// The server may be writing this chunk right now; it's read again once the world is saved.
					retry.add(new ChunkRef(file, index));
				}
			}
		}
		catch(IOException e)
		{
			fail("could not read "+root.relativize(file), e);
		}
	}

	private void scanChunk(FileChannel channel, Path file, int index, int location) throws IOException
	{
		long offset = (long)(location>>>8)*SECTOR;
		ByteBuffer head = read(channel, offset, 5);
		int length = head.getInt();
		int version = head.get();
		if(length<=0||length>(location&0xFF)*SECTOR)
			throw new IOException("bad chunk length "+length);
		InputStream in;
		if((version&0x80)!=0)
		{
			String[] parts = file.getFileName().toString().split("\\.");
			int x = Integer.parseInt(parts[1])*32+index%32, z = Integer.parseInt(parts[2])*32+index/32;
			in = Files.newInputStream(file.resolveSibling("c."+x+"."+z+".mcc"));
			version &= 0x7F;
		}
		else
			in = new ByteArrayInputStream(read(channel, offset+5, length-1).array());
		RegionFileVersion format = RegionFileVersion.fromId(version);
		if(format==null)
			throw new IOException("unknown chunk compression "+version);
		try(InputStream data = format.wrap(in))
		{
			scanNbt(data.readAllBytes());
		}
		chunks++;
	}

	private void scanDataFiles(Path dir, long newerThan)
	{
		if(!Files.isDirectory(dir))
			return;
		Path own = root.resolve("data").resolve(EngineeredComputation.MODID);
		try(Stream<Path> list = Files.walk(dir))
		{
			for(Path file : list.filter(path -> path.toString().endsWith(".dat")&&!path.startsWith(own)).toList())
			{
				if(cancelled||failure!=null)
					return;
				scanDataFile(file, newerThan);
			}
		}
		catch(IOException e)
		{
			fail("could not list "+root.relativize(dir), e);
		}
	}

	private void scanDataFile(Path file, long newerThan)
	{
		try
		{
			if(!Files.isRegularFile(file)||Files.getLastModifiedTime(file).toMillis()<newerThan)
				return;
			byte[] bytes = Files.readAllBytes(file);
			try(InputStream unzipped = new GZIPInputStream(new ByteArrayInputStream(bytes)))
			{
				bytes = unzipped.readAllBytes();
			}
			catch(IOException notGzip)
			{
				// Some mods save their data uncompressed.
			}
			scanNbt(bytes);
			files++;
		}
		catch(IOException|RuntimeException e)
		{
			fail("could not read "+root.relativize(file)+", which holds cells", e);
		}
	}

	/** Parses only data that mentions cell ids at all, which skips nearly every chunk. */
	private void scanNbt(byte[] bytes) throws IOException
	{
		if(indexOf(bytes, keyBytes)<0)
			return;
		collect(NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)), NbtAccounter.unlimitedHeap()));
	}

	private void collect(Tag start)
	{
		Deque<Tag> pending = new ArrayDeque<>();
		pending.push(start);
		while(!pending.isEmpty())
		{
			Tag tag = pending.pop();
			if(tag instanceof CompoundTag compound)
				for(String name : compound.getAllKeys())
				{
					Tag value = compound.get(name);
					if(name.equals(key)&&value instanceof IntArrayTag ints&&ints.size()==4)
						found.add(UUIDUtil.uuidFromIntArray(ints.getAsIntArray()));
					else if(value instanceof CompoundTag||value instanceof ListTag)
						pending.push(value);
				}
			else if(tag instanceof ListTag list)
				list.forEach(pending::push);
		}
	}

	private void finish()
	{
		read = false;
		if(cancelled)
			return;
		long begin = System.currentTimeMillis();
		try
		{
			if(failure==null)
			{
				server.saveEverything(true, true, true);
				// Region timestamps are in whole seconds, so anything saved in the same second is read again too.
				scanWorld(savedMillis-1000);
				List<ChunkRef> again = List.copyOf(retry);
				retry.clear();
				for(ChunkRef ref : again)
					rescan(ref);
				scanPlayersInMemory();
			}
			if(failure!=null)
			{
				result = new Result(found.size(), 0, 0, null, failure);
				report(Component.translatable(KEY_PREFIX+"failed", failure));
				return;
			}
			CellStorage storage = CellStorage.get(server);
			Set<UUID> orphans = new HashSet<>(storage.idsAssignedBy(startMillis));
			orphans.removeAll(found);
			Set<UUID> unsaved = new HashSet<>(found);
			unsaved.removeAll(storage.ids());
			Path backup = null;
			if(!orphans.isEmpty())
			{
				backup = CellBackup.fileFor(storage, CellBackup.nameFor(
						LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))));
				storage.removeWithBackup(orphans, backup);
			}
			result = new Result(found.size(), orphans.size(), unsaved.size(), backup, null);
			EngineeredComputation.LOGGER.info("Cell sweep read {} chunks and {} files: {} cells found, {} removed. "
							+"Background read took {} ms, the server thread {} ms", chunks, files, found.size(),
					orphans.size(), backgroundMillis, System.currentTimeMillis()-begin);
			report(orphans.isEmpty()? Component.translatable(KEY_PREFIX+"done_none", found.size())
					: Component.translatable(KEY_PREFIX+"done", found.size(), orphans.size(), backupLink(backup)));
			if(!unsaved.isEmpty())
				report(Component.translatable(KEY_PREFIX+"unsaved", unsaved.size()));
		}
		catch(IOException|RuntimeException e)
		{
			fail("cell storage could not be updated", e);
			result = new Result(found.size(), 0, 0, null, failure);
			report(Component.translatable(KEY_PREFIX+"failed", failure));
		}
		finally
		{
			if(running==this)
				running = null;
		}
	}

	private static Component backupLink(Path backup)
	{
		String name = backup.getFileName().toString().replaceFirst("\\.dat$", "");
		return Component.literal(name).withStyle(style -> style.withColor(ChatFormatting.AQUA)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, CellBackupCommand.COMMAND+"backup "+name)));
	}

	private void rescan(ChunkRef ref)
	{
		try(FileChannel channel = FileChannel.open(ref.file, StandardOpenOption.READ))
		{
			int location = read(channel, ref.index*4L, 4).getInt();
			if(location!=0)
				scanChunk(channel, ref.file, ref.index, location);
		}
		catch(IOException|RuntimeException e)
		{
			fail("could not read a chunk in "+root.relativize(ref.file), e);
		}
	}

	/** What a player holds on the cursor or in a crafting grid isn't saved anywhere. */
	private void scanPlayersInMemory()
	{
		var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		for(ServerPlayer player : server.getPlayerList().getPlayers())
		{
			List<ItemStack> stacks = new ArrayList<>(player.containerMenu.getItems());
			stacks.add(player.containerMenu.getCarried());
			for(ItemStack stack : stacks)
				if(!stack.isEmpty())
					collect(ItemStack.CODEC.encodeStart(ops, stack).getOrThrow());
		}
	}

	private void fail(String reason, Exception e)
	{
		EngineeredComputation.LOGGER.error("Cell sweep: {}", reason, e);
		if(failure==null)
			failure = reason;
	}

	private void report(Component message)
	{
		EngineeredComputation.LOGGER.info(message.getString());
		report.accept(message);
	}

	private static ByteBuffer read(FileChannel channel, long position, int length) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(length);
		while(buffer.hasRemaining())
			if(channel.read(buffer, position+buffer.position())<0)
				throw new IOException("file ends early");
		return buffer.flip();
	}

	private static int indexOf(byte[] data, byte[] pattern)
	{
		outer:
		for(int i = 0; i<=data.length-pattern.length; i++)
		{
			for(int j = 0; j<pattern.length; j++)
				if(data[i+j]!=pattern[j])
					continue outer;
			return i;
		}
		return -1;
	}

	/** {@code backup} and {@code failure} are null when nothing was removed or nothing went wrong. */
	public record Result(int found, int removed, int unsaved, @Nullable Path backup, @Nullable String failure)
	{
	}

	private record ChunkRef(Path file, int index)
	{
	}
}

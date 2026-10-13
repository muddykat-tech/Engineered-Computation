package com.muddykat.engineeredcomputation.pneumatic.carrier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.pneumatic.TubeMember;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.pressurizer.Pressure;
import com.muddykat.engineeredcomputation.pneumatic.station.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECCapabilities;

public final class TubeCarriers extends SavedData
{
	private static final String NAME = EngineeredComputation.MODID+"_tube_carriers";
	private static final Factory<TubeCarriers> FACTORY = new Factory<>(TubeCarriers::new, TubeCarriers::load, null);

	private static final double SYNC_RANGE = 96;

	private final List<Carrier> carriers = new ArrayList<>();
	private final List<Outgoing> outgoing = new ArrayList<>();
	private final Map<Long, Long> nextLaunch = new HashMap<>();
	private long nextId;

	public static TubeCarriers of(ServerLevel level)
	{
		return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
			if(event.getLevel() instanceof ServerLevel level)
				of(level).tick(level);
		});
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> syncAll(event.getEntity()));
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> syncAll(event.getEntity()));
	}

	private static void syncAll(Player player)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return;
		List<CarrierSyncPayload.Flight> flights = of(serverPlayer.serverLevel()).carriers.stream()
				.map(TubeCarriers::flight).toList();
		PacketDistributor.sendToPlayer(serverPlayer, new CarrierSyncPayload(flights, List.of()));
	}

	private static CarrierSyncPayload.Flight flight(Carrier carrier)
	{
		return new CarrierSyncPayload.Flight(carrier.getId(), Arrays.stream(carrier.getPathLongs()).boxed().toList(),
				carrier.getLaunched());
	}

	public Carrier launch(ServerLevel level, ItemStack items, List<BlockPos> path)
	{
		Carrier carrier = Carrier.launch(nextId++, items.copy(), path, level.getGameTime());
		carriers.add(carrier);
		setDirty();
		level.playSound(null, path.get(0), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.3F, 1.7F);
		CarrierSyncPayload launched = new CarrierSyncPayload(List.of(flight(carrier)), List.of());
		for(ServerPlayer player : level.players())
			if(path.stream().anyMatch(pos -> pos.closerToCenterThan(player.position(), SYNC_RANGE)))
				PacketDistributor.sendToPlayer(player, launched);
		return carrier;
	}

	public void send(ItemStack items, List<BlockPos> path)
	{
		if(items.isEmpty()||path.size()<2)
			return;
		outgoing.add(new Outgoing(items.copy(), path.stream().mapToLong(BlockPos::asLong).toArray()));
		setDirty();
	}

	public int waitingAt(BlockPos origin)
	{
		return outgoing.stream().filter(out -> out.path[0]==origin.asLong()).mapToInt(out -> out.items.getCount()).sum();
	}

	private void dispatch(ServerLevel level, long now)
	{
		for(Outgoing out : List.copyOf(outgoing))
		{
			long origin = out.path[0];
			if(nextLaunch.getOrDefault(origin, Long.MIN_VALUE)>now||!level.isLoaded(BlockPos.of(origin)))
				continue;
			TubeNetwork network = TubeNetwork.of(level, BlockPos.of(origin));
			boolean high = Pressure.high(level, network);
			ItemStack carried = out.items.split(Pressure.carrierItems(high, out.items.getMaxStackSize()));
			if(high)
				Pressure.draw(level, network);
			launch(level, carried, Arrays.stream(out.path).mapToObj(BlockPos::of).toList());
			nextLaunch.put(origin, now+Pressure.carrierTicks(high));
			if(out.items.isEmpty())
				outgoing.remove(out);
			setDirty();
		}
	}

	private record Outgoing(ItemStack items, long[] path)
	{
	}

	public List<Carrier> getCarriers()
	{
		return Collections.unmodifiableList(carriers);
	}

	private void tick(ServerLevel level)
	{
		long now = level.getGameTime();
		if(!outgoing.isEmpty())
			dispatch(level, now);
		if(carriers.isEmpty())
			return;
		for(Carrier carrier : List.copyOf(carriers))
			if(!travel(level, carrier, now)||arrive(level, carrier))
			{
				carriers.remove(carrier);
				setDirty();
				PacketDistributor.sendToPlayersInDimension(level, new CarrierSyncPayload(List.of(),
						List.of(carrier.getId())));
			}
	}

	private boolean travel(ServerLevel level, Carrier carrier, long now)
	{
		int reached = carrier.reached(now);
		while(carrier.getPassed()<reached)
		{
			int next = carrier.getPassed()+1;
			BlockPos pos = carrier.at(next);
			if(next<carrier.lastIndex()&&level.isLoaded(pos)&&!isTube(level, pos))
			{
				spill(level, pos, carrier.getItems());
				return false;
			}
			carrier.setPassed(next);
			setDirty();
		}
		return true;
	}

	private boolean arrive(ServerLevel level, Carrier carrier)
	{
		BlockPos destination = carrier.getDestination();
		if(carrier.getPassed()<carrier.lastIndex()||!level.isLoaded(destination))
			return false;
		if(level.getBlockEntity(destination) instanceof TubeStationBlockEntity station)
		{
			sendBack(level, carrier, station.accept(carrier.getItems()));
			return true;
		}
		if(level.getBlockEntity(destination) instanceof AutotubeBlockEntity autotube)
		{
			sendBack(level, carrier, autotube.accept(carrier.getItems()));
			return true;
		}
		if(!(level.getBlockEntity(destination) instanceof ReceivingStationBlockEntity receiver))
		{
			spill(level, destination, carrier.getItems());
			return true;
		}
		ItemStack left = receiver.accept(carrier.getItems());
		if(left.getCount()!=carrier.getItems().getCount())
		{
			carrier.setItems(left);
			setDirty();
		}
		if(left.isEmpty())
			land(level, destination);
		return left.isEmpty();
	}

	private void sendBack(ServerLevel level, Carrier carrier, ItemStack left)
	{
		BlockPos destination = carrier.getDestination();
		land(level, destination);
		if(left.isEmpty())
			return;
		if(carrier.isReturning())
		{
			spill(level, destination, left);
			return;
		}
		List<BlockPos> back = new ArrayList<>(carrier.getPath());
		Collections.reverse(back);
		launch(level, left, back).setReturning(true);
	}

	private static boolean isTube(ServerLevel level, BlockPos pos)
	{
		TubeMember member = level.getCapability(ECCapabilities.TUBE_MEMBER, pos, null);
		return member!=null&&member.carries();
	}

	private static void land(ServerLevel level, BlockPos pos)
	{
		level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.8F, 0.8F);
	}

	private static void spill(ServerLevel level, BlockPos pos, ItemStack items)
	{
		level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS, 0.8F, 1.2F);
		Containers.dropItemStack(level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, items);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
	{
		ListTag list = new ListTag();
		for(Carrier carrier : carriers)
			list.add(carrier.save(registries));
		tag.put("carriers", list);
		ListTag waiting = new ListTag();
		for(Outgoing out : outgoing)
		{
			CompoundTag entry = new CompoundTag();
			entry.put("items", out.items.saveOptional(registries));
			entry.putLongArray("path", out.path);
			waiting.add(entry);
		}
		tag.put("outgoing", waiting);
		tag.putLong("next_id", nextId);
		return tag;
	}

	public static TubeCarriers load(CompoundTag tag, HolderLookup.Provider registries)
	{
		TubeCarriers loaded = new TubeCarriers();
		loaded.nextId = tag.getLong("next_id");
		for(Tag carrier : tag.getList("carriers", Tag.TAG_COMPOUND))
			loaded.carriers.add(Carrier.load((CompoundTag)carrier, registries));
		for(Tag entry : tag.getList("outgoing", Tag.TAG_COMPOUND))
		{
			CompoundTag out = (CompoundTag)entry;
			ItemStack items = ItemStack.parseOptional(registries, out.getCompound("items"));
			if(!items.isEmpty())
				loaded.outgoing.add(new Outgoing(items, out.getLongArray("path")));
		}
		return loaded;
	}
}

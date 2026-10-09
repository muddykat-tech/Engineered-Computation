package com.muddykat.engineeredcomputation.pneumatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.network.ECPayloads;
import com.muddykat.engineeredcomputation.registration.ECSounds;

public final class TubeCarriers extends SavedData
{
	private static final String NAME = EngineeredComputation.MODID+"_tube_carriers";

	private static final double SYNC_RANGE = 96;

	private final List<Carrier> carriers = new ArrayList<>();
	private long nextId;

	public static TubeCarriers of(ServerLevel level)
	{
		return level.getDataStorage().computeIfAbsent(TubeCarriers::load, TubeCarriers::new, NAME);
	}

	public static void register()
	{
		MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.LevelTickEvent.class, event -> {
			if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level)
				of(level).tick(level);
		});
		MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedInEvent.class,
				event -> syncAll(event.getEntity()));
		MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerChangedDimensionEvent.class,
				event -> syncAll(event.getEntity()));
	}

	private static void syncAll(Player player)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return;
		List<CarrierSyncPayload.Flight> flights = of(serverPlayer.serverLevel()).carriers.stream()
				.map(TubeCarriers::flight).toList();
		ECPayloads.sendToPlayer(serverPlayer, new CarrierSyncPayload(flights, List.of()));
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
		level.playSound(null, path.get(0), ECSounds.BREEZE_SHOOT.get(), SoundSource.BLOCKS, 0.3F, 1.7F);
		CarrierSyncPayload launched = new CarrierSyncPayload(List.of(flight(carrier)), List.of());
		for(ServerPlayer player : level.players())
			if(path.stream().anyMatch(pos -> pos.closerToCenterThan(player.position(), SYNC_RANGE)))
				ECPayloads.sendToPlayer(player, launched);
		return carrier;
	}

	public List<Carrier> getCarriers()
	{
		return Collections.unmodifiableList(carriers);
	}

	private void tick(ServerLevel level)
	{
		if(carriers.isEmpty())
			return;
		long now = level.getGameTime();
		for(Carrier carrier : List.copyOf(carriers))
			if(!travel(level, carrier, now)||arrive(level, carrier))
			{
				carriers.remove(carrier);
				setDirty();
				ECPayloads.sendToPlayersInDimension(level, new CarrierSyncPayload(List.of(),
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
			ItemStack left = station.accept(carrier.getItems());
			land(level, destination);
			if(!left.isEmpty())
			{
				List<BlockPos> back = new ArrayList<>(carrier.getPath());
				Collections.reverse(back);
				launch(level, left, back);
			}
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

	private static boolean isTube(ServerLevel level, BlockPos pos)
	{
		TubeMember member = TubeMemberBlock.at(level, pos, null);
		return member!=null&&member.carries();
	}

	private static void land(ServerLevel level, BlockPos pos)
	{
		level.playSound(null, pos, ECSounds.DECORATED_POT_INSERT.get(), SoundSource.BLOCKS, 0.8F, 0.8F);
	}

	private static void spill(ServerLevel level, BlockPos pos, ItemStack items)
	{
		level.playSound(null, pos, ECSounds.DECORATED_POT_INSERT_FAIL.get(), SoundSource.BLOCKS, 0.8F, 1.2F);
		Containers.dropItemStack(level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, items);
	}

	@Override
	public CompoundTag save(CompoundTag tag)
	{
		ListTag list = new ListTag();
		for(Carrier carrier : carriers)
			list.add(carrier.save());
		tag.put("carriers", list);
		tag.putLong("next_id", nextId);
		return tag;
	}

	public static TubeCarriers load(CompoundTag tag)
	{
		TubeCarriers loaded = new TubeCarriers();
		loaded.nextId = tag.getLong("next_id");
		for(Tag carrier : tag.getList("carriers", Tag.TAG_COMPOUND))
			loaded.carriers.add(Carrier.load((CompoundTag)carrier));
		return loaded;
	}
}

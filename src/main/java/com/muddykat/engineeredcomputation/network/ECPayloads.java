package com.muddykat.engineeredcomputation.network;

import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.ProgramEditPayload;
import com.muddykat.engineeredcomputation.client.ClientHooks;
import com.muddykat.engineeredcomputation.crafting.CancelOrderPayload;
import com.muddykat.engineeredcomputation.crafting.CraftingComputerPayload;
import com.muddykat.engineeredcomputation.crafting.OrderPlanPayload;
import com.muddykat.engineeredcomputation.crafting.OrderRequestPayload;
import com.muddykat.engineeredcomputation.crafting.PatternEditPayload;
import com.muddykat.engineeredcomputation.crafting.TerminalCraftingPayload;
import com.muddykat.engineeredcomputation.pneumatic.CarrierSyncPayload;
import com.muddykat.engineeredcomputation.pneumatic.StationNamePayload;
import com.muddykat.engineeredcomputation.terminal.TeletypeOutletPayload;
import com.muddykat.engineeredcomputation.terminal.TeletypeSlipPayload;
import com.muddykat.engineeredcomputation.terminal.TerminalClickPayload;
import com.muddykat.engineeredcomputation.terminal.TerminalContentsPayload;
import com.muddykat.engineeredcomputation.terminal.TerminalUnreachablePayload;

/** The mod's network channel. Every packet is handled on the receiving side's main thread, by the player it's for. */
public final class ECPayloads
{
	private static final String PROTOCOL_VERSION = "1";

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "main"), () -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

	private static int nextId;

	private ECPayloads()
	{
	}

	public static void register(IEventBus modEventBus)
	{
		toClient(TerminalContentsPayload.class, TerminalContentsPayload::encode, TerminalContentsPayload::decode,
				TerminalContentsPayload::handle);
		toClient(TerminalUnreachablePayload.class, TerminalUnreachablePayload::encode,
				TerminalUnreachablePayload::decode, TerminalUnreachablePayload::handle);
		toClient(TeletypeSlipPayload.class, TeletypeSlipPayload::encode, TeletypeSlipPayload::decode,
				TeletypeSlipPayload::handle);
		toClient(CarrierSyncPayload.class, CarrierSyncPayload::encode, CarrierSyncPayload::decode,
				CarrierSyncPayload::handle);
		toServer(TeletypeOutletPayload.class, TeletypeOutletPayload::encode, TeletypeOutletPayload::decode,
				TeletypeOutletPayload::handle);
		toServer(StationNamePayload.class, StationNamePayload::encode, StationNamePayload::decode,
				StationNamePayload::handle);
		toServer(TerminalClickPayload.class, TerminalClickPayload::encode, TerminalClickPayload::decode,
				TerminalClickPayload::handle);
		toServer(ProgramEditPayload.class, ProgramEditPayload::encode, ProgramEditPayload::decode,
				ProgramEditPayload::handle);
		toServer(PatternEditPayload.class, PatternEditPayload::encode, PatternEditPayload::decode,
				PatternEditPayload::handle);
		toServer(OrderRequestPayload.class, OrderRequestPayload::encode, OrderRequestPayload::decode,
				OrderRequestPayload::handle);
		toServer(CancelOrderPayload.class, CancelOrderPayload::encode, CancelOrderPayload::decode,
				CancelOrderPayload::handle);
		toClient(OrderPlanPayload.class, OrderPlanPayload::encode, OrderPlanPayload::decode, OrderPlanPayload::handle);
		toClient(CraftingComputerPayload.class, CraftingComputerPayload::encode, CraftingComputerPayload::decode,
				CraftingComputerPayload::handle);
		toClient(TerminalCraftingPayload.class, TerminalCraftingPayload::encode, TerminalCraftingPayload::decode,
				TerminalCraftingPayload::handle);
	}

	public static void sendToServer(Object payload)
	{
		CHANNEL.sendToServer(payload);
	}

	public static void sendToPlayer(ServerPlayer player, Object payload)
	{
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
	}

	public static void sendToPlayersInDimension(ServerLevel level, Object payload)
	{
		CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), payload);
	}

	private static <T> void toClient(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
			Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Player> handler)
	{
		CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(encoder)
				.decoder(decoder)
				.consumer((payload, context) -> {
					context.get().enqueueWork(() -> handler.accept(payload, ClientHooks.player()));
					context.get().setPacketHandled(true);
				})
				.add();
	}

	private static <T> void toServer(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
			Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Player> handler)
	{
		CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_SERVER)
				.encoder(encoder)
				.decoder(decoder)
				.consumer((payload, context) -> {
					context.get().enqueueWork(() -> handler.accept(payload, context.get().getSender()));
					context.get().setPacketHandled(true);
				})
				.add();
	}
}

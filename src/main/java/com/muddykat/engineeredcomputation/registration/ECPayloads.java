package com.muddykat.engineeredcomputation.registration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import com.muddykat.engineeredcomputation.crafting.computer.CraftingComputerPayload;
import com.muddykat.engineeredcomputation.crafting.order.CancelOrderPayload;
import com.muddykat.engineeredcomputation.crafting.order.OrderPlanPayload;
import com.muddykat.engineeredcomputation.crafting.order.OrderRequestPayload;
import com.muddykat.engineeredcomputation.crafting.order.TerminalCraftingPayload;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternEditPayload;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternFillPayload;
import com.muddykat.engineeredcomputation.pneumatic.carrier.CarrierSyncPayload;
import com.muddykat.engineeredcomputation.pneumatic.station.StationNamePayload;
import com.muddykat.engineeredcomputation.program.ProgramEditPayload;
import com.muddykat.engineeredcomputation.remote.TerminalRemotePayload;
import com.muddykat.engineeredcomputation.terminal.TerminalClickPayload;
import com.muddykat.engineeredcomputation.terminal.TerminalContentsPayload;
import com.muddykat.engineeredcomputation.terminal.crafting.GridFillPayload;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeLockedPayload;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeOutletPayload;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeSlipPayload;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeUnreachablePayload;

public final class ECPayloads
{
	private static final String PROTOCOL_VERSION = "1";

	private ECPayloads()
	{
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(PROTOCOL_VERSION)
				.playToClient(TerminalContentsPayload.TYPE, TerminalContentsPayload.STREAM_CODEC,
						TerminalContentsPayload::handle)
				.playToClient(TeletypeUnreachablePayload.TYPE, TeletypeUnreachablePayload.STREAM_CODEC,
						TeletypeUnreachablePayload::handle)
				.playToClient(TeletypeSlipPayload.TYPE, TeletypeSlipPayload.STREAM_CODEC, TeletypeSlipPayload::handle)
				.playToClient(TeletypeLockedPayload.TYPE, TeletypeLockedPayload.STREAM_CODEC, TeletypeLockedPayload::handle)
				.playToClient(CarrierSyncPayload.TYPE, CarrierSyncPayload.STREAM_CODEC, CarrierSyncPayload::handle)
				.playToServer(TeletypeOutletPayload.TYPE, TeletypeOutletPayload.STREAM_CODEC, TeletypeOutletPayload::handle)
				.playToServer(StationNamePayload.TYPE, StationNamePayload.STREAM_CODEC, StationNamePayload::handle)
				.playToServer(TerminalClickPayload.TYPE, TerminalClickPayload.STREAM_CODEC,
						TerminalClickPayload::handle)
				.playToServer(ProgramEditPayload.TYPE, ProgramEditPayload.STREAM_CODEC, ProgramEditPayload::handle)
				.playToServer(PatternEditPayload.TYPE, PatternEditPayload.STREAM_CODEC, PatternEditPayload::handle)
				.playToServer(PatternFillPayload.TYPE, PatternFillPayload.STREAM_CODEC, PatternFillPayload::handle)
				.playToServer(GridFillPayload.TYPE, GridFillPayload.STREAM_CODEC, GridFillPayload::handle)
				.playToServer(OrderRequestPayload.TYPE, OrderRequestPayload.STREAM_CODEC, OrderRequestPayload::handle)
				.playToServer(CancelOrderPayload.TYPE, CancelOrderPayload.STREAM_CODEC, CancelOrderPayload::handle)
				.playToClient(OrderPlanPayload.TYPE, OrderPlanPayload.STREAM_CODEC, OrderPlanPayload::handle)
				.playToClient(CraftingComputerPayload.TYPE, CraftingComputerPayload.STREAM_CODEC,
						CraftingComputerPayload::handle)
				.playToClient(TerminalCraftingPayload.TYPE, TerminalCraftingPayload.STREAM_CODEC,
						TerminalCraftingPayload::handle)
				.playToClient(TerminalRemotePayload.TYPE, TerminalRemotePayload.STREAM_CODEC,
						TerminalRemotePayload::handle));
	}
}

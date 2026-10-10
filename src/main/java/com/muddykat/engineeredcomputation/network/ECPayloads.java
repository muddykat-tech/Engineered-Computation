package com.muddykat.engineeredcomputation.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import com.muddykat.engineeredcomputation.adapter.ProgramEditPayload;
import com.muddykat.engineeredcomputation.crafting.CancelOrderPayload;
import com.muddykat.engineeredcomputation.crafting.CraftingComputerPayload;
import com.muddykat.engineeredcomputation.crafting.OrderPlanPayload;
import com.muddykat.engineeredcomputation.crafting.OrderRequestPayload;
import com.muddykat.engineeredcomputation.crafting.PatternEditPayload;
import com.muddykat.engineeredcomputation.crafting.PatternFillPayload;
import com.muddykat.engineeredcomputation.crafting.TerminalCraftingPayload;
import com.muddykat.engineeredcomputation.pneumatic.CarrierSyncPayload;
import com.muddykat.engineeredcomputation.pneumatic.StationNamePayload;
import com.muddykat.engineeredcomputation.terminal.GridFillPayload;
import com.muddykat.engineeredcomputation.terminal.TeletypeOutletPayload;
import com.muddykat.engineeredcomputation.terminal.TeletypeSlipPayload;
import com.muddykat.engineeredcomputation.terminal.TerminalClickPayload;
import com.muddykat.engineeredcomputation.terminal.TerminalContentsPayload;
import com.muddykat.engineeredcomputation.terminal.TerminalUnreachablePayload;

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
				.playToClient(TerminalUnreachablePayload.TYPE, TerminalUnreachablePayload.STREAM_CODEC,
						TerminalUnreachablePayload::handle)
				.playToClient(TeletypeSlipPayload.TYPE, TeletypeSlipPayload.STREAM_CODEC, TeletypeSlipPayload::handle)
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
						TerminalCraftingPayload::handle));
	}
}

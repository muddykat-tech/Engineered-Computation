package com.muddykat.engineeredcomputation.terminal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.catalogue.RequestSlip;

public record TeletypeSlipPayload(int containerId, List<RequestSlip.Line> lines, Component outlet, boolean home,
		List<Outlet> outlets)
{
	public record Outlet(BlockPos pos, Component label, boolean chosen)
	{
		public void write(FriendlyByteBuf buf)
		{
			buf.writeBlockPos(pos);
			buf.writeComponent(label);
			buf.writeBoolean(chosen);
		}

		public static Outlet read(FriendlyByteBuf buf)
		{
			return new Outlet(buf.readBlockPos(), buf.readComponent(), buf.readBoolean());
		}
	}

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeCollection(lines, (out, line) -> line.write(out));
		buf.writeComponent(outlet);
		buf.writeBoolean(home);
		buf.writeCollection(outlets, (out, entry) -> entry.write(out));
	}

	public static TeletypeSlipPayload decode(FriendlyByteBuf buf)
	{
		return new TeletypeSlipPayload(buf.readVarInt(),
				buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, RequestSlip.SIZE),
						RequestSlip.Line::read), buf.readComponent(),
				buf.readBoolean(), buf.readList(Outlet::read));
	}

	public static void handle(TeletypeSlipPayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applySlip(payload);
	}
}

package com.muddykat.engineeredcomputation.pneumatic;

import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.pneumatic.CarrierFlights;

public record CarrierSyncPayload(List<Flight> launched, List<Long> gone)
{
	public record Flight(long id, List<Long> path, long launched)
	{
		public void write(FriendlyByteBuf buf)
		{
			buf.writeVarLong(id);
			buf.writeCollection(path, FriendlyByteBuf::writeVarLong);
			buf.writeVarLong(launched);
		}

		public static Flight read(FriendlyByteBuf buf)
		{
			return new Flight(buf.readVarLong(), buf.readList(FriendlyByteBuf::readVarLong), buf.readVarLong());
		}
	}

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeCollection(launched, (out, flight) -> flight.write(out));
		buf.writeCollection(gone, FriendlyByteBuf::writeVarLong);
	}

	public static CarrierSyncPayload decode(FriendlyByteBuf buf)
	{
		return new CarrierSyncPayload(buf.readList(Flight::read), buf.readList(FriendlyByteBuf::readVarLong));
	}

	public static void handle(CarrierSyncPayload payload, Player player)
	{
		CarrierFlights.apply(payload);
	}
}

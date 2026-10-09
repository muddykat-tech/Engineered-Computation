package com.muddykat.engineeredcomputation.pneumatic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;

public final class CarrierFlights
{
	public record Flight(BlockPos[] path, long launched)
	{
	}

	private static final Map<Long, Flight> FLIGHTS = new HashMap<>();

	private CarrierFlights()
	{
	}

	static void apply(CarrierSyncPayload payload)
	{
		for(CarrierSyncPayload.Flight flight : payload.launched())
			FLIGHTS.put(flight.id(), new Flight(flight.path().stream().map(BlockPos::of).toArray(BlockPos[]::new),
					flight.launched()));
		payload.gone().forEach(FLIGHTS::remove);
	}

	public static Collection<Flight> all()
	{
		return Collections.unmodifiableCollection(FLIGHTS.values());
	}

	public static void clear()
	{
		FLIGHTS.clear();
	}
}

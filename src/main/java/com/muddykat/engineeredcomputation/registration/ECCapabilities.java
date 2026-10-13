package com.muddykat.engineeredcomputation.registration;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.pneumatic.TubeMember;

public final class ECCapabilities
{
	public static final BlockCapability<NetworkMember, @Nullable Direction> NETWORK_MEMBER = BlockCapability.createSided(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "network_member"), NetworkMember.class);

	public static final BlockCapability<TubeMember, @Nullable Direction> TUBE_MEMBER = BlockCapability.createSided(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "tube_member"), TubeMember.class);

	private ECCapabilities()
	{
	}
}

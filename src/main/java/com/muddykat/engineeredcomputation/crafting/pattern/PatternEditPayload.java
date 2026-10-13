package com.muddykat.engineeredcomputation.crafting.pattern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public record PatternEditPayload(int containerId, Action action, int slot, int amount, String tag)
		implements CustomPacketPayload
{
	public static final Type<PatternEditPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "pattern_edit"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PatternEditPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PatternEditPayload::containerId,
			NeoForgeStreamCodecs.enumCodec(Action.class), PatternEditPayload::action,
			ByteBufCodecs.VAR_INT, PatternEditPayload::slot,
			ByteBufCodecs.VAR_INT, PatternEditPayload::amount,
			ByteBufCodecs.STRING_UTF8, PatternEditPayload::tag,
			PatternEditPayload::new);

	public PatternEditPayload(int containerId, Action action, int slot, int amount)
	{
		this(containerId, action, slot, amount, "");
	}

	public enum Action
	{
		SET,
		ADJUST,
		CLEAR,
		SET_TAG,
		TOGGLE_MODE,
		CLEAR_ALL
	}

	@Override
	public Type<PatternEditPayload> type()
	{
		return TYPE;
	}

	public static void handle(PatternEditPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof PatternFloppyMenu menu&&menu.containerId==payload.containerId())
			menu.edit(payload.action(), payload.slot(), payload.amount(), payload.tag());
	}
}

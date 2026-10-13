package com.muddykat.engineeredcomputation.program;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;

/** SET_ITEM uses {@code item}, or the carried stack when it's empty. */
public record ProgramEditPayload(int containerId, int index, Action action, String tag, ItemStack item)
		implements CustomPacketPayload
{
	public static final Type<ProgramEditPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "program_edit"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ProgramEditPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ProgramEditPayload::containerId,
			ByteBufCodecs.VAR_INT, ProgramEditPayload::index,
			NeoForgeStreamCodecs.enumCodec(Action.class), ProgramEditPayload::action,
			ByteBufCodecs.stringUtf8(256), ProgramEditPayload::tag,
			ItemStack.OPTIONAL_STREAM_CODEC, ProgramEditPayload::item,
			ProgramEditPayload::new);

	public ProgramEditPayload(int containerId, int index, Action action, String tag)
	{
		this(containerId, index, action, tag, ItemStack.EMPTY);
	}

	public enum Action
	{
		SET_ITEM,
		SET_TAG,
		SET_NAME,
		CLEAR
	}

	@Override
	public Type<ProgramEditPayload> type()
	{
		return TYPE;
	}

	public static void handle(ProgramEditPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof ProgramEditor editor
				&&context.player().containerMenu.containerId==payload.containerId())
			editor.edit(payload.index(), payload.action(), payload.tag(), payload.item());
	}
}

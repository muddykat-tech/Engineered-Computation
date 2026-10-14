package com.muddykat.engineeredcomputation.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public final class ECSounds
{
	private static final DeferredRegister<SoundEvent> REGISTER =
			DeferredRegister.create(Registries.SOUND_EVENT, EngineeredComputation.MODID);

	public static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_35_INSERT = register("block.floppy_35.insert");
	public static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_35_EJECT = register("block.floppy_35.eject");
	public static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_525_INSERT = register("block.floppy_525.insert");
	public static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_525_EJECT = register("block.floppy_525.eject");

	private ECSounds()
	{
	}

	private static DeferredHolder<SoundEvent, SoundEvent> register(String name)
	{
		return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, name)));
	}

	public static void register(IEventBus modEventBus)
	{
		REGISTER.register(modEventBus);
	}
}

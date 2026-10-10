package com.muddykat.engineeredcomputation.registration;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.EngineeredComputation;

/**
 * Vanilla 1.21 sounds the 1.21.1 version of the mod plays, which 1.20.1 doesn't have: the files and definitions are
 * copied from 1.21.1 (assets/engineered_computation/sounds.json), under their vanilla names.
 */
public final class ECSounds
{
	public static final DeferredRegister<SoundEvent> REGISTER =
			DeferredRegister.create(Registry.SOUND_EVENT_REGISTRY, EngineeredComputation.MODID);

	public static final RegistryObject<SoundEvent> BREEZE_SHOOT = register("entity.breeze.shoot");
	public static final RegistryObject<SoundEvent> DECORATED_POT_INSERT = register("block.decorated_pot.insert");
	public static final RegistryObject<SoundEvent> DECORATED_POT_INSERT_FAIL =
			register("block.decorated_pot.insert_fail");

	private ECSounds()
	{
	}

	private static RegistryObject<SoundEvent> register(String name)
	{
		return REGISTER.register(name, () -> new SoundEvent(
				ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, name)));
	}

	public static void register(IEventBus modEventBus)
	{
		REGISTER.register(modEventBus);
	}
}

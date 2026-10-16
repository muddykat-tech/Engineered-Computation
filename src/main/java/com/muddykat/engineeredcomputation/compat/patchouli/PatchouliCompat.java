package com.muddykat.engineeredcomputation.compat.patchouli;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import vazkii.patchouli.api.IStateMatcher;
import vazkii.patchouli.api.PatchouliAPI;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.EraMachine;
import com.muddykat.engineeredcomputation.guide.GuideOutline;
import com.muddykat.engineeredcomputation.guide.GuideTexts;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.registration.ECContent;

// Only loaded when Patchouli is installed
public final class PatchouliCompat
{
	public static final ResourceLocation HANDBOOK = id("handbook");
	// Value for multiblocks, ensures they fit correctly on the guide pages
	private static final double PATCHOULI_SCALE_SPAN = 90;
	private static final double FRAME = 86;

	private PatchouliCompat()
	{
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(PatchouliCompat::registerMultiblocks));
		if(FMLEnvironment.dist.isClient())
			modEventBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(PatchouliCompat::registerTextFunctions));
	}

	/** The multiblock pages' ids, which the exported handbook names. */
	public static ResourceLocation multiblockId(String structure)
	{
		return id(structure);
	}

	/** The handbook category each era's guide opens at. */
	public static ResourceLocation eraCategory(ComputerEra era)
	{
		return id(era.getSerializedName());
	}

	public static void openLanding()
	{
		PatchouliAPI.get().openBookGUI(HANDBOOK);
	}

	public static void openEntry(ResourceLocation entry, int page)
	{
		PatchouliAPI.get().openBookEntry(HANDBOOK, entry, page);
	}

	private static void registerMultiblocks()
	{
		for(EraDrive drive : EraDrive.values())
			register(drive.getSerializedName(), ECContent.casing(drive.era()).get(), drive.geometry());
		register(EraMachine.CRAFTING_COMPUTER.blockName(), ECContent.casing(EraMachine.CRAFTING_COMPUTER.era()).get(),
				EraMachine.CRAFTING_COMPUTER.geometry());
		register(EraMachine.DATA_PROCESSOR.blockName(), ECContent.PROCESSOR_CASING.get(),
				EraMachine.DATA_PROCESSOR.geometry());
	}

	/** Every block of the structure is its casing, as the guide's build pages show it. */
	private static void register(String structure, Block casing, EraDriveGeometry geometry)
	{
		int pad = padding(geometry.size());
		Map<BlockPos, IStateMatcher> blocks = new HashMap<>();
		IStateMatcher matcher = PatchouliAPI.get().looseBlockMatcher(casing);
		geometry.shapes().keySet().forEach(pos -> blocks.put(pos.offset(pad, 0, 0), matcher));
		IStateMatcher nothing = PatchouliAPI.get().anyMatcher();
		for(int z = 0; z<geometry.size().getZ()&&pad>0; z++)
		{
			blocks.putIfAbsent(new BlockPos(0, 0, z), nothing);
			blocks.putIfAbsent(new BlockPos(geometry.size().getX()-1+2*pad, 0, z), nothing);
		}
		// Patchouli centres the picture on positions counted from zero, so the padding shifts the structure, and the
		// offset puts the structure's own corner back where the player aims the in-world preview.
		PatchouliAPI.get().registerMultiblock(multiblockId(structure),
				PatchouliAPI.get().makeSparseMultiblock(blocks).offset(pad, 0, 0));
	}

	// used when a multiblock is too tall and would spill outside the main view pane
	private static int padding(BlockPos size)
	{
		double across = Math.hypot(size.getX(), size.getZ());
		double fits = Math.min(FRAME/(Math.cos(Math.toRadians(30))*size.getY()+Math.sin(Math.toRadians(30))*across),
				FRAME/across);
		double wanted = PATCHOULI_SCALE_SPAN/fits;
		int pad = 0;
		while(Math.hypot(size.getX()+2*pad, size.getZ())<wanted)
			pad++;
		return pad;
	}

	private static void registerTextFunctions()
	{
		function(GuideOutline.ERA_FUNCTION, era -> era(era).displayName());
		function(GuideOutline.MEMORY_FUNCTION, era -> era(era).memoryDisplayName());
		function(GuideOutline.PART_FUNCTION, era -> ECContent.component(era(era)).get().getDescription());
		function(GuideOutline.GUIDE_FUNCTION, era -> ECContent.guide(era(era)).get().getDescription());
		function(GuideOutline.CONFIG_FUNCTION, name -> Component.literal(Integer.toString(switch(name)
		{
			case GuideTexts.CARRIER_SIZE_UNPRESSURED -> ECConfig.carrierItems(false);
			case GuideTexts.CARRIER_TICKS -> ECConfig.carrierTicks(false);
			case GuideTexts.CARRIER_SIZE_PRESSURED -> ECConfig.carrierItems(true);
			case GuideTexts.CARRIER_PRESSURED_TICKS -> ECConfig.carrierTicks(true);
			case GuideTexts.CARRIERS_PER_FUEL -> ECConfig.carriersPerFuel();
			default -> throw new IllegalArgumentException("No handbook config value called "+name);
		})));
		function(GuideOutline.CODE_FUNCTION, code -> {
			for(StatusCode status : StatusCode.values())
				if(Integer.toString(status.code()).equals(code))
					return status.title();
			throw new IllegalArgumentException("No status code "+code);
		});
	}

	private static void function(String name, Function<String, Component> value)
	{
		PatchouliAPI.get().registerFunction(name, (param, style) -> {
			try
			{
				return value.apply(param).getString();
			}
			catch(IllegalArgumentException e)
			{
				EngineeredComputation.LOGGER.warn("Handbook text asked for $({}:{}): {}", name, param, e.getMessage());
				return param;
			}
		});
	}

	private static ComputerEra era(String name)
	{
		for(ComputerEra era : ComputerEra.values())
			if(era.getSerializedName().equals(name))
				return era;
		throw new IllegalArgumentException("No era called "+name);
	}

	private static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, path);
	}
}

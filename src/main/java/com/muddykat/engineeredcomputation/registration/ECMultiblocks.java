package com.muddykat.engineeredcomputation.registration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.crafting.CraftingComputerState;
import com.muddykat.engineeredcomputation.crafting.DataProcessorState;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraMachine;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.storage.StorageRole;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockDefinition;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlock;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.ECCapabilities;
import com.muddykat.engineeredcomputation.pneumatic.TubeMember;

public final class ECMultiblocks
{
	private static final BlockBehaviour.Properties PART_PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.forceSolidOn()
			.isViewBlocking((state, level, pos) -> false)
			.pushReaction(PushReaction.BLOCK);

	private static final Map<EraDrive, DeferredBlock<MultiblockPartBlock>> ERA_DRIVE_BLOCKS =
			new EnumMap<>(EraDrive.class);

	public static final Map<EraDrive, MultiblockDefinition<EraDriveState>> ERA_DRIVES = registerEraDrives();

	private static final DeferredBlock<MultiblockPartBlock> CRAFTING_COMPUTER_BLOCK = ECRegistrationHolder.BLOCKS.register(
			EraMachine.CRAFTING_COMPUTER.blockName(), () -> new MultiblockPartBlock(PART_PROPERTIES, () -> ECMultiblocks.CRAFTING_COMPUTER));

	public static final MultiblockDefinition<CraftingComputerState> CRAFTING_COMPUTER =
			MultiblockDefinition.<CraftingComputerState>builder(id("multiblocks/"+EraMachine.CRAFTING_COMPUTER.blockName()))
					.positions(EraMachine.CRAFTING_COMPUTER.geometry().master(), EraMachine.CRAFTING_COMPUTER.geometry().trigger())
					.shapes(EraMachine.CRAFTING_COMPUTER.geometry()::shapeAt)
					.blocks(CRAFTING_COMPUTER_BLOCK, ECContent.casing(EraMachine.CRAFTING_COMPUTER.era()))
					.state(CraftingComputerState::new)
					.ticking()
					.build();

	private static final DeferredBlock<MultiblockPartBlock> DATA_PROCESSOR_BLOCK = ECRegistrationHolder.BLOCKS.register(
			EraMachine.DATA_PROCESSOR.blockName(),
			() -> new MultiblockPartBlock(PART_PROPERTIES, () -> ECMultiblocks.DATA_PROCESSOR));

	public static final MultiblockDefinition<DataProcessorState> DATA_PROCESSOR =
			MultiblockDefinition.<DataProcessorState>builder(id("multiblocks/"+EraMachine.DATA_PROCESSOR.blockName()))
					.positions(EraMachine.DATA_PROCESSOR.geometry().master(), EraMachine.DATA_PROCESSOR.geometry().trigger())
					.shapes(EraMachine.DATA_PROCESSOR.geometry()::shapeAt)
					.blocks(DATA_PROCESSOR_BLOCK, ECContent.PROCESSOR_CASING)
					.state(DataProcessorState::new)
					.ticking()
					.build();

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiblockPartBlockEntity>> PART_BLOCK_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("multiblock_part", () -> BlockEntityType.Builder.of(
					MultiblockPartBlockEntity::new,
					Stream.concat(ERA_DRIVE_BLOCKS.values().stream(), Stream.of(CRAFTING_COMPUTER_BLOCK, DATA_PROCESSOR_BLOCK))
							.map(DeferredBlock::get).toArray(MultiblockPartBlock[]::new)
			).build(null));

	private static Map<EraDrive, MultiblockDefinition<EraDriveState>> registerEraDrives()
	{
		Map<EraDrive, MultiblockDefinition<EraDriveState>> drives = new EnumMap<>(EraDrive.class);
		for(EraDrive drive : EraDrive.values())
		{
			DeferredBlock<MultiblockPartBlock> block = ECRegistrationHolder.BLOCKS.register(
					drive.blockName(), () -> new MultiblockPartBlock(PART_PROPERTIES, () -> ERA_DRIVES.get(drive)));
			ERA_DRIVE_BLOCKS.put(drive, block);

			EraDriveGeometry geometry = drive.geometry();
			MultiblockDefinition.Builder<EraDriveState> builder =
					MultiblockDefinition.<EraDriveState>builder(id("multiblocks/"+drive.blockName()))
							.positions(geometry.master(), geometry.trigger())
							.shapes(geometry::shapeAt)
							.blocks(block, ECContent.casing(drive.era()))
							.state(host -> new EraDriveState(drive, host));
			if(StorageRole.of(drive.era())==StorageRole.BULK||drive.era()==ComputerEra.PUNCHCARD)
				builder.ticking();
			drives.put(drive, builder.build());
		}
		return Collections.unmodifiableMap(drives);
	}

	private ECMultiblocks()
	{
	}

	public static void init()
	{
	}

	public static List<MultiblockDefinition<EraDriveState>> all()
	{
		return ERA_DRIVES.entrySet().stream()
				.sorted(Comparator.comparingInt(entry -> -entry.getKey().geometry().shapes().size()))
				.map(Map.Entry::getValue)
				.toList();
	}

	public static List<MultiblockDefinition<?>> formable()
	{
		List<MultiblockDefinition<?>> formable = new ArrayList<>(all());
		formable.add(CRAFTING_COMPUTER);
		formable.add(DATA_PROCESSOR);
		formable.sort(Comparator.comparingInt(definition -> -geometryOf(definition).shapes().size()));
		return formable;
	}

	private static EraDriveGeometry geometryOf(MultiblockDefinition<?> definition)
	{
		EraDrive drive = driveOf(definition);
		if(drive!=null)
			return drive.geometry();
		return definition==DATA_PROCESSOR? EraMachine.DATA_PROCESSOR.geometry(): EraMachine.CRAFTING_COMPUTER.geometry();
	}

	@Nullable
	public static EraDrive driveOf(MultiblockDefinition<?> definition)
	{
		for(Map.Entry<EraDrive, MultiblockDefinition<EraDriveState>> entry : ERA_DRIVES.entrySet())
			if(entry.getValue()==definition)
				return entry.getKey();
		return null;
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> {
			event.registerBlockEntity(
					Capabilities.ItemHandler.BLOCK, PART_BLOCK_ENTITY.get(), MultiblockPartBlockEntity::getItemHandler);
			event.registerBlockEntity(ECCapabilities.NETWORK_MEMBER, PART_BLOCK_ENTITY.get(), (part, side) -> {
				MultiblockPartBlockEntity master = part.getMaster();
				if(master==null)
					return null;
				NetworkMember member;
				EraDriveGeometry geometry;
				if(master.getState() instanceof EraDriveState drive&&drive.getEra()==ComputerEra.PUNCHCARD)
					return null;
				if(master.getState() instanceof EraDriveState drive)
				{
					member = drive;
					geometry = drive.getDrive().geometry();
				}
				else if(master.getState() instanceof CraftingComputerState computer)
				{
					member = computer;
					geometry = EraMachine.CRAFTING_COMPUTER.geometry();
				}
				else if(master.getState() instanceof DataProcessorState processor)
				{
					member = processor;
					geometry = EraMachine.DATA_PROCESSOR.geometry();
				}
				else
					return null;
				if(side==null||isSameStructure(part, side))
					return member;
				Direction facing = part.getBlockState().getValue(MultiblockPartBlock.FACING);
				return geometry.connectsOn(part.getPosInMultiblock(), MultiblockOrientation.toTemplateSide(side, facing))
						? member: null;
			});
			event.registerBlockEntity(ECCapabilities.TUBE_MEMBER, PART_BLOCK_ENTITY.get(), (part, side) -> {
				MultiblockPartBlockEntity master = part.getMaster();
				if(master==null||!(master.getState() instanceof EraDriveState drive)
						||drive.getEra()!=ComputerEra.PUNCHCARD)
					return null;
				if(side==null||isSameStructure(part, side))
					return TubeMember.END;
				Direction facing = part.getBlockState().getValue(MultiblockPartBlock.FACING);
				return drive.getDrive().geometry().connectsOn(part.getPosInMultiblock(),
						MultiblockOrientation.toTemplateSide(side, facing))? TubeMember.END: null;
			});
		});
	}

	private static boolean isSameStructure(MultiblockPartBlockEntity part, Direction side)
	{
		return part.getLevel()!=null
				&&part.getLevel().getBlockEntity(part.getBlockPos().relative(side)) instanceof MultiblockPartBlockEntity other
				&&other.getMasterPos().equals(part.getMasterPos());
	}

	private static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, path);
	}
}

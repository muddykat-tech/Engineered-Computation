package com.muddykat.engineeredcomputation.multiblock.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public final class MultiblockDefinition<S extends MultiblockState>
{
	private final ResourceLocation templateLocation;
	private final BlockPos masterPos;
	private final BlockPos triggerPos;
	private final Function<BlockPos, VoxelShape> shapes;
	private final Function<MultiblockStateHost, S> stateFactory;
	private final Supplier<? extends Block> partBlock;
	private final Supplier<? extends Block> templateBlock;
	private final boolean ticking;

	private final Map<ShapeKey, VoxelShape> shapeCache = new ConcurrentHashMap<>();
	@Nullable
	private volatile LoadedTemplate loaded;

	private MultiblockDefinition(Builder<S> builder)
	{
		this.templateLocation = Objects.requireNonNull(builder.templateLocation);
		this.masterPos = Objects.requireNonNull(builder.masterPos);
		this.triggerPos = Objects.requireNonNull(builder.triggerPos);
		this.shapes = builder.shapes;
		this.stateFactory = Objects.requireNonNull(builder.stateFactory);
		this.partBlock = Objects.requireNonNull(builder.partBlock);
		this.templateBlock = Objects.requireNonNull(builder.templateBlock);
		this.ticking = builder.ticking;
	}

	public static <S extends MultiblockState> Builder<S> builder(ResourceLocation templateLocation)
	{
		return new Builder<>(templateLocation);
	}

	public BlockPos masterPos()
	{
		return masterPos;
	}

	public Block partBlock()
	{
		return partBlock.get();
	}

	public Block templateBlock()
	{
		return templateBlock.get();
	}

	public boolean isTicking()
	{
		return ticking;
	}

	S createState(MultiblockStateHost host)
	{
		return stateFactory.apply(host);
	}

	public VoxelShape shapeAt(BlockPos posInMultiblock, Direction facing)
	{
		return shapeCache.computeIfAbsent(new ShapeKey(posInMultiblock, facing),
				key -> MultiblockOrientation.rotateShape(shapes.apply(key.pos()), key.facing()));
	}

	public boolean tryForm(Level level, BlockPos clicked, Direction side)
	{
		if(!(level instanceof ServerLevel serverLevel)||side.getAxis()==Direction.Axis.Y)
			return false;
		LoadedTemplate template = getTemplate(serverLevel);
		if(template==null||!level.getBlockState(clicked).is(template.trigger().getBlock()))
			return false;

		Direction facing = side.getOpposite();
		BlockPos origin = clicked.subtract(MultiblockOrientation.toWorldOffset(triggerPos, facing));
		for(StructureBlockInfo info : template.blocks())
		{
			BlockPos here = origin.offset(MultiblockOrientation.toWorldOffset(info.pos(), facing));
			if(!level.getBlockState(here).is(info.state().getBlock()))
				return false;
		}

		BlockState partState = partBlock().defaultBlockState().setValue(MultiblockPartBlock.FACING, facing);
		List<BlockPos> placed = new ArrayList<>();
		List<BlockState> replaced = new ArrayList<>();
		for(StructureBlockInfo info : template.blocks())
		{
			BlockPos here = origin.offset(MultiblockOrientation.toWorldOffset(info.pos(), facing));
			replaced.add(level.getBlockState(here));
			level.setBlock(here, partState, Block.UPDATE_NONE);
			if(level.getBlockEntity(here) instanceof MultiblockPartBlockEntity part)
				part.initialize(info.pos());
			placed.add(here);
		}
		for(int i = 0; i<placed.size(); i++)
			level.markAndNotifyBlock(placed.get(i), level.getChunkAt(placed.get(i)), replaced.get(i), partState,
					Block.UPDATE_ALL, 512);
		sendToWatchers(serverLevel, placed);
		return true;
	}

	/**
	 * Sends the formed blocks to nearby players straight away, because some servers miss the usual update and players
	 * will see casings until they rejoin.
	 */
	private static void sendToWatchers(ServerLevel level, List<BlockPos> placed)
	{
		for(BlockPos pos : placed)
		{
			List<ServerPlayer> watching = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false);
			if(watching.isEmpty())
				continue;
			Packet<?> block = new ClientboundBlockUpdatePacket(level, pos);
			Packet<?> data = level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part? part.getUpdatePacket(): null;
			for(ServerPlayer player : watching)
			{
				player.connection.send(block);
				if(data!=null)
					player.connection.send(data);
			}
		}
	}

	public void disassemble(Level level, MultiblockPartBlockEntity broken)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;
		LoadedTemplate template = getTemplate(serverLevel);
		if(template==null)
			return;

		Direction facing = broken.getFacing();
		BlockPos masterWorldPos = broken.getMasterPos();
		BlockPos origin = masterWorldPos.subtract(MultiblockOrientation.toWorldOffset(masterPos, facing));
		List<BlockPos> positions = new ArrayList<>(template.blocks().size());
		for(StructureBlockInfo info : template.blocks())
			positions.add(origin.offset(MultiblockOrientation.toWorldOffset(info.pos(), facing)));

		broken.markDisassembling();
		for(BlockPos pos : positions)
			if(level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part&&part.getDefinition()==this)
				part.markDisassembling();

		MultiblockPartBlockEntity master = broken.isMaster()? broken: broken.getMaster();
		if(master!=null&&master.getState()!=null)
			master.getState().onDisassemble(master.getContext());

		Rotation rotation = MultiblockOrientation.rotationFor(facing);
		for(int i = 0; i<positions.size(); i++)
		{
			BlockPos pos = positions.get(i);
			if(pos.equals(broken.getBlockPos())||!level.getBlockState(pos).is(partBlock()))
				continue;
			level.setBlockAndUpdate(pos, template.blocks().get(i).state().rotate(rotation));
		}
	}

	@Nullable
	public BlockState getOriginalBlock(ServerLevel level, BlockPos posInMultiblock, Direction facing)
	{
		LoadedTemplate template = getTemplate(level);
		if(template!=null)
			for(StructureBlockInfo info : template.blocks())
				if(info.pos().equals(posInMultiblock))
					return info.state().rotate(MultiblockOrientation.rotationFor(facing));
		return null;
	}

	@Nullable
	private LoadedTemplate getTemplate(ServerLevel level)
	{
		StructureTemplate source = level.getStructureManager().get(templateLocation).orElse(null);
		if(source==null)
		{
			EngineeredComputation.LOGGER.error("Multiblock template {} does not exist", templateLocation);
			return null;
		}
		LoadedTemplate current = loaded;
		if(current!=null&&current.source()==source)
			return current;

		List<StructureBlockInfo> blocks = readBlocks(source, level.holderLookup(Registries.BLOCK));
		BlockState trigger = null;
		for(StructureBlockInfo info : blocks)
			if(info.pos().equals(triggerPos))
				trigger = info.state();
		if(trigger==null)
		{
			EngineeredComputation.LOGGER.error("Multiblock template {} has no block at its trigger position {}",
					templateLocation, triggerPos);
			return null;
		}
		current = new LoadedTemplate(source, List.copyOf(blocks), trigger);
		loaded = current;
		return current;
	}

	private static List<StructureBlockInfo> readBlocks(StructureTemplate template, HolderGetter<Block> blockLookup)
	{
		CompoundTag tag = template.save(new CompoundTag());
		ListTag paletteTag = tag.contains("palettes", Tag.TAG_LIST)
				? tag.getList("palettes", Tag.TAG_LIST).getList(0)
				: tag.getList("palette", Tag.TAG_COMPOUND);
		List<BlockState> palette = new ArrayList<>(paletteTag.size());
		for(int i = 0; i<paletteTag.size(); i++)
			palette.add(NbtUtils.readBlockState(blockLookup, paletteTag.getCompound(i)));

		ListTag blocksTag = tag.getList("blocks", Tag.TAG_COMPOUND);
		List<StructureBlockInfo> blocks = new ArrayList<>(blocksTag.size());
		for(int i = 0; i<blocksTag.size(); i++)
		{
			CompoundTag blockTag = blocksTag.getCompound(i);
			ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
			BlockState state = palette.get(blockTag.getInt("state"));
			if(!state.isAir())
				blocks.add(new StructureBlockInfo(
						new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2)), state, null));
		}
		return blocks;
	}

	private record LoadedTemplate(StructureTemplate source, List<StructureBlockInfo> blocks, BlockState trigger)
	{
	}

	private record ShapeKey(BlockPos pos, Direction facing)
	{
	}

	public static final class Builder<S extends MultiblockState>
	{
		private final ResourceLocation templateLocation;
		private BlockPos masterPos;
		private BlockPos triggerPos;
		private Function<BlockPos, VoxelShape> shapes = pos -> Shapes.block();
		private Function<MultiblockStateHost, S> stateFactory;
		private Supplier<? extends Block> partBlock;
		private Supplier<? extends Block> templateBlock;
		private boolean ticking;

		private Builder(ResourceLocation templateLocation)
		{
			this.templateLocation = templateLocation;
		}

		public Builder<S> positions(BlockPos master, BlockPos trigger)
		{
			this.masterPos = master;
			this.triggerPos = trigger;
			return this;
		}

		public Builder<S> shapes(Function<BlockPos, VoxelShape> shapes)
		{
			this.shapes = shapes;
			return this;
		}

		public Builder<S> state(Function<MultiblockStateHost, S> stateFactory)
		{
			this.stateFactory = stateFactory;
			return this;
		}

		public Builder<S> blocks(Supplier<? extends Block> partBlock, Supplier<? extends Block> templateBlock)
		{
			this.partBlock = partBlock;
			this.templateBlock = templateBlock;
			return this;
		}

		public Builder<S> ticking()
		{
			this.ticking = true;
			return this;
		}

		public MultiblockDefinition<S> build()
		{
			return new MultiblockDefinition<>(this);
		}
	}
}

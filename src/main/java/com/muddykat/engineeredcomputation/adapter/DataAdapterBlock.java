package com.muddykat.engineeredcomputation.adapter;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.registration.ECAdapters;

public class DataAdapterBlock extends DirectionalBlock implements EntityBlock
{
	public static final MapCodec<DataAdapterBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ComputerEra.CODEC.fieldOf("era").forGetter(DataAdapterBlock::getEra),
			AdapterKind.CODEC.fieldOf("kind").forGetter(DataAdapterBlock::getKind),
			propertiesCodec()
	).apply(instance, DataAdapterBlock::new));

	public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static
	{
		for(Direction facing : Direction.values())
			SHAPES.put(facing, Shapes.or(box(facing, 1, 1, 0, 15, 15, 12), box(facing, 6, 6, 12, 10, 10, 16)));
	}

	private final ComputerEra era;
	private final AdapterKind kind;

	public DataAdapterBlock(ComputerEra era, AdapterKind kind, Properties properties)
	{
		super(properties);
		this.era = era;
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
	}

	public ComputerEra getEra()
	{
		return era;
	}

	public AdapterKind getKind()
	{
		return kind;
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec()
	{
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, ACTIVE);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return SHAPES.get(state.getValue(FACING));
	}

	private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1)
	{
		double[] a = turn(facing, x0, y0, z0);
		double[] b = turn(facing, x1, y1, z1);
		return Block.box(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
				Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2]));
	}

	private static double[] turn(Direction facing, double x, double y, double z)
	{
		return switch(facing)
		{
			case NORTH -> new double[]{x, y, z};
			case SOUTH -> new double[]{16-x, y, 16-z};
			case WEST -> new double[]{z, y, 16-x};
			case EAST -> new double[]{16-z, y, x};
			case UP -> new double[]{x, 16-z, y};
			case DOWN -> new double[]{x, z, 16-y};
		};
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
	{
		if(!level.isClientSide&&level.getBlockEntity(pos) instanceof DataAdapterBlockEntity adapter)
			player.openMenu(adapter, buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new DataAdapterBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		if(level.isClientSide||type!=ECAdapters.BLOCK_ENTITY.get())
			return null;
		return (tickLevel, pos, tickState, adapter) -> ((DataAdapterBlockEntity)adapter).tickServer();
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock()))
			StorageNetwork.changed(level);
		if(!state.is(newState.getBlock())&&level.getBlockEntity(pos) instanceof DataAdapterBlockEntity adapter
				&&AdapterFilter.isProgram(adapter.getFilter()))
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), adapter.getFilter());
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
	{
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if(!oldState.is(state.getBlock()))
			StorageNetwork.changed(level);
	}
}

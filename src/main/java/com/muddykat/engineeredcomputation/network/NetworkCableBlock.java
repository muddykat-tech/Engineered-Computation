package com.muddykat.engineeredcomputation.network;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.adapter.DataAdapterBlock;

public class NetworkCableBlock extends Block
{
	public static final MapCodec<NetworkCableBlock> CODEC = simpleCodec(NetworkCableBlock::new);
	public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;

	public static final NetworkMember NETWORK_MEMBER = new NetworkMember()
	{
	};

	private static final double MIN = 6;
	private static final double MAX = 10;
	private static final VoxelShape[] SHAPES = new VoxelShape[64];

	static
	{
		for(int index = 0; index<SHAPES.length; index++)
		{
			VoxelShape shape = Block.box(MIN, MIN, MIN, MAX, MAX, MAX);
			for(Direction direction : Direction.values())
				if((index&1<<direction.ordinal())!=0)
					shape = Shapes.or(shape, arm(direction));
			SHAPES[index] = shape;
		}
	}

	public NetworkCableBlock(Properties properties)
	{
		super(properties);
		BlockState state = stateDefinition.any();
		for(BooleanProperty connection : CONNECTIONS.values())
			state = state.setValue(connection, false);
		registerDefaultState(state);
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		CONNECTIONS.values().forEach(builder::add);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		BlockState state = defaultBlockState();
		for(Direction direction : Direction.values())
			state = state.setValue(CONNECTIONS.get(direction),
					connectsTo(context.getLevel(), context.getClickedPos().relative(direction), direction));
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
			LevelAccessor level, BlockPos pos, BlockPos neighborPos)
	{
		if(level instanceof Level fullLevel)
			return state.setValue(CONNECTIONS.get(direction), connectsTo(fullLevel, neighborPos, direction));
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return SHAPES[shapeIndex(state)];
	}

	private static boolean connectsTo(Level level, BlockPos neighbor, Direction towards)
	{
		BlockState state = level.getBlockState(neighbor);
		if(state.getBlock() instanceof DataAdapterBlock)
			return state.getValue(DataAdapterBlock.FACING)==towards;
		return level.getCapability(ECCapabilities.NETWORK_MEMBER, neighbor, towards.getOpposite())!=null;
	}

	private static int shapeIndex(BlockState state)
	{
		int index = 0;
		for(Map.Entry<Direction, BooleanProperty> connection : CONNECTIONS.entrySet())
			if(state.getValue(connection.getValue()))
				index |= 1<<connection.getKey().ordinal();
		return index;
	}

	private static VoxelShape arm(Direction direction)
	{
		return switch(direction)
		{
			case DOWN -> Block.box(MIN, 0, MIN, MAX, MIN, MAX);
			case UP -> Block.box(MIN, MAX, MIN, MAX, 16, MAX);
			case NORTH -> Block.box(MIN, MIN, 0, MAX, MAX, MIN);
			case SOUTH -> Block.box(MIN, MIN, MAX, MAX, MAX, 16);
			case WEST -> Block.box(0, MIN, MIN, MIN, MAX, MAX);
			case EAST -> Block.box(MAX, MIN, MIN, 16, MAX, MAX);
		};
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
	{
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if(!oldState.is(state.getBlock()))
			StorageNetwork.changed(level);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock()))
			StorageNetwork.changed(level);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}

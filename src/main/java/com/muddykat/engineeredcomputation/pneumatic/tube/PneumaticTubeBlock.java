package com.muddykat.engineeredcomputation.pneumatic.tube;

import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.connector.ConnectorBlockEntity;
import com.muddykat.engineeredcomputation.connector.ConnectorPorts;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.registration.ECCapabilities;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;

public class PneumaticTubeBlock extends Block implements EntityBlock, ConnectorPorts.Connector
{
	public static final MapCodec<PneumaticTubeBlock> CODEC = simpleCodec(PneumaticTubeBlock::new);
	public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;
	public static final EnumProperty<Straight> STRAIGHT = EnumProperty.create("straight", Straight.class);
	public static final BooleanProperty WINDOWED = BooleanProperty.create("windowed");
	public static final IntegerProperty TINT = IntegerProperty.create("tint", 0, DyeColor.values().length);

	private static final double MIN = 5;
	private static final double MAX = 11;
	private static final double BOX_MIN = 4;
	private static final double BOX_MAX = 12;
	private static final VoxelShape[] SHAPES = new VoxelShape[64];
	/** Includes straight runs, which need the junction box when a side is capped. */
	private static final VoxelShape[] JUNCTIONS = new VoxelShape[64];
	private static final VoxelShape[] CAPS = {
			Block.box(4.5, 3, 4.5, 11.5, 4, 11.5), Block.box(4.5, 12, 4.5, 11.5, 13, 11.5),
			Block.box(4.5, 4.5, 3, 11.5, 11.5, 4), Block.box(4.5, 4.5, 12, 11.5, 11.5, 13),
			Block.box(3, 4.5, 4.5, 4, 11.5, 11.5), Block.box(12, 4.5, 4.5, 13, 11.5, 11.5)};

	public enum Straight implements StringRepresentable
	{
		NONE, X, Y, Z;

		@Override
		public String getSerializedName()
		{
			return name().toLowerCase(Locale.ROOT);
		}

		static Straight of(Direction.Axis axis)
		{
			return switch(axis)
			{
				case X -> X;
				case Y -> Y;
				case Z -> Z;
			};
		}
	}

	static
	{
		for(int index = 0; index<SHAPES.length; index++)
		{
			VoxelShape junction = Block.box(BOX_MIN, BOX_MIN, BOX_MIN, BOX_MAX, BOX_MAX, BOX_MAX);
			for(Direction direction : Direction.values())
				if((index&1<<direction.ordinal())!=0)
					junction = Shapes.or(junction, arm(direction));
			JUNCTIONS[index] = junction;
			SHAPES[index] = switch(straightOf(index))
			{
				case NONE -> junction;
				case X -> Block.box(0, MIN, MIN, 16, MAX, MAX);
				case Y -> Block.box(MIN, 0, MIN, MAX, 16, MAX);
				case Z -> Block.box(MIN, MIN, 0, MAX, MAX, 16);
			};
		}
	}

	public PneumaticTubeBlock(Properties properties)
	{
		super(properties);
		BlockState state = stateDefinition.any().setValue(STRAIGHT, Straight.NONE).setValue(WINDOWED, false)
				.setValue(TINT, 0);
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
		builder.add(STRAIGHT, WINDOWED, TINT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		BlockState state = defaultBlockState();
		for(Direction direction : Direction.values())
			state = state.setValue(CONNECTIONS.get(direction),
					connectsTo(context.getLevel(), context.getClickedPos().relative(direction), direction));
		return withStraight(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
			LevelAccessor level, BlockPos pos, BlockPos neighborPos)
	{
		if(level instanceof Level fullLevel)
			return rejoin(fullLevel, state, pos, direction);
		return state;
	}

	@Override
	public BlockState rejoin(Level level, BlockState state, BlockPos pos, Direction side)
	{
		boolean target = connectsTo(level, pos.relative(side), side);
		ConnectorPorts.forgetCut(level, pos, side, target);
		BlockState rejoined = withStraight(state.setValue(CONNECTIONS.get(side),
				!ConnectorBlockEntity.isCut(level, pos, side)&&target));
		// A cap sits on the junction box, so a capped tube keeps the box even when it runs straight.
		return level.getBlockEntity(pos) instanceof ConnectorBlockEntity connector&&connector.getCut()!=0
				? rejoined.setValue(STRAIGHT, Straight.NONE): rejoined;
	}

	@Override
	public boolean isJoined(BlockState state, Direction side)
	{
		return state.getValue(CONNECTIONS.get(side));
	}

	@Override
	public VoxelShape armShape(BlockState state, Direction side)
	{
		return arm(side);
	}

	@Override
	public VoxelShape capShape(Direction side)
	{
		return CAPS[side.ordinal()];
	}

	@Override
	public void networkChanged(Level level)
	{
		TubeNetwork.changed(level);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new ConnectorBlockEntity(ECPneumatics.TUBE_ENTITY.get(), pos, state);
	}

	private static BlockState withStraight(BlockState state)
	{
		return state.setValue(STRAIGHT, straightOf(shapeIndex(state)));
	}

	private static Straight straightOf(int index)
	{
		for(Direction.Axis axis : Direction.Axis.values())
		{
			int pair = 1<<Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).ordinal()
					|1<<Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE).ordinal();
			if(index==pair)
				return Straight.of(axis);
		}
		return Straight.NONE;
	}

	public static int tintOf(@Nullable DyeColor colour)
	{
		return colour==null? 0: colour.getId()+1;
	}

	@Nullable
	public static DyeColor tintColour(BlockState state)
	{
		int tint = state.getValue(TINT);
		return tint==0? null: DyeColor.byId(tint-1);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult)
	{
		if(!(stack.getItem() instanceof DyeItem dye)||!state.getValue(WINDOWED))
			return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
		if(!level.isClientSide)
		{
			DyeColor colour = tintColour(state)==dye.getDyeColor()? null: dye.getDyeColor();
			level.setBlockAndUpdate(pos, state.setValue(TINT, tintOf(colour)));
			level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1, 1);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
	{
		return new ItemStack(state.getValue(WINDOWED)? ECPneumatics.WINDOWED_TUBE_ITEM.get()
				: ECPneumatics.TUBE.get().asItem());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hitResult)
	{
		if(!player.isSecondaryUseActive())
			return InteractionResult.PASS;
		if(!level.isClientSide)
		{
			boolean windowed = !state.getValue(WINDOWED);
			level.setBlockAndUpdate(pos, state.setValue(WINDOWED, windowed));
			level.playSound(null, pos, windowed? SoundEvents.GLASS_PLACE: SoundEvents.COPPER_PLACE, SoundSource.BLOCKS,
					0.8F, 1.1F);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		VoxelShape[] shapes = state.getValue(STRAIGHT)==Straight.NONE? JUNCTIONS: SHAPES;
		return ConnectorPorts.withCaps(shapes[shapeIndex(state)], level, pos, this);
	}

	private static boolean connectsTo(Level level, BlockPos neighbor, Direction towards)
	{
		return level.getCapability(ECCapabilities.TUBE_MEMBER, neighbor, towards.getOpposite())!=null;
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
			case DOWN -> Block.box(MIN, 0, MIN, MAX, BOX_MIN, MAX);
			case UP -> Block.box(MIN, BOX_MAX, MIN, MAX, 16, MAX);
			case NORTH -> Block.box(MIN, MIN, 0, MAX, MAX, BOX_MIN);
			case SOUTH -> Block.box(MIN, MIN, BOX_MAX, MAX, MAX, 16);
			case WEST -> Block.box(0, MIN, MIN, BOX_MIN, MAX, MAX);
			case EAST -> Block.box(BOX_MAX, MIN, MIN, 16, MAX, MAX);
		};
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
	{
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if(!oldState.is(state.getBlock()))
			TubeNetwork.changed(level);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock()))
			TubeNetwork.changed(level);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}

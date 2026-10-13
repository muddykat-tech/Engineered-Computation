package com.muddykat.engineeredcomputation.pneumatic.autotube;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.registration.ECCapabilities;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;

public class AutotubeBlock extends Block implements EntityBlock
{
	public static final MapCodec<AutotubeBlock> CODEC = simpleCodec(AutotubeBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final Map<Direction, EnumProperty<Port>> PORTS = new EnumMap<>(Direction.class);
	/** Matches a dropper's delay. */
	private static final int FIRE_DELAY = 4;

	private static final VoxelShape BODY = Block.box(3, 3, 3, 13, 13, 13);
	private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

	public enum Port implements StringRepresentable
	{
		NONE, JOINED, CAPPED;

		@Override
		public String getSerializedName()
		{
			return name().toLowerCase(Locale.ROOT);
		}
	}

	static
	{
		for(Direction side : Direction.values())
			PORTS.put(side, EnumProperty.create(side.getSerializedName(), Port.class));
	}

	public AutotubeBlock(Properties properties)
	{
		super(properties);
		BlockState state = stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(POWERED, false);
		for(EnumProperty<Port> port : PORTS.values())
			state = state.setValue(port, Port.NONE);
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
		PORTS.values().forEach(builder::add);
		builder.add(FACING, POWERED);
	}

	public boolean observes()
	{
		return false;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		if(level.isClientSide||!observes()||type!=ECPneumatics.AUTOTUBE_ENTITY.get())
			return null;
		return (tickLevel, pos, tickState, autotube) -> ((AutotubeBlockEntity)autotube).watch();
	}

	public static boolean connectsOn(BlockState state, @Nullable Direction side)
	{
		return side!=state.getValue(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite())
				.setValue(POWERED, level.hasNeighborSignal(pos));
		for(Direction direction : Direction.values())
			state = state.setValue(PORTS.get(direction), port(level, state, pos, direction));
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
			LevelAccessor level, BlockPos pos, BlockPos neighborPos)
	{
		if(level instanceof Level fullLevel)
			return state.setValue(PORTS.get(direction), port(fullLevel, state, pos, direction));
		return state;
	}

	private static Port port(Level level, BlockState state, BlockPos pos, Direction side)
	{
		if(!connectsOn(state, side))
			return Port.NONE;
		BlockPos neighbor = pos.relative(side);
		boolean target = level.getCapability(ECCapabilities.TUBE_MEMBER, neighbor, side.getOpposite())!=null
				||isInventory(level, neighbor, side);
		// A cap with nothing left beside it is removed, so whatever is placed there next connects as usual.
		if(!target&&!level.isClientSide&&level.getBlockEntity(pos) instanceof AutotubeBlockEntity autotube
				&&autotube.isCut(side))
		{
			autotube.setCut(side, false);
			level.invalidateCapabilities(pos);
		}
		if(isCut(level, pos, side))
			return Port.CAPPED;
		return target? Port.JOINED: Port.NONE;
	}

	private static boolean isCut(BlockGetter level, BlockPos pos, Direction side)
	{
		return level.getBlockEntity(pos) instanceof AutotubeBlockEntity autotube&&autotube.isCut(side);
	}

	public static boolean isInventory(Level level, BlockPos neighbor, Direction side)
	{
		return level.getCapability(ECCapabilities.TUBE_MEMBER, neighbor, side.getOpposite())==null
				&&!BuiltInRegistries.BLOCK.getKey(level.getBlockState(neighbor).getBlock()).getNamespace()
						.equals(EngineeredComputation.MODID)
				&&level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, side.getOpposite())!=null;
	}

	public static boolean wrench(Level level, BlockPos pos, Vec3 local)
	{
		BlockState state = level.getBlockState(pos);
		Direction side = portAt(state, local);
		if(side==null||!(level.getBlockEntity(pos) instanceof AutotubeBlockEntity autotube))
			return false;
		if(level.isClientSide)
			return true;
		boolean cut = !autotube.isCut(side);
		autotube.setCut(side, cut);
		level.invalidateCapabilities(pos);
		TubeNetwork.changed(level);
		level.setBlockAndUpdate(pos, state.setValue(PORTS.get(side), port(level, state, pos, side)));
		level.playSound(null, pos, cut? SoundEvents.COPPER_TRAPDOOR_CLOSE: SoundEvents.COPPER_TRAPDOOR_OPEN,
				SoundSource.BLOCKS, 0.8F, 1.2F);
		return true;
	}

	@Nullable
	public static Direction portAt(BlockState state, Vec3 local)
	{
		if(!(state.getBlock() instanceof AutotubeBlock))
			return null;
		for(Direction side : Direction.values())
		{
			VoxelShape port = portShape(side, state.getValue(PORTS.get(side)));
			if(!port.isEmpty()&&port.bounds().inflate(0.001).contains(local))
				return side;
		}
		return null;
	}

	public static VoxelShape portShape(Direction side, Port port)
	{
		return switch(port)
		{
			case NONE -> Shapes.empty();
			case JOINED -> arm(side);
			case CAPPED -> cap(side);
		};
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			BlockPos neighborPos, boolean movedByPiston)
	{
		boolean powered = level.hasNeighborSignal(pos);
		if(level.isClientSide||powered==state.getValue(POWERED))
			return;
		if(powered)
			level.scheduleTick(pos, this, FIRE_DELAY);
		level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		if(level.getBlockEntity(pos) instanceof AutotubeBlockEntity autotube)
			autotube.fire();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hitResult)
	{
		if(!player.isSecondaryUseActive())
			return InteractionResult.PASS;
		if(player instanceof ServerPlayer serverPlayer&&level.getBlockEntity(pos) instanceof AutotubeBlockEntity autotube)
			AutotubeMenu.open(serverPlayer, autotube);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return SHAPES.computeIfAbsent(state, key -> {
			VoxelShape shape = Shapes.or(BODY, intake(key.getValue(FACING)));
			for(Direction side : Direction.values())
				shape = Shapes.or(shape, portShape(side, key.getValue(PORTS.get(side))));
			return shape;
		});
	}

	private static VoxelShape intake(Direction facing)
	{
		return switch(facing)
		{
			case DOWN -> Shapes.or(Block.box(2, 0, 2, 14, 1, 14), Block.box(4, 1, 4, 12, 3, 12));
			case UP -> Shapes.or(Block.box(2, 15, 2, 14, 16, 14), Block.box(4, 13, 4, 12, 15, 12));
			case NORTH -> Shapes.or(Block.box(2, 2, 0, 14, 14, 1), Block.box(4, 4, 1, 12, 12, 3));
			case SOUTH -> Shapes.or(Block.box(2, 2, 15, 14, 14, 16), Block.box(4, 4, 13, 12, 12, 15));
			case WEST -> Shapes.or(Block.box(0, 2, 2, 1, 14, 14), Block.box(1, 4, 4, 3, 12, 12));
			case EAST -> Shapes.or(Block.box(15, 2, 2, 16, 14, 14), Block.box(13, 4, 4, 15, 12, 12));
		};
	}

	private static VoxelShape cap(Direction side)
	{
		return switch(side)
		{
			case DOWN -> Block.box(4.5, 2, 4.5, 11.5, 3, 11.5);
			case UP -> Block.box(4.5, 13, 4.5, 11.5, 14, 11.5);
			case NORTH -> Block.box(4.5, 4.5, 2, 11.5, 11.5, 3);
			case SOUTH -> Block.box(4.5, 4.5, 13, 11.5, 11.5, 14);
			case WEST -> Block.box(2, 4.5, 4.5, 3, 11.5, 11.5);
			case EAST -> Block.box(13, 4.5, 4.5, 14, 11.5, 11.5);
		};
	}

	private static VoxelShape arm(Direction side)
	{
		return switch(side)
		{
			case DOWN -> Block.box(5, 0, 5, 11, 3, 11);
			case UP -> Block.box(5, 13, 5, 11, 16, 11);
			case NORTH -> Block.box(5, 5, 0, 11, 11, 3);
			case SOUTH -> Block.box(5, 5, 13, 11, 11, 16);
			case WEST -> Block.box(0, 5, 5, 3, 11, 11);
			case EAST -> Block.box(13, 5, 5, 16, 11, 11);
		};
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new AutotubeBlockEntity(pos, state);
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

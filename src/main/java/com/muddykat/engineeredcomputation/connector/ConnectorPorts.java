package com.muddykat.engineeredcomputation.connector;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeBlock;

public final class ConnectorPorts
{
	private ConnectorPorts()
	{
	}

	public interface Connector
	{
		boolean isJoined(BlockState state, Direction side);

		VoxelShape armShape(BlockState state, Direction side);

		VoxelShape capShape(Direction side);

		BlockState rejoin(Level level, BlockState state, BlockPos pos, Direction side);

		void networkChanged(Level level);

		default boolean cuttable(BlockState state)
		{
			return true;
		}
	}

	@Nullable
	public static Direction portAt(BlockGetter level, BlockPos pos, BlockState state, Vec3 local)
	{
		if(state.getBlock() instanceof AutotubeBlock)
			return AutotubeBlock.portAt(state, local);
		if(!(state.getBlock() instanceof Connector connector)||!connector.cuttable(state))
			return null;
		for(Direction side : Direction.values())
		{
			VoxelShape port = portShape(level, pos, state, side);
			if(!port.isEmpty()&&port.bounds().inflate(0.001).contains(local))
				return side;
		}
		return null;
	}

	public static VoxelShape portShape(BlockGetter level, BlockPos pos, BlockState state, Direction side)
	{
		if(state.getBlock() instanceof AutotubeBlock)
			return AutotubeBlock.portShape(side, state.getValue(AutotubeBlock.PORTS.get(side)));
		if(!(state.getBlock() instanceof Connector connector))
			return Shapes.empty();
		if(ConnectorBlockEntity.isCut(level, pos, side))
			return connector.capShape(side);
		return connector.isJoined(state, side)? connector.armShape(state, side): Shapes.empty();
	}

	public static VoxelShape withCaps(VoxelShape shape, BlockGetter level, BlockPos pos, Connector connector)
	{
		if(!(level.getBlockEntity(pos) instanceof ConnectorBlockEntity entity)||entity.getCut()==0)
			return shape;
		for(Direction side : Direction.values())
			if(entity.isCut(side))
				shape = Shapes.or(shape, connector.capShape(side));
		return shape;
	}

	/** Removes a cap once its neighbour is gone, so whatever is placed there next joins as usual. */
	public static void forgetCut(Level level, BlockPos pos, Direction side, boolean target)
	{
		if(!target&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ConnectorBlockEntity entity
				&&entity.isCut(side))
		{
			entity.setCut(side, false);
			level.invalidateCapabilities(pos);
		}
	}

	public static boolean wrench(Level level, BlockPos pos, Vec3 local)
	{
		BlockState state = level.getBlockState(pos);
		if(state.getBlock() instanceof AutotubeBlock)
			return AutotubeBlock.wrench(level, pos, local);
		Direction side = portAt(level, pos, state, local);
		if(side==null||!(state.getBlock() instanceof Connector connector)
				||!(level.getBlockEntity(pos) instanceof ConnectorBlockEntity entity))
			return false;
		if(level.isClientSide)
			return true;
		boolean cut = !entity.isCut(side);
		entity.setCut(side, cut);
		level.invalidateCapabilities(pos);
		connector.networkChanged(level);
		BlockState rejoined = connector.rejoin(level, state, pos, side);
		if(rejoined!=state)
			level.setBlockAndUpdate(pos, rejoined);
		BlockPos neighbour = pos.relative(side);
		BlockState neighbourState = level.getBlockState(neighbour);
		BlockState updated = neighbourState.updateShape(side.getOpposite(), level.getBlockState(pos), level, neighbour,
				pos);
		if(updated!=neighbourState)
			level.setBlockAndUpdate(neighbour, updated);
		level.playSound(null, pos, cut? SoundEvents.COPPER_TRAPDOOR_CLOSE: SoundEvents.COPPER_TRAPDOOR_OPEN,
				SoundSource.BLOCKS, 0.8F, 1.2F);
		return true;
	}
}

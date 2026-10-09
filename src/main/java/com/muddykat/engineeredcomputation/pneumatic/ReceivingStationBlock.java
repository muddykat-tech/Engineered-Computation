package com.muddykat.engineeredcomputation.pneumatic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import com.muddykat.engineeredcomputation.registration.ECPneumatics;

public class ReceivingStationBlock extends HorizontalDirectionalBlock implements EntityBlock, TubeMemberBlock
{
	public ReceivingStationBlock(Properties properties)
	{
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	@Nullable
	public TubeMember getTubeMember(Level level, BlockPos pos, BlockState state, @Nullable Direction side)
	{
		return TubeMember.END;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult)
	{
		if(hand!=InteractionHand.MAIN_HAND)
			return InteractionResult.PASS;
		if(player instanceof ServerPlayer serverPlayer&&level.getBlockEntity(pos) instanceof ReceivingStationBlockEntity receiver)
			ReceivingStationMenu.open(serverPlayer, receiver);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new ReceivingStationBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		if(level.isClientSide||type!=ECPneumatics.RECEIVING_STATION_ENTITY.get())
			return null;
		return (tickLevel, pos, tickState, receiver) -> ((ReceivingStationBlockEntity)receiver).tickServer();
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
	{
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if(!oldState.is(state.getBlock()))
			TubeNetwork.changed(level);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock()))
		{
			TubeNetwork.changed(level);
			if(level.getBlockEntity(pos) instanceof ReceivingStationBlockEntity receiver)
				Containers.dropContents(level, pos, receiver.getTray());
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}

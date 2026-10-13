package com.muddykat.engineeredcomputation.pneumatic.pressurizer;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;

public class PressurizerBlock extends HorizontalDirectionalBlock implements EntityBlock
{
	public static final MapCodec<PressurizerBlock> CODEC = simpleCodec(PressurizerBlock::new);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public PressurizerBlock(Properties properties)
	{
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec()
	{
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, LIT);
	}

	public static boolean connectsOn(BlockState state, @Nullable Direction side)
	{
		return side!=state.getValue(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hitResult)
	{
		if(player instanceof ServerPlayer serverPlayer&&level.getBlockEntity(pos) instanceof PressurizerBlockEntity pressurizer)
			PressurizerMenu.open(serverPlayer, pressurizer);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new PressurizerBlockEntity(pos, state);
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
		{
			if(level.getBlockEntity(pos) instanceof PressurizerBlockEntity pressurizer)
				Containers.dropContents(level, pos, pressurizer.fuelContainer());
			TubeNetwork.changed(level);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}

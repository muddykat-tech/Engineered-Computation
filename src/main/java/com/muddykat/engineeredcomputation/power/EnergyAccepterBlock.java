package com.muddykat.engineeredcomputation.power;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECPower;

/** Takes RF on any side and joins network cables on any side, paying its network's machines while power is enabled. */
public class EnergyAccepterBlock extends BaseEntityBlock
{
	public static final MapCodec<EnergyAccepterBlock> CODEC = simpleCodec(EnergyAccepterBlock::new);

	public EnergyAccepterBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec()
	{
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state)
	{
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new EnergyAccepterBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type)
	{
		return level.isClientSide? null: createTickerHelper(type, ECPower.BLOCK_ENTITY.get(),
				(tickLevel, pos, tickState, accepter) -> accepter.tickServer());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hitResult)
	{
		if(!level.isClientSide&&level.getBlockEntity(pos) instanceof EnergyAccepterBlockEntity accepter)
			player.openMenu(accepter, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
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

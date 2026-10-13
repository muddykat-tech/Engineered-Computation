package com.muddykat.engineeredcomputation.pneumatic.station;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import com.muddykat.engineeredcomputation.catalogue.CatalogueCardItem;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;

public class TubeStationBlock extends DirectionalBlock implements EntityBlock
{
	public static final MapCodec<TubeStationBlock> CODEC = simpleCodec(TubeStationBlock::new);

	public TubeStationBlock(Properties properties)
	{
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public boolean filters()
	{
		return false;
	}

	public static boolean connectsOn(BlockState state, @Nullable Direction side)
	{
		return side!=state.getValue(FACING);
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec()
	{
		return CODEC;
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
		return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult)
	{
		return stack.getItem() instanceof CatalogueCardItem? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
				: super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hitResult)
	{
		if(player instanceof ServerPlayer serverPlayer&&level.getBlockEntity(pos) instanceof TubeStationBlockEntity station)
			TubeStationMenu.open(serverPlayer, station);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new TubeStationBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		if(level.isClientSide||type!=ECPneumatics.TUBE_STATION_ENTITY.get())
			return null;
		return (tickLevel, pos, tickState, station) -> ((TubeStationBlockEntity)station).tickServer();
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
			if(level.getBlockEntity(pos) instanceof TubeStationBlockEntity station)
				Containers.dropContents(level, pos, station.filterContainer());
			TubeNetwork.changed(level);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}

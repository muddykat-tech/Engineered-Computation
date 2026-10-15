package com.muddykat.engineeredcomputation.remote;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.muddykat.engineeredcomputation.registration.ECRemote;

public class StorageAnalyzerBlock extends RemoteLinkBlock
{
	public static final MapCodec<StorageAnalyzerBlock> CODEC = simpleCodec(StorageAnalyzerBlock::new);

	public StorageAnalyzerBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec()
	{
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
	{
		if(player instanceof ServerPlayer serverPlayer&&level.getBlockEntity(pos) instanceof StorageAnalyzerBlockEntity analyzer)
			StorageAnalyzerMenu.open(serverPlayer, analyzer);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new StorageAnalyzerBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		if(level.isClientSide||type!=ECRemote.ANALYZER_ENTITY.get())
			return null;
		return (tickLevel, pos, tickState, analyzer) -> ((StorageAnalyzerBlockEntity)analyzer).tickServer();
	}
}

package com.muddykat.engineeredcomputation.terminal.teletype;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.terminal.EraTerminalBlock;

public class TeletypeBlock extends EraTerminalBlock implements EntityBlock
{
	public static final MapCodec<TeletypeBlock> CODEC = simpleCodec(TeletypeBlock::new);

	public TeletypeBlock(Properties properties)
	{
		super(ComputerEra.PUNCHCARD, properties);
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec()
	{
		return CODEC;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock())&&level.getBlockEntity(pos) instanceof TeletypeBlockEntity teletype)
			Containers.dropContents(level, pos, teletype.cardContainer());
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new TeletypeBlockEntity(pos, state);
	}
}

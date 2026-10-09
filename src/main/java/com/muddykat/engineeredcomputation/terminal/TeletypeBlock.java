package com.muddykat.engineeredcomputation.terminal;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.era.ComputerEra;

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

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new TeletypeBlockEntity(pos, state);
	}
}

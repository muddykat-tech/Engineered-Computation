package com.muddykat.engineeredcomputation.terminal;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.era.ComputerEra;

public class CraftingTerminalBlock extends EraTerminalBlock implements EntityBlock
{
	public static final MapCodec<CraftingTerminalBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ComputerEra.CODEC.fieldOf("era").forGetter(EraTerminalBlock::getEra),
			propertiesCodec()
	).apply(instance, CraftingTerminalBlock::new));

	public CraftingTerminalBlock(ComputerEra era, Properties properties)
	{
		super(era, properties);
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
		return new CraftingTerminalBlockEntity(pos, state);
	}

	@Nullable
	@Override
	protected CraftingGrid craftingGrid(Level level, BlockPos pos)
	{
		return level.getBlockEntity(pos) instanceof CraftingTerminalBlockEntity terminal? terminal.getCraftingGrid(): null;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock())&&level.getBlockEntity(pos) instanceof CraftingTerminalBlockEntity terminal)
			terminal.getCraftingGrid().dropContents(level, pos);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}

package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.registration.ECTerminals;

public class CraftingTerminalBlockEntity extends BlockEntity
{
	private final CraftingGrid craftingGrid = new CraftingGrid(this::setChanged);

	public CraftingTerminalBlockEntity(BlockPos pos, BlockState state)
	{
		this(ECTerminals.CRAFTING_TERMINAL.get(), pos, state);
	}

	protected CraftingTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public CraftingGrid getCraftingGrid()
	{
		return craftingGrid;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.put("crafting_grid", craftingGrid.save(registries));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		if(tag.contains("crafting_grid"))
			craftingGrid.load(tag.getCompound("crafting_grid"), registries);
	}
}

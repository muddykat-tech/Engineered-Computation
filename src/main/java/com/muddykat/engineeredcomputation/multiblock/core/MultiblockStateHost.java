package com.muddykat.engineeredcomputation.multiblock.core;

import org.jetbrains.annotations.Nullable;

public interface MultiblockStateHost
{
	void setChanged();

	void syncToClient();

	@Nullable
	default MultiblockContext context()
	{
		return null;
	}
}

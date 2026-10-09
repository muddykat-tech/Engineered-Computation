package com.muddykat.engineeredcomputation.storage;

import com.muddykat.engineeredcomputation.era.ComputerEra;

public enum StorageRole
{
	VARIETY,
	BALANCED,
	BULK;

	public static StorageRole of(ComputerEra era)
	{
		return switch(era)
		{
			case TAPE -> BULK;
			case DISK -> VARIETY;
			default -> BALANCED;
		};
	}
}

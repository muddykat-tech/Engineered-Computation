package com.muddykat.engineeredcomputation.storage;

import java.util.Locale;

public final class ByteSize
{
	private static final String[] UNITS = {"KB", "MB", "GB", "TB", "PB", "EB"};

	private ByteSize()
	{
	}

	public static String format(long bytes)
	{
		if(bytes<1024)
			return bytes+" B";
		double value = bytes;
		int unit = -1;
		while(value>=1024&&unit<UNITS.length-1)
		{
			value /= 1024;
			unit++;
		}
		return value<10
				? String.format(Locale.ROOT, "%.1f %s", Math.floor(value*10)/10, UNITS[unit])
				: (long)value+" "+UNITS[unit];
	}
}

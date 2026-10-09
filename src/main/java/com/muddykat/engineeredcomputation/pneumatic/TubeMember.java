package com.muddykat.engineeredcomputation.pneumatic;

public interface TubeMember
{
	TubeMember TUBE = () -> true;
	TubeMember END = () -> false;

	boolean carries();
}

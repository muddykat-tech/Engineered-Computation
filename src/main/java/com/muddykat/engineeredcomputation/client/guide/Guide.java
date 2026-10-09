package com.muddykat.engineeredcomputation.client.guide;

import java.util.List;
import java.util.Map;

import net.minecraft.world.item.Item;

import com.muddykat.engineeredcomputation.era.ComputerEra;

record Guide(ComputerEra era, List<GuidePage> pages, Map<Item, Integer> recipePages)
{
}

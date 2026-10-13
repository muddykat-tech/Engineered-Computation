package com.muddykat.engineeredcomputation.program;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

public interface ProgramEditor
{
	void edit(int index, ProgramEditPayload.Action action, String text, ItemStack item);

	@Nullable
	static ProgramEntry entry(ProgramEditPayload.Action action, String text, ItemStack item)
	{
		return switch(action)
		{
			case SET_ITEM -> ProgramEntry.of(item);
			case SET_TAG ->
			{
				ResourceLocation id = ResourceLocation.tryParse(text.trim().replaceFirst("^#", ""));
				yield id==null? null: ProgramEntry.of(TagKey.create(Registries.ITEM, id));
			}
			case SET_NAME -> ProgramEntry.ofName(text);
			case CLEAR -> ProgramEntry.EMPTY;
		};
	}
}

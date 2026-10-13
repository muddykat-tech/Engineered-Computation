package com.muddykat.engineeredcomputation.client.model.split;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public final class SplitModelLoader implements IGeometryLoader<UnbakedSplitModel>
{
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "split");
	public static final SplitModelLoader INSTANCE = new SplitModelLoader();

	private SplitModelLoader()
	{
	}

	@Override
	public UnbakedSplitModel read(JsonObject json, JsonDeserializationContext context) throws JsonParseException
	{
		if(!json.has("inner_model")||!json.has("split_parts"))
			throw new JsonParseException("Split model needs both 'inner_model' and 'split_parts'");
		BlockModel inner = context.deserialize(json.get("inner_model"), BlockModel.class);

		Set<Vec3i> parts = new HashSet<>();
		for(JsonElement element : json.getAsJsonArray("split_parts"))
		{
			JsonArray part = element.getAsJsonArray();
			parts.add(new Vec3i(part.get(0).getAsInt(), part.get(1).getAsInt(), part.get(2).getAsInt()));
		}
		if(parts.isEmpty())
			throw new JsonParseException("Split model has no 'split_parts'");
		return new UnbakedSplitModel(inner, parts);
	}
}

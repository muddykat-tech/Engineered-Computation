package com.muddykat.engineeredcomputation.client.model;

import java.util.Set;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

public final class UnbakedSplitModel implements IUnbakedGeometry<UnbakedSplitModel>
{
	private final BlockModel inner;
	private final Set<Vec3i> parts;

	UnbakedSplitModel(BlockModel inner, Set<Vec3i> parts)
	{
		this.inner = inner;
		this.parts = Set.copyOf(parts);
	}

	@Override
	public BakedModel bake(
			IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
			ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation)
	{
		BakedModel whole = inner.bake(baker, spriteGetter, BlockModelRotation.X0_Y0, modelLocation);
		return new BakedSplitModel(whole, parts, modelState.getRotation(), context.getTransforms());
	}

	@Override
	public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context)
	{
		inner.resolveParents(modelGetter);
	}
}

package com.muddykat.engineeredcomputation.client.model;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.geometry.IModelGeometry;

public final class UnbakedSplitModel implements IModelGeometry<UnbakedSplitModel>
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
			IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter,
			ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation)
	{
		BakedModel whole = inner.bake(bakery, spriteGetter, BlockModelRotation.X0_Y0, modelLocation);
		return new BakedSplitModel(whole, parts, modelState.getRotation(), owner.getCameraTransforms());
	}

	@Override
	public Collection<Material> getTextures(IModelConfiguration owner,
			Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
	{
		return inner.getMaterials(modelGetter, missingTextureErrors);
	}
}

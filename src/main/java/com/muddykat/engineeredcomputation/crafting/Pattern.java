package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import com.muddykat.engineeredcomputation.storage.ItemKey;

public record Pattern(Mode mode, List<ItemStack> inputs, List<ItemStack> outputs, Map<Integer, TagKey<Item>> inputTags,
		Map<Integer, TagKey<Item>> outputTags)
{
	public static final int INPUTS = 9;
	public static final int OUTPUTS = 3;
	public static final int TAG_WARNING_SIZE = 8;
	public static final Pattern EMPTY = new Pattern(Mode.DIRECT, List.of(), List.of());

	private record TagSlot(boolean output, int index, TagKey<Item> tag)
	{
		static final Codec<TagSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.BOOL.fieldOf("output").forGetter(TagSlot::output),
				Codec.INT.fieldOf("index").forGetter(TagSlot::index),
				TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(TagSlot::tag)
		).apply(instance, TagSlot::new));
		static final StreamCodec<RegistryFriendlyByteBuf, TagSlot> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, TagSlot::output,
				ByteBufCodecs.VAR_INT, TagSlot::index,
				ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id), TagKey::location),
				TagSlot::tag,
				TagSlot::new);
	}

	public static final Codec<Pattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Mode.CODEC.fieldOf("mode").forGetter(Pattern::mode),
			ItemStack.OPTIONAL_CODEC.listOf().fieldOf("inputs").forGetter(Pattern::inputs),
			ItemStack.OPTIONAL_CODEC.listOf().fieldOf("outputs").forGetter(Pattern::outputs),
			TagSlot.CODEC.listOf().optionalFieldOf("tags", List.of()).forGetter(Pattern::tagSlots)
	).apply(instance, Pattern::of));
	public static final StreamCodec<RegistryFriendlyByteBuf, Pattern> STREAM_CODEC = StreamCodec.composite(
			NeoForgeStreamCodecs.enumCodec(Mode.class), Pattern::mode,
			ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(INPUTS)), Pattern::inputs,
			ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(OUTPUTS)), Pattern::outputs,
			TagSlot.STREAM_CODEC.apply(ByteBufCodecs.list(INPUTS+OUTPUTS)), Pattern::tagSlots,
			Pattern::of);

	public enum Mode implements StringRepresentable
	{
		DIRECT("direct"),
		ABSTRACT("abstract");

		public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

		private final String name;

		Mode(String name)
		{
			this.name = name;
		}

		@Override
		public String getSerializedName()
		{
			return name;
		}
	}

	public Pattern
	{
		inputs = padded(inputs, INPUTS);
		outputs = padded(outputs, OUTPUTS);
		inputTags = tagsOn(mode, inputTags, inputs);
		outputTags = tagsOn(mode, outputTags, outputs);
	}

	public Pattern(Mode mode, List<ItemStack> inputs, List<ItemStack> outputs)
	{
		this(mode, inputs, outputs, Map.of(), Map.of());
	}

	private static Pattern of(Mode mode, List<ItemStack> inputs, List<ItemStack> outputs, List<TagSlot> tags)
	{
		Map<Integer, TagKey<Item>> inputTags = new HashMap<>();
		Map<Integer, TagKey<Item>> outputTags = new HashMap<>();
		for(TagSlot slot : tags)
			(slot.output()? outputTags: inputTags).put(slot.index(), slot.tag());
		return new Pattern(mode, inputs, outputs, inputTags, outputTags);
	}

	private List<TagSlot> tagSlots()
	{
		List<TagSlot> slots = new ArrayList<>();
		inputTags.forEach((index, tag) -> slots.add(new TagSlot(false, index, tag)));
		outputTags.forEach((index, tag) -> slots.add(new TagSlot(true, index, tag)));
		return slots;
	}

	private static Map<Integer, TagKey<Item>> tagsOn(Mode mode, Map<Integer, TagKey<Item>> tags, List<ItemStack> slots)
	{
		if(mode!=Mode.ABSTRACT||tags.isEmpty())
			return Map.of();
		Map<Integer, TagKey<Item>> kept = new TreeMap<>();
		tags.forEach((index, tag) -> {
			if(index>=0&&index<slots.size()&&!slots.get(index).isEmpty())
				kept.put(index, tag);
		});
		return Map.copyOf(kept);
	}

	private static List<ItemStack> padded(List<ItemStack> stacks, int size)
	{
		List<ItemStack> padded = new ArrayList<>(size);
		for(int i = 0; i<size; i++)
			padded.add(i<stacks.size()? stacks.get(i).copy(): ItemStack.EMPTY);
		return List.copyOf(padded);
	}

	public ItemStack input(int index)
	{
		return inputs.get(index);
	}

	public ItemStack output(int index)
	{
		return outputs.get(index);
	}

	public Optional<TagKey<Item>> inputTag(int index)
	{
		return Optional.ofNullable(inputTags.get(index));
	}

	public Optional<TagKey<Item>> outputTag(int index)
	{
		return Optional.ofNullable(outputTags.get(index));
	}

	public boolean hasTags()
	{
		return !inputTags.isEmpty()||!outputTags.isEmpty();
	}

	public Pattern withMode(Mode mode)
	{
		return mode==this.mode? this: new Pattern(mode, List.of(), List.of());
	}

	public Pattern withInput(int index, ItemStack stack)
	{
		return withSlot(false, index, stack, keepsTag(inputs.get(index), stack)? inputTags.get(index): null);
	}

	public Pattern withOutput(int index, ItemStack stack)
	{
		return withSlot(true, index, stack, keepsTag(outputs.get(index), stack)? outputTags.get(index): null);
	}

	public Pattern withTag(boolean output, int index, @Nullable TagKey<Item> tag)
	{
		if(tag==null)
			return withSlot(output, index, ItemStack.EMPTY, null);
		ItemStack current = (output? outputs: inputs).get(index);
		Item first = firstOf(tag);
		if(first==null)
			return this;
		return withSlot(output, index, new ItemStack(first, current.isEmpty()? 1: current.getCount()), tag);
	}

	private static boolean keepsTag(ItemStack current, ItemStack next)
	{
		return !next.isEmpty()&&ItemStack.isSameItemSameComponents(current, next);
	}

	private Pattern withSlot(boolean output, int index, ItemStack stack, @Nullable TagKey<Item> tag)
	{
		List<ItemStack> changed = new ArrayList<>(output? outputs: inputs);
		changed.set(index, stack);
		Map<Integer, TagKey<Item>> tags = new HashMap<>(output? outputTags: inputTags);
		if(tag==null)
			tags.remove(index);
		else
			tags.put(index, tag);
		return output? new Pattern(mode, inputs, changed, inputTags, tags):
				new Pattern(mode, changed, outputs, tags, outputTags);
	}

	public Optional<Pattern> resolve(Map<Integer, Item> choices)
	{
		List<ItemStack> resolved = new ArrayList<>(inputs);
		for(Map.Entry<Integer, TagKey<Item>> entry : inputTags.entrySet())
		{
			Item chosen = choices.get(entry.getKey());
			if(chosen==null||!chosen.builtInRegistryHolder().is(entry.getValue()))
				return Optional.empty();
			resolved.set(entry.getKey(), new ItemStack(chosen, inputs.get(entry.getKey()).getCount()));
		}
		return Optional.of(new Pattern(mode, resolved, outputs, Map.of(), outputTags));
	}

	public boolean outputMatches(int index, ItemKey key)
	{
		TagKey<Item> tag = outputTags.get(index);
		return tag!=null? key.getItem().builtInRegistryHolder().is(tag): ItemKey.of(outputs.get(index)).equals(key);
	}

	public static List<Item> itemsOf(TagKey<Item> tag)
	{
		return BuiltInRegistries.ITEM.getTag(tag).map(set -> set.stream().map(Holder::value).toList()).orElse(List.of());
	}

	@Nullable
	private static Item firstOf(TagKey<Item> tag)
	{
		List<Item> items = itemsOf(tag);
		return items.isEmpty()? null: items.get(0);
	}

	public boolean warnsAt(int index)
	{
		TagKey<Item> tag = inputTags.get(index);
		return tag!=null&&itemsOf(tag).size()>TAG_WARNING_SIZE;
	}

	public Pattern withCraftedOutput(Level level)
	{
		ItemStack result = recipe(level)
				.map(recipe -> recipe.value().assemble(craftingInput(), level.registryAccess()))
				.orElse(ItemStack.EMPTY);
		return new Pattern(mode, inputs, List.of(result));
	}

	public Optional<RecipeHolder<CraftingRecipe>> recipe(Level level)
	{
		return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInput(), level);
	}

	public CraftingInput craftingInput()
	{
		List<ItemStack> grid = new ArrayList<>(INPUTS);
		for(ItemStack stack : inputs)
			grid.add(stack.isEmpty()? ItemStack.EMPTY: stack.copyWithCount(1));
		return CraftingInput.of(3, 3, grid);
	}

	public boolean isValid()
	{
		return outputs.stream().anyMatch(stack -> !stack.isEmpty())&&inputs.stream().anyMatch(stack -> !stack.isEmpty());
	}

	public boolean isEmpty()
	{
		return inputs.stream().allMatch(ItemStack::isEmpty)&&outputs.stream().allMatch(ItemStack::isEmpty);
	}

	public Map<ItemKey, Long> inputTotals()
	{
		return totals(inputs);
	}

	public Map<ItemKey, Long> outputTotals()
	{
		return totals(outputs);
	}

	public ItemStack primaryOutput()
	{
		int index = primaryIndex();
		return index<0? ItemStack.EMPTY: outputs.get(index);
	}

	public int primaryIndex()
	{
		for(int index = 0; index<outputs.size(); index++)
			if(!outputs.get(index).isEmpty())
				return index;
		return -1;
	}

	private static Map<ItemKey, Long> totals(List<ItemStack> stacks)
	{
		Map<ItemKey, Long> totals = new LinkedHashMap<>();
		for(ItemStack stack : stacks)
			if(!stack.isEmpty())
				totals.merge(ItemKey.of(stack), (long)stack.getCount(), Long::sum);
		return totals;
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof Pattern pattern&&mode==pattern.mode&&ItemStack.listMatches(inputs, pattern.inputs)
				&&ItemStack.listMatches(outputs, pattern.outputs)&&inputTags.equals(pattern.inputTags)
				&&outputTags.equals(pattern.outputTags);
	}

	@Override
	public int hashCode()
	{
		return 31*(31*(31*mode.hashCode()+ItemStack.hashStackList(inputs))+ItemStack.hashStackList(outputs))
				+inputTags.hashCode()+outputTags.hashCode();
	}
}

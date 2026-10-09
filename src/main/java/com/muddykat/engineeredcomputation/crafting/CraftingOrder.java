package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

public final class CraftingOrder
{
	private final int id;
	private final ItemKey target;
	@Nullable
	private final TagKey<Item> targetTag;
	private final long amount;
	private final List<Step> steps;
	private final Map<ItemKey, Long> buffer = new LinkedHashMap<>();
	private StatusCode status = StatusCode.PROCESSING;
	private boolean cancelled;

	private CraftingOrder(int id, ItemKey target, @Nullable TagKey<Item> targetTag, long amount, List<Step> steps)
	{
		this.id = id;
		this.target = target;
		this.targetTag = targetTag;
		this.amount = amount;
		this.steps = steps;
	}

	static CraftingOrder of(int id, CraftingPlan plan)
	{
		List<Step> steps = new ArrayList<>();
		for(CraftingPlan.Step step : plan.steps())
			steps.add(new Step(step.provided().pattern(), step.provided().processor(), step.provided().choices(),
					step.runs()));
		CraftingOrder order = new CraftingOrder(id, plan.target(), plan.targetTag().orElse(null), plan.amount(), steps);
		order.buffer.putAll(plan.fromStorage());
		return order;
	}

	public int id()
	{
		return id;
	}

	public ItemKey target()
	{
		return target;
	}

	public OrderTarget shownTarget()
	{
		return new OrderTarget(target, Optional.ofNullable(targetTag));
	}

	private boolean isTarget(ItemKey key)
	{
		return key.equals(target)||targetTag!=null&&key.getItem().builtInRegistryHolder().is(targetTag);
	}

	public long amount()
	{
		return amount;
	}

	public StatusCode status()
	{
		return status;
	}

	boolean isCancelled()
	{
		return cancelled;
	}

	void cancel()
	{
		cancelled = true;
	}

	public float progress()
	{
		double done = 0, total = 0;
		for(Step step : steps)
		{
			total += step.runs;
			done += step.runsDone();
		}
		return total==0? 1: (float)(done/total);
	}

	OrderView view()
	{
		List<OrderView.Node> tree = new ArrayList<>();
		Step root = maker(target, List.of());
		if(root!=null)
			addNode(tree, root, target, 0, new ArrayList<>());
		return new OrderView(shownTarget(), amount, status, progress(), tree);
	}

	private void addNode(List<OrderView.Node> tree, Step step, ItemKey key, int depth, List<Step> above)
	{
		if(tree.size()>=OrderView.MAX_NODES)
			return;
		long made = step.runs*step.resolved.outputTotals().getOrDefault(key, 1L);
		tree.add(new OrderView.Node(depth, key, made, true, step.processor!=null, (float)(step.runsDone()/step.runs)));
		above.add(step);
		step.resolved.inputTotals().forEach((input, each) -> {
			Step maker = maker(input, above);
			if(maker!=null)
				addNode(tree, maker, input, depth+1, above);
			else if(tree.size()<OrderView.MAX_NODES)
				tree.add(new OrderView.Node(depth+1, input, each*step.runs, false, false, 1));
		});
		above.remove(above.size()-1);
	}

	@Nullable
	private Step maker(ItemKey key, List<Step> above)
	{
		for(Step step : steps)
			if(!above.contains(step)&&step.resolved.outputTotals().containsKey(key))
				return step;
		return null;
	}

	boolean tick(ServerLevel level, NetworkStorage storage, int[] budget)
	{
		status = StatusCode.PROCESSING;
		boolean complete = true;
		for(Step step : steps)
		{
			if(step.isComplete())
				continue;
			if(step.processor==null)
				while(budget[0]>0&&step.done<step.runs&&craft(step, level))
					budget[0]--;
			else
				runAbstract(step, level, storage);
			complete &= step.isComplete();
		}
		for(Map.Entry<ItemKey, Long> entry : List.copyOf(buffer.entrySet()))
			if(isTarget(entry.getKey()))
				deliver(entry.getKey(), entry.getValue(), storage);
		if(!complete)
			return false;
		for(Map.Entry<ItemKey, Long> entry : List.copyOf(buffer.entrySet()))
			deliver(entry.getKey(), entry.getValue(), storage);
		if(!buffer.isEmpty())
			status = StatusCode.INSUFFICIENT_STORAGE;
		return buffer.isEmpty();
	}

	private boolean craft(Step step, ServerLevel level)
	{
		Map<ItemKey, Long> inputs = step.resolved.inputTotals();
		if(!hasAll(inputs))
			return false;
		Optional<CraftingRecipe> recipe = step.resolved.recipe(level);
		CraftingContainer input = step.resolved.craftingInput();
		ItemStack result = recipe.map(found -> found.assemble(input, level.registryAccess()))
				.orElse(ItemStack.EMPTY);
		if(!ItemStack.isSameItemSameTags(result, step.resolved.primaryOutput())
				||result.getCount()!=step.resolved.primaryOutput().getCount())
		{
			status = StatusCode.NOT_FOUND;
			return false;
		}
		inputs.forEach(this::remove);
		add(result);
		NonNullList<ItemStack> leftovers = recipe.get().getRemainingItems(input);
		for(ItemStack leftover : leftovers)
			add(leftover);
		step.done++;
		return true;
	}

	private void runAbstract(Step step, ServerLevel level, NetworkStorage storage)
	{
		DataProcessorState processor = DataProcessorState.at(level, step.processor);
		IItemHandler target = processor!=null&&processor.runs(step.pattern)? processor.target(): null;
		if(target==null)
		{
			status = StatusCode.SERVICE_UNAVAILABLE;
			return;
		}
		processor.markProcessing();
		Map<ItemKey, Long> inputs = step.resolved.inputTotals();
		int runSize = (int)Math.min(Integer.MAX_VALUE, inputs.values().stream().mapToLong(Long::longValue).sum());
		long left = step.done<step.runs? processor.takeMove(runSize): 0;
		while(step.done<step.runs&&left>=runSize&&hasAll(inputs)&&fits(target, inputs))
		{
			inputs.forEach((key, count) -> {
				remove(key, count);
				add(key, insert(target, key, count, false));
			});
			step.done++;
			left -= runSize;
		}
		collect(step, processor, storage);
	}

	private static boolean fits(IItemHandler target, Map<ItemKey, Long> inputs)
	{
		for(Map.Entry<ItemKey, Long> input : inputs.entrySet())
			if(insert(target, input.getKey(), input.getValue(), true)>0)
				return false;
		return true;
	}

	private static long insert(IItemHandler target, ItemKey key, long count, boolean simulate)
	{
		long left = count;
		while(left>0)
		{
			int stack = (int)Math.min(left, key.getMaxStackSize());
			int rest = ItemHandlerHelper.insertItem(target, key.toStack(stack), simulate).getCount();
			left -= stack-rest;
			if(rest>0)
				break;
		}
		return left;
	}

	private void collect(Step step, DataProcessorState processor, NetworkStorage storage)
	{
		for(int index = 0; index<Pattern.OUTPUTS; index++)
		{
			long wanted = step.owed(index);
			if(wanted<=0)
				continue;
			int output = index;
			processor.take(key -> step.resolved.outputMatches(output, key), wanted).forEach((key, count) -> {
				add(key, count);
				step.received[output] += count;
				if(isTarget(key))
					deliver(key, count, storage);
			});
		}
	}

	private void deliver(ItemKey key, long count, NetworkStorage storage)
	{
		if(count<=0)
			return;
		long stored = storage.insert(key, count, false);
		if(stored>0)
			remove(key, stored);
	}

	void expectFrom(BlockPos processor, BiConsumer<Predicate<ItemKey>, Long> into)
	{
		for(Step step : steps)
			if(processor.equals(step.processor))
				for(int index = 0; index<Pattern.OUTPUTS; index++)
				{
					long owed = step.owed(index);
					int output = index;
					if(owed>0)
						into.accept(key -> step.resolved.outputMatches(output, key), owed);
				}
	}

	List<ItemStack> empty(NetworkStorage storage)
	{
		List<ItemStack> left = new ArrayList<>();
		buffer.forEach((key, count) -> {
			long rest = count-storage.insert(key, count, false);
			while(rest>0)
			{
				int stack = (int)Math.min(rest, key.getMaxStackSize());
				left.add(key.toStack(stack));
				rest -= stack;
			}
		});
		buffer.clear();
		return left;
	}

	private boolean hasAll(Map<ItemKey, Long> items)
	{
		for(Map.Entry<ItemKey, Long> entry : items.entrySet())
			if(buffer.getOrDefault(entry.getKey(), 0L)<entry.getValue())
				return false;
		return true;
	}

	private void add(ItemStack stack)
	{
		if(!stack.isEmpty())
			add(ItemKey.of(stack), stack.getCount());
	}

	private void add(ItemKey key, long count)
	{
		if(count>0)
			buffer.merge(key, count, Long::sum);
	}

	private void remove(ItemKey key, long count)
	{
		long left = buffer.getOrDefault(key, 0L)-count;
		if(left>0)
			buffer.put(key, left);
		else
			buffer.remove(key);
	}

	private static final class Step
	{
		final Pattern pattern;
		final Pattern resolved;
		@Nullable
		final BlockPos processor;
		final Map<Integer, Item> choices;
		final long runs;
		long done;
		final long[] received = new long[Pattern.OUTPUTS];

		Step(Pattern pattern, @Nullable BlockPos processor, Map<Integer, Item> choices, long runs)
		{
			this.pattern = pattern;
			this.choices = choices;
			this.resolved = pattern.inputTags().isEmpty()? pattern: pattern.resolve(choices).orElse(pattern);
			this.processor = processor;
			this.runs = runs;
		}

		long owed(int index)
		{
			ItemStack output = resolved.output(index);
			return output.isEmpty()? 0: done*output.getCount()-received[index];
		}

		boolean isComplete()
		{
			if(processor==null)
				return done>=runs;
			for(int index = 0; index<Pattern.OUTPUTS; index++)
				if(received[index]<runs*resolved.output(index).getCount())
					return false;
			return true;
		}

		double runsDone()
		{
			if(processor==null)
				return done;
			double fraction = Double.MAX_VALUE;
			for(int index = 0; index<Pattern.OUTPUTS; index++)
				if(!resolved.output(index).isEmpty())
					fraction = Math.min(fraction, received[index]/(double)resolved.output(index).getCount());
			return Math.min(runs, fraction);
		}
	}

	CompoundTag save()
	{
		CompoundTag tag = new CompoundTag();
		tag.putInt("id", id);
		tag.put("target", encode(ItemKey.CODEC, target));
		if(targetTag!=null)
			tag.putString("target_tag", targetTag.location().toString());
		tag.putLong("amount", amount);
		ListTag stepTags = new ListTag();
		for(Step step : steps)
		{
			CompoundTag stepTag = new CompoundTag();
			stepTag.put("pattern", encode(Pattern.CODEC, step.pattern));
			if(step.processor!=null)
				stepTag.put("processor", NbtUtils.writeBlockPos(step.processor));
			CompoundTag choices = new CompoundTag();
			step.choices.forEach((slot, item) -> choices.putString(slot.toString(),
					BuiltInRegistries.ITEM.getKey(item).toString()));
			stepTag.put("choices", choices);
			stepTag.putLong("runs", step.runs);
			stepTag.putLong("done", step.done);
			stepTag.putLongArray("received", step.received);
			stepTags.add(stepTag);
		}
		tag.put("steps", stepTags);
		tag.put("buffer", saveCounts(buffer));
		return tag;
	}

	static CraftingOrder load(CompoundTag tag)
	{
		List<Step> steps = new ArrayList<>();
		for(Tag entry : tag.getList("steps", Tag.TAG_COMPOUND))
		{
			CompoundTag stepTag = (CompoundTag)entry;
			Map<Integer, Item> choices = new HashMap<>();
			CompoundTag choiceTag = stepTag.getCompound("choices");
			for(String slot : choiceTag.getAllKeys())
				BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(choiceTag.getString(slot)))
						.ifPresent(item -> choices.put(Integer.parseInt(slot), item));
			Step step = new Step(decode(Pattern.CODEC, stepTag.get("pattern")),
					stepTag.contains("processor")? NbtUtils.readBlockPos(stepTag.getCompound("processor")): null,
							choices,
					stepTag.getLong("runs"));
			step.done = stepTag.getLong("done");
			long[] received = stepTag.getLongArray("received");
			System.arraycopy(received, 0, step.received, 0, Math.min(received.length, step.received.length));
			steps.add(step);
		}
		TagKey<Item> targetTag = tag.contains("target_tag")? TagKey.create(Registries.ITEM,
				ResourceLocation.parse(tag.getString("target_tag"))): null;
		CraftingOrder order = new CraftingOrder(tag.getInt("id"), decode(ItemKey.CODEC, tag.get("target")),
				targetTag, tag.getLong("amount"), steps);
		loadCounts(tag.getList("buffer", Tag.TAG_COMPOUND), order.buffer);
		return order;
	}

	private static ListTag saveCounts(Map<ItemKey, Long> counts)
	{
		ListTag list = new ListTag();
		counts.forEach((key, count) -> {
			CompoundTag entry = new CompoundTag();
			entry.put("item", encode(ItemKey.CODEC, key));
			entry.putLong("count", count);
			list.add(entry);
		});
		return list;
	}

	private static void loadCounts(ListTag list, Map<ItemKey, Long> into)
	{
		for(Tag entry : list)
		{
			CompoundTag compound = (CompoundTag)entry;
			into.put(decode(ItemKey.CODEC, compound.get("item")), compound.getLong("count"));
		}
	}

	private static <T> Tag encode(com.mojang.serialization.Codec<T> codec, T value)
	{
		return codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, error -> {});
	}

	private static <T> T decode(com.mojang.serialization.Codec<T> codec, Tag tag)
	{
		return codec.parse(NbtOps.INSTANCE, tag).getOrThrow(false, error -> {});
	}
}

package com.muddykat.engineeredcomputation.crafting.order;

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
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import com.muddykat.engineeredcomputation.crafting.CraftingPlan;
import com.muddykat.engineeredcomputation.crafting.pattern.FuzzyPatterns;
import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.crafting.processor.DataProcessorState;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.remote.StorageAnalyzerBlockEntity;
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
	private final List<Request> requests;
	private final Map<ItemKey, Long> buffer = new LinkedHashMap<>();
	private StatusCode status = StatusCode.PROCESSING;
	private boolean cancelled;
	/** Only counts while the computer is loaded. */
	private long age;

	private CraftingOrder(int id, ItemKey target, @Nullable TagKey<Item> targetTag, long amount, List<Step> steps,
			List<Request> requests)
	{
		this.id = id;
		this.target = target;
		this.targetTag = targetTag;
		this.amount = amount;
		this.steps = steps;
		this.requests = requests;
	}

	public static CraftingOrder of(int id, CraftingPlan plan)
	{
		List<Step> steps = new ArrayList<>();
		for(CraftingPlan.Step step : plan.steps())
			steps.add(new Step(step.provided().pattern(), step.provided().processor(), step.provided().choices(),
					step.runs()));
		List<Request> requests = new ArrayList<>();
		for(CraftingPlan.Request request : plan.requests())
			requests.add(new Request(request.analyzer(), request.key(), request.amount()));
		CraftingOrder order = new CraftingOrder(id, plan.target(), plan.targetTag().orElse(null), plan.amount(), steps,
				requests);
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

	/** Only what the order makes or fetches: an order that sends items away has its first item as target. */
	private boolean isTarget(ItemKey key)
	{
		// Crafted or fetched items a send needs would go straight back to storage, leaving it waiting on them forever.
		if(sends())
			return false;
		boolean matches = key.equals(target)||targetTag!=null&&key.getItem().builtInRegistryHolder().is(targetTag);
		return matches&&(steps.stream().anyMatch(step -> step.resolved.outputTotals().containsKey(key))
				||requests.stream().anyMatch(request -> request.key.equals(key))
				||targetTag!=null);
	}

	private boolean sends()
	{
		return steps.stream().anyMatch(step -> step.resolved.sendsOnly());
	}

	public long amount()
	{
		return amount;
	}

	public StatusCode status()
	{
		return status;
	}

	public boolean isCancelled()
	{
		return cancelled;
	}

	public void cancel()
	{
		cancelled = true;
	}

	public long age()
	{
		return age;
	}

	public void grow()
	{
		age++;
	}

	public float progress()
	{
		double done = 0, total = 0;
		for(Step step : steps)
		{
			total += step.runs;
			done += step.runsDone();
		}
		for(Request request : requests)
		{
			total++;
			done += Math.min(1, request.received/(double)request.amount);
		}
		return total==0? 1: (float)(done/total);
	}

	public OrderView view(long gameTime)
	{
		List<OrderView.Node> tree = new ArrayList<>();
		Step root = sends()? null: maker(target, List.of());
		if(root!=null)
			addNode(tree, root, target, 0, new ArrayList<>());
		for(Step step : steps)
			if(step.resolved.sendsOnly())
				addNode(tree, step, ItemKey.of(step.resolved.input(step.resolved.firstInput())), 0, new ArrayList<>());
		for(Request request : requests)
			if(tree.size()<OrderView.MAX_NODES)
				tree.add(new OrderView.Node(tree.isEmpty()? 0: 1, request.key, request.amount, true, true,
						(float)Math.min(1, request.received/(double)request.amount)));
		return new OrderView(id, shownTarget(), amount, status, progress(), gameTime-age, tree);
	}

	private void addNode(List<OrderView.Node> tree, Step step, ItemKey key, int depth, List<Step> above)
	{
		if(tree.size()>=OrderView.MAX_NODES)
			return;
		long made = step.runs*(step.resolved.sendsOnly()? step.resolved.inputTotals(): step.resolved.outputTotals())
				.getOrDefault(key, 1L);
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

	public boolean tick(ServerLevel level, NetworkStorage storage, int[] budget)
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
		for(Request request : requests)
		{
			if(request.isComplete())
				continue;
			runRequest(request, level);
			complete &= request.isComplete();
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
		Pattern grid = fill(step.resolved, FuzzyPatterns.accepted(level, step.resolved));
		if(grid==null)
			return false;
		Map<ItemKey, Long> inputs = grid.inputTotals();
		Optional<RecipeHolder<CraftingRecipe>> recipe = grid.recipe(level);
		CraftingInput input = grid.craftingInput();
		ItemStack result = recipe.map(holder -> holder.value().assemble(input, level.registryAccess()))
				.orElse(ItemStack.EMPTY);
		if(!ItemStack.isSameItemSameComponents(result, step.resolved.primaryOutput())
				||result.getCount()!=step.resolved.primaryOutput().getCount())
		{
			status = StatusCode.NOT_FOUND;
			return false;
		}
		inputs.forEach(this::remove);
		add(result);
		NonNullList<ItemStack> leftovers = recipe.get().value().getRemainingItems(input);
		for(ItemStack leftover : leftovers)
			add(leftover);
		step.done++;
		return true;
	}

	/**
	 * The grid to craft from what the order holds, or null if it's short: each slot takes its own item, or for a fuzzy
	 * pattern, anything held that the recipe accepts in its place.
	 */
	@Nullable
	private Pattern fill(Pattern pattern, Map<ItemKey, List<ItemKey>> accepted)
	{
		if(accepted.isEmpty())
			return hasAll(pattern.inputTotals())? pattern: null;
		Map<ItemKey, Long> using = new HashMap<>();
		List<ItemStack> grid = new ArrayList<>();
		for(ItemStack stack : pattern.inputs())
		{
			if(stack.isEmpty())
			{
				grid.add(ItemStack.EMPTY);
				continue;
			}
			ItemKey wanted = ItemKey.of(stack);
			ItemKey chosen = null;
			for(ItemKey candidate : accepted.getOrDefault(wanted, List.of(wanted)))
				if(buffer.getOrDefault(candidate, 0L)-using.getOrDefault(candidate, 0L)>=stack.getCount())
				{
					chosen = candidate;
					break;
				}
			if(chosen==null)
				return null;
			using.merge(chosen, (long)stack.getCount(), Long::sum);
			grid.add(chosen.toStack(stack.getCount()));
		}
		return new Pattern(Pattern.Mode.DIRECT, grid, pattern.outputs());
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

	private void runRequest(Request request, ServerLevel level)
	{
		StorageAnalyzerBlockEntity analyzer = StorageAnalyzerBlockEntity.at(level, request.analyzer);
		if(analyzer==null||!analyzer.canRequest())
		{
			status = StatusCode.SERVICE_UNAVAILABLE;
			return;
		}
		if(request.asked<request.amount&&request.cooldown--<=0)
		{
			request.cooldown = Request.ASK_TICKS;
			request.asked += analyzer.request(request.key, (int)Math.min(Integer.MAX_VALUE,
					request.amount-request.asked));
		}
		long got = analyzer.collect(request.key, request.asked-request.received);
		if(got>0)
		{
			add(request.key, got);
			request.received += got;
			request.stalled = 0;
		}
		// Tallies can be a second old, so a station may send fewer than it said it had; those are asked for again.
		else if(request.asked>request.received&&++request.stalled>=Request.STALL_TICKS)
		{
			request.asked = request.received;
			request.stalled = 0;
		}
		if(request.asked==request.received&&!request.isComplete())
			status = StatusCode.NOT_FOUND;
	}

	/** Hands what's still coming over to the analyzers, which store it when it lands. */
	public void release(Level level)
	{
		for(Request request : requests)
		{
			StorageAnalyzerBlockEntity analyzer = StorageAnalyzerBlockEntity.at(level, request.analyzer);
			if(analyzer!=null)
				analyzer.adopt(request.key, request.asked-request.received);
		}
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

	public void expectFrom(BlockPos processor, BiConsumer<Predicate<ItemKey>, Long> into)
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

	public List<ItemStack> empty(NetworkStorage storage)
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

	private static final class Request
	{
		static final int ASK_TICKS = 20;
		static final int STALL_TICKS = 600;

		final BlockPos analyzer;
		final ItemKey key;
		final long amount;
		long asked;
		long received;
		int cooldown;
		int stalled;

		Request(BlockPos analyzer, ItemKey key, long amount)
		{
			this.analyzer = analyzer;
			this.key = key;
			this.amount = amount;
		}

		boolean isComplete()
		{
			return received>=amount;
		}
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
			if(processor==null||resolved.sendsOnly())
				return done>=runs;
			for(int index = 0; index<Pattern.OUTPUTS; index++)
				if(received[index]<runs*resolved.output(index).getCount())
					return false;
			return true;
		}

		double runsDone()
		{
			if(processor==null||resolved.sendsOnly())
				return done;
			double fraction = Double.MAX_VALUE;
			for(int index = 0; index<Pattern.OUTPUTS; index++)
				if(!resolved.output(index).isEmpty())
					fraction = Math.min(fraction, received[index]/(double)resolved.output(index).getCount());
			return Math.min(runs, fraction);
		}
	}

	public CompoundTag save(HolderLookup.Provider registries)
	{
		CompoundTag tag = new CompoundTag();
		tag.putInt("id", id);
		tag.put("target", encode(registries, ItemKey.CODEC, target));
		if(targetTag!=null)
			tag.putString("target_tag", targetTag.location().toString());
		tag.putLong("amount", amount);
		tag.putLong("age", age);
		ListTag stepTags = new ListTag();
		for(Step step : steps)
		{
			CompoundTag stepTag = new CompoundTag();
			stepTag.put("pattern", encode(registries, Pattern.CODEC, step.pattern));
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
		ListTag requestTags = new ListTag();
		for(Request request : requests)
		{
			CompoundTag requestTag = new CompoundTag();
			requestTag.put("analyzer", NbtUtils.writeBlockPos(request.analyzer));
			requestTag.put("item", encode(registries, ItemKey.CODEC, request.key));
			requestTag.putLong("amount", request.amount);
			requestTag.putLong("asked", request.asked);
			requestTag.putLong("received", request.received);
			requestTags.add(requestTag);
		}
		if(!requestTags.isEmpty())
			tag.put("requests", requestTags);
		tag.put("buffer", saveCounts(registries, buffer));
		return tag;
	}

	public static CraftingOrder load(CompoundTag tag, HolderLookup.Provider registries)
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
			Step step = new Step(decode(registries, Pattern.CODEC, stepTag.get("pattern")),
					NbtUtils.readBlockPos(stepTag, "processor").orElse(null), choices, stepTag.getLong("runs"));
			step.done = stepTag.getLong("done");
			long[] received = stepTag.getLongArray("received");
			System.arraycopy(received, 0, step.received, 0, Math.min(received.length, step.received.length));
			steps.add(step);
		}
		List<Request> requests = new ArrayList<>();
		for(Tag entry : tag.getList("requests", Tag.TAG_COMPOUND))
		{
			CompoundTag requestTag = (CompoundTag)entry;
			Request request = new Request(NbtUtils.readBlockPos(requestTag, "analyzer").orElse(BlockPos.ZERO),
					decode(registries, ItemKey.CODEC, requestTag.get("item")), requestTag.getLong("amount"));
			request.asked = requestTag.getLong("asked");
			request.received = requestTag.getLong("received");
			requests.add(request);
		}
		TagKey<Item> targetTag = tag.contains("target_tag")? TagKey.create(Registries.ITEM,
				ResourceLocation.parse(tag.getString("target_tag"))): null;
		CraftingOrder order = new CraftingOrder(tag.getInt("id"), decode(registries, ItemKey.CODEC, tag.get("target")),
				targetTag, tag.getLong("amount"), steps, requests);
		loadCounts(registries, tag.getList("buffer", Tag.TAG_COMPOUND), order.buffer);
		order.age = tag.getLong("age");
		return order;
	}

	private static ListTag saveCounts(HolderLookup.Provider registries, Map<ItemKey, Long> counts)
	{
		ListTag list = new ListTag();
		counts.forEach((key, count) -> {
			CompoundTag entry = new CompoundTag();
			entry.put("item", encode(registries, ItemKey.CODEC, key));
			entry.putLong("count", count);
			list.add(entry);
		});
		return list;
	}

	private static void loadCounts(HolderLookup.Provider registries, ListTag list, Map<ItemKey, Long> into)
	{
		for(Tag entry : list)
		{
			CompoundTag compound = (CompoundTag)entry;
			into.put(decode(registries, ItemKey.CODEC, compound.get("item")), compound.getLong("count"));
		}
	}

	private static <T> Tag encode(HolderLookup.Provider registries, com.mojang.serialization.Codec<T> codec, T value)
	{
		return codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value).getOrThrow();
	}

	private static <T> T decode(HolderLookup.Provider registries, com.mojang.serialization.Codec<T> codec, Tag tag)
	{
		return codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
	}
}

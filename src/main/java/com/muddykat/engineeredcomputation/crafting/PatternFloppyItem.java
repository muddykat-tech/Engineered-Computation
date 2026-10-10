package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public class PatternFloppyItem extends Item
{
	private static final String PATTERN = "pattern";

	public PatternFloppyItem(Properties properties)
	{
		super(properties);
	}

	public static Pattern pattern(ItemStack stack)
	{
		CompoundTag tag = stack.getTag();
		return tag==null||!tag.contains(PATTERN)? Pattern.EMPTY
				: Pattern.CODEC.parse(NbtOps.INSTANCE, tag.get(PATTERN)).result().orElse(Pattern.EMPTY);
	}

	public static void setPattern(ItemStack stack, Pattern pattern)
	{
		Pattern.CODEC.encodeStart(NbtOps.INSTANCE, pattern).result()
				.ifPresent(encoded -> stack.getOrCreateTag().put(PATTERN, encoded));
	}

	public static boolean holds(ItemStack stack, Pattern.Mode mode)
	{
		if(!(stack.getItem() instanceof PatternFloppyItem))
			return false;
		Pattern pattern = pattern(stack);
		return pattern.mode()==mode&&pattern.isValid();
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Player player = context.getPlayer();
		if(player!=null&&player.isSecondaryUseActive()
				&&context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MultiblockPartBlockEntity part)
			return CraftingComputerState.insertIntoBay(part, player, context.getHand(), context.getClickLocation());
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		if(player instanceof ServerPlayer serverPlayer)
			NetworkHooks.openGui(serverPlayer, new SimpleMenuProvider(
					(id, inventory, p) -> new PatternFloppyMenu(id, inventory, hand),
					player.getItemInHand(hand).getHoverName()), buf -> buf.writeEnum(hand));
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag)
	{
		Pattern pattern = pattern(stack);
		if(pattern.isEmpty())
		{
			lines.add(new TranslatableComponent("tooltip.engineered_computation.pattern_floppy.blank")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		String mode = pattern.mode().getSerializedName();
		lines.add(new TranslatableComponent("tooltip.engineered_computation.pattern_floppy."+mode)
				.withStyle(ChatFormatting.AQUA));
		addSlots(lines, "makes", pattern, true);
		addSlots(lines, "from", pattern, false);
		if(!pattern.isValid())
			lines.add(new TranslatableComponent("tooltip.engineered_computation.pattern_floppy.invalid")
					.withStyle(ChatFormatting.RED));
		lines.add(new TranslatableComponent("tooltip.engineered_computation.pattern_floppy."+mode+".where")
				.withStyle(ChatFormatting.DARK_GRAY));
	}

	private static void addSlots(List<Component> lines, String heading, Pattern pattern, boolean outputs)
	{
		Map<ItemKey, Long> items = new LinkedHashMap<>();
		List<Component> tags = new ArrayList<>();
		for(int index = 0; index<(outputs? Pattern.OUTPUTS: Pattern.INPUTS); index++)
		{
			ItemStack stack = outputs? pattern.output(index): pattern.input(index);
			Optional<TagKey<Item>> tag = outputs? pattern.outputTag(index): pattern.inputTag(index);
			if(tag.isPresent())
				tags.add(new TextComponent("  "+stack.getCount()+" × #"+tag.get().location())
						.withStyle(ChatFormatting.DARK_AQUA));
			else if(!stack.isEmpty())
				items.merge(ItemKey.of(stack), (long)stack.getCount(), Long::sum);
		}
		if(items.isEmpty()&&tags.isEmpty())
			return;
		lines.add(new TranslatableComponent("tooltip.engineered_computation.pattern_floppy."+heading)
				.withStyle(ChatFormatting.GRAY));
		items.forEach((key, count) -> lines.add(new TextComponent("  "+count+" × ")
				.append(key.displayStack().getHoverName()).withStyle(ChatFormatting.GRAY)));
		lines.addAll(tags);
	}
}

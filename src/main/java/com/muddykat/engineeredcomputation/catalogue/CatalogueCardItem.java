package com.muddykat.engineeredcomputation.catalogue;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;

public class CatalogueCardItem extends Item
{
	private static final String CARD = "catalogue_card";

	public CatalogueCardItem(Properties properties)
	{
		super(properties);
	}

	@Nullable
	public static CatalogueCard card(ItemStack stack)
	{
		CompoundTag tag = stack.getTag();
		return !(stack.getItem() instanceof CatalogueCardItem)||tag==null||!tag.contains(CARD)? null
				: CatalogueCard.CODEC.parse(NbtOps.INSTANCE, tag.get(CARD)).result().orElse(null);
	}

	public static ItemStack punched(CatalogueCard card)
	{
		ItemStack stack = new ItemStack(ECCatalogue.CARD.get());
		CatalogueCard.CODEC.encodeStart(NbtOps.INSTANCE, card).result()
				.ifPresent(encoded -> stack.getOrCreateTag().put(CARD, encoded));
		return stack;
	}

	@Override
	public int getItemStackLimit(ItemStack stack)
	{
		return card(stack)!=null? 1: super.getItemStackLimit(stack);
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		ItemStack held = context.getItemInHand();
		if(card(held)!=null||!(level.getBlockEntity(pos) instanceof TubeStationBlockEntity station))
			return InteractionResult.PASS;
		if(level.isClientSide)
			return InteractionResult.SUCCESS;
		ItemStack punched = punched(new CatalogueCard(GlobalPos.of(level.dimension(), pos.immutable()), station.label(),
				station.tally(), level.getGameTime()));
		Player player = context.getPlayer();
		if(player==null||!player.getAbilities().instabuild)
			held.shrink(1);
		if(player==null)
			return InteractionResult.CONSUME;
		if(!player.getInventory().add(punched))
			player.drop(punched, false);
		level.playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 0.8F, 1.2F);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag)
	{
		CatalogueCard card = card(stack);
		if(card==null)
		{
			lines.add(new TranslatableComponent("tooltip.engineered_computation.catalogue_card.blank")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		lines.addAll(describe(card));
	}

	static List<Component> describe(CatalogueCard card)
	{
		BlockPos pos = card.station().pos();
		return List.of(card.label().copy().withStyle(ChatFormatting.YELLOW),
				new TranslatableComponent("tooltip.engineered_computation.catalogue_card.station", pos.getX(),
						pos.getY(), pos.getZ()).withStyle(ChatFormatting.GRAY),
				new TranslatableComponent("tooltip.engineered_computation.catalogue_card.tally", card.tally().types(),
						String.format(Locale.ROOT, "%,d", card.tally().totalCount())).withStyle(ChatFormatting.GRAY));
	}
}

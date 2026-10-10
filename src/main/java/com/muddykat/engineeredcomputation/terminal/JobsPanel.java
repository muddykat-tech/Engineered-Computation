package com.muddykat.engineeredcomputation.terminal;

import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.crafting.CancelOrderPayload;
import com.muddykat.engineeredcomputation.crafting.OrderSummary;
import com.muddykat.engineeredcomputation.network.ECPayloads;

final class JobsPanel
{
	private static final int ROW_HEIGHT = 18;
	private static final int CANCEL_SIZE = 9;
	private static final int BAR_WIDTH = 34;
	private static final String[] CANCEL_ICON = {
			"#...#",
			".#.#.",
			"..#..",
			".#.#.",
			"#...#"};

	private final TerminalMenu menu;
	private final Font font;
	private final int x;
	private final int y;
	private final int width;
	private final int height;
	private int scroll;

	JobsPanel(TerminalMenu menu, Font font, int x, int y, int width, int height)
	{
		this.menu = menu;
		this.font = font;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	private int rows()
	{
		return height/ROW_HEIGHT;
	}

	void render(PoseStack pose, int mouseX, int mouseY)
	{
		pose.pushPose();
		pose.translate(0, 0, 350);
		GuiComponent.fill(pose, x, y, x+width, y+height, 0xFF101814);
		List<OrderSummary> orders = menu.getClientOrders();
		scroll = Math.max(0, Math.min(scroll, orders.size()-rows()));
		if(orders.isEmpty())
			GuiComponent.drawCenteredString(pose, font,
					new TranslatableComponent("gui.engineered_computation.jobs.none"), x+width/2, y+height/2-4,
					0x8C9890);
		for(int i = 0; i<Math.min(rows(), orders.size()-scroll); i++)
		{
			OrderSummary order = orders.get(scroll+i);
			int rowY = y+i*ROW_HEIGHT+1;
			ItemStack stack = order.target().icon(Minecraft.getInstance().level.getGameTime());
			ECGui.renderItem(pose, stack, x+2, rowY);
			if(order.target().isTag())
				ECGui.tagBadge(pose, font, x+2, rowY);
			Component name = new TextComponent(String.format(Locale.ROOT, "%,d× ", order.amount()))
					.append(order.target().name());
			int textWidth = width-20-BAR_WIDTH-CANCEL_SIZE-12;
			font.draw(pose, ECGui.fit(font, name, textWidth), x+20, rowY+1, 0xE0E0E0);
			pose.pushPose();
			pose.translate(x+20, rowY+10, 0);
			pose.scale(0.666F, 0.666F, 1);
			font.draw(pose, order.status().title(), 0, 0, order.status().colour());
			pose.popPose();
			int barX = x+width-BAR_WIDTH-CANCEL_SIZE-6;
			GuiComponent.fill(pose, barX, rowY+5, barX+BAR_WIDTH, rowY+10, 0xFF2A332E);
			GuiComponent.fill(pose, barX, rowY+5, barX+Math.round(BAR_WIDTH*order.progress()), rowY+10, 0xFFFFD24A);
			int cancelX = x+width-CANCEL_SIZE-2;
			ECGui.button(pose, cancelX, rowY+3, CANCEL_SIZE, CANCEL_SIZE,
					isOver(mouseX, mouseY, cancelX, rowY+3, CANCEL_SIZE, CANCEL_SIZE));
			ECGui.icon(pose, cancelX+2, rowY+5, CANCEL_ICON, 0xFFFF8070);
		}
		pose.popPose();
	}

	List<Component> tooltip(int mouseX, int mouseY)
	{
		List<OrderSummary> orders = menu.getClientOrders();
		for(int i = 0; i<Math.min(rows(), orders.size()-scroll); i++)
		{
			int rowY = y+i*ROW_HEIGHT+1;
			if(isOver(mouseX, mouseY, x+width-CANCEL_SIZE-2, rowY+3, CANCEL_SIZE, CANCEL_SIZE))
				return List.of(new TranslatableComponent("gui.engineered_computation.jobs.cancel"));
			if(isOver(mouseX, mouseY, x+2, rowY, 16, 16))
				return List.of(orders.get(scroll+i).target().name(),
						new TranslatableComponent("gui.engineered_computation.jobs.progress",
								Math.round(orders.get(scroll+i).progress()*100)));
		}
		return List.of();
	}

	boolean isOver(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, x, y, width, height);
	}

	boolean mouseClicked(double mouseX, double mouseY)
	{
		List<OrderSummary> orders = menu.getClientOrders();
		for(int i = 0; i<Math.min(rows(), orders.size()-scroll); i++)
		{
			int rowY = y+i*ROW_HEIGHT+1;
			if(isOver(mouseX, mouseY, x+width-CANCEL_SIZE-2, rowY+3, CANCEL_SIZE, CANCEL_SIZE))
			{
				OrderSummary order = orders.get(scroll+i);
				ECPayloads.sendToServer(new CancelOrderPayload(menu.containerId, order.computer(), order.id()));
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
				return true;
			}
		}
		return isOver(mouseX, mouseY);
	}

	void mouseScrolled(double scrollY)
	{
		scroll = Math.max(0, scroll-(int)Math.signum(scrollY));
	}

	private static boolean isOver(double mouseX, double mouseY, int left, int top, int w, int h)
	{
		return mouseX>=left&&mouseX<left+w&&mouseY>=top&&mouseY<top+h;
	}
}

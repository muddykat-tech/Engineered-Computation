package com.muddykat.engineeredcomputation.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;

/**
 * The mod's config screen, laid out like the one NeoForge builds from the spec (Forge 1.20.1 has none): a row per
 * setting, then Undo, Reset and Done. The settings are server settings, so they can only be changed with a world open.
 * The machines' power costs are left to the config file.
 */
public class ECConfigScreen extends Screen
{
	private static final String KEY = EngineeredComputation.MODID+".configuration.";
	private static final int ROW_WIDTH = 310;
	private static final int VALUE_WIDTH = 150;
	private static final int BUTTON_WIDTH = 100;
	private static final int GAP = 5;
	private static final int FIRST_ROW = 32;
	private static final int ROW = 24;
	private static final List<String> SETTINGS = List.of("tapeArchiving", "powerEnabled", "accepterCapacity");

	private final Screen parent;
	private final boolean editable = ECConfig.SPEC.isLoaded();
	private final boolean originalArchiving = ECConfig.tapeArchiving();
	private final boolean originalPower = ECConfig.powerEnabled();
	private final int originalCapacity = ECConfig.accepterCapacity();
	private CycleButton<Boolean> archiving;
	private CycleButton<Boolean> power;
	private EditBox capacity;
	private Button undo;
	private Button reset;

	public ECConfigScreen(Screen parent)
	{
		super(Component.translatable(KEY+"section.engineered.computation.server.toml.title"));
		this.parent = parent;
	}

	@Override
	protected void init()
	{
		int valueX = width/2-ROW_WIDTH/2+ROW_WIDTH-VALUE_WIDTH;
		archiving = addRenderableWidget(CycleButton.onOffBuilder(ECConfig.tapeArchiving()).displayOnlyValue()
				.create(valueX, rowY(0), VALUE_WIDTH, 20, Component.translatable(KEY+"tapeArchiving"),
						(button, value) -> set(value, ECConfig.powerEnabled(), ECConfig.accepterCapacity())));
		power = addRenderableWidget(CycleButton.onOffBuilder(ECConfig.powerEnabled()).displayOnlyValue()
				.create(valueX, rowY(1), VALUE_WIDTH, 20, Component.translatable(KEY+"powerEnabled"),
						(button, value) -> set(ECConfig.tapeArchiving(), value, ECConfig.accepterCapacity())));
		capacity = addRenderableWidget(new EditBox(font, valueX+1, rowY(2)+1, VALUE_WIDTH-2, 18,
				Component.translatable(KEY+"accepterCapacity")));
		capacity.setValue(Integer.toString(ECConfig.accepterCapacity()));
		capacity.setFilter(text -> text.matches("\\d{0,10}"));
		capacity.setResponder(text -> {
			int value = parse(text);
			if(value>0&&value!=ECConfig.accepterCapacity())
				set(ECConfig.tapeArchiving(), ECConfig.powerEnabled(), value);
		});
		List<AbstractWidget> widgets = List.of(archiving, power, capacity);
		for(int row = 0; row<SETTINGS.size(); row++)
		{
			widgets.get(row).setTooltip(Tooltip.create(Component.translatable(KEY+SETTINGS.get(row)+".tooltip")));
			widgets.get(row).active = editable;
		}
		capacity.setEditable(editable);
		int buttonsLeft = width/2-(3*BUTTON_WIDTH+2*GAP)/2;
		undo = addRenderableWidget(Button.builder(Component.translatable(KEY+"undo"),
						button -> set(originalArchiving, originalPower, originalCapacity))
				.bounds(buttonsLeft, height-26, BUTTON_WIDTH, 20).build());
		reset = addRenderableWidget(Button.builder(Component.translatable(KEY+"reset"),
						button -> set(ECConfig.TAPE_ARCHIVING.getDefault(), ECConfig.POWER.getDefault(),
								ECConfig.ACCEPTER_CAPACITY.getDefault()))
				.bounds(buttonsLeft+BUTTON_WIDTH+GAP, height-26, BUTTON_WIDTH, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(buttonsLeft+2*(BUTTON_WIDTH+GAP), height-26, BUTTON_WIDTH, 20).build());
		updateButtons();
	}

	private static int rowY(int row)
	{
		return FIRST_ROW+row*ROW;
	}

	private static int parse(String text)
	{
		try
		{
			return text.isEmpty()? 0: Integer.parseInt(text);
		}
		catch(NumberFormatException tooBig)
		{
			return Integer.MAX_VALUE;
		}
	}

	private void set(boolean tapeArchiving, boolean powerEnabled, int accepterCapacity)
	{
		ECConfig.TAPE_ARCHIVING.set(tapeArchiving);
		ECConfig.POWER.set(powerEnabled);
		ECConfig.ACCEPTER_CAPACITY.set(accepterCapacity);
		archiving.setValue(tapeArchiving);
		power.setValue(powerEnabled);
		if(parse(capacity.getValue())!=accepterCapacity)
			capacity.setValue(Integer.toString(accepterCapacity));
		updateButtons();
	}

	private void updateButtons()
	{
		undo.active = editable&&(ECConfig.tapeArchiving()!=originalArchiving||ECConfig.powerEnabled()!=originalPower
				||ECConfig.accepterCapacity()!=originalCapacity);
		reset.active = editable&&(ECConfig.tapeArchiving()!=ECConfig.TAPE_ARCHIVING.getDefault()
				||ECConfig.powerEnabled()!=ECConfig.POWER.getDefault()
				||ECConfig.accepterCapacity()!=ECConfig.ACCEPTER_CAPACITY.getDefault());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(graphics);
		graphics.drawCenteredString(font, title, width/2, 15, 0xFFFFFF);
		for(int row = 0; row<SETTINGS.size(); row++)
			graphics.drawString(font, Component.translatable(KEY+SETTINGS.get(row)), width/2-ROW_WIDTH/2,
					rowY(row)+6, 0xFFFFFF);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose()
	{
		if(ECConfig.SPEC.isLoaded())
			ECConfig.SPEC.save();
		minecraft.setScreen(parent);
	}
}

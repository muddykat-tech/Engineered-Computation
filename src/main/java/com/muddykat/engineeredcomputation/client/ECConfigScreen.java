package com.muddykat.engineeredcomputation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;

/**
 * The mod's config screen, laid out like the one NeoForge builds from the spec (Forge 1.20.1 has none): a row per
 * setting, then Undo, Reset and Done. The settings are server settings, so they can only be changed with a world open.
 */
public class ECConfigScreen extends Screen
{
	private static final String KEY = EngineeredComputation.MODID+".configuration.";
	private static final int ROW_WIDTH = 310;
	private static final int VALUE_WIDTH = 150;
	private static final int BUTTON_WIDTH = 100;
	private static final int GAP = 5;

	private final Screen parent;
	private final boolean editable = ECConfig.SPEC.isLoaded();
	private final boolean original = ECConfig.tapeArchiving();
	private CycleButton<Boolean> archiving;
	private Button undo;
	private Button reset;

	public ECConfigScreen(Screen parent)
	{
		super(new TranslatableComponent(KEY+"section.engineered.computation.server.toml.title"));
		this.parent = parent;
	}

	@Override
	protected void init()
	{
		int left = width/2-ROW_WIDTH/2;
		archiving = addRenderableWidget(CycleButton.onOffBuilder(ECConfig.tapeArchiving()).displayOnlyValue()
				.create(left+ROW_WIDTH-VALUE_WIDTH, 32, VALUE_WIDTH, 20, new TranslatableComponent(KEY+"tapeArchiving"),
						(button, value) -> set(value)));
		archiving.active = editable;
		int buttonsLeft = width/2-(3*BUTTON_WIDTH+2*GAP)/2;
		undo = addRenderableWidget(new Button(buttonsLeft, height-26, BUTTON_WIDTH, 20,
				new TranslatableComponent(KEY+"undo"), button -> set(original)));
		reset = addRenderableWidget(new Button(buttonsLeft+BUTTON_WIDTH+GAP, height-26, BUTTON_WIDTH, 20,
				new TranslatableComponent(KEY+"reset"), button -> set(ECConfig.TAPE_ARCHIVING_DEFAULT)));
		addRenderableWidget(new Button(buttonsLeft+2*(BUTTON_WIDTH+GAP), height-26, BUTTON_WIDTH, 20,
				CommonComponents.GUI_DONE, button -> onClose()));
		updateButtons();
	}

	private void set(boolean value)
	{
		ECConfig.TAPE_ARCHIVING.set(value);
		archiving.setValue(value);
		updateButtons();
	}

	private void updateButtons()
	{
		undo.active = editable&&ECConfig.tapeArchiving()!=original;
		reset.active = editable&&ECConfig.tapeArchiving()!=ECConfig.TAPE_ARCHIVING_DEFAULT;
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(pose);
		drawCenteredString(pose, font, title, width/2, 15, 0xFFFFFF);
		font.drawShadow(pose, new TranslatableComponent(KEY+"tapeArchiving"), width/2-ROW_WIDTH/2, 38, 0xFFFFFF);
		super.render(pose, mouseX, mouseY, partialTick);
		if(archiving.isMouseOver(mouseX, mouseY))
			renderTooltip(pose, font.split(new TranslatableComponent(KEY+"tapeArchiving.tooltip"), 200),
					mouseX, mouseY);
	}

	@Override
	public void onClose()
	{
		if(ECConfig.SPEC.isLoaded())
			ECConfig.SPEC.save();
		minecraft.setScreen(parent);
	}
}

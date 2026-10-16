package com.muddykat.engineeredcomputation.compat.patchouli;

import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookCategory;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import com.muddykat.engineeredcomputation.compat.patchouli.PatchouliCompat;
import com.muddykat.engineeredcomputation.era.ComputerEra;

/** Client-side navigation that Patchouli's public API doesn't expose. */
public final class PatchouliClientCompat
{
	private PatchouliClientCompat()
	{
	}

	public static void openEraCategory(ComputerEra era)
	{
		Book book = BookRegistry.INSTANCE.books.get(PatchouliCompat.HANDBOOK);
		if(book == null)
		{
			PatchouliAPI.get().openBookGUI(PatchouliCompat.HANDBOOK);
			return;
		}

		BookCategory category = book.getContents().categories.get(PatchouliCompat.eraCategory(era));
		if(category == null)
		{
			PatchouliAPI.get().openBookGUI(PatchouliCompat.HANDBOOK);
			return;
		}

		BookContents contents = book.getContents();
		contents.checkValidCurrentEntry();
		GuiBook previous = contents.getCurrentGui();
		GuiBookCategory target = new GuiBookCategory(book, category);
		if(!target.equals(previous))
			contents.guiStack.push(previous);
		contents.openLexiconGui(target, false);
	}
}

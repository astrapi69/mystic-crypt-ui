/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.component;
import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Locale;

import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.lethenon.Wallet;

/**
 * Words pasted into the grid of 24 fields do not stay on the system clipboard (#804): once they are
 * in the fields the clipboard is emptied, but only if it still holds exactly what was pasted, the
 * conditional clear the host's {@code ClipboardClearWatchdog} makes for a copied password. Step 1 of
 * the backup says "not to the clipboard"; the fields invite a paste all the same, and this is what
 * cleans up after it
 */
class WordGridClipboardTest
{

	private final TypedWords words = new TypedWords();

	private final WordGrid grid = new WordGrid("Words", words, Locale.ENGLISH, sentence -> {});

	private final String phrase = Wallet.create().phrase();

	@BeforeEach
	void requireAClipboard()
	{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"a system clipboard needs a display");
	}

	@AfterEach
	void emptyTheRealClipboard()
	{
		copyToClipboard("");
	}

	@Test
	@DisplayName("a phrase pasted from the clipboard fills the fields and leaves the clipboard empty")
	void aPhrasePastedFromTheClipboard_isGoneFromIt()
	{
		copyToClipboard(phrase);

		field(5).paste();

		assertEquals(phrase, words.phrase());
		assertEquals(List.of(phrase.split(" ")).getLast(), textOf(grid, "txtWords24"));
		assertEquals("", readClipboard());
	}

	@Test
	@DisplayName("a single word pasted from the clipboard goes into its field and leaves the clipboard empty")
	void aWordPastedFromTheClipboard_isGoneFromIt()
	{
		copyToClipboard("abandon");

		field(5).paste();

		assertEquals("abandon", textOf(grid, "txtWords5"));
		assertEquals("", readClipboard());
	}

	@Test
	@DisplayName("a paste that did not come from the clipboard leaves what the clipboard holds")
	void aPasteNotFromTheClipboard_leavesTheClipboardAlone()
	{
		copyToClipboard("something the person copied elsewhere");

		Windows.paste(grid, "txtWords1", phrase);

		assertEquals(phrase, words.phrase());
		assertEquals("something the person copied elsewhere", readClipboard());
	}

	private JTextField field(final int position)
	{
		return component(grid, "txtWords" + position, JTextField.class);
	}

	private static void copyToClipboard(final String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text),
			null);
	}

	private static String readClipboard()
	{
		try
		{
			return (String)Toolkit.getDefaultToolkit().getSystemClipboard()
				.getData(DataFlavor.stringFlavor);
		}
		catch (Exception unreadable)
		{
			return null;
		}
	}
}

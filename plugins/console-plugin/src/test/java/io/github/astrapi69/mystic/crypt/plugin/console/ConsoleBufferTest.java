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
package io.github.astrapi69.mystic.crypt.plugin.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the console keeps, and what it lets go of (#375).
 * <p>
 * The buffer behind the console grew without limit, and closing the window did not shorten it: a
 * session was measured at 452 lines and 39410 characters, holding vault paths out of a stack trace.
 */
class ConsoleBufferTest
{

	@Test
	@DisplayName("the oldest lines are dropped once the limit is reached")
	void theOldestLinesAreDropped() throws Exception
	{
		JTextArea textArea = new JTextArea();
		try (PrintStream console = new PrintStream(new ConsoleBuffer(textArea, 10), true))
		{
			for (int line = 1; line <= 100; line++)
			{
				console.println("line " + line);
			}
		}
		waitForTheEventThread();

		String shown = textArea.getText();
		assertTrue(textArea.getLineCount() <= 11,
			"at most the configured ten lines are kept, plus the empty one after the last line "
				+ "break; measured " + textArea.getLineCount());
		assertTrue(shown.contains("line 100"), "the newest line is there");
		assertFalse(shown.contains("line 1\n"),
			"and the oldest is gone, rather than lying in a buffer nothing trims");
	}

	@Test
	@DisplayName("clearing erases what is shown and what has not been written yet")
	void clearingErasesEverything() throws Exception
	{
		JTextArea textArea = new JTextArea();
		ConsoleBuffer buffer = new ConsoleBuffer(textArea, 100);
		PrintStream console = new PrintStream(buffer, true);
		console.println("/home/somebody/vaults/private.mcrdb");
		console.print("a line that never ended");
		waitForTheEventThread();

		buffer.clear();
		waitForTheEventThread();

		assertEquals("", textArea.getText(),
			"a cleared console shows nothing. The lock closes the window, and a window that is "
				+ "gone must not leave its scrollback behind");
		console.flush();
		waitForTheEventThread();
		assertEquals("", textArea.getText(),
			"the unwritten bytes went with it, instead of arriving after the erase");
	}

	private static void waitForTheEventThread()
		throws InterruptedException, InvocationTargetException
	{
		SwingUtilities.invokeAndWait(() -> {
		});
	}
}

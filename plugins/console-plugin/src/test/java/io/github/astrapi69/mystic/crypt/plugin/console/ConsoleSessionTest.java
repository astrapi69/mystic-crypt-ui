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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.JInternalFrame;
import javax.swing.JTextArea;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * One console at a time (#892): a second {@code opened} while one is open used to overwrite the
 * frame, the buffer and the saved standard streams, so closing handed {@code System.out} back to a
 * console that was gone. Now the session refuses it and keeps the first session whole, and the menu
 * brings the open console to the front instead
 */
class ConsoleSessionTest
{

	@AfterEach
	void endWhateverIsOpen()
	{
		if (ConsoleSession.isOpen())
		{
			ConsoleSession.ended();
		}
	}

	@Test
	@DisplayName("a second opened is refused and the first session stays whole")
	void aSecondOpened_isRefusedAndTheFirstSessionStaysWhole()
	{
		JInternalFrame firstFrame = new JInternalFrame("Console");
		ConsoleBuffer firstBuffer = new ConsoleBuffer(new JTextArea(), 10);
		PrintStream firstOut = new PrintStream(new ByteArrayOutputStream(), true);
		PrintStream firstErr = new PrintStream(new ByteArrayOutputStream(), true);
		ConsoleSession.opened(firstFrame, firstBuffer, firstOut, firstErr);

		JInternalFrame secondFrame = new JInternalFrame("Console");
		assertThrows(IllegalStateException.class,
			() -> ConsoleSession.opened(secondFrame, new ConsoleBuffer(new JTextArea(), 10),
				new PrintStream(new ByteArrayOutputStream(), true),
				new PrintStream(new ByteArrayOutputStream(), true)),
			"a second console must not take over the open session");

		assertTrue(ConsoleSession.isOpen());
		assertSame(firstFrame, ConsoleSession.openFrame(),
			"the first console is still the session");
	}

	@Test
	@DisplayName("ended after a refused second opened hands back the first saved streams")
	void endedAfterARefusedSecondOpened_handsBackTheFirstStreams()
	{
		PrintStream outBefore = System.out;
		PrintStream errBefore = System.err;
		try
		{
			JInternalFrame frame = new JInternalFrame("Console");
			PrintStream savedOut = new PrintStream(new ByteArrayOutputStream(), true);
			PrintStream savedErr = new PrintStream(new ByteArrayOutputStream(), true);
			ConsoleSession.opened(frame, new ConsoleBuffer(new JTextArea(), 10), savedOut,
				savedErr);
			assertThrows(IllegalStateException.class,
				() -> ConsoleSession.opened(new JInternalFrame("Console"),
					new ConsoleBuffer(new JTextArea(), 10),
					new PrintStream(new ByteArrayOutputStream(), true),
					new PrintStream(new ByteArrayOutputStream(), true)));

			ConsoleSession.ended();

			assertFalse(ConsoleSession.isOpen());
			assertSame(savedOut, System.out, "System.out is the stream the FIRST opened saved");
			assertSame(savedErr, System.err, "System.err is the stream the FIRST opened saved");
		}
		finally
		{
			System.setOut(outBefore);
			System.setErr(errBefore);
		}
	}

	@Test
	@DisplayName("after ended a console can be opened again")
	void afterEnded_aConsoleOpensAgain()
	{
		JInternalFrame firstFrame = new JInternalFrame("Console");
		ConsoleSession.opened(firstFrame, new ConsoleBuffer(new JTextArea(), 10),
			new PrintStream(new ByteArrayOutputStream(), true),
			new PrintStream(new ByteArrayOutputStream(), true));
		ConsoleSession.ended();

		JInternalFrame secondFrame = new JInternalFrame("Console");
		ConsoleSession.opened(secondFrame, new ConsoleBuffer(new JTextArea(), 10),
			new PrintStream(new ByteArrayOutputStream(), true),
			new PrintStream(new ByteArrayOutputStream(), true));

		assertTrue(ConsoleSession.isOpen());
		assertEquals(secondFrame, ConsoleSession.openFrame());
	}
}

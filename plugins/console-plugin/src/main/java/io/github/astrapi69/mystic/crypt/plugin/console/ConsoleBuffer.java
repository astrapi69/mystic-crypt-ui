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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

/**
 * What the console shows, with a line limit and a way to erase it (#375).
 * <p>
 * The library's own {@code JTextAreaOutputStream} keeps every byte ever written in a
 * {@code StringBuilder} that nothing trims; a console left open for a session was measured at 452
 * lines and 39410 characters, and the only way to shorten it was to close the window - which did
 * not shorten it either, because the stream kept writing into the text area of a disposed panel.
 * <p>
 * This one holds at most {@code maxLines} lines, drops the oldest when more arrive, and erases both
 * the text area and the bytes it has not written yet when the console goes.
 */
final class ConsoleBuffer extends OutputStream
{

	private final JTextArea textArea;

	private final int maxLines;

	/** Bytes of a line that has not ended yet; a character can arrive one byte at a time */
	private final ByteArrayOutputStream incomplete = new ByteArrayOutputStream();

	ConsoleBuffer(final JTextArea textArea, final int maxLines)
	{
		this.textArea = textArea;
		this.maxLines = Math.max(1, maxLines);
	}

	@Override
	public void write(final int singleByte)
	{
		incomplete.write(singleByte);
		if (singleByte == '\n')
		{
			flush();
		}
	}

	@Override
	public void write(final byte[] bytes, final int offset, final int length)
	{
		incomplete.write(bytes, offset, length);
		for (int index = offset; index < offset + length; index++)
		{
			if (bytes[index] == '\n')
			{
				flush();
				return;
			}
		}
	}

	@Override
	public void flush()
	{
		if (incomplete.size() == 0)
		{
			return;
		}
		String text = incomplete.toString(StandardCharsets.UTF_8);
		incomplete.reset();
		SwingUtilities.invokeLater(() -> {
			textArea.append(text);
			dropTheOldestLines();
		});
	}

	/** Erases what is shown and what is on the way to being shown */
	void clear()
	{
		incomplete.reset();
		SwingUtilities.invokeLater(() -> textArea.setText(""));
	}

	private void dropTheOldestLines()
	{
		int lines = textArea.getLineCount();
		if (lines <= maxLines)
		{
			return;
		}
		try
		{
			textArea.getDocument().remove(0, textArea.getLineEndOffset(lines - maxLines - 1));
		}
		catch (BadLocationException impossible)
		{
			// the offsets come from the document itself; leaving the text as it is beats losing the
			// console over an offset it just handed out
			textArea.setText("");
		}
	}
}

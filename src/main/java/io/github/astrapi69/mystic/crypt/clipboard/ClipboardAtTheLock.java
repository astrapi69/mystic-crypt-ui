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
package io.github.astrapi69.mystic.crypt.clipboard;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * What the lock does to the clipboard: it empties it, so that a password copied before the lock
 * does not outlive it (#238), and it remembers whether it emptied text, so that the unlock prompt
 * can say so (#1001). Without that line a password copied before the lock pastes nothing into the
 * prompt, which reads as "paste does not work here".
 * <p>
 * The answer is kept here and not in the action that locks, because two instances of that action
 * lock the workspace: the File menu's and the idle lock's
 */
public final class ClipboardAtTheLock
{

	private static volatile boolean emptiedText;

	private ClipboardAtTheLock()
	{
	}

	/**
	 * Empties the clipboard for a lock and remembers whether it held text
	 *
	 * @param clipboard
	 *            the clipboard to empty, the system clipboard in the application
	 */
	public static void empty(final Clipboard clipboard)
	{
		emptiedText = holdsText(clipboard);
		clipboard.setContents(new StringSelection(""), null);
	}

	/**
	 * Whether the last lock emptied a clipboard that held text, and nobody has unlocked since
	 *
	 * @return true if the unlock prompt should say that the clipboard was emptied
	 */
	public static boolean emptiedText()
	{
		return emptiedText;
	}

	/**
	 * Forgets what the last lock emptied, once the workspace is unlocked
	 */
	public static void forget()
	{
		emptiedText = false;
	}

	/**
	 * Whether the clipboard holds text that is not empty. A clipboard that cannot be read counts as
	 * holding none: the prompt would otherwise claim to have emptied something nobody saw
	 */
	private static boolean holdsText(final Clipboard clipboard)
	{
		try
		{
			return clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
				&& !((String)clipboard.getData(DataFlavor.stringFlavor)).isEmpty();
		}
		catch (IllegalStateException | UnsupportedFlavorException | IOException unreadable)
		{
			return false;
		}
	}
}

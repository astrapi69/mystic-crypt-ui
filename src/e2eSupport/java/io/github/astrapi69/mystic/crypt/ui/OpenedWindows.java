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
package io.github.astrapi69.mystic.crypt.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JFileChooser;

import org.assertj.swing.edt.GuiActionRunner;

/**
 * Closes whatever a fired action or a clicked button left open, so the next one starts from the
 * same state. Shared by the two harnesses that fire things at a locked workspace: the core one over
 * the action package ({@code LockInvariantUiTest}) and the plugin one over what the plugins offer
 * while locked ({@code PluginsUsableWhileLockedHoldTheLockUiTest}, #301)
 */
final class OpenedWindows
{

	private OpenedWindows()
	{
	}

	/**
	 * Closes every showing dialog - a refusal, a file chooser, a message. A file chooser is
	 * CANCELLED, never approved: approving one would measure what the caller does with a target
	 * file, which is a different question
	 */
	static void dismissWhateverOpened()
	{
		GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (!window.isShowing())
				{
					continue;
				}
				JFileChooser fileChooser = fileChooserIn(window);
				if (fileChooser != null)
				{
					fileChooser.cancelSelection();
				}
				if (window instanceof JDialog dialog)
				{
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		});
		UiTestSpeed.windowManagerSettle();
	}

	private static JFileChooser fileChooserIn(final Container container)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JFileChooser fileChooser)
			{
				return fileChooser;
			}
			if (component instanceof Container nested)
			{
				JFileChooser found = fileChooserIn(nested);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}

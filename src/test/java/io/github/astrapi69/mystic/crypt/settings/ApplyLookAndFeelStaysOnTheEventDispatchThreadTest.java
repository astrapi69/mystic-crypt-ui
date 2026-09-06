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
package io.github.astrapi69.mystic.crypt.settings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The application builds its frame outside the event dispatch thread
 * ({@code StartMysticCryptApplication}), and {@code onAfterInitialize} applies the persisted look
 * and feel from there. {@code updateComponentTreeUI} takes the AWT tree lock and then asks a text
 * component for its preferred size, which needs that component's document - so doing it off the
 * event dispatch thread deadlocks against the thread writing into such a document, which is exactly
 * what the console tool does with the output it captures.
 * <p>
 * The window hung at startup before the fix. What is pinned here is the property that prevents it:
 * the component tree is only ever touched on the event dispatch thread, whoever calls.
 */
class ApplyLookAndFeelStaysOnTheEventDispatchThreadTest
{

	private JFrame frame;

	@AfterEach
	void disposeFrame() throws Exception
	{
		if (frame != null)
		{
			SwingUtilities.invokeAndWait(frame::dispose);
			frame = null;
		}
	}

	/** Records which thread its user interface was re-installed on */
	private static final class ThreadRecordingPanel extends JPanel
	{
		private final AtomicBoolean updatedOffTheEventDispatchThread = new AtomicBoolean(false);
		private final AtomicBoolean updated = new AtomicBoolean(false);

		@Override
		public void updateUI()
		{
			super.updateUI();
			// JPanel's own constructor calls this before the fields above exist, so the first
			// call arrives with them still null - that one is not the one under test
			if (updated == null)
			{
				return;
			}
			updated.set(true);
			if (!SwingUtilities.isEventDispatchThread())
			{
				updatedOffTheEventDispatchThread.set(true);
			}
		}
	}

	@Test
	@DisplayName("a look and feel applied from another thread still re-installs the ui on the event dispatch thread")
	void theComponentTreeIsOnlyTouchedOnTheEventDispatchThread() throws Exception
	{
		GeneralSettingsPanel.applyLookAndFeel("Metal");
		ThreadRecordingPanel panel = new ThreadRecordingPanel();
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("look and feel probe");
			frame.getContentPane().add(panel, BorderLayout.CENTER);
			frame.getContentPane().add(new JTextArea("some text"), BorderLayout.SOUTH);
			frame.pack();
		});

		Thread caller = new Thread(() -> GeneralSettingsPanel.applyLookAndFeel("Nimbus"),
			"not-the-event-dispatch-thread");
		caller.start();
		caller.join(Duration.ofSeconds(30).toMillis());

		assertFalse(caller.isAlive(),
			"applying a look and feel from another thread did not come back - the window hangs");
		assertTrue(panel.updated.get(),
			"the probe's ui was never re-installed, so nothing was proven");
		assertFalse(panel.updatedOffTheEventDispatchThread.get(),
			"the component tree was touched off the event dispatch thread, which is what deadlocks "
				+ "against a thread writing into a text component's document");
	}
}

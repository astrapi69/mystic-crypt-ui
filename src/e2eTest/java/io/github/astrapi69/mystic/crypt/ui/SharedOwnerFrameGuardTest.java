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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Frame;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link SharedOwnerFrameGuard} records a dialog without a parent and none with one (#63). It runs
 * on its own instance, not through {@link AbstractUiTest}, whose own guard would fail this test for
 * the ownerless dialog it opens on purpose.
 */
class SharedOwnerFrameGuardTest
{

	private final SharedOwnerFrameGuard guard = new SharedOwnerFrameGuard();

	@BeforeEach
	void requireADisplay()
	{
		TestPrerequisites.requireADisplay();
		guard.switchOn();
	}

	@AfterEach
	void switchTheGuardOff()
	{
		guard.switchOff();
	}

	@Test
	@DisplayName("a dialog without a parent is recorded by class and title, and fails the check")
	void aDialogWithoutAParent_isRecorded() throws Exception
	{
		JDialog dialog = GuiActionRunner
			.execute(() -> shown(new JDialog((Frame)null, "no parent")));
		try
		{
			awaitTheEventQueue();
			assertEquals(List.of("javax.swing.JDialog 'no parent'"), guard.recorded());
			AssertionError failure = assertThrows(AssertionError.class, guard::requireNone);
			assertTrue(failure.getMessage().contains("javax.swing.JDialog 'no parent'"),
				failure.getMessage());
		}
		finally
		{
			GuiActionRunner.execute(dialog::dispose);
		}
	}

	@Test
	@DisplayName("a dialog with a frame as parent is not recorded, and the check passes")
	void aDialogWithAFrameAsParent_isNotRecorded() throws Exception
	{
		JFrame frame = GuiActionRunner.execute(() -> {
			JFrame owner = new JFrame("owner");
			owner.setSize(200, 120);
			owner.setVisible(true);
			return owner;
		});
		JDialog dialog = GuiActionRunner.execute(() -> shown(new JDialog(frame, "with parent")));
		try
		{
			awaitTheEventQueue();
			assertTrue(guard.recorded().isEmpty(), "recorded: " + guard.recorded());
			guard.requireNone();
		}
		finally
		{
			GuiActionRunner.execute(() -> {
				dialog.dispose();
				frame.dispose();
			});
		}
	}

	private static JDialog shown(final JDialog dialog)
	{
		dialog.setSize(160, 90);
		dialog.setVisible(true);
		return dialog;
	}

	/** WINDOW_OPENED is posted, not sent: two passes through the queue deliver it */
	private static void awaitTheEventQueue() throws Exception
	{
		javax.swing.SwingUtilities.invokeAndWait(() -> {
		});
		javax.swing.SwingUtilities.invokeAndWait(() -> {
		});
	}
}

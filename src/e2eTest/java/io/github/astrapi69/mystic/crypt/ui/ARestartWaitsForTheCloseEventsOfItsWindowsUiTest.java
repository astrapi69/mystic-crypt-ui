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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Why a restart in a UI test lost its sign-in dialog at random (#1070), and that
 * {@link AbstractUiTest#shutdownApplication()} prevents it.
 * <p>
 * A dialog without a parent belongs to Swing's shared owner frame. When the windows are disposed,
 * the JDK disposes that frame as well, also on its own once its last window closes
 * ({@code SwingUtilities.SharedOwnerFrame.windowClosed}). AssertJ-Swing answers the frame's
 * WINDOW_CLOSED with a task it queues on the event thread, which ignores every window the frame
 * owns when the task runs. A dialog of the next start that is created before that task runs is
 * owned by the frame, so the task hides it from the robot for good, though it is showing.
 * <p>
 * The tests make that order certain instead of likely: while the old dialog's WINDOW_CLOSED is
 * dispatched, a listener holds the event thread until the next dialog has been queued, or two
 * seconds at most, so the frame's close comes after the next start. The first test holds the
 * libraries' behaviour without the harness; the second runs the harness's restart through it.
 */
class ARestartWaitsForTheCloseEventsOfItsWindowsUiTest extends AbstractUiTest
{

	private static final String OLD_DIALOG = "before the restart";

	/**
	 * Holds what AssertJ-Swing 3.17.1 and the JDK (Temurin 25.0.5) do, not what this application
	 * does: the JDK disposes Swing's shared owner frame with the windows, and on its own once its
	 * last window closes ({@code SwingUtilities.SharedOwnerFrame.windowClosed}); AssertJ-Swing
	 * answers the frame's WINDOW_CLOSED with a task on the event thread that ignores every window
	 * the frame owns when the task runs. A dialog without a parent that is created before that task
	 * runs is showing and hidden from the robot.
	 * <p>
	 * This behaviour is the reason {@link AbstractUiTest#shutdownApplication()} waits until the
	 * event queue has run empty (#1070). If this test turns red after an update of AssertJ-Swing or
	 * the JDK, the behaviour changed: reassess that wait in {@code shutdownApplication()}, not the
	 * product code.
	 */
	@Test
	@DisplayName("AssertJ-Swing 3.17.1 and JDK 25 hide a dialog without a parent created behind the close of the shared owner frame")
	void assertjSwingAndTheJdk_hideADialogCreatedBehindTheCloseOfTheSharedOwnerFrame()
		throws Exception
	{
		showOwnerless(OLD_DIALOG);
		AtomicReference<JDialog> next = new AtomicReference<>();
		GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				window.dispose();
			}
			SwingUtilities.invokeLater(() -> next.set(newShownOwnerless("behind the close")));
		});
		robot.waitForIdle();

		assertTrue(GuiActionRunner.execute(() -> next.get().isShowing()),
			"precondition: the dialog is showing");
		assertFalse(robotFinds("behind the close"),
			"AssertJ-Swing's task for the closed shared frame ran after the dialog was created");
		GuiActionRunner.execute(() -> next.get().dispose());
	}

	@Test
	@DisplayName("a dialog without a parent that the next start creates right after shutdownApplication is found by the robot")
	void aDialogCreatedRightAfterShutdownApplication_isFoundByTheRobot() throws Exception
	{
		showOwnerless(OLD_DIALOG);
		CountDownLatch nextStartQueued = new CountDownLatch(1);
		AWTEventListener holdTheEventThread = holdOnTheCloseOf(OLD_DIALOG, nextStartQueued);
		Toolkit.getDefaultToolkit().addAWTEventListener(holdTheEventThread,
			AWTEvent.WINDOW_EVENT_MASK);
		AtomicReference<JDialog> next = new AtomicReference<>();
		try
		{
			shutdownApplication();
			SwingUtilities.invokeLater(() -> next.set(newShownOwnerless("after the restart")));
			nextStartQueued.countDown();
			robot.waitForIdle();
		}
		finally
		{
			Toolkit.getDefaultToolkit().removeAWTEventListener(holdTheEventThread);
		}

		assertTrue(GuiActionRunner.execute(() -> next.get().isShowing()),
			"precondition: the dialog is showing");
		assertTrue(robotFinds("after the restart"),
			"shutdownApplication returns only after the close of the shared frame is handled");
		GuiActionRunner.execute(() -> next.get().dispose());
	}

	/**
	 * Holds the event thread while the WINDOW_CLOSED of the dialog with the given title is
	 * dispatched, until the latch is counted down or two seconds have passed
	 */
	private static AWTEventListener holdOnTheCloseOf(final String title,
		final CountDownLatch release)
	{
		return event -> {
			if (event.getID() == WindowEvent.WINDOW_CLOSED && event instanceof WindowEvent closed
				&& closed.getWindow()instanceof Dialog dialog && title.equals(dialog.getTitle()))
			{
				try
				{
					release.await(2, TimeUnit.SECONDS);
				}
				catch (InterruptedException interrupted)
				{
					Thread.currentThread().interrupt();
				}
			}
		};
	}

	private void showOwnerless(final String title)
	{
		GuiActionRunner.execute(() -> newShownOwnerless(title));
		robot.waitForIdle();
	}

	private static JDialog newShownOwnerless(final String title)
	{
		JDialog dialog = new JDialog((Frame)null, title);
		dialog.setSize(160, 90);
		dialog.setVisible(true);
		return dialog;
	}

	private boolean robotFinds(final String title)
	{
		return !robot.finder().findAll(new GenericTypeMatcher<JDialog>(JDialog.class)
		{
			@Override
			protected boolean isMatching(final JDialog candidate)
			{
				return title.equals(candidate.getTitle()) && candidate.isShowing();
			}
		}).isEmpty();
	}
}

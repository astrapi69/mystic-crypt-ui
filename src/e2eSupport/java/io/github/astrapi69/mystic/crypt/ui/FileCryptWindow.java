/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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

import javax.swing.JButton;

import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;

/**
 * Presses a button of the file encryption window and waits for its answer.
 * <p>
 * The window derives the key on a thread of its own and offers Cancel while it does (#1053), so the
 * press returns before the answer is there and waiting until the event queue is idle waits for
 * nothing. The answer is there once Cancel is gone again: the window shows Cancel inside the press
 * and takes it away in the same step that puts the answer on screen.
 */
final class FileCryptWindow
{

	/** How long the key derivation may take on a loaded runner before the test gives up */
	private static final int ANSWER_TIMEOUT_MILLIS = 60_000;

	private FileCryptWindow()
	{
	}

	/**
	 * Presses the button and returns once the window has answered
	 *
	 * @param frame
	 *            the main frame the window is open in
	 * @param robot
	 *            the robot of the test
	 * @param buttonName
	 *            the name of the button, for example {@code btnEncryptFile}
	 */
	static void pressAndAwaitTheAnswer(final FrameFixture frame, final Robot robot,
		final String buttonName)
	{
		GuiActionRunner.execute(() -> frame.button(buttonName).target().doClick());
		awaitTheAnswer(robot, buttonName);
	}

	/**
	 * Returns once the window has answered the press that came before
	 *
	 * @param robot
	 *            the robot of the test
	 * @param pressed
	 *            what was pressed, for the message when no answer comes
	 */
	static void awaitTheAnswer(final Robot robot, final String pressed)
	{
		Pause.pause(new Condition("the file encryption window answers the press of " + pressed)
		{
			@Override
			public boolean test()
			{
				return GuiActionRunner.execute(() -> !cancelButton(robot).isShowing());
			}
		}, ANSWER_TIMEOUT_MILLIS);
		robot.waitForIdle();
	}

	/**
	 * The Cancel button of the window, which is only showing while an operation runs
	 *
	 * @param robot
	 *            the robot of the test
	 * @return the button
	 */
	static JButton cancelButton(final Robot robot)
	{
		return robot.finder().findByName("btnCancelFileCrypt", JButton.class, false);
	}
}

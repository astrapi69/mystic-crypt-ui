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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A restart ends the start that was running before it returns (#1070 investigation).
 * <p>
 * Disposing the sign-in dialog of a start that is still waiting on it releases that start's thread,
 * which then builds the rest of the frame, including its look-and-feel switch. A restart that did
 * not wait for it ran that thread beside the next start: two starts in one JVM, one building its
 * sign-in dialog while the other switched the look and feel, which killed the second with
 * {@code ClassCastException: FlatLightLaf cannot be cast to NimbusLookAndFeel} at
 * {@code NimbusStyle.validate}. A real process has one start, so this was a defect of the harness.
 */
class RestartWaitsForTheReleasedStartUiTest extends AbstractUiTest
{

	@Test
	@DisplayName("when a restart returns, the start thread whose sign-in dialog was still open has ended")
	void aRestartEndsTheStartItReleased() throws Exception
	{
		launchApplication();

		shutdownApplication();

		assertFalse(
			Thread.getAllStackTraces().keySet().stream()
				.anyMatch(thread -> WhatWasOnScreen.START_THREAD.equals(thread.getName())
					&& thread.isAlive()),
			"the start thread was still running beside the next start");
	}
}

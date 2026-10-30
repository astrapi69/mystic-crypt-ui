/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The start reports how far it has come only in test mode, and counts each step, so a harness can
 * wait for the next one instead of for a window that happens to be showing (#522)
 */
class StartupSignalTest
{

	@BeforeEach
	void startFromNothing()
	{
		System.clearProperty(StartupSignal.PROPERTY);
		StartupSignal.reset();
	}

	@AfterEach
	void leaveNothingBehind()
	{
		System.clearProperty(StartupSignal.PROPERTY);
		StartupSignal.reset();
	}

	@Test
	@DisplayName("outside test mode the start reports nothing")
	void report_countsNothing_whenNotInTestMode()
	{
		StartupSignal.startBegun();
		StartupSignal.signInDialogShown();
		StartupSignal.startCompleted();

		assertEquals(0, StartupSignal.startsBegun());
		assertEquals(0, StartupSignal.signInDialogsShown());
		assertEquals(0, StartupSignal.startsCompleted());
	}

	@Test
	@DisplayName("in test mode every step is counted, each on its own")
	void report_countsEachStep_whenInTestMode()
	{
		System.setProperty(StartupSignal.PROPERTY, "true");

		StartupSignal.startBegun();
		StartupSignal.signInDialogShown();
		StartupSignal.signInDialogShown();
		StartupSignal.startCompleted();

		assertEquals(1, StartupSignal.startsBegun());
		assertEquals(2, StartupSignal.signInDialogsShown());
		assertEquals(1, StartupSignal.startsCompleted());
	}

	@Test
	@DisplayName("a start is in progress from its beginning until it has completed")
	void startInProgress_isTrue_betweenBeginAndCompletion()
	{
		System.setProperty(StartupSignal.PROPERTY, "true");
		assertFalse(StartupSignal.startInProgress());

		StartupSignal.startBegun();
		assertTrue(StartupSignal.startInProgress());

		StartupSignal.startCompleted();
		assertFalse(StartupSignal.startInProgress());
	}

	@Test
	@DisplayName("a reset starts the count again from zero")
	void reset_setsEveryCountBackToZero()
	{
		System.setProperty(StartupSignal.PROPERTY, "true");
		StartupSignal.startBegun();
		StartupSignal.signInDialogShown();
		StartupSignal.startCompleted();

		StartupSignal.reset();

		assertEquals(0, StartupSignal.startsBegun());
		assertEquals(0, StartupSignal.signInDialogsShown());
		assertEquals(0, StartupSignal.startsCompleted());
	}
}

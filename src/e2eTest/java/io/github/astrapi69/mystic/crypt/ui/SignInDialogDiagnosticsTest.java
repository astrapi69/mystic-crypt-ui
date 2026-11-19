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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The report a failed lookup of the sign-in dialog carries (#1070): the part that formats the
 * record, driven with made-up facts, because the failure it describes cannot be made on demand.
 */
class SignInDialogDiagnosticsTest
{

	private static final long ROBOT_BUILT = 1_000_000_000L;

	private static long plusMillis(final long millis)
	{
		return ROBOT_BUILT + millis * 1_000_000L;
	}

	@Test
	@DisplayName("a sign-in dialog that exists is reported with its owner chain, the filter's answers, the method number, the points of the test and the window events in order")
	void aDialogThatExistsIsReportedWithEverythingNeeded()
	{
		List<SignInDialogDiagnostics.Mark> marks = List.of(
			new SignInDialogDiagnostics.Mark("robot created", ROBOT_BUILT),
			new SignInDialogDiagnostics.Mark("shutdownApplication ended, windows disposed",
				plusMillis(40)),
			new SignInDialogDiagnostics.Mark("launchApplication begun", plusMillis(41)));
		List<SignInDialogDiagnostics.WindowRecord> events = List.of(
			new SignInDialogDiagnostics.WindowRecord("WINDOW_CLOSED",
				"javax.swing.SwingUtilities$SharedOwnerFrame@1a", plusMillis(42)),
			new SignInDialogDiagnostics.WindowRecord("WINDOW_OPENED",
				"MasterPwFileDialog@2b title='Enter your credentials'", plusMillis(300)));
		List<SignInDialogDiagnostics.DialogFacts> dialogs = List
			.of(new SignInDialogDiagnostics.DialogFacts("MasterPwFileDialog@2b", true, true, false,
				List.of(new SignInDialogDiagnostics.WindowFacts("SharedOwnerFrame@1a", true, false,
					true))));

		String report = SignInDialogDiagnostics.format(2, ROBOT_BUILT, plusMillis(15_300), marks,
			events, dialogs);

		assertTrue(report.contains("test method number 2 of this JVM"), report);
		assertTrue(report.contains("the lookup gave up at +15300"), report);
		assertTrue(report.contains("+40 shutdownApplication ended, windows disposed"), report);
		assertTrue(
			report.contains(
				"MasterPwFileDialog@2b showing=true ignored=true " + "implicitlyIgnored=false"),
			report);
		assertTrue(report.contains("owner: SharedOwnerFrame@1a displayable=true "
			+ "ignored=false implicitlyIgnored=true"), report);
		assertTrue(report.indexOf("+42 WINDOW_CLOSED") < report.indexOf("+300 WINDOW_OPENED"),
			"the window events stay in the order they came: " + report);
	}

	@Test
	@DisplayName("no sign-in dialog at all is said so, and a dialog without an owner says that")
	void noDialogAndNoOwnerAreSaid()
	{
		String none = SignInDialogDiagnostics.format(1, ROBOT_BUILT, plusMillis(5), List.of(),
			List.of(), List.of());
		String ownerless = SignInDialogDiagnostics.format(1, ROBOT_BUILT, plusMillis(5), List.of(),
			List.of(), List.of(new SignInDialogDiagnostics.DialogFacts("MasterPwFileDialog@2b",
				false, false, false, List.of())));

		assertTrue(none.contains("no window titled \"Enter your credentials\" exists"), none);
		assertTrue(ownerless.contains("no owner (null)"), ownerless);
	}
}

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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.shows;
import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * What a window that runs an action shows in its answer area before the first action (rule G3,
 * draft 06, #816, #820): not empty fields, but one sentence that says what will show there
 */
class AnswerToComeInEveryWindowTest
{

	@TempDir
	static File configuration;

	@BeforeEach
	void aConfigurationOfItsOwn()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.getAbsolutePath());
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	/**
	 * One window
	 *
	 * @param window
	 *            what the user sees
	 * @param panel
	 *            builds the window, once the configuration directory is set
	 * @param toCome
	 *            the sentence in the place of the answer, in English
	 */
	record Window(String window, Supplier<JPanel> panel, String toCome)
	{
		@Override
		public String toString()
		{
			return window;
		}
	}

	static Stream<Arguments> windows()
	{
		return Stream.of(
			Arguments.of(new Window("Check my chain", LethenonChainPanel::new,
				"After the check, this shows whether your chain holds.")),
			Arguments.of(new Window("Show my balance", LethenonBalancePanel::new,
				"Your balance shows here once you ask for it.")),
			Arguments.of(new Window("Mine the pun", LethenonMinePanel::new,
				"After mining, this shows the new block and what it brought.")),
			Arguments.of(new Window("Start my node", LethenonNodePanel::new,
				"Once your node runs, this shows how it is reached and whom it is connected to.")),
			Arguments.of(new Window("Send", LethenonSendPanel::new,
				"After sending, this shows what went out and what still waits.")),
			Arguments.of(new Window("Sweep onto my account", LethenonSweepPanel::new,
				"After sweeping, this shows what went to your account and what still waits.")),
			Arguments.of(new Window("Bring my chain up to date", LethenonSyncPanel::new,
				"After the update, this shows up to which block your chain reaches.")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("windows")
	void beforeTheFirstAction_aSentenceSaysWhatWillShow(final Window window)
	{
		JPanel panel = window.panel().get();

		assertFalse(shows(panel, "txtAnswer"), "an empty answer shows before the first action");
		assertFalse(shows(panel, "btnDetails"), "details of nothing can be opened");
		assertTrue(shows(panel, AnswerArea.TO_COME), "nothing says what will show");
		assertEquals(window.toCome(), textOf(panel, AnswerArea.TO_COME));
	}
}

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

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.component;
import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
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
 * draft 06, #820): the windows of verify, balance, mine, node, sweep and synchronise
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
	 */
	record Window(String window, Supplier<JPanel> panel)
	{
		@Override
		public String toString()
		{
			return window;
		}
	}

	static Stream<Arguments> windows()
	{
		return Stream.of(Arguments.of(new Window("Check my chain", LethenonChainPanel::new)),
			Arguments.of(new Window("Show my balance", LethenonBalancePanel::new)),
			Arguments.of(new Window("Mine the pun", LethenonMinePanel::new)),
			Arguments.of(new Window("Start my node", LethenonNodePanel::new)),
			Arguments.of(new Window("Sweep onto my account", LethenonSweepPanel::new)),
			Arguments.of(new Window("Bring my chain up to date", LethenonSyncPanel::new)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("windows")
	void beforeTheFirstAction_theAnswerShowsEmpty(final Window window)
	{
		JPanel panel = window.panel().get();

		assertTrue(component(panel, "txtAnswer", Component.class).isVisible(),
			"the answer is hidden before the first action (pinned, #820)");
		assertEquals("", textOf(panel, "txtAnswer"));
	}
}

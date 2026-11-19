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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JMenuItem;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.mystic.crypt.ForkedJvm;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use case "choose a look in View > Look and Feel and find it again after a restart".
 * The restart is a JVM of its own on the same {@code user.home}, so that no {@code UIManager} or
 * other JVM-global state of this test JVM can answer for the application
 */
class LookAndFeelMenuChoiceSurvivesRestartUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@ParameterizedTest(name = "{0}")
	@DisplayName("the look chosen in the View menu is the look the next start applies")
	@CsvSource({ "FlatLaf Dark, com.formdev.flatlaf.FlatDarkLaf",
			"Metal, javax.swing.plaf.metal.MetalLookAndFeel" })
	void theLookChosenInTheViewMenu_isTheLookTheNextStartApplies(final String menuText,
		final String expectedClass) throws Exception
	{
		File databaseFile = new File(tempHome, "look-restart-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		JMenuItem item = robot.finder()
			.find(new GenericTypeMatcher<JMenuItem>(JMenuItem.class, false)
			{
				@Override
				protected boolean isMatching(final JMenuItem candidate)
				{
					return menuText.equals(candidate.getText());
				}
			});
		GuiActionRunner.execute(() -> item.doClick());
		robot.waitForIdle();

		String applied = lookAndFeelAppliedAtStartInAJvmOfItsOwn();

		assertEquals(expectedClass, applied,
			"the next start did not apply the look chosen in the menu");
	}

	private String lookAndFeelAppliedAtStartInAJvmOfItsOwn() throws Exception
	{
		List<String> command = new ArrayList<>();
		command.add(System.getProperty("java.home") + "/bin/java");
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		command.add("-Duser.home=" + tempHome.getAbsolutePath());
		command.addAll(ForkedJvm.localeOptions());
		command.add(StartedLookAndFeelProbe.class.getName());
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		if (!process.waitFor(120, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IllegalStateException("the probe did not finish: " + output);
		}
		return output.lines().filter(line -> line.startsWith(StartedLookAndFeelProbe.OUTPUT_PREFIX))
			.map(line -> line.substring(StartedLookAndFeelProbe.OUTPUT_PREFIX.length())).findFirst()
			.orElseThrow(() -> new IllegalStateException("the probe printed no look: " + output));
	}
}

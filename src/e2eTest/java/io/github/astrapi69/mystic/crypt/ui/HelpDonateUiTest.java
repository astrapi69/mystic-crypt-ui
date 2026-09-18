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
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use case "Help -> Donate": the library's own default was one hardcoded, dead
 * SourceForge URL unrelated to this project (#113) - this proves the real menu click reaches the
 * replacement, a popup of several real, currently active donation targets, not only that the data
 * behind it is correct in isolation.
 */
class HelpDonateUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void donateShowsEveryRealTarget() throws IOException
	{
		File databaseFile = new File(tempHome, "donate-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		application.clickDonateMenuItem();
		robot.waitForIdle();

		JPopupMenu popup = (JPopupMenu)robot.finder()
			.findAll(component -> component instanceof JPopupMenu && component.isShowing()).stream()
			.findFirst()
			.orElseThrow(() -> new AssertionError("Donate must show a popup of targets"));

		Set<String> labels = GuiActionRunner.execute(
			() -> java.util.Arrays.stream(popup.getComponents()).filter(JMenuItem.class::isInstance)
				.map(component -> ((JMenuItem)component).getText()).collect(Collectors.toSet()));

		assertEquals(Set.of("GitHub Sponsors", "Liberapay", "Ko-fi", "PayPal"), labels);

		GuiActionRunner.execute(() -> popup.setVisible(false));
		robot.waitForIdle();
	}
}

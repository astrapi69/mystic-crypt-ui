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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwWithApplicationFilePanel;
import io.github.astrapi69.mystic.crypt.panel.signin.NewMasterPwFilePanel;

/**
 * TC-0140: the Help button of each sign-in panel opens a wiki page that exists in
 * {@code docs/wiki/} (#868). The panels need the application frame, so the application is started
 * first; the panels are then built on the event thread. The browser and the network are the two
 * seams each panel carries ({@code isHelpPageReachable}, {@code displayHelpPage}); the test records
 * the link instead of opening it, and clicks the real button.
 */
class SignInHelpButtonsUiTest extends AbstractUiTest
{

	private static final String WIKI = "https://github.com/astrapi69/mystic-crypt-ui/wiki/";

	/** A panel that records the pages its Help button would open, and finds them reachable */
	private static final class Recorded
	{
		final List<String> opened = new ArrayList<>();
	}

	private static JButton helpButton(final Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JButton button && "btnHelp".equals(button.getName()))
			{
				return button;
			}
			if (child instanceof Container nested)
			{
				JButton found = helpButton(nested);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static JComponent openPanel(final Recorded recorded)
	{
		return new MasterPwWithApplicationFilePanel(
			BaseModel.of(MasterPwFileModelBean.builder().minPasswordLength(6).build()))
		{
			@Override
			protected boolean isHelpPageReachable(final String helpLink)
			{
				return true;
			}

			@Override
			protected void displayHelpPage(final String helpLink)
			{
				recorded.opened.add(helpLink);
			}
		};
	}

	private static JComponent createPanel(final Recorded recorded)
	{
		return new NewMasterPwFilePanel()
		{
			@Override
			protected boolean isHelpPageReachable(final String helpLink)
			{
				return true;
			}

			@Override
			protected void displayHelpPage(final String helpLink)
			{
				recorded.opened.add(helpLink);
			}
		};
	}

	private static void assertTheHelpButtonOpens(final JComponent panel, final Recorded recorded,
		final String expectedPage)
	{
		JButton help = helpButton(panel);
		assertNotNull(help, "the panel has a Help button named btnHelp");
		GuiActionRunner.execute(() -> {
			help.doClick();
		});

		assertEquals(List.of(WIKI + expectedPage), recorded.opened,
			"one click on Help opens exactly the page of the plan's TC-0140");
		Path source = Path.of("docs", "wiki", expectedPage + ".md");
		assertTrue(Files.isRegularFile(source),
			"the page the button opens is in the wiki's source: " + source);
	}

	@Test
	@DisplayName("the Help button of the sign-in to an existing database opens its wiki page, which exists")
	void signInHelpButton_opensTheOpenExistingPage_whenClicked()
	{
		launchApplication();
		Recorded recorded = new Recorded();

		assertTheHelpButtonOpens(GuiActionRunner.execute(() -> openPanel(recorded)), recorded,
			"open-existing-mystic-crypt-database");
	}

	@Test
	@DisplayName("the Help button of the creation of a new database opens its wiki page, which exists")
	void createHelpButton_opensTheCreateNewPage_whenClicked()
	{
		launchApplication();
		Recorded recorded = new Recorded();

		assertTheHelpButtonOpens(GuiActionRunner.execute(() -> createPanel(recorded)), recorded,
			"create-new-mystic-crypt-database");
	}
}

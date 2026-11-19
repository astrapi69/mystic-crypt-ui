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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.assertj.core.api.Assertions;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwWithApplicationFilePanel;
import io.github.astrapi69.mystic.crypt.panel.signin.NewMasterPwFilePanel;

/**
 * The fallback dialog a Help button shows when the wiki cannot be reached takes its title and its
 * text from the message bundle, and the create panel's dialog is titled for creating a database,
 * not for signing in. The panels need the application frame, so the application is started first.
 */
class HelpFallbackDialogsUiTest extends AbstractUiTest
{

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

	private DialogFixture clickHelpAndFindTheDialog(final JComponent panel, final String title)
	{
		JButton help = helpButton(panel);
		SwingUtilities.invokeLater(help::doClick);
		return WindowFinder.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
		{
			@Override
			protected boolean isMatching(final Dialog dialog)
			{
				return title.equals(dialog.getTitle()) && dialog.isShowing();
			}
		}).withTimeout(15, TimeUnit.SECONDS).using(robot);
	}

	@Test
	@DisplayName("the fallback of the sign-in Help button is titled from the bundle, for signing in")
	void signInFallback_isTitledFromTheBundle_whenTheWikiIsUnreachable()
	{
		launchApplication();
		JComponent panel = GuiActionRunner.execute(() -> new MasterPwWithApplicationFilePanel(
			BaseModel.of(MasterPwFileModelBean.builder().minPasswordLength(6).build()))
		{
			@Override
			protected boolean isHelpPageReachable(final String helpLink)
			{
				return false;
			}
		});
		String title = Messages.getString("signin.help.open.dialog.title", "");

		DialogFixture dialog = clickHelpAndFindTheDialog(panel, title);

		Assertions.assertThat(title).isNotEmpty();
		dialog.close();
	}

	@Test
	@DisplayName("the fallback of the create Help button is titled for creating, not for signing in")
	void createFallback_isTitledForCreating_whenTheWikiIsUnreachable()
	{
		launchApplication();
		JComponent panel = GuiActionRunner.execute(() -> new NewMasterPwFilePanel()
		{
			@Override
			protected boolean isHelpPageReachable(final String helpLink)
			{
				return false;
			}
		});
		String title = Messages.getString("signin.help.create.dialog.title", "");
		assertFalse(title.isEmpty());
		assertFalse(title.toLowerCase().contains("sign in"), title);

		DialogFixture dialog = clickHelpAndFindTheDialog(panel, title);

		assertEquals(title, dialog.target().getTitle());
		dialog.close();
	}
}

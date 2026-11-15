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
import java.util.Optional;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.MenuElement;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A plugin that needs a vault is offered once a vault is open, whichever way it was opened: signing
 * in to an existing database, or creating a new one with "New..." in the sign-in dialog and signing
 * in to it. The key generation plugin stands for every plugin that does not declare itself usable
 * without a vault; its submenu "Key Generation" is greyed out before the sign-in
 * ({@code PublicAccess})
 */
class NewDatabaseOffersThePluginsUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String KEY_GENERATION = "Key Generation";

	@Test
	@DisplayName("signed in to an existing database, the plugins that need a vault are offered")
	void anExistingDatabase_offersThePluginsThatNeedAVault() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);
		File databaseFile = new File(tempHome, "existing.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		assertEquals(Optional.of(true), pluginSubmenuEnabled(KEY_GENERATION));
	}

	@Test
	@DisplayName("signed in to a database New... just created, the plugins that need a vault are offered")
	void aNewDatabase_offersThePluginsThatNeedAVault() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);
		File databaseFile = new File(tempHome, "new.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(databaseFile).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).requireOkEnabled().okAndAwaitClose();
		signIn.requireOkEnabled().okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();

		assertEquals(Optional.of(true), pluginSubmenuEnabled(KEY_GENERATION));
	}

	@Test
	@DisplayName("signed in to a database New... just created with a password and a key file, the plugins that need a vault are offered")
	void aNewDatabaseWithAKeyFile_offersThePluginsThatNeedAVault() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);
		File databaseFile = new File(tempHome, "new-with-key.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(databaseFile).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile()
			.createKeyFile("new-with-key.pem").requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();

		assertEquals(Optional.of(true), pluginSubmenuEnabled(KEY_GENERATION));
	}

	/**
	 * Whether the submenu of the "Plugins" menu with the given text is enabled
	 *
	 * @param text
	 *            the submenu's text
	 * @return empty when there is no such submenu
	 */
	private static Optional<Boolean> pluginSubmenuEnabled(final String text)
	{
		return GuiActionRunner.execute(() -> {
			JMenuBar menubar = MysticCryptApplicationFrame.getInstance().getJMenuBar();
			for (MenuElement top : menubar.getSubElements())
			{
				if (top.getComponent()instanceof JMenu menu
					&& MenuId.PLUGINS.propertiesKey().equals(menu.getName()))
				{
					for (int index = 0; index < menu.getItemCount(); index++)
					{
						if (menu.getItem(index)instanceof JMenu submenu
							&& text.equals(submenu.getText()))
						{
							return Optional.of(submenu.isEnabled());
						}
					}
				}
			}
			return Optional.<Boolean> empty();
		});
	}
}

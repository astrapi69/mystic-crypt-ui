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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.NewApplicationFileAction;

/**
 * Creating a vault while another one is locked used to sign the workspace back in: the flow ends in
 * setting the signed-in flag and rebuilding the menu, and nothing in it asked whether the workspace
 * it lands in was locked. The locked vault's master password was never entered (#270).
 * <p>
 * The refusal has since widened to every open vault, locked or not (#279), so the decision no
 * longer reads the signed-in state at all. This class keeps measuring the locked case, because that
 * is the one where the refusal also protects a password nobody entered.
 * <p>
 * The action is fired directly rather than through the toolbar button, on purpose. The button is
 * disabled in this state (#269), which is the first line; this test is about the second one, the
 * refusal in the action itself, which holds for every other way the action can be reached.
 */
class LockRefusesANewVaultUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/**
	 * The refusal covers every open vault since #279, not only a locked one, so it names the open
	 * database rather than the lock. The locked case is still the one measured here
	 */
	private static final String REFUSAL_TITLE = "A database is already open";

	@Test
	@DisplayName("a locked workspace refuses a new vault, and says why")
	void aLockedWorkspaceRefusesANewVault() throws IOException
	{
		File databaseFile = new File(tempHome, "locked-refuses-new.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		application.lockWorkspace();

		SwingUtilities.invokeLater(() -> new NewApplicationFileAction("New Application")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		Pause.pause(new Condition("the refusal is on screen")
		{
			@Override
			public boolean test()
			{
				return dialogTitled(REFUSAL_TITLE) != null;
			}
		}, TimeUnit.SECONDS.toMillis(15));

		assertTrue(dialogTitled(REFUSAL_TITLE) != null,
			"the refusal has to be visible - an action that silently does nothing is the defect "
				+ "this application already had in Lock workspace");
		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"and the workspace stays locked: creating a vault must not be a way past the unlock "
				+ "prompt");
		assertTrue(dialogTitled("Specify the database file to save") == null,
			"no file chooser either - the refusal comes before anything is created on disk");
	}

	@Test
	@DisplayName("the button is disabled while locked and enabled with nothing open")
	void theButtonFollowsTheSameDecisionAsTheAction() throws IOException
	{
		File databaseFile = new File(tempHome, "button-state-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		application.lockWorkspace();

		assertFalse(newDatabaseButtonEnabled(),
			"first line: with a vault locked the button must be disabled. The inventory test "
				+ "cannot say this - the entry is on its list because it MAY be offered, and a "
				+ "subset check is blind to when");
	}

	@Test
	@DisplayName("with nothing open the button stays available, or there is no way to a first vault")
	void theButtonStaysAvailableWithNothingOpen()
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();

		assertTrue(newDatabaseButtonEnabled(),
			"creating a vault is the one way into one from this state - refusing it here would "
				+ "close the door this fix must not touch");
	}

	private boolean newDatabaseButtonEnabled()
	{
		return GuiActionRunner.execute(() -> robot.finder()
			.findByName(MenuId.NEW_DATABASE_TOOL_BAR.propertiesKey(), JButton.class, false)
			.isEnabled());
	}

	private static JDialog dialogTitled(final String title)
	{
		return GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing()
					&& title.equals(dialog.getTitle()))
				{
					return dialog;
				}
			}
			return null;
		});
	}
}

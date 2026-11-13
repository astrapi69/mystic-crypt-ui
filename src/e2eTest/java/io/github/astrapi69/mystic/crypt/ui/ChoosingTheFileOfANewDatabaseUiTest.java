/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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

import java.awt.Dialog;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.NewApplicationFileAction;

/**
 * Choosing the file of a new database creates nothing on disk (#952).
 * <p>
 * Measured on develop {@code 62a559f5}: each of the three ways to the file of a new database
 * created it at once, with 0 bytes, through {@code FileFactory.newFile} - New... in the sign-in
 * dialog, Browse... in "Create your master key" and the menu's New. Cancel then left the empty file
 * behind, and a later sign-in offered it as a database it could not open. The database is written
 * by OK in "Create your master key", which creates the file itself.
 * <p>
 * Now none of the three creates anything, Cancel leaves the disk as it was, and OK still writes the
 * database to the file chosen.
 */
class ChoosingTheFileOfANewDatabaseUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** The title of the dialog in which a new database gets its master key */
	private static final String CREATE_MASTER_KEY = "Create your master key";

	@Test
	@DisplayName("New... in the sign-in dialog creates nothing, and Cancel leaves nothing (#952)")
	void newInTheSignInDialog() throws Exception
	{
		File chosen = new File(tempHome, "chosen-in-new.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(chosen);

		assertNothingThere(chosen, "when the path is chosen");
		createMasterKey.cancelAndAwaitClose();
		assertNothingThere(chosen, "after Cancel");
	}

	@Test
	@DisplayName("Browse... in Create your master key creates nothing, and Cancel leaves nothing (#952)")
	void browseInCreateYourMasterKey() throws Exception
	{
		File first = new File(tempHome, "first-choice.mcrdb");
		File second = new File(tempHome, "second-choice.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(first);
		createMasterKey.browseApplicationFile(second)
			.requireApplicationFile(second.getAbsolutePath());

		assertNothingThere(second, "when Browse... chose it");
		createMasterKey.cancelAndAwaitClose();
		assertNothingThere(first, "for the first choice, after Cancel");
		assertNothingThere(second, "for the second choice, after Cancel");
	}

	@Test
	@DisplayName("New in the menu creates nothing, and Cancel leaves nothing (#952)")
	void newInTheMenu() throws Exception
	{
		File chosen = new File(tempHome, "chosen-in-the-menu.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();
		SwingUtilities.invokeLater(() -> new NewApplicationFileAction("New").actionPerformed(
			new java.awt.event.ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(15, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(chosen);
			fileChooser.approveSelection();
		});
		CreateMasterKeySteps createMasterKey = awaitCreateMasterKey();

		assertNothingThere(chosen, "when the path is chosen");
		createMasterKey.cancelAndAwaitClose();
		assertNothingThere(chosen, "after Cancel");
	}

	@Test
	@DisplayName("a path chosen that is not there yet: OK writes the database to it (#952)")
	void okWritesTheDatabaseToTheChosenPath() throws Exception
	{
		File chosen = new File(tempHome, "written-by-ok.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(chosen);
		assertNothingThere(chosen, "when the path is chosen");
		createMasterKey.checkMasterPassword().typeMasterPasswordWithRepeat(MASTER_PASSWORD)
			.requireOkEnabled().okAndAwaitClose();

		assertTrue(chosen.length() > 0,
			"OK creates the file and writes the database to it: " + chosen);
	}

	private static void assertNothingThere(final File file, final String when)
	{
		assertFalse(file.exists(), "nothing is created " + when + ": " + file);
	}

	private CreateMasterKeySteps awaitCreateMasterKey()
	{
		DialogFixture createMasterKeyDialog = WindowFinder
			.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
			{
				@Override
				protected boolean isMatching(final Dialog candidate)
				{
					return CREATE_MASTER_KEY.equals(candidate.getTitle()) && candidate.isShowing();
				}
			}).withTimeout(10, TimeUnit.SECONDS).using(robot);
		return new CreateMasterKeySteps(robot, createMasterKeyDialog);
	}
}

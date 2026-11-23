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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.OpenExistingDatabaseAction;
import io.github.astrapi69.mystic.crypt.lock.IdleLockDecision;
import io.github.astrapi69.mystic.crypt.lock.IdleLockWatchdog;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * A locked vault does not stay decrypted forever (#242).
 * <p>
 * Locking keeps the decrypted content on purpose, so unlocking can rebuild the view without paying
 * 600,000 PBKDF2 iterations again (#237), and for a long time nothing bounded that: a vault locked
 * at five o'clock was still decrypted in the process the next morning. The idle watchdog is the
 * bound, and this is the test of it.
 * <p>
 * What locking DOES take away is the key material - the master password, its repeat, the private
 * key. That half is {@link LockingErasesTheKeyMaterialUiTest}; the split between the two is
 * recorded in {@code docs/decisions/}.
 */
class LockedVaultClosesItselfUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_TITLE = "still-in-memory-while-locked";

	@Test
	@DisplayName("the decrypted vault is still in memory while locked, and gone after the close")
	void aLockedVaultIsClosedOnceItHasBeenLockedLongEnough() throws IOException
	{
		File databaseFile = new File(tempHome, "locked-closes-itself.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		application.saveDatabase();
		application.lockWorkspace();
		application.cancelUnlock();

		assertTrue(theModelStillHolds(ENTRY_TITLE),
			"precondition, and the whole reason this issue exists: locking hides the vault and "
				+ "keeps it decrypted");

		closeTheLockedVaultThroughTheWatchdog();

		assertNull(GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.getModelObject().getMasterPwFileModelBean()),
			"the vault is closed, not merely hidden again");
		assertFalse(theModelStillHolds(ENTRY_TITLE),
			"and its entries are out of the model. This is the assertion the issue asked for - a "
				+ "memory fact, not a screen fact");
		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"closing a locked vault must not sign the application back in");
		assertTrue(databaseFile.exists(), "and the file it came from is untouched");
	}

	@Test
	@DisplayName("locking keeps pending changes instead of writing them, and the close waits")
	void lockingKeepsPendingChanges_soTheTimedCloseLeavesThemAlone() throws IOException
	{
		File databaseFile = new File(tempHome, "locking-keeps-changes.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		long lengthBeforeLocking = databaseFile.length();

		// deliberately NOT saved: what locking does with it is the question
		application.lockWorkspace();
		application.cancelUnlock();

		assertTrue(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isDirty()),
			"locking keeps the change rather than committing it: a timer does not decide to write "
				+ "in a password manager (#304)");
		assertEquals(lengthBeforeLocking, databaseFile.length(), "and the file is untouched");

		closeTheLockedVaultThroughTheWatchdog();

		assertTrue(theModelStillHolds(ENTRY_TITLE),
			"the timed close leaves a dirty vault alone - it stays locked until somebody unlocks "
				+ "it and decides, which is the other half of the same rule");
	}

	/**
	 * The test plan's TC-0046 through the frame's own watchdog: the timeout is the setting "Close a
	 * locked database after minutes", read from the settings on every check, and the clock is the
	 * system clock with the timer's own ten second rhythm. The database is opened with a password
	 * AND a key file, and opening it again after the close needs both.
	 */
	@Test
	@DisplayName("the frame's own watchdog closes a locked vault after the minutes of the setting, and opening it again needs the password and the key file")
	void theFramesOwnWatchdogClosesALockedVaultAfterTheSettingsMinutes() throws Exception
	{
		String password = TestPasswords.throwaway();
		File keyFile = new File(tempHome, "closes-itself.pem");
		File databaseFile = new File(tempHome, "closes-itself-combined.mcrdb");
		createDatabaseFileWithKeyFileHeadless(databaseFile, password, keyFile);
		SignInDialogSteps signIn = launchApplication();
		signIn.checkMasterPassword().typeMasterPassword(password).checkKeyFile()
			.browseKeyFile(keyFile).browseApplicationFile(databaseFile).requireOkEnabled()
			.okAndAwaitSignIn();
		ApplicationSteps application = new ApplicationSteps(robot).awaitSignedIn();
		changeTheSettingsOfTheRunningApplication(settings -> {
			settings.setAutoLockMinutes(0);
			settings.setCloseLockedAfterMinutes(1);
		});
		application.lockWorkspace();
		application.cancelUnlock();
		assertTrue(theVaultIsOpen(), "the precondition: locked, and the vault is still open");

		Pause.pause(new Condition("the frame's own watchdog closes the locked vault")
		{
			@Override
			public boolean test()
			{
				return !theVaultIsOpen();
			}
		}, TimeUnit.SECONDS.toMillis(100));

		assertFalse(signedInNow(), "closing a locked vault must not sign the application in");
		assertTrue(databaseFile.exists(), "and the file it came from is untouched");
		SignInDialogSteps again = openTheSignInDialogOfTheRunningApplication();
		assertEquals(keyFile.getAbsolutePath(), again.selectedKeyFile(),
			"after the close the dialog offers the key file the database was opened with (#997)");
		assertTrue(again.keyFileIsChecked(), "and its checkbox is on");
		again.browseApplicationFile(databaseFile);
		assertEquals(keyFile.getAbsolutePath(), again.selectedKeyFile(),
			"choosing the database offers its key file again, in both directions (#997)");
		assertTrue(again.keyFileIsChecked() && again.masterPasswordIsChecked(),
			"with both factors, as the database was opened");
		again.requireOkDisabled();
		again.uncheckKeyFile().checkMasterPassword().typeMasterPassword(password).requireOkEnabled()
			.clickOk();
		awaitAndDismissAnErrorDialog();
		assertFalse(signedInNow(), "the password alone does not open it again");
		again.uncheckMasterPassword().checkKeyFile().browseKeyFile(keyFile).requireOkEnabled()
			.clickOk();
		awaitAndDismissAnErrorDialog();
		assertFalse(signedInNow(), "the key file alone does not open it either");
		File wrongKeyFile = new File(tempHome, "closes-itself-wrong.pem");
		createDatabaseFileWithKeyFileHeadless(new File(tempHome, "closes-itself-other.mcrdb"),
			password, wrongKeyFile);
		again.checkMasterPassword().typeMasterPassword(password).checkKeyFile()
			.browseKeyFile(wrongKeyFile).requireOkEnabled().clickOk();
		awaitAndDismissAnErrorDialog();
		assertFalse(signedInNow(),
			"the password with the key file of another database does not " + "open it either");
		again.browseKeyFile(keyFile).typeMasterPassword(password).okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();
		assertTrue(signedInNow(), "the password and the key file do");
	}

	/**
	 * With the setting "Remember key files" off, nothing is pre-filled after the close: the
	 * database is offered, the key file is not, and the open needs the key file chosen again (#997)
	 */
	@Test
	@DisplayName("with 'Remember key files' off, the dialog after the close pre-fills no key file")
	void withRememberKeyFilesOff_theDialogAfterTheCloseOffersNoKeyFile() throws Exception
	{
		String password = TestPasswords.throwaway();
		File keyFile = new File(tempHome, "not-remembered.pem");
		File databaseFile = new File(tempHome, "not-remembered.mcrdb");
		createDatabaseFileWithKeyFileHeadless(databaseFile, password, keyFile);
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		configurationDirectory.mkdirs();
		MysticCryptSettings notRemembering = new MysticCryptSettings();
		notRemembering.setRememberKeyFiles(false);
		notRemembering.save(configurationDirectory);
		SignInDialogSteps signIn = launchApplication();
		signIn.checkMasterPassword().typeMasterPassword(password).checkKeyFile()
			.browseKeyFile(keyFile).browseApplicationFile(databaseFile).requireOkEnabled()
			.okAndAwaitSignIn();
		ApplicationSteps application = new ApplicationSteps(robot).awaitSignedIn();
		changeTheSettingsOfTheRunningApplication(settings -> {
			settings.setAutoLockMinutes(0);
			settings.setCloseLockedAfterMinutes(1);
		});
		application.lockWorkspace();
		application.cancelUnlock();
		Pause.pause(new Condition("the frame's own watchdog closes the locked vault")
		{
			@Override
			public boolean test()
			{
				return !theVaultIsOpen();
			}
		}, TimeUnit.SECONDS.toMillis(100));

		SignInDialogSteps again = openTheSignInDialogOfTheRunningApplication();
		String offeredOnOpen = again.selectedKeyFile();
		assertTrue(offeredOnOpen == null || offeredOnOpen.isEmpty(),
			"no key file is pre-filled when the dialog opens either: " + offeredOnOpen);
		assertTrue(again.keyFileIsChecked(),
			"the factor the database was opened with stays offered");
		again.browseApplicationFile(databaseFile);

		String offered = again.selectedKeyFile();
		assertTrue(offered == null || offered.isEmpty(),
			"no key file is pre-filled with the setting off: " + offered);
		again.checkMasterPassword().typeMasterPassword(password).uncheckKeyFile().clickOk();
		awaitAndDismissAnErrorDialog();
		assertFalse(signedInNow(), "the password alone does not open a database with a key file");
	}

	private static boolean theVaultIsOpen()
	{
		return GuiActionRunner.execute(
			() -> MysticCryptApplicationFrame.getInstance().asLockableWorkspace().aVaultIsOpen());
	}

	/**
	 * Changes the settings of the running application the way the settings dialog does: load the
	 * file of the frame's own configuration directory, change it, save it there. The frame's
	 * watchdog reads it on its next check, so nothing a test writes before the launch can stand in
	 * for it
	 */
	private static void changeTheSettingsOfTheRunningApplication(
		final java.util.function.Consumer<MysticCryptSettings> change)
	{
		File configurationDirectory = GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getConfigurationDirectory());
		MysticCryptSettings settings = MysticCryptSettings.load(configurationDirectory);
		change.accept(settings);
		settings.save(configurationDirectory);
	}

	private static boolean signedInNow()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}

	private void awaitAndDismissAnErrorDialog()
	{
		Pause.pause(new Condition("the refused open is reported")
		{
			@Override
			public boolean test()
			{
				return anErrorPane() != null;
			}
		}, TimeUnit.SECONDS.toMillis(30));
		JOptionPane pane = anErrorPane();
		GuiActionRunner.execute(() -> pane.setValue(JOptionPane.OK_OPTION));
		robot.waitForIdle();
	}

	private static JOptionPane anErrorPane()
	{
		return GuiActionRunner.execute(() -> {
			for (java.awt.Window window : java.awt.Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing())
				{
					for (java.awt.Component child : dialog.getContentPane().getComponents())
					{
						if (child instanceof JOptionPane pane
							&& pane.getMessageType() == JOptionPane.ERROR_MESSAGE)
						{
							return pane;
						}
					}
				}
			}
			return null;
		});
	}

	/**
	 * Opens the sign-in dialog of the application that is already running, through the entry #266
	 * added. Not a second {@code signInWithExistingDatabase}: that launches another application,
	 * and this test is about the one whose vault was just closed
	 *
	 * @return steps for the sign-in dialog
	 */
	private SignInDialogSteps openTheSignInDialogOfTheRunningApplication()
	{
		SwingUtilities.invokeLater(() -> new OpenExistingDatabaseAction("Open Database File")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		return awaitSignInDialog();
	}

	/**
	 * Drives the real close against the real application frame, over a clock this test moves.
	 * <p>
	 * A watchdog of its own rather than the frame's: the frame's runs on the system clock, and
	 * waiting fifteen real minutes measures nothing the unit tests do not already measure. What
	 * this adds is the other half - that the close it decides on actually empties the model of this
	 * running application. It goes through the frame's own workspace adapter, so nothing here is a
	 * stand-in for the code under test
	 */
	private void closeTheLockedVaultThroughTheWatchdog()
	{
		AtomicLong clock = new AtomicLong();
		IdleLockWatchdog watchdog = new IdleLockWatchdog(
			MysticCryptApplicationFrame.getInstance().asLockableWorkspace(),
			() -> IdleLockDecision.OFF, () -> 15, clock::get);
		GuiActionRunner.execute(() -> {
			// the first check starts the locked clock, the second one finds it expired
			watchdog.checkNow();
			clock.set(TimeUnit.MINUTES.toMillis(15));
			watchdog.checkNow();
		});
		robot.waitForIdle();
	}

	/**
	 * Whether the application model still holds an entry with the given title, in either of the two
	 * places an entry lives - the map of a node's entries and the tree the nodes hang in
	 *
	 * @param title
	 *            the entry's title
	 * @return true if it is still in memory
	 */
	private static boolean theModelStillHolds(final String title)
	{
		return GuiActionRunner.execute(() -> {
			var model = MysticCryptApplicationFrame.getInstance().getModelObject();
			if (model.getDataOfNodes() != null && model.getDataOfNodes().values().stream()
				.filter(entries -> entries != null).flatMap(List::stream).anyMatch(
					entry -> entry != null && Arrays.equals(title.toCharArray(), entry.getTitle())))
			{
				return true;
			}
			if (model.getRootTreeAsMap() == null)
			{
				return false;
			}
			for (TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : model
				.getRootTreeAsMap().values())
			{
				if (node == null || node.getValue() == null
					|| node.getValue().getDefaultContent() == null)
				{
					continue;
				}
				for (MysticCryptEntryModelBean entry : node.getValue().getDefaultContent())
				{
					if (entry != null && Arrays.equals(title.toCharArray(), entry.getTitle()))
					{
						return true;
					}
				}
			}
			return false;
		});
	}
}

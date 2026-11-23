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

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;

/**
 * The setting "Remember key files" (#997): switched off in the settings dialog, no key file path
 * stays on disk, and the sign-in dialog after a restart offers none, while it still offers the
 * database signed in to last.
 */
class RememberKeyFilesSettingUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("switching the setting off removes the key file paths at once, and a restart offers none")
	void switchingRememberKeyFilesOff_removesTheKeyFilePaths_andARestartOffersNone()
		throws Exception
	{
		File database = new File(tempHome, "remember.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(database).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile()
			.createKeyFile("remember-key.pem").requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		ApplicationSteps application = new ApplicationSteps(robot);
		application.awaitSignedIn();
		File configurationDirectory = MysticCryptApplicationFrame.getInstance()
			.getConfigurationDirectory();
		File remembered = new File(configurationDirectory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		assertTrue(Files.readString(remembered.toPath()).contains("remember-key.pem"),
			"the premise: by default the key file is remembered");

		File keePassMemo = new File(configurationDirectory,
			io.github.astrapi69.mystic.crypt.keepass.MemoizedKeePassModelBean.JSON_FILENAME);
		io.github.astrapi69.mystic.crypt.keepass.MemoizedKeePassModelBean.builder()
			.lastImportFilePath("/vaults/import.kdbx").lastImportKeyFilePath("/keys/import.key")
			.lastExportFilePath("/vaults/export.kdbx").lastExportKeyFilePath("/keys/export.key")
			.build().save(configurationDirectory);
		assertTrue(Files.readString(keePassMemo.toPath()).contains("import.key"), "the premise");

		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "General");
		assertTrue(
			GuiActionRunner
				.execute(() -> settings.checkBox("chkRememberKeyFiles").target().isSelected()),
			"on by default, as before");
		settings.checkBox("chkRememberKeyFiles").uncheck();
		settings.button("btnCloseSettings").click();
		robot.waitForIdle();

		assertFalse(MysticCryptSettings.load(configurationDirectory).isRememberKeyFiles());
		assertFalse(Files.readString(remembered.toPath()).contains("remember-key.pem"),
			"no key file path stays in the remembered sign-in");
		String keePassText = Files.readString(keePassMemo.toPath());
		assertFalse(keePassText.contains(".key"),
			"nor in the remembered KeePass paths: " + keePassText);
		assertTrue(keePassText.contains("/vaults/import.kdbx"), "whose file paths stay");
		shutdownApplication();

		SignInDialogSteps afterRestart = launchApplication();
		String offered = afterRestart.selectedKeyFile();
		assertTrue(offered == null || offered.isEmpty(),
			"no key file is offered after a restart: " + offered);
		afterRestart.requireSelectedApplicationFile(database.getAbsolutePath());
	}

	@Test
	@DisplayName("with the setting off from the start, a sign-in writes no key file path and keeps the factors")
	void signingInWithRememberKeyFilesOff_writesNoKeyFilePath() throws Exception
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		configurationDirectory.mkdirs();
		MysticCryptSettings notRemembering = new MysticCryptSettings();
		notRemembering.setRememberKeyFiles(false);
		notRemembering.save(configurationDirectory);
		File database = new File(tempHome, "never-remembered.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(database).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile()
			.createKeyFile("never-remembered-key.pem").requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();

		File remembered = new File(configurationDirectory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		assertTrue(remembered.isFile(), "the premise: the sign-in wrote its file");
		String text = Files.readString(remembered.toPath());
		assertFalse(text.contains("never-remembered-key"), "no key file path in it: " + text);
		Map<?, ?> written = JsonStringToObjectExtensions.toObject(text, Map.class);
		assertEquals(Boolean.TRUE, written.get("withKeyFile"), "the factors stay remembered");
		assertTrue(text.contains("never-remembered.mcrdb"), "and so does the database");
	}

	@Test
	@DisplayName("with a settings file that cannot be read, a sign-in writes no key file path and tells the user why")
	void signingInWithAnUnreadableSettingsFile_writesNoKeyFilePath_andTellsWhy() throws Exception
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		configurationDirectory.mkdirs();
		Files.writeString(
			new File(configurationDirectory, MysticCryptSettings.JSON_FILENAME).toPath(),
			"{ not json");
		File database = new File(tempHome, "unreadable-settings.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(database).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile()
			.createKeyFile("unreadable-settings-key.pem").requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();

		File remembered = new File(configurationDirectory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		assertTrue(remembered.isFile(), "the premise: the sign-in wrote its file");
		String text = Files.readString(remembered.toPath());
		assertFalse(text.contains("unreadable-settings-key"), "no key file path in it: " + text);
		assertTrue(text.contains("unreadable-settings.mcrdb"), "the database is remembered");
		Pause.pause(new Condition("the notice that key files are not remembered")
		{
			@Override
			public boolean test()
			{
				return aWarningPane() != null;
			}
		}, TimeUnit.SECONDS.toMillis(30));
		JOptionPane pane = aWarningPane();
		String message = String.valueOf(pane.getMessage());
		assertTrue(message.contains(
			new File(configurationDirectory, MysticCryptSettings.JSON_FILENAME).getAbsolutePath()),
			"the notice names the settings file: " + message);
		GuiActionRunner.execute(() -> pane.setValue(JOptionPane.OK_OPTION));
		robot.waitForIdle();
	}

	@Test
	@DisplayName("closing the settings over a settings file that cannot be read leaves the remembered key file paths")
	void closingTheSettingsOverAnUnreadableSettingsFile_leavesTheKeyFilePaths() throws Exception
	{
		File database = new File(tempHome, "closing-unreadable.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(database).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile()
			.createKeyFile("closing-unreadable-key.pem").requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		ApplicationSteps application = new ApplicationSteps(robot);
		application.awaitSignedIn();
		File configurationDirectory = MysticCryptApplicationFrame.getInstance()
			.getConfigurationDirectory();
		File remembered = new File(configurationDirectory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		assertTrue(Files.readString(remembered.toPath()).contains("closing-unreadable-key"),
			"the premise: the sign-in remembered the key file");
		Files.writeString(
			new File(configurationDirectory, MysticCryptSettings.JSON_FILENAME).toPath(),
			"{ not json");

		DialogFixture settings = application.openSettingsDialog();
		SwingUtilities.invokeLater(() -> settings.button("btnCloseSettings").target().doClick());
		dismissTheNoticesUntilClosed(settings);

		assertTrue(Files.readString(remembered.toPath()).contains("closing-unreadable-key"),
			"the key file path stays in the remembered sign-in");
	}

	/**
	 * Waits until the dialog is closed, closing each warning on the way: the save keeps the
	 * unreadable settings file aside and says so
	 */
	private void dismissTheNoticesUntilClosed(final DialogFixture dialog)
	{
		Pause.pause(new Condition("the dialog is closed and no warning is left")
		{
			@Override
			public boolean test()
			{
				JOptionPane pane = aWarningPane();
				if (pane != null)
				{
					GuiActionRunner.execute(() -> pane.setValue(JOptionPane.OK_OPTION));
					return false;
				}
				return !GuiActionRunner.execute(() -> dialog.target().isShowing());
			}
		}, TimeUnit.SECONDS.toMillis(30));
		robot.waitForIdle();
	}

	@Test
	@DisplayName("switching the setting off while the remembered sign-in cannot be read warns that the paths remain")
	void switchingRememberKeyFilesOff_warns_whenTheRememberedSignInCannotBeRead() throws Exception
	{
		File database = new File(tempHome, "unreadable-memo.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(database).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile()
			.createKeyFile("unreadable-memo-key.pem").requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		ApplicationSteps application = new ApplicationSteps(robot);
		application.awaitSignedIn();
		File remembered = new File(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(),
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		Files.writeString(remembered.toPath(), "{\"selectedKeyFilePath\": \"/keys/a-key.pem\", ");

		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "General");
		settings.checkBox("chkRememberKeyFiles").uncheck();
		SwingUtilities.invokeLater(() -> settings.button("btnCloseSettings").target().doClick());

		Pause.pause(new Condition("the warning that the key file paths remain")
		{
			@Override
			public boolean test()
			{
				return aWarningPane() != null;
			}
		}, TimeUnit.SECONDS.toMillis(30));
		JOptionPane pane = aWarningPane();
		String message = String.valueOf(pane.getMessage());
		assertTrue(message.contains(remembered.getAbsolutePath()),
			"the warning names the file, its name and its directory: " + message);
		assertTrue(message.contains(io.github.astrapi69.mystic.crypt.Messages.getString(
			"settings.general.remember.key.files.reason.unreadable", "the file cannot be read")),
			"and says why: " + message);
		GuiActionRunner.execute(() -> pane.setValue(JOptionPane.OK_OPTION));
		robot.waitForIdle();
	}

	@Test
	@DisplayName("switching the setting off names the copies kept aside as unreadable, with their paths")
	void switchingRememberKeyFilesOff_namesTheKeptAsideCopies() throws Exception
	{
		File database = new File(tempHome, "kept-aside.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(database).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile()
			.createKeyFile("kept-aside-key.pem").requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		ApplicationSteps application = new ApplicationSteps(robot);
		application.awaitSignedIn();
		File configurationDirectory = MysticCryptApplicationFrame.getInstance()
			.getConfigurationDirectory();
		File signinCopy = new File(configurationDirectory,
			"memoizedSignin.json.unreadable-20261101T101010");
		File keePassCopy = new File(configurationDirectory,
			"memoizedKeePass.json.unreadable-20261102T111111");
		Files.writeString(signinCopy.toPath(), "{\"selectedKeyFilePath\": \"/keys/a-key.pem\", ");
		Files.writeString(keePassCopy.toPath(), "{\"lastImportKeyFilePath\": \"/keys/b.key\", ");

		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "General");
		settings.checkBox("chkRememberKeyFiles").uncheck();
		SwingUtilities.invokeLater(() -> settings.button("btnCloseSettings").target().doClick());

		Pause.pause(new Condition("the notice about the copies kept aside")
		{
			@Override
			public boolean test()
			{
				return aWarningPane() != null;
			}
		}, TimeUnit.SECONDS.toMillis(30));
		JOptionPane pane = aWarningPane();
		String message = String.valueOf(pane.getMessage());
		assertTrue(message.contains(signinCopy.getAbsolutePath()),
			"lists the sign-in copy: " + message);
		assertTrue(message.contains(keePassCopy.getAbsolutePath()),
			"lists the KeePass copy: " + message);
		assertFalse(
			message.contains(io.github.astrapi69.mystic.crypt.Messages
				.getString("settings.general.remember.key.files.not.removed", "not removed")),
			"and reports no failure, since the key file paths themselves were removed: " + message);
		GuiActionRunner.execute(() -> pane.setValue(JOptionPane.OK_OPTION));
		robot.waitForIdle();

		DialogFixture again = application.openSettingsDialog();
		SwingUtilities.invokeLater(() -> again.button("btnCloseSettings").target().doClick());
		// the notice is modal and opens before the dialog is disposed, so the wait ends on either
		Pause.pause(new Condition("the settings dialog is closed again, or a notice shows")
		{
			@Override
			public boolean test()
			{
				return !again.target().isShowing() || aWarningPane() != null;
			}
		}, TimeUnit.SECONDS.toMillis(15));
		JOptionPane leftOver = aWarningPane();
		if (leftOver != null)
		{
			GuiActionRunner.execute(() -> leftOver.setValue(JOptionPane.OK_OPTION));
		}
		assertNull(leftOver, "closing the dialog again with the setting still off shows no notice");
	}

	private static JOptionPane aWarningPane()
	{
		return GuiActionRunner.execute(() -> {
			for (java.awt.Window window : java.awt.Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing())
				{
					for (java.awt.Component child : dialog.getContentPane().getComponents())
					{
						if (child instanceof JOptionPane pane
							&& pane.getMessageType() == JOptionPane.WARNING_MESSAGE)
						{
							return pane;
						}
					}
				}
			}
			return null;
		});
	}
}

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
import java.awt.Window;
import java.io.File;
import java.nio.file.Files;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * What saving and opening leave at the path of a key file that is gone (#937), through the
 * application: the key file is on a drive that is no longer connected, so its directory is gone as
 * well.
 * <p>
 * PIN of the behaviour before the fix, green on it: the failed save and the failed open create the
 * directory and an empty file at the key file's path. The fix turns it.
 */
class AKeyFileThatIsGoneIsNeverCreatedUiTest extends AbstractUiTest
{

	private static final String ENTRY_TITLE = "written-once-the-key-file-is-back";

	@Test
	@DisplayName("PIN: a save while the key file is gone fails and leaves an empty file and its directory at its path")
	void aSaveWithoutTheKeyFile_createsItEmpty() throws Exception
	{
		String password = TestPasswords.throwaway();
		File drive = new File(tempHome, "drive");
		File keyFile = new File(drive, "database.pem");
		File database = databaseWithAKeyFile(password, keyFile);
		ApplicationSteps application = signIn(password, keyFile, database);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		Files.move(drive.toPath(), new File(tempHome, "drive-in-a-drawer").toPath());

		application.fireMenuItem(MenuId.SAVE_APPLICATION_FILE.propertiesKey());
		String failure = awaitAndCloseTheErrorDialog("the failed save is reported");

		assertNotNull(failure);
		assertTrue(GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isDirty()));
		assertTrue(drive.isDirectory(), "today the save creates the drive's directory");
		assertEquals(0L, keyFile.length(), "and an empty file where the key file was");
	}

	@Test
	@DisplayName("PIN: a key file that is gone between choosing it and OK is created empty at its path")
	void anOpenWithoutTheKeyFile_createsItEmpty() throws Exception
	{
		String password = TestPasswords.throwaway();
		File drive = new File(tempHome, "drive");
		File keyFile = new File(drive, "database.pem");
		File database = databaseWithAKeyFile(password, keyFile);
		SignInDialogSteps signIn = launchApplication();
		signIn.checkMasterPassword().typeMasterPassword(password).checkKeyFile()
			.browseKeyFile(keyFile).browseApplicationFile(database).requireOkEnabled();
		Files.move(drive.toPath(), new File(tempHome, "drive-in-a-drawer").toPath());

		signIn.clickOk();
		String failure = awaitAndCloseTheErrorDialog("the failed open is reported");

		assertNotNull(failure);
		assertTrue(drive.isDirectory(), "today the open creates the drive's directory");
		assertEquals(0L, keyFile.length(), "and an empty file where the key file was");
	}

	/**
	 * A database with a master password and a key file, made without the application, with the key
	 * file on a drive of its own
	 */
	private static File databaseWithAKeyFile(final String password, final File keyFile)
		throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		assertTrue(keyFile.getParentFile().mkdirs());
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), keyFile);
		File database = new File(keyFile.getParentFile().getParentFile(), "database.mcrdb");
		Files.createFile(database.toPath());
		ApplicationXmlFileFactory.newApplicationFileWithPasswordAndPrivateKey(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(database))
				.selectedApplicationFilePath(database.getAbsolutePath())
				.keyFileInfo(FileInfo.toFileInfo(keyFile)).masterPw(password.toCharArray())
				.withMasterPw(true).withKeyFile(true).minPasswordLength(6).build());
		return database;
	}

	private ApplicationSteps signIn(final String password, final File keyFile, final File database)
	{
		launchApplication().checkMasterPassword().typeMasterPassword(password).checkKeyFile()
			.browseKeyFile(keyFile).browseApplicationFile(database).requireOkEnabled()
			.okAndAwaitSignIn();
		return new ApplicationSteps(robot).awaitSignedIn();
	}

	/** Waits for an error message, closes it and returns its text */
	private String awaitAndCloseTheErrorDialog(final String description)
	{
		Pause.pause(new Condition(description)
		{
			@Override
			public boolean test()
			{
				return errorMessage() != null;
			}
		}, TimeUnit.SECONDS.toMillis(30));
		JOptionPane message = errorMessage();
		String text = GuiActionRunner.execute(() -> String.valueOf(message.getMessage()));
		GuiActionRunner.execute(() -> message.setValue(JOptionPane.OK_OPTION));
		robot.waitForIdle();
		return text;
	}

	private static JOptionPane errorMessage()
	{
		return GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing())
				{
					JOptionPane optionPane = optionPaneIn(dialog);
					if (optionPane != null
						&& optionPane.getMessageType() == JOptionPane.ERROR_MESSAGE)
					{
						return optionPane;
					}
				}
			}
			return null;
		});
	}

	private static JOptionPane optionPaneIn(final Container container)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JOptionPane optionPane)
			{
				return optionPane;
			}
			if (component instanceof Container child)
			{
				JOptionPane inside = optionPaneIn(child);
				if (inside != null)
				{
					return inside;
				}
			}
		}
		return null;
	}
}

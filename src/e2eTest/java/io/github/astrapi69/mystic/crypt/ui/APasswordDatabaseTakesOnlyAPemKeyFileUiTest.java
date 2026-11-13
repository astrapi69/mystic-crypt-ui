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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * A database with a master password and a key file reads its key file as PEM only, so the dialogs
 * that create and open one are not to offer it a DER key file (#943). A database with its key file
 * alone keeps taking DER.
 * <p>
 * With the master password checked, both dialogs took a DER key file and enabled OK, and so did
 * checking the master password after a DER key file was chosen; OK then failed. Now the dialog says
 * which file is not PEM and keeps OK disabled until a PEM key file is chosen.
 */
class APasswordDatabaseTakesOnlyAPemKeyFileUiTest extends AbstractUiTest
{

	private final String password = TestPasswords.throwaway();

	private PrivateKey key;

	private File derKeyFile;

	@BeforeEach
	void aKeyInDer() throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		key = KeyPairFactory.newKeyPair("RSA").getPrivate();
		derKeyFile = new File(tempHome, "database.der");
		try (OutputStream out = new FileOutputStream(derKeyFile))
		{
			PrivateKeyWriter.write(key, out, KeyFileFormat.DER, KeyFormat.PKCS_8);
		}
	}

	@Test
	@DisplayName("creating with a master password: a DER key file is named and not taken, a PEM one is")
	void create_takesOnlyAPemKeyFile_withAMasterPassword() throws Exception
	{
		CreateMasterKeySteps create = launchApplication()
			.startNewDatabase(new File(tempHome, "new.mcrdb")).checkMasterPassword()
			.typeMasterPasswordWithRepeat(password).checkKeyFile().browseKeyFile(derKeyFile);

		requireTheNoticeFor(derKeyFile);
		create.requireOkDisabled().browseKeyFile(aPemKeyFile()).requireOkEnabled();
	}

	@Test
	@DisplayName("signing in with a master password: a DER key file is named and not taken, a PEM one is")
	void signIn_takesOnlyAPemKeyFile_withAMasterPassword() throws Exception
	{
		File database = aPasswordAndKeyFileDatabase();
		SignInDialogSteps signIn = launchApplication().checkMasterPassword()
			.typeMasterPassword(password).checkKeyFile().browseKeyFile(derKeyFile);

		requireTheNoticeFor(derKeyFile);
		signIn.browseApplicationFile(database).requireOkDisabled().browseKeyFile(aPemKeyFile())
			.requireOkEnabled();
	}

	@Test
	@DisplayName("a DER key file chosen first, then the master password checked: the file is named and OK stays disabled")
	void signIn_namesTheDerKeyFile_whenTheMasterPasswordIsCheckedAfterIt() throws Exception
	{
		File database = aPasswordAndKeyFileDatabase();
		SignInDialogSteps signIn = launchApplication().uncheckMasterPassword().checkKeyFile()
			.browseKeyFile(derKeyFile).browseApplicationFile(database);

		signIn.checkMasterPassword();
		requireTheNoticeFor(derKeyFile);
		signIn.typeMasterPassword(password).requireOkDisabled();
	}

	@Test
	@DisplayName("signing in with the key file alone, a DER key file chosen: OK goes ahead, as opening takes it")
	void signIn_takesADerKeyFile_withTheKeyFileAlone() throws Exception
	{
		File database = aPasswordAndKeyFileDatabase();

		launchApplication().uncheckMasterPassword().checkKeyFile().browseKeyFile(derKeyFile)
			.browseApplicationFile(database).requireOkEnabled();
	}

	/** Waits for the notice that names the key file, checks its words and closes it */
	private void requireTheNoticeFor(final File keyFile)
	{
		Pause.pause(new Condition("the notice that the key file is not PEM")
		{
			@Override
			public boolean test()
			{
				return warning() != null;
			}
		}, TimeUnit.SECONDS.toMillis(15));
		JOptionPane notice = warning();
		String words = GuiActionRunner.execute(() -> String.valueOf(notice.getMessage()));
		GuiActionRunner.execute(() -> notice.setValue(JOptionPane.OK_OPTION));
		robot.waitForIdle();
		assertTrue(words.contains(keyFile.getAbsolutePath())
			&& words.contains("takes its key file as PEM"), words);
	}

	private static JOptionPane warning()
	{
		return GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing())
				{
					JOptionPane optionPane = optionPaneIn(dialog);
					if (optionPane != null
						&& optionPane.getMessageType() == JOptionPane.WARNING_MESSAGE)
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

	private File aPemKeyFile() throws Exception
	{
		File pemKeyFile = new File(tempHome, "database.pem");
		if (!pemKeyFile.isFile())
		{
			PrivateKeyWriter.writeInPemFormat(key, pemKeyFile);
		}
		return pemKeyFile;
	}

	/** A database with a master password and a key file, made with a PEM copy of the key */
	private File aPasswordAndKeyFileDatabase() throws Exception
	{
		File pemKeyFile = aPemKeyFile();
		File database = new File(tempHome, "database.mcrdb");
		Files.createFile(database.toPath());
		ApplicationXmlFileFactory.newApplicationFileWithPasswordAndPrivateKey(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(database))
				.selectedApplicationFilePath(database.getAbsolutePath())
				.keyFileInfo(FileInfo.toFileInfo(pemKeyFile))
				.privateKeyInfo(KeyModelExtensions.toKeyModel(key)).masterPw(password.toCharArray())
				.withMasterPw(true).withKeyFile(true).minPasswordLength(6).build());
		return database;
	}
}

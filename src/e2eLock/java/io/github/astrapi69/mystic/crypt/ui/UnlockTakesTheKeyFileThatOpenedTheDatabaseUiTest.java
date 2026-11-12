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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.Security;
import java.util.stream.Stream;

import org.assertj.swing.edt.GuiActionRunner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The key file that opened a database unlocks it again (#937), in every format opening takes: PEM
 * and DER for a database with its key file alone, PEM for one with a master password and a key file
 * (the formats {@code WhatOpeningAndSavingTakeAsAKeyFileTest} pins).
 * <p>
 * The unlock used to read PEM only, so a database with its key file alone that opened with a DER
 * key file refused the same file at the unlock. It reads the key file the way opening does now.
 */
class UnlockTakesTheKeyFileThatOpenedTheDatabaseUiTest extends AbstractUiTest
{

	static Stream<Arguments> whatOpeningTakesAndUnlockingTakesToo()
	{
		return Stream.of(
			Arguments.of("key file alone, PEM as Create key file writes it", false,
				"writeInPemFormat"),
			Arguments.of("key file alone, PEM PKCS#8", false, "PEM/PKCS_8"),
			Arguments.of("key file alone, PEM PKCS#1", false, "PEM/PKCS_1"),
			Arguments.of("key file alone, DER PKCS#8", false, "DER/PKCS_8"),
			Arguments.of("key file alone, DER as the key generator writes PKCS#1", false,
				"DER/PKCS_1"),
			Arguments.of("password and key file, PEM as Create key file writes it", true,
				"writeInPemFormat"),
			Arguments.of("password and key file, PEM PKCS#8", true, "PEM/PKCS_8"),
			Arguments.of("password and key file, PEM PKCS#1", true, "PEM/PKCS_1"));
	}

	@ParameterizedTest(name = "{0}: opened, locked, and unlocked with the same file")
	@MethodSource("whatOpeningTakesAndUnlockingTakesToo")
	void unlocksWithTheKeyFileThatOpenedIt(final String caseName, final boolean withPassword,
		final String format) throws Exception
	{
		String password = TestPasswords.throwaway();
		ApplicationSteps application = openedWith(withPassword, format, password);

		application.lockWorkspace().unlockWorkspaceWith(withPassword ? password : null, null);

		assertTrue(signedIn(), caseName);
	}

	/**
	 * A database whose key file is written in the given format, opened through the sign-in dialog
	 * with that file
	 */
	private ApplicationSteps openedWith(final boolean withPassword, final String format,
		final String password) throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		PrivateKey key = KeyPairFactory.newKeyPair("RSA").getPrivate();
		File database = new File(tempHome, "database.mcrdb");
		File keyFile = new File(tempHome, "database.key");
		PrivateKeyWriter.writeInPemFormat(key, keyFile);
		Files.createFile(database.toPath());
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(database))
			.selectedApplicationFilePath(database.getAbsolutePath())
			.keyFileInfo(FileInfo.toFileInfo(keyFile))
			.privateKeyInfo(KeyModelExtensions.toKeyModel(key)).withKeyFile(true)
			.withMasterPw(withPassword).minPasswordLength(6)
			.masterPw(withPassword ? password.toCharArray() : null).build();
		if (withPassword)
		{
			ApplicationXmlFileFactory.newApplicationFileWithPasswordAndPrivateKey(credentials);
		}
		else
		{
			ApplicationXmlFileFactory.newApplicationFileWithPrivateKey(credentials);
		}
		write(key, keyFile, format);

		SignInDialogSteps signIn = launchApplication();
		if (withPassword)
		{
			signIn.checkMasterPassword().typeMasterPassword(password);
		}
		else
		{
			signIn.uncheckMasterPassword();
		}
		signIn.checkKeyFile().browseKeyFile(keyFile).browseApplicationFile(database)
			.requireOkEnabled().okAndAwaitSignIn();
		return new ApplicationSteps(robot).awaitSignedIn();
	}

	/** Writes the key the way "Create key file..." or the key generator writes it */
	private static void write(final PrivateKey key, final File keyFile, final String format)
		throws Exception
	{
		if (format.equals("writeInPemFormat"))
		{
			PrivateKeyWriter.writeInPemFormat(key, keyFile);
			return;
		}
		String[] fileAndKeyFormat = format.split("/");
		try (OutputStream out = new FileOutputStream(keyFile))
		{
			PrivateKeyWriter.write(key, out, KeyFileFormat.valueOf(fileAndKeyFormat[0]),
				KeyFormat.valueOf(fileAndKeyFormat[1]));
		}
	}

	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}
}

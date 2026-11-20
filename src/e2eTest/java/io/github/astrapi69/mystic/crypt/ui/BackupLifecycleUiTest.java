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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Security;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The lifecycle of the backup file on its own (#1056, #847): a successful sign-in removes the
 * {@code .bak} a save kept, and a failed one keeps it.
 * <p>
 * {@link BackupFileUiTest} reaches the removal only after its own first sign-in has passed the same
 * check, so no breakage of the removal turns that assertion red alone. Here the database and its
 * {@code .bak} are written before the application starts, without a sign-in, and the one sign-in
 * under test is the only thing that can remove the backup. The three ways in are the three branches
 * of {@code ApplicationXmlFileReader.read}.
 */
class BackupLifecycleUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** The three ways in, one branch of the reader each */
	enum WayIn
	{
		PASSWORD(true, false, "Authentication with Password"), KEY_FILE(false, true,
			"Authentication with key file"), PASSWORD_AND_KEY_FILE(true, true,
				"Authentication with Password or key file");

		final boolean withPassword;
		final boolean withKeyFile;
		final String refusalTitle;

		WayIn(final boolean withPassword, final boolean withKeyFile, final String refusalTitle)
		{
			this.withPassword = withPassword;
			this.withKeyFile = withKeyFile;
			this.refusalTitle = refusalTitle;
		}
	}

	/** A database, its key file (if any) and the backup written next to it */
	private record Vault(File database, File backup, File keyFile) {
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(WayIn.class)
	@DisplayName("a successful sign-in removes the .bak, leaves the database as it was and shows its content")
	void aSuccessfulSignInRemovesTheBackup(final WayIn wayIn) throws Exception
	{
		Vault vault = vaultWithBackup(wayIn);
		byte[] databaseBefore = Files.readAllBytes(vault.database().toPath());

		signIn(wayIn, vault, MASTER_PASSWORD, vault.keyFile());
		new ApplicationSteps(robot).awaitSignedIn().showMainFrame();

		assertFalse(vault.backup().exists(),
			"a successful sign-in by " + wayIn + " must remove the .bak (#847)");
		assertArrayEquals(databaseBefore, Files.readAllBytes(vault.database().toPath()),
			"the sign-in must not change the database file");
		assertEquals(Set.of(vault.database().getName()),
			Set.of(vault.database().getParentFile().list()),
			"next to the database there must be nothing else once the backup is gone");
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"the sign-in must have opened the database");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(WayIn.class)
	@DisplayName("a failed sign-in keeps the .bak, the way back after a save")
	void aFailedSignInKeepsTheBackup(final WayIn wayIn) throws Exception
	{
		Vault vault = vaultWithBackup(wayIn);
		byte[] backupBefore = Files.readAllBytes(vault.backup().toPath());

		SignInDialogSteps signInDialog = signIn(wayIn, vault,
			wayIn.withPassword ? TestPasswords.throwaway() : MASTER_PASSWORD,
			wayIn.withPassword ? vault.keyFile() : foreignKeyFile());
		new ApplicationSteps(robot).dismissMessageDialog(wayIn.refusalTitle);

		assertTrue(signInDialog.dialogIsShowing(), "the failed sign-in must leave the dialog open");
		assertTrue(vault.backup().exists(),
			"a failed sign-in by " + wayIn + " must keep the .bak (#847)");
		assertArrayEquals(backupBefore, Files.readAllBytes(vault.backup().toPath()),
			"the kept .bak must be untouched");
		signInDialog.cancel();
	}

	/**
	 * Writes the database and a copy of it as its {@code .bak}, with no sign-in. Writing the
	 * database already leaves a {@code .bak} of the empty file it replaced, which the copy replaces
	 */
	private Vault vaultWithBackup(final WayIn wayIn) throws Exception
	{
		File directory = Files
			.createDirectories(tempHome.toPath().resolve("lifecycle-" + wayIn.name().toLowerCase()))
			.toFile();
		File database = new File(directory, "lifecycle.mcrdb");
		File keyFile = new File(tempHome, "lifecycle-" + wayIn.name().toLowerCase() + ".pem");
		if (wayIn.withKeyFile)
		{
			writeDatabaseWithKeyFile(wayIn, database, keyFile);
		}
		else
		{
			createDatabaseFileHeadless(database, MASTER_PASSWORD);
		}
		File backup = new File(directory, database.getName() + ".bak");
		Files.copy(database.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
		assertTrue(backup.exists(), "the precondition: a .bak next to the database");
		return new Vault(database, backup, keyFile);
	}

	private void writeDatabaseWithKeyFile(final WayIn wayIn, final File database,
		final File keyFile) throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), keyFile);
		Files.createFile(database.toPath());
		MasterPwFileModelBean.MasterPwFileModelBeanBuilder builder = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(database))
			.selectedApplicationFilePath(database.getAbsolutePath())
			.keyFileInfo(FileInfo.toFileInfo(keyFile)).withKeyFile(true).minPasswordLength(6);
		if (wayIn.withPassword)
		{
			ApplicationXmlFileFactory.newApplicationFileWithPasswordAndPrivateKey(
				builder.masterPw(MASTER_PASSWORD.toCharArray()).withMasterPw(true).build());
		}
		else
		{
			ApplicationXmlFileFactory
				.newApplicationFileWithPrivateKey(builder.withMasterPw(false).build());
		}
	}

	private File foreignKeyFile() throws Exception
	{
		File foreign = new File(tempHome, "foreign-key.pem");
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), foreign);
		return foreign;
	}

	/** Fills the sign-in dialog for the way in and clicks OK, without waiting for the outcome */
	private SignInDialogSteps signIn(final WayIn wayIn, final Vault vault, final String password,
		final File keyFile)
	{
		SignInDialogSteps signInDialog = launchApplication();
		if (wayIn.withPassword)
		{
			signInDialog.checkMasterPassword().typeMasterPassword(password);
		}
		else
		{
			signInDialog.uncheckMasterPassword();
		}
		if (wayIn.withKeyFile)
		{
			signInDialog.checkKeyFile().browseKeyFile(keyFile);
		}
		signInDialog.browseApplicationFile(vault.database()).requireOkEnabled().clickOk();
		return signInDialog;
	}
}

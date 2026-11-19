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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * What the sign-in shows when a database does not open (#1005): the refusal is read the way the
 * dialog of the sign-in reads it, as {@code title::html}, and carries the user's words, never the
 * text of the exception the cipher or the format reader threw.
 */
class SignInRefusalTextTest
{

	@TempDir
	File directory;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/** One way a database fails to open, as the refusal the sign-in path throws for it */
	@FunctionalInterface
	interface Attempt
	{
		RuntimeException refusal(SignInRefusalTextTest test) throws Exception;
	}

	static Stream<Arguments> everyFailureToOpen()
	{
		return Stream.of(
			Arguments.of("a wrong password, password alone",
				(Attempt)test -> test.openWithPassword(test.writtenWithPassword(), "other")),
			Arguments.of("a wrong password, password and key file",
				(Attempt)test -> test.openWithPasswordAndKey(true, false)),
			Arguments.of("a foreign key file, password and key file",
				(Attempt)test -> test.openWithPasswordAndKey(false, true)),
			Arguments.of("a foreign key file, key file alone",
				(Attempt)test -> test.openWithKeyAlone()),
			Arguments.of("a file that is cut off after the marker",
				(Attempt)test -> test.openWithPassword(test.truncatedVault(), null)),
			Arguments.of("a file that is not a database at all",
				(Attempt)test -> test.openWithPassword(test.notADatabase(), null)));
	}

	@ParameterizedTest(name = "{0}: the refusal carries the exception text")
	@MethodSource("everyFailureToOpen")
	void aFailureToOpen_showsTheExceptionText_today(final String caseName, final Attempt attempt)
		throws Exception
	{
		RuntimeException refused = attempt.refusal(this);

		Throwable cause = refused.getCause();
		assertNotNull(cause, "the refusal keeps the cause");
		String shown = refused.getMessage();
		assertTrue(shown.contains("::"), "title::html as the sign-in dialog reads it: " + shown);
		assertTrue(shown.endsWith("<p>" + cause.getMessage()),
			() -> "today the user sees the exception text: " + shown);
	}

	private RuntimeException openWithPassword(final File vault, final String wrongPassword)
	{
		char[] password = wrongPassword == null ? passwordOfTheVault : wrongPassword.toCharArray();
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath()).masterPw(password)
			.withMasterPw(true).withKeyFile(false).build();
		return assertThrows(RuntimeException.class,
			() -> ApplicationXmlFileReader.read(credentials));
	}

	private char[] passwordOfTheVault = TestPasswords.throwaway().toCharArray();

	private File writtenWithPassword()
	{
		return SampleVaults.save(SampleVaults.aVaultWith(entry -> {
		}), directory, passwordOfTheVault);
	}

	private File truncatedVault() throws Exception
	{
		File vault = writtenWithPassword();
		byte[] content = Files.readAllBytes(vault.toPath());
		File cut = new File(directory, "cut.mcrdb");
		Files.write(cut.toPath(), java.util.Arrays.copyOf(content, 12));
		return cut;
	}

	private File notADatabase() throws Exception
	{
		File vault = new File(directory, "notes.mcrdb");
		Files.writeString(vault.toPath(), "this is a text file, not a database\n",
			StandardCharsets.UTF_8);
		return vault;
	}

	private RuntimeException openWithPasswordAndKey(final boolean wrongPassword,
		final boolean foreignKey) throws Exception
	{
		File keyFile = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(newKey().getPrivate(), keyFile);
		File vault = new File(directory, "both.mcrdb");
		ApplicationModelBean model = SampleVaults.aVaultWith(entry -> {
		});
		model.setMasterPwFileModelBean(credentials(vault, keyFile, passwordOfTheVault, true));
		ApplicationXmlFileStoreWorker.saveToFileWithPasswordAndPrivateKey(model);
		File keyToOpenWith = keyFile;
		if (foreignKey)
		{
			keyToOpenWith = new File(directory, "foreign.pem");
			PrivateKeyWriter.writeInPemFormat(newKey().getPrivate(), keyToOpenWith);
		}
		char[] password = wrongPassword ? "other".toCharArray() : passwordOfTheVault;
		MasterPwFileModelBean opening = credentials(vault, keyToOpenWith, password, true);
		return assertThrows(RuntimeException.class, () -> ApplicationXmlFileReader.read(opening));
	}

	private RuntimeException openWithKeyAlone() throws Exception
	{
		File keyFile = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(newKey().getPrivate(), keyFile);
		File vault = new File(directory, "key-only.mcrdb");
		ApplicationModelBean model = SampleVaults.aVaultWith(entry -> {
		});
		model.setMasterPwFileModelBean(credentials(vault, keyFile, null, false));
		ApplicationXmlFileStoreWorker.saveToFileWithPrivateKey(model);
		File foreign = new File(directory, "foreign.pem");
		PrivateKeyWriter.writeInPemFormat(newKey().getPrivate(), foreign);
		MasterPwFileModelBean opening = credentials(vault, foreign, null, false);
		return assertThrows(RuntimeException.class, () -> ApplicationXmlFileReader.read(opening));
	}

	private static KeyPair newKey() throws Exception
	{
		return KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
	}

	private static MasterPwFileModelBean credentials(final File vault, final File keyFile,
		final char[] password, final boolean withPassword)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath())
			.keyFileInfo(FileInfo.toFileInfo(keyFile)).masterPw(password).withMasterPw(withPassword)
			.withKeyFile(true).minPasswordLength(6).build();
	}
}

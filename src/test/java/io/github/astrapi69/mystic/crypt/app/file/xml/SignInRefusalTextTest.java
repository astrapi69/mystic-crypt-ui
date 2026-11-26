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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import io.github.astrapi69.mystic.crypt.app.file.xml.UnauthenticatedVaults.Decryption;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * What the sign-in shows when a database does not open (#1005): the refusal is read the way the
 * dialog of the sign-in reads it, as {@code title::html}, and carries the user's words, never the
 * text of the exception the cipher or the format reader threw.
 * <p>
 * Where the decryption authenticates nothing, a wrong password passes its padding check by chance,
 * so the outcome depends on the password (#1188). Those cases still draw their password at runtime,
 * and keep the first one that gives the outcome the case names, one case for each outcome.
 */
class SignInRefusalTextTest
{

	private static final String WRONG_BOTH = "The master password or the key file is wrong.";
	private static final String WRONG_KEY = "This key file does not unlock this database.";
	private static final String MESSAGE_FOR_A_WRONG_MASTER = "Wrong master password.";
	private static final String NOT_A_DATABASE = "This file is not a database this application can open. It was not opened, and the file is unchanged.";
	private static final String DECRYPTED_BUT_NO_XML = "This database was decrypted, but its content could not be read: the reader failed with XmlPullParserException. It was not opened, and the file is unchanged.";

	/** A class name no version of this application has: what a vault of an unknown version holds */
	private static final String UNKNOWN_TYPE = "io.github.astrapi69.mystic.crypt.NoSuchTypeInAnyVersion";
	private static final String DECRYPTED_BUT_AN_UNKNOWN_TYPE = "This database was decrypted, but its content could not be read: it names the type "
		+ UNKNOWN_TYPE
		+ ", which this version does not know. It was not opened, and the file is unchanged.";

	@TempDir
	File directory;

	private final char[] passwordOfTheVault = TestPasswords.throwaway().toCharArray();

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

	/**
	 * Picks a password by how a decryption without authentication answers it (#1188): from the
	 * password layer of a database with both factors, or from the decryption of a file
	 */
	@FunctionalInterface
	interface PasswordPick<T>
	{
		String from(T source) throws Exception;
	}

	static Stream<Arguments> everyFailureToOpen()
	{
		return Stream.of(
			Arguments.of("a wrong password, password alone",
				(Attempt)test -> test.open(test.writtenWithPassword(), null,
					TestPasswords.throwaway().toCharArray()),
				MESSAGE_FOR_A_WRONG_MASTER),
			Arguments.of(
				"a wrong password that both formats of the password layer refuse, password and key file",
				(Attempt)test -> test.openBothWithAWrongPassword(layer -> UnauthenticatedVaults
					.aPasswordRefusedBy(UnauthenticatedVaults.passwordLayerOfEight(layer),
						UnauthenticatedVaults.passwordLayerOfThisVersion(layer))),
				WRONG_BOTH),
			Arguments.of(
				"a wrong password that passes the padding check of 8's password layer by chance, password and key file",
				(Attempt)test -> test.openBothWithAWrongPassword(layer -> UnauthenticatedVaults
					.aPasswordPassing(UnauthenticatedVaults.passwordLayerOfEight(layer),
						UnauthenticatedVaults.passwordLayerOfThisVersion(layer))),
				DECRYPTED_BUT_NO_XML),
			Arguments.of(
				"a database with a password and a key file whose xml names a type this version does not know",
				(Attempt)test -> test.openBothOnAnUnknownType(), DECRYPTED_BUT_AN_UNKNOWN_TYPE),
			Arguments.of(
				"a database without the marker whose xml names a type this version does not know",
				(Attempt)test -> test.openWithoutTheMarkerOnAnUnknownType(),
				DECRYPTED_BUT_AN_UNKNOWN_TYPE),
			Arguments.of("a foreign key file, password and key file",
				(Attempt)test -> test.openBothWith(test.pem(2048)), WRONG_BOTH),
			Arguments.of("a foreign 1024-bit key file, password and key file",
				(Attempt)test -> test.openBothWith(test.pem(1024)), WRONG_BOTH),
			Arguments.of("a foreign key file, key file alone",
				(Attempt)test -> test.openKeyAloneWith(test.pem(2048)), WRONG_KEY),
			Arguments.of("a foreign 1024-bit key file, key file alone",
				(Attempt)test -> test.openKeyAloneWith(test.pem(1024)), WRONG_KEY),
			Arguments.of("a key file that holds no private key (a DER public key), key file alone",
				(Attempt)test -> test.openKeyAloneWith(test.derPublicKey()), WRONG_KEY),
			Arguments.of("a file that is cut off after the marker",
				(Attempt)test -> test.open(test.truncatedVault(), null, test.passwordOfTheVault),
				NOT_A_DATABASE),
			Arguments.of("a text file whose length no password database has",
				(Attempt)test -> test.open(test.textFile(36), null, test.passwordOfTheVault),
				NOT_A_DATABASE),
			Arguments.of(
				"a text file of 40 bytes, which the cipher cannot tell from a wrong password, and a password whose key fails the padding check",
				(Attempt)test -> test.openTextFileOf40Bytes(
					ofTheFile -> UnauthenticatedVaults.aPasswordRefusedBy(ofTheFile)),
				MESSAGE_FOR_A_WRONG_MASTER),
			Arguments.of(
				"a text file of 40 bytes, which the cipher cannot tell from a wrong password, and a password whose key passes the padding check by chance",
				(Attempt)test -> test.openTextFileOf40Bytes(
					ofTheFile -> UnauthenticatedVaults.aPasswordPassing(ofTheFile)),
				DECRYPTED_BUT_NO_XML),
			Arguments.of("a database cut to half, key file alone",
				(Attempt)test -> test.openKeyAloneCut(), NOT_A_DATABASE),
			Arguments.of("a text file as the database, key file alone",
				(Attempt)test -> test.openKeyAloneOn(test.textFile(36)), NOT_A_DATABASE),
			Arguments.of("a password-only database, opened with password and key file",
				(Attempt)test -> test.openPasswordOnlyDatabaseWithBoth(), NOT_A_DATABASE));
	}

	@ParameterizedTest(name = "{0}: the user reads a message, not the exception text")
	@MethodSource("everyFailureToOpen")
	void aFailureToOpen_showsAMessage_notTheExceptionText(final String caseName,
		final Attempt attempt, final String expectedMessage) throws Exception
	{
		RuntimeException refused = attempt.refusal(this);

		Throwable cause = refused.getCause();
		assertNotNull(cause, "the refusal keeps the cause for the log");
		String shown = refused.getMessage();
		assertTrue(shown.contains("::"), "title::html as the sign-in dialog reads it: " + shown);
		assertTrue(shown.endsWith("<p>" + expectedMessage),
			() -> "the user reads the bundle's words: " + shown);
		assertFalse(shown.contains(String.valueOf(cause.getMessage())),
			() -> "and never the exception text: " + shown);
	}

	@Test
	void aMissingKeyFile_isNamedByItsOwnText_alone() throws Exception
	{
		File vault = writtenWithKeyAlone(newKey(2048));
		File missing = new File(directory, "gone.pem");

		RuntimeException refused = open(vault, missing, null);

		assertTrue(refused.getMessage().endsWith("<p>The key file " + missing.getAbsolutePath()
			+ " is not there. It was moved or deleted, or it is on a drive that is not connected. "
			+ "Put it back at this path and try again."), refused.getMessage());
	}

	@Test
	void everyRefusalText_isInTheGermanBundle_withRealUmlauts()
	{
		java.util.ResourceBundle german = java.util.ResourceBundle.getBundle("ui.messages",
			java.util.Locale.GERMAN);

		for (String key : new String[] { "unlock.failed.title", "unlock.failed.both",
				"unlock.failed.key.file", "unlock.failed.password",
				"dialog.vault.not.a.database.message", "dialog.vault.not.a.database.title",
				"dialog.signin.both.title", "dialog.signin.key.file.title",
				"dialog.signin.password.title" })
		{
			String text = german.getString(key);
			assertFalse(text.contains("Schluessel") || text.contains("geoeffnet"), text);
		}
		assertTrue(german.getString("dialog.vault.not.a.database.message").contains("geöffnet"));
		assertTrue(german.getString("unlock.failed.key.file").contains("Schlüsseldatei"));
		assertTrue(
			german.getString("dialog.signin.both.reason").endsWith("oder beides ist nicht gültig"));
		assertTrue(german.getString("unlock.failed.title").equals("Entsperren fehlgeschlagen"));
	}

	private RuntimeException open(final File vault, final File keyFile, final char[] password)
	{
		MasterPwFileModelBean.MasterPwFileModelBeanBuilder credentials = MasterPwFileModelBean
			.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath()).masterPw(password)
			.withMasterPw(password != null).withKeyFile(keyFile != null).minPasswordLength(6);
		if (keyFile != null)
		{
			credentials.keyFileInfo(FileInfo.toFileInfo(keyFile));
		}
		MasterPwFileModelBean opening = credentials.build();
		return assertThrows(RuntimeException.class, () -> ApplicationXmlFileReader.read(opening));
	}

	private File writtenWithPassword()
	{
		return SampleVaults.save(SampleVaults.aVaultWith(entry -> {
		}), directory, passwordOfTheVault);
	}

	private File truncatedVault() throws Exception
	{
		File vault = writtenWithPassword();
		File cut = new File(directory, "cut.mcrdb");
		Files.write(cut.toPath(), Arrays.copyOf(Files.readAllBytes(vault.toPath()), 12));
		return cut;
	}

	private File textFile(final int length) throws Exception
	{
		File vault = new File(directory, "notes-" + length + ".mcrdb");
		Files.writeString(vault.toPath(), "a".repeat(length - 1) + "\n", StandardCharsets.UTF_8);
		return vault;
	}

	private KeyPair newKey(final int bits) throws Exception
	{
		return KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, bits);
	}

	private File pem(final int bits) throws Exception
	{
		File keyFile = new File(directory, "key-" + UUID.randomUUID() + ".pem");
		PrivateKeyWriter.writeInPemFormat(newKey(bits).getPrivate(), keyFile);
		return keyFile;
	}

	private File derPublicKey() throws Exception
	{
		File keyFile = new File(directory, "public-" + UUID.randomUUID() + ".der");
		Files.write(keyFile.toPath(), newKey(2048).getPublic().getEncoded());
		return keyFile;
	}

	private File writtenWithKeyAlone(final KeyPair key) throws Exception
	{
		File keyFile = new File(directory, "own-key.pem");
		PrivateKeyWriter.writeInPemFormat(key.getPrivate(), keyFile);
		File vault = new File(directory, "key-only-" + UUID.randomUUID() + ".mcrdb");
		ApplicationModelBean model = SampleVaults.aVaultWith(entry -> {
		});
		model.setMasterPwFileModelBean(credentials(vault, keyFile, null));
		ApplicationXmlFileStoreWorker.saveToFileWithPrivateKey(model);
		return vault;
	}

	/** Opens a database of both factors with the given key file and the right password */
	private RuntimeException openBothWith(final File keyFileToOpenWith) throws Exception
	{
		File keyFile = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(newKey(2048).getPrivate(), keyFile);
		File vault = UnauthenticatedVaults.savedWithBoth(new File(directory, "both.mcrdb"), keyFile,
			passwordOfTheVault);
		return open(vault, keyFileToOpenWith, passwordOfTheVault);
	}

	/**
	 * Opens a database of both factors with its own key file and a wrong password, picked from the
	 * password layer the key file opens: neither format of that layer authenticates (#1188)
	 */
	private RuntimeException openBothWithAWrongPassword(final PasswordPick<String> pick)
		throws Exception
	{
		KeyPair key = newKey(2048);
		File keyFile = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(key.getPrivate(), keyFile);
		File vault = UnauthenticatedVaults.savedWithBoth(new File(directory, "both.mcrdb"), keyFile,
			passwordOfTheVault);
		String wrongPassword = pick
			.from(UnauthenticatedVaults.passwordLayerOf(vault, key.getPrivate()));
		return open(vault, keyFile, wrongPassword.toCharArray());
	}

	/** Opens a database of both factors, with the right ones, whose xml names an unknown type */
	private RuntimeException openBothOnAnUnknownType() throws Exception
	{
		KeyPair key = newKey(2048);
		File keyFile = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(key.getPrivate(), keyFile);
		File vault = UnauthenticatedVaults.writtenWithBoth(new File(directory, "unknown.mcrdb"),
			xmlNamingAnUnknownType(), String.valueOf(passwordOfTheVault), key.getPublic());
		return open(vault, keyFile, passwordOfTheVault);
	}

	/**
	 * Opens a database without the marker, with the right password, whose xml names an unknown type
	 */
	private RuntimeException openWithoutTheMarkerOnAnUnknownType() throws Exception
	{
		File vault = UnauthenticatedVaults.writtenBeforeTheMarker(directory,
			xmlNamingAnUnknownType(), String.valueOf(passwordOfTheVault));
		return open(vault, null, passwordOfTheVault);
	}

	/** Opens a text file of 40 bytes with a password picked by how its decryption answers it */
	private RuntimeException openTextFileOf40Bytes(final PasswordPick<Decryption> pick)
		throws Exception
	{
		File textFile = textFile(40);
		String password = pick.from(UnauthenticatedVaults.withThePasswordAlone(textFile));
		return open(textFile, null, password.toCharArray());
	}

	private static String xmlNamingAnUnknownType()
	{
		return new String(VaultXmlCodec.toXml(SampleVaults.aVaultWith(entry -> {
		}))).replaceFirst("<list>", "<list><" + UNKNOWN_TYPE + "/>");
	}

	private RuntimeException openKeyAloneWith(final File keyFileToOpenWith) throws Exception
	{
		return open(writtenWithKeyAlone(newKey(2048)), keyFileToOpenWith, null);
	}

	private RuntimeException openKeyAloneCut() throws Exception
	{
		KeyPair key = newKey(2048);
		File vault = writtenWithKeyAlone(key);
		File keyFile = new File(directory, "own-key.pem");
		byte[] content = Files.readAllBytes(vault.toPath());
		File cut = new File(directory, "key-only-cut.mcrdb");
		Files.write(cut.toPath(), Arrays.copyOf(content, content.length / 2));
		return open(cut, keyFile, null);
	}

	private RuntimeException openKeyAloneOn(final File notADatabase) throws Exception
	{
		return open(notADatabase, pem(2048), null);
	}

	private RuntimeException openPasswordOnlyDatabaseWithBoth() throws Exception
	{
		return open(writtenWithPassword(), pem(2048), passwordOfTheVault);
	}

	private static MasterPwFileModelBean credentials(final File vault, final File keyFile,
		final char[] password)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath())
			.keyFileInfo(FileInfo.toFileInfo(keyFile)).masterPw(password)
			.withMasterPw(password != null).withKeyFile(true).minPasswordLength(6).build();
	}
}

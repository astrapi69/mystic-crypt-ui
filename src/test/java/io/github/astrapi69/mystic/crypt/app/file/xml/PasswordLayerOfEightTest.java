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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Base64;
import java.util.Locale;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * A database that 8 wrote with a password and a key file, whose password layer the format this
 * version writes decrypts without a failure.
 * <p>
 * The reader decrypts the password layer in this version's format first and in the one 8 wrote
 * second (#650). Neither ends in more than a padding check. A layer of 8 read in this version's
 * format fails that check nearly always, and the reader goes on to 8's format. But where the
 * layer's length after the salt is a whole number of this version's cipher blocks, which the length
 * of the content decides for about half of all databases, the check passes for about one password
 * in 256, and this version's format then gives back text that is no xml. Whether it passes is
 * decided by the password and the content alone, so such a database behaves the same way at every
 * sign-in: up to this test the reader stopped at that text and called the database's own password
 * wrong every time. It now reads the layer the way 8 wrote it as well.
 * <p>
 * The test writes one at runtime: content whose layer has that length, and passwords drawn until
 * one gives a layer that this version's format decrypts to text that cannot begin an xml document.
 * It prints how many passwords it drew to standard output. The same chance meets a wrong password
 * on a database this version wrote, and that one stays a wrong password (#1188).
 */
class PasswordLayerOfEightTest
{

	/** How many passwords a draw takes at most; about one in 256 gives the layer it needs */
	private static final int MOST_DRAWS = 1 << 12;

	/**
	 * The salt this version's password layer starts with, the length mystic-crypt's
	 * {@code PbeCipherSupport.SALT_LENGTH} gives it
	 */
	private static final int SALT_LENGTH_OF_THE_PASSWORD_LAYER = 8;

	/** The block size of the cipher of this version's password layer, AES */
	private static final int BLOCK_SIZE_OF_THE_PASSWORD_LAYER = 16;

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

	/** The vault, and the xml it is written as */
	private record Content(ApplicationModelBean vault, String xml) {
	}

	/** A drawn password, and the password layer 8 writes with it */
	private record Drawn(String password, String passwordLayer) {
	}

	@Test
	@DisplayName("a database 8 wrote with a password and a key file opens with its password, also when this version's format decrypts its password layer to text that is no xml")
	void aVaultOfEight_whosePasswordLayerThisVersionDecryptsToNoXml_opensWithItsPassword()
		throws Exception
	{
		KeyPair key = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		File keyFile = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(key.getPrivate(), keyFile);
		File database = new File(directory, "written-as-eight-did.mcrdb");
		Content content = aVaultWhoseLayerOfEightFillsWholeBlocks(database, keyFile);
		Drawn drawn = aPasswordWhoseLayerOfEightThisVersionDecryptsToNoXml(content.xml());
		UnauthenticatedVaults.writtenAsEightDidWithBoth(database, drawn.passwordLayer(),
			key.getPublic());
		assertEquals(content.xml(),
			PasswordVaultFormat.decryptLegacy(Base64.getDecoder().decode(
				UnauthenticatedVaults.passwordLayerOfAVaultOfEight(database, key.getPrivate())),
				drawn.password()),
			"8's format reads the password layer with the database's own password");

		ApplicationModelBean opened = ApplicationXmlFileReader
			.read(signingIn(database, keyFile, drawn.password()));

		assertEquals(SampleVaults.entryLines(content.vault()), SampleVaults.entryLines(opened));
	}

	@Test
	@DisplayName("a database this version wrote with a password and a key file, with a wrong password that this version's format decrypts to text that is no xml, is still refused as a wrong password")
	void aVaultOfThisVersion_aWrongPasswordThisVersionDecryptsToNoXml_isRefusedAsAWrongPassword()
		throws Exception
	{
		KeyPair key = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		File keyFile = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(key.getPrivate(), keyFile);
		File database = UnauthenticatedVaults.savedWithBoth(
			new File(directory, "written-by-this-version.mcrdb"), keyFile,
			TestPasswords.throwawayChars());
		String passwordLayer = UnauthenticatedVaults.passwordLayerOf(database, key.getPrivate());
		String wrongPassword = UnauthenticatedVaults.aPasswordPassing(
			UnauthenticatedVaults.passwordLayerOfThisVersion(passwordLayer),
			UnauthenticatedVaults.passwordLayerOfEight(passwordLayer));

		RuntimeException refusal = assertThrows(RuntimeException.class,
			() -> ApplicationXmlFileReader.read(signingIn(database, keyFile, wrongPassword)));

		assertEquals(
			asTheDialogReadsIt("Authentication with Password or key file",
				"The master password or the key file is wrong."),
			refusal.getMessage(), "a wrong password is called one (#1188)");
	}

	@Test
	@DisplayName("the password layer these tests write is the one 8 wrote: the checked-in database of 8, read in 8's format and written again, gives its own layer byte for byte")
	void thePasswordLayerWrittenHere_isTheOneEightWrote() throws Exception
	{
		File vault = copy("/compat/vaults/vault-8-password-and-key-file.mcrdb",
			new File(directory, "vault-8-password-and-key-file.mcrdb"));
		PrivateKey privateKey = PrivateKeyReader
			.readPemPrivateKey(copy("/pem/private.pem", new File(directory, "private.pem")));
		byte[] layerOfEight = Base64.getDecoder()
			.decode(UnauthenticatedVaults.passwordLayerOfAVaultOfEight(vault, privateKey));
		String xml = PasswordVaultFormat.decryptLegacy(layerOfEight,
			TestPasswords.VAULT_OF_8_FIXTURE);

		assertArrayEquals(layerOfEight, Base64.getDecoder().decode(UnauthenticatedVaults
			.passwordLayerAsEightWroteIt(xml, TestPasswords.VAULT_OF_8_FIXTURE)));
	}

	/**
	 * The sample vault with the sign-in settings every save writes into it, and the notes of its
	 * first entry lengthened until the password layer 8 writes around its xml is, after the salt, a
	 * whole number of this version's cipher blocks. The length of that layer follows from the
	 * length of the content alone, whatever the password, and this version's format refuses a layer
	 * of any other length before it decrypts anything
	 *
	 * @param database
	 *            the file the vault is written to
	 * @param keyFile
	 *            the key file
	 * @return the vault and its xml
	 * @throws Exception
	 *             if a layer cannot be written
	 */
	private static Content aVaultWhoseLayerOfEightFillsWholeBlocks(final File database,
		final File keyFile) throws Exception
	{
		for (int added = 0; added < BLOCK_SIZE_OF_THE_PASSWORD_LAYER; added++)
		{
			char[] notes = ("Grüße, with umlauts" + ".".repeat(added)).toCharArray();
			ApplicationModelBean vault = SampleVaults.aVaultWith(entry -> entry.setNotes(notes));
			vault.setMasterPwFileModelBean(signInSettings(database, keyFile));
			String xml = new String(VaultXmlCodec.toXml(vault));
			if (fillsWholeBlocks(
				UnauthenticatedVaults.passwordLayerAsEightWroteIt(xml, TestPasswords.throwaway())))
			{
				return new Content(vault, xml);
			}
		}
		return fail("no length of the notes gave a password layer of 8 of whole blocks");
	}

	private static boolean fillsWholeBlocks(final String passwordLayer)
	{
		int afterTheSalt = Base64.getDecoder().decode(passwordLayer).length
			- SALT_LENGTH_OF_THE_PASSWORD_LAYER;
		return 0 < afterTheSalt && afterTheSalt % BLOCK_SIZE_OF_THE_PASSWORD_LAYER == 0;
	}

	/**
	 * A password drawn at runtime whose password layer of 8 around the xml this version's format
	 * decrypts without a failure, the way the reader tries it first, to text that cannot begin an
	 * xml document. Both the layer and that decryption follow from the password and the xml alone
	 *
	 * @param xml
	 *            the content
	 * @return the password and its layer
	 * @throws Exception
	 *             when a decryption fails for another reason than the padding check
	 */
	private static Drawn aPasswordWhoseLayerOfEightThisVersionDecryptsToNoXml(final String xml)
		throws Exception
	{
		long started = System.nanoTime();
		for (int draw = 1; draw <= MOST_DRAWS; draw++)
		{
			String password = TestPasswords.throwaway();
			String passwordLayer = UnauthenticatedVaults.passwordLayerAsEightWroteIt(xml, password);
			String text = UnauthenticatedVaults.textOrNull(
				UnauthenticatedVaults.passwordLayerOfThisVersion(passwordLayer), password);
			if (text != null && !UnauthenticatedVaults.couldStartAnXmlDocument(text))
			{
				System.out.println(String.format(Locale.ROOT,
					"password layer of 8: %d passwords drawn until this version's format decrypted "
						+ "one to text that is no xml, %d ms",
					draw, (System.nanoTime() - started) / 1_000_000));
				return new Drawn(password, passwordLayer);
			}
		}
		return fail("no password of " + MOST_DRAWS
			+ " gave a password layer of 8 that this version's format decrypts");
	}

	/** The sign-in settings of a database with a password and a key file, without the password */
	private static MasterPwFileModelBean signInSettings(final File database, final File keyFile)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(database))
			.selectedApplicationFilePath(database.getAbsolutePath())
			.keyFileInfo(FileInfo.toFileInfo(keyFile)).withMasterPw(true).withKeyFile(true)
			.minPasswordLength(6).build();
	}

	private static MasterPwFileModelBean signingIn(final File database, final File keyFile,
		final String password)
	{
		MasterPwFileModelBean credentials = signInSettings(database, keyFile);
		credentials.setMasterPw(password.toCharArray());
		return credentials;
	}

	private static String asTheDialogReadsIt(final String title, final String text)
	{
		return title + "::<html><body width='350'><h2>" + title + "</h2><p>" + text;
	}

	private static File copy(final String resource, final File target) throws IOException
	{
		try (InputStream in = PasswordLayerOfEightTest.class.getResourceAsStream(resource))
		{
			assertTrue(in != null, resource + " is on the class path");
			Files.copy(in, target.toPath());
		}
		return target;
	}
}

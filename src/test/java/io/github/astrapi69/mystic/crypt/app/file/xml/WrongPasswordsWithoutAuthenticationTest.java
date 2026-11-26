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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.security.KeyPair;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.UnauthenticatedVaults.Decryption;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The proof of concept of #1188, kept as its regression test: on a path whose decryption
 * authenticates nothing, a wrong password passes the padding check now and then, and the sign-in
 * then says the database was decrypted.
 * <p>
 * Each test draws its wrong passwords at runtime, counts how many pass the padding check and prints
 * the count to standard output, so the rate is measured in every run instead of estimated. The
 * first of them whose content cannot even begin an xml document is then given to the real sign-in
 * path, and its refusal is read the way the sign-in dialog reads it, as {@code title::html}.
 */
class WrongPasswordsWithoutAuthenticationTest
{

	/** How many wrong passwords a count draws; at a rate about 1 in 256, a few dozen pass */
	private static final int DRAWS = 20000;

	/** What the sign-in shows for content it decrypted and could not parse (#624) */
	private static final String DECRYPTED_BUT_NO_XML = "This database was decrypted, but its content could not be read: the reader failed with XmlPullParserException. It was not opened, and the file is unchanged.";

	@TempDir
	File directory;

	private final String passwordOfTheVault = TestPasswords.throwaway();

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/** How one decryption answered the wrong passwords of one count */
	private static final class Count
	{
		private final String what;
		private int passed;
		private final List<String> passedWithNoXml = new ArrayList<>();

		Count(final String what)
		{
			this.what = what;
		}

		/** Takes the answer of the decryption for one wrong password */
		boolean take(final Decryption decryption, final String wrongPassword) throws Exception
		{
			String text = UnauthenticatedVaults.textOrNull(decryption, wrongPassword);
			if (text == null)
			{
				return false;
			}
			passed++;
			if (!UnauthenticatedVaults.couldStartAnXmlDocument(text))
			{
				passedWithNoXml.add(wrongPassword);
			}
			return true;
		}

		String line(final int draws, final long millis)
		{
			return String.format(Locale.ROOT,
				"#1188 %s: %d wrong passwords, %d passed the padding check, rate %.5f (1 in %.1f), "
					+ "%d of them with content that cannot begin an xml document, %d ms",
				what, draws, passed, passed / (double)draws,
				passed == 0 ? Double.NaN : draws / (double)passed, passedWithNoXml.size(), millis);
		}
	}

	@Test
	@DisplayName("password alone, a database without the marker: a wrong password passes the padding check by chance, and the sign-in says the database was decrypted (#1188)")
	void passwordAlone_aWrongPasswordPassingThePadding_isReportedAsDecrypted() throws Exception
	{
		File database = UnauthenticatedVaults.writtenBeforeTheMarker(directory, xmlOfAVault(),
			passwordOfTheVault);
		Decryption theReaders = UnauthenticatedVaults.withThePasswordAlone(database);
		Count count = new Count(
			"password alone, the format before the marker (PBEWithMD5AndDES), the reader's decryption");

		long started = System.nanoTime();
		for (int draw = 0; draw < DRAWS; draw++)
		{
			count.take(theReaders, TestPasswords.throwaway());
		}
		System.out.println(count.line(DRAWS, (System.nanoTime() - started) / 1_000_000));

		assertFalse(count.passedWithNoXml.isEmpty(),
			"no wrong password passed the padding check: " + count.line(DRAWS, 0));
		String wrongPassword = count.passedWithNoXml.get(0);
		assertEquals(asTheDialogReadsIt("Database not opened", DECRYPTED_BUT_NO_XML),
			refusalOf(database, null, wrongPassword).getMessage(),
			"today: a wrong password is called a database that was decrypted");
	}

	@Test
	@DisplayName("password and key file, the password layer: a wrong password passes the padding check by chance, and the sign-in says the database was decrypted (#1188)")
	void passwordAndKeyFile_aWrongPasswordPassingThePadding_isReportedAsDecrypted() throws Exception
	{
		KeyPair key = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		File keyFile = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(key.getPrivate(), keyFile);
		File database = UnauthenticatedVaults.savedWithBoth(new File(directory, "both.mcrdb"),
			keyFile, passwordOfTheVault.toCharArray());
		String passwordLayer = UnauthenticatedVaults.passwordLayerOf(database, key.getPrivate());
		Decryption ofThisVersion = UnauthenticatedVaults
			.passwordLayerOfThisVersionInOneIteration(passwordLayer);
		Decryption ofEight = UnauthenticatedVaults.passwordLayerOfEight(passwordLayer);
		Count countOfThisVersion = new Count(
			"password and key file, the password layer of this version (PBEWITHSHA1AND128BITAES-CBC-BC, keys of 1 iteration instead of 65536)");
		Count countOfEight = new Count(
			"password and key file, the password layer of 8 (PBEWithMD5AndDES), the reader's second try");
		int eitherPassed = 0;

		long started = System.nanoTime();
		for (int draw = 0; draw < DRAWS; draw++)
		{
			String wrongPassword = TestPasswords.throwaway();
			boolean thisVersionPassed = countOfThisVersion.take(ofThisVersion, wrongPassword);
			if (countOfEight.take(ofEight, wrongPassword) || thisVersionPassed)
			{
				eitherPassed++;
			}
		}
		long millis = (System.nanoTime() - started) / 1_000_000;
		System.out.println(countOfThisVersion.line(DRAWS, millis));
		System.out.println(countOfEight.line(DRAWS, millis));
		System.out.println(String.format(Locale.ROOT,
			"#1188 password and key file, either layer passed (the reader reads the content then): "
				+ "%d of %d, rate %.5f",
			eitherPassed, DRAWS, eitherPassed / (double)DRAWS));

		Decryption theReadersFirstTry = UnauthenticatedVaults
			.passwordLayerOfThisVersion(passwordLayer);
		String wrongPassword = null;
		for (String passedTheFormatOfEight : countOfEight.passedWithNoXml)
		{
			if (UnauthenticatedVaults.textOrNull(theReadersFirstTry,
				passedTheFormatOfEight) == null)
			{
				wrongPassword = passedTheFormatOfEight;
				break;
			}
		}
		assertNotNull(wrongPassword,
			"no wrong password passed the padding check of 8's format alone: "
				+ countOfEight.line(DRAWS, millis));
		assertEquals(asTheDialogReadsIt("Database not opened", DECRYPTED_BUT_NO_XML),
			refusalOf(database, keyFile, wrongPassword).getMessage(),
			"today: a wrong password is called a database that was decrypted");
	}

	private static String xmlOfAVault()
	{
		return new String(VaultXmlCodec.toXml(SampleVaults.aVaultWith(entry -> {
		})));
	}

	/** The refusal the sign-in path throws for the database, the key file and the password */
	private static RuntimeException refusalOf(final File database, final File keyFile,
		final String wrongPassword)
	{
		MasterPwFileModelBean.MasterPwFileModelBeanBuilder credentials = MasterPwFileModelBean
			.builder().applicationFileInfo(FileInfo.toFileInfo(database))
			.selectedApplicationFilePath(database.getAbsolutePath())
			.masterPw(wrongPassword.toCharArray()).withMasterPw(true).withKeyFile(keyFile != null)
			.minPasswordLength(6);
		if (keyFile != null)
		{
			credentials.keyFileInfo(FileInfo.toFileInfo(keyFile));
		}
		MasterPwFileModelBean opening = credentials.build();
		return assertThrows(RuntimeException.class, () -> ApplicationXmlFileReader.read(opening));
	}

	private static String asTheDialogReadsIt(final String title, final String text)
	{
		return title + "::<html><body width='350'><h2>" + title + "</h2><p>" + text;
	}
}

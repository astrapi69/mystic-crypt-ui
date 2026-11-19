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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A key file saved with a password has to open in another program, not only in the library that
 * wrote it: a file the library reads back can still use an encryption scheme a current program
 * refuses. The file is read here with the JDK alone (no crypt-data, no Bouncy Castle on the
 * reading side) and, where it is installed, with openssl.
 */
class ProtectedKeyOpensElsewhereTest
{

	/** Named on purpose: Bouncy Castle may be registered in this JVM and must not do the reading */
	private static final String JDK_PROVIDER = "SunJCE";

	/** Named for the same reason, for the key factory */
	private static final String JDK_KEY_PROVIDER = "SunRsaSign";

	@BeforeAll
	static void registerBouncyCastleForTheWritingSide()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static File protectedKeyFor(final GenerateKeysPanel panel, final File directory,
		final String password)
	{
		File keyFile = new File(directory, "protected.der");
		panel.savePrivateKeyWithPasswordTo(keyFile, password);
		return keyFile;
	}

	/** Opens the file with the JDK alone; the cipher is initialised with what the file names */
	private static PrivateKey openWithTheJdk(final File keyFile, final String password)
		throws Exception
	{
		EncryptedPrivateKeyInfo info = new EncryptedPrivateKeyInfo(
			Files.readAllBytes(keyFile.toPath()));
		SecretKey passwordKey = SecretKeyFactory.getInstance(info.getAlgName(), JDK_PROVIDER)
			.generateSecret(new PBEKeySpec(password.toCharArray()));
		Cipher cipher = Cipher.getInstance(info.getAlgName(), JDK_PROVIDER);
		cipher.init(Cipher.DECRYPT_MODE, passwordKey, info.getAlgParameters());
		PKCS8EncodedKeySpec keySpec = info.getKeySpec(cipher);
		return KeyFactory.getInstance("RSA", JDK_KEY_PROVIDER).generatePrivate(keySpec);
	}

	@Test
	@DisplayName("the JDK alone decrypts the file with the password and gets the generated key")
	void theJdkAloneDecryptsTheFileWithThePasswordAndGetsTheGeneratedKey(@TempDir File directory)
		throws Exception
	{
		GenerateKeysPanel panel = KeygenTestPanels.panelWithAnRsaKeyPair();
		String password = KeygenTestPanels.throwawayPassword();
		File keyFile = protectedKeyFor(panel, directory, password);

		PrivateKey opened = openWithTheJdk(keyFile, password);

		assertEquals(panel.getModelObject().getPrivateKey().getEncoded().length,
			opened.getEncoded().length, "the JDK got a key of another size out of the file");
		assertArrayEquals(
			panel.getModelObject().getPrivateKey().getEncoded(), opened.getEncoded(),
			"the JDK got another key out of the file than the one generated");
	}

	@Test
	@DisplayName("the JDK refuses the file with another password")
	void theJdkRefusesTheFileWithAnotherPassword(@TempDir File directory) throws Exception
	{
		GenerateKeysPanel panel = KeygenTestPanels.panelWithAnRsaKeyPair();
		File keyFile = protectedKeyFor(panel, directory, KeygenTestPanels.throwawayPassword());

		assertThrows(GeneralSecurityException.class,
			() -> openWithTheJdk(keyFile, KeygenTestPanels.throwawayPassword()),
			"the file opened with a password that is not the one it was saved with");
	}

	@Test
	@DisplayName("openssl pkcs8 -inform DER opens the file and prints the generated key")
	void opensslPkcs8OpensTheFileAndPrintsTheGeneratedKey(@TempDir File directory)
		throws Exception
	{
		if (!opensslIsInstalled())
		{
			assertFalse(System.getenv("CI") != null,
				"openssl is missing on a CI machine, so the interop check cannot run");
			assumeTrue(false, "openssl is not installed here");
		}
		GenerateKeysPanel panel = KeygenTestPanels.panelWithAnRsaKeyPair();
		String password = KeygenTestPanels.throwawayPassword();
		File keyFile = protectedKeyFor(panel, directory, password);

		Process openssl = new ProcessBuilder("openssl", "pkcs8", "-inform", "DER", "-in",
			keyFile.getAbsolutePath(), "-passin", "stdin").redirectErrorStream(true).start();
		openssl.getOutputStream().write((password + "\n").getBytes(StandardCharsets.UTF_8));
		openssl.getOutputStream().close();
		String output = new String(openssl.getInputStream().readAllBytes(),
			StandardCharsets.UTF_8);
		assertTrue(openssl.waitFor(60, TimeUnit.SECONDS), "openssl did not finish");

		assertEquals(0, openssl.exitValue(), "openssl refused the file, exit code and no output "
			+ "shown on purpose, it may hold the key");
		RSAPrivateKey printed = (RSAPrivateKey)KeyFactory.getInstance("RSA", JDK_KEY_PROVIDER)
			.generatePrivate(new PKCS8EncodedKeySpec(pemBodyOf(output)));
		RSAPrivateKey generated = (RSAPrivateKey)panel.getModelObject().getPrivateKey();
		assertEquals(generated.getModulus(), printed.getModulus(),
			"openssl printed a key with another modulus than the generated one");
		assertEquals(generated.getPrivateExponent(), printed.getPrivateExponent(),
			"openssl printed a key with another private exponent than the generated one");
	}

	/** The bytes between the BEGIN and END lines of the PEM text, base64 decoded */
	private static byte[] pemBodyOf(final String pem)
	{
		StringBuilder body = new StringBuilder();
		for (String line : pem.split("\\R"))
		{
			if (!line.isBlank() && !line.startsWith("-----"))
			{
				body.append(line.trim());
			}
		}
		return Base64.getDecoder().decode(body.toString());
	}

	private static boolean opensslIsInstalled()
	{
		try
		{
			Process version = new ProcessBuilder("openssl", "version").redirectErrorStream(true)
				.start();
			version.getInputStream().readAllBytes();
			return version.waitFor(30, TimeUnit.SECONDS) && version.exitValue() == 0;
		}
		catch (IOException | InterruptedException notAvailable)
		{
			return false;
		}
	}

}

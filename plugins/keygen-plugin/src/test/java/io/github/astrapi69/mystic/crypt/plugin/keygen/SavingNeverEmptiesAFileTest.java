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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.util.stream.Stream;

import javax.swing.JButton;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.api.key.KeySize;

/**
 * Saving a key never costs the user the file that was there (#882).
 * <p>
 * Pinned as the window behaves today: the writer opens the target before it has anything to write,
 * which empties it, so a write that fails - no key generated yet, a format the key has no form in -
 * leaves a file of 0 bytes where a private key may have been. The save buttons can be pressed
 * before Generate, and a save after Generate replaces a file that is there without asking.
 */
class SavingNeverEmptiesAFileTest
{

	private static final byte[] WHAT_WAS_THERE = "the only copy of another private key"
		.getBytes(StandardCharsets.UTF_8);

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/** A private key write that fails, with the reason it fails */
	record FailingWrite(String name, PrivateKey privateKey, KeyFormat format,
		KeyFileFormat saveFormat)
	{
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<FailingWrite> failingWrites() throws Exception
	{
		PrivateKey x25519 = KeyPairGenerator
			.getInstance(KeyPairGeneratorAlgorithm.X25519.getAlgorithm(),
				BouncyCastleProvider.PROVIDER_NAME)
			.generateKeyPair().getPrivate();
		return Stream.of(
			new FailingWrite("no key generated yet, PEM PKCS#8", null, KeyFormat.PKCS_8,
				KeyFileFormat.PEM),
			new FailingWrite("no key generated yet, PEM PKCS#1", null, KeyFormat.PKCS_1,
				KeyFileFormat.PEM),
			new FailingWrite("no key generated yet, DER", null, KeyFormat.PKCS_8,
				KeyFileFormat.DER),
			new FailingWrite("an X25519 key asked for PKCS#1 as PEM", x25519, KeyFormat.PKCS_1,
				KeyFileFormat.PEM));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("failingWrites")
	@DisplayName("a private key write that fails empties the file that was there, today")
	void aFailingWriteEmptiesTheFile_today(final FailingWrite write, @TempDir File directory)
		throws Exception
	{
		File existing = anExistingFile(directory);

		assertThrows(Exception.class, () -> KeygenSupport.writePrivateKey(write.privateKey(),
			existing, write.format(), write.saveFormat()));

		assertEquals(0, existing.length(), "today the file that was there is empty");
	}

	@Test
	@DisplayName("the three save buttons can be pressed before Generate, today")
	void theSaveButtonsCanBePressedBeforeGenerate_today()
	{
		CryptographyPanel cryptography = new GenerateKeysPanel().getCryptographyPanel();

		Stream.of(cryptography.getBtnSavePrivateKey(), cryptography.getBtnSavePrivKeyWithPw(),
			cryptography.getBtnSavePublicKey()).map(JButton::isEnabled)
			.forEach(enabled -> assertTrue(enabled, "today the button is enabled"));
	}

	@Test
	@DisplayName("saving the private key over a file that is there replaces it without asking, today")
	void savingReplacesAFileWithoutAsking_today(@TempDir File directory) throws Exception
	{
		GenerateKeysPanel panel = panelWithAnRsaKeyPair();
		File existing = anExistingFile(directory);

		panel.savePrivateKeyTo(existing);

		assertFalse(java.util.Arrays.equals(WHAT_WAS_THERE, Files.readAllBytes(existing.toPath())),
			"today the file is replaced");
		assertTrue(Files.readString(existing.toPath()).contains("PRIVATE KEY"));
	}

	private static GenerateKeysPanel panelWithAnRsaKeyPair()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.getCmbAlgorithm().setSelectedItem(KeyPairGeneratorAlgorithm.RSA);
		panel.getCryptographyPanel().getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		panel.getCryptographyPanel().getBtnGenerate().doClick();
		return panel;
	}

	private static File anExistingFile(final File directory) throws Exception
	{
		File existing = new File(directory, "id_existing.pem");
		Files.write(existing.toPath(), WHAT_WAS_THERE);
		assertArrayEquals(WHAT_WAS_THERE, Files.readAllBytes(existing.toPath()));
		return existing;
	}
}

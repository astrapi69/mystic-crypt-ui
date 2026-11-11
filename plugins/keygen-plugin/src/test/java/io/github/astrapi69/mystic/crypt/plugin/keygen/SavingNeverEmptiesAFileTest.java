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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.write.DataClass;

/**
 * Saving a key never costs the user the file that was there (#882).
 * <p>
 * The writer used to open the target before it had anything to write, which empties it, so a write
 * that failed - no key generated yet, a format the key has no form in - left a file of 0 bytes
 * where a private key may have been. The save buttons could be pressed before Generate, and a save
 * after Generate replaced a file that was there without asking. Now every key is encoded before
 * its file is opened, the save buttons wait for a key pair, and a file that is there is replaced
 * only after the question, which names a private key as irreplaceable.
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

	/** A write into a file that cannot succeed */
	@FunctionalInterface
	interface WriteInto
	{
		void file(File file) throws Exception;
	}

	/** A key write that fails, with the reason it fails */
	record FailingWrite(String name, WriteInto write)
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
		String protection = UUID.randomUUID().toString();
		return Stream.of(
			new FailingWrite("no private key generated yet, PEM PKCS#8",
				file -> KeygenSupport.writePrivateKey(null, file, KeyFormat.PKCS_8,
					KeyFileFormat.PEM)),
			new FailingWrite("no private key generated yet, PEM PKCS#1",
				file -> KeygenSupport.writePrivateKey(null, file, KeyFormat.PKCS_1,
					KeyFileFormat.PEM)),
			new FailingWrite("no private key generated yet, DER",
				file -> KeygenSupport.writePrivateKey(null, file, KeyFormat.PKCS_8,
					KeyFileFormat.DER)),
			new FailingWrite("an X25519 key asked for PKCS#1 as PEM",
				file -> KeygenSupport.writePrivateKey(x25519, file, KeyFormat.PKCS_1,
					KeyFileFormat.PEM)),
			new FailingWrite("no private key generated yet, with a password",
				file -> KeygenSupport.writeEncryptedPrivateKey(null, file, protection)),
			new FailingWrite("no public key generated yet, PEM",
				file -> KeygenSupport.writePublicKey(null, file, KeyFileFormat.PEM)),
			new FailingWrite("no public key generated yet, DER",
				file -> KeygenSupport.writePublicKey(null, file, KeyFileFormat.DER)),
			new FailingWrite("a certificate without the keys it needs",
				file -> KeygenSupport.writeCertificate(CertificateInfoModel.builder().build(),
					file)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("failingWrites")
	@DisplayName("a key write that fails leaves the file that was there as it was")
	void aFailingWriteLeavesTheFileAsItWas(final FailingWrite write, @TempDir File directory)
		throws Exception
	{
		File existing = anExistingFile(directory);

		assertThrows(Exception.class, () -> write.write().file(existing));

		assertArrayEquals(WHAT_WAS_THERE, Files.readAllBytes(existing.toPath()),
			"the file that was there keeps its bytes");
	}

	@Test
	@DisplayName("the three save buttons wait for Generate, say why, and wait again after Clear")
	void theSaveButtonsWaitForAKeyPair()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		CryptographyPanel cryptography = panel.getCryptographyPanel();
		List<JButton> saveButtons = List.of(cryptography.getBtnSavePrivateKey(),
			cryptography.getBtnSavePrivKeyWithPw(), cryptography.getBtnSavePublicKey());

		saveButtons.forEach(button -> {
			assertFalse(button.isEnabled(), button.getText() + " waits for a key pair");
			assertEquals(CryptographyPanel.NOTHING_TO_SAVE_YET, button.getToolTipText(),
				button.getText() + " says why");
		});

		panel.getCmbAlgorithm().setSelectedItem(KeyPairGeneratorAlgorithm.RSA);
		cryptography.getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		cryptography.getBtnGenerate().doClick();
		saveButtons.forEach(button -> {
			assertTrue(button.isEnabled(), button.getText() + " can be pressed after Generate");
			assertTrue(button.getToolTipText().startsWith("writes the generated"),
				button.getText() + " has its own tooltip back: " + button.getToolTipText());
		});

		cryptography.getBtnClear().doClick();
		saveButtons.forEach(button -> assertFalse(button.isEnabled(),
			button.getText() + " waits again after Clear"));
	}

	@Test
	@DisplayName("saving over a file that is there asks first, as about a private key, and No keeps the file")
	void savingOverAFileAsksFirst(@TempDir File directory) throws Exception
	{
		AnsweringPanel panel = panelWithAnRsaKeyPair(false);
		File existing = anExistingFile(directory);

		panel.savePrivateKeyTo(existing);
		panel.savePrivateKeyWithPasswordTo(existing, UUID.randomUUID().toString());
		panel.savePublicKeyTo(existing);

		assertEquals(List.of(DataClass.PRIVATE_KEY, DataClass.PRIVATE_KEY, DataClass.DERIVED),
			panel.asked, "each save asks, a private key as irreplaceable");
		assertArrayEquals(WHAT_WAS_THERE, Files.readAllBytes(existing.toPath()),
			"No keeps the file");
		assertTrue(panel.told.isEmpty(), "No is not a failure: " + panel.told);
	}

	@Test
	@DisplayName("Yes replaces the file with the generated private key")
	void yesReplacesTheFile(@TempDir File directory) throws Exception
	{
		AnsweringPanel panel = panelWithAnRsaKeyPair(true);
		File existing = anExistingFile(directory);

		panel.savePrivateKeyTo(existing);

		assertEquals(List.of(DataClass.PRIVATE_KEY), panel.asked);
		assertArrayEquals(panel.getModelObject().getPrivateKey().getEncoded(),
			PrivateKeyReader.readPemPrivateKey(existing).getEncoded(),
			"the file holds the key that was generated");
	}

	@Test
	@DisplayName("the certificate asks the same question, as about a file that can be made again")
	void theCertificateAsksFirst(@TempDir File directory) throws Exception
	{
		AnsweringPanel panel = panelWithAnRsaKeyPair(false);
		File existing = anExistingFile(directory);
		CertificateInfoModel certificateInfo = KeygenSupport.newCertificateInfo(
			panel.getModelObject().getPublicKey(), panel.getModelObject().getPrivateKey());
		certificateInfo.setIssuer(DistinguishedNameInfoModel.builder().commonName("CN=a").build());
		certificateInfo
			.setSubject(DistinguishedNameInfoModel.builder().commonName("CN=a").build());

		panel.getCryptographyPanel().writeCertificateTo(existing, certificateInfo);

		assertEquals(List.of(DataClass.DERIVED), panel.asked);
		assertArrayEquals(WHAT_WAS_THERE, Files.readAllBytes(existing.toPath()));
	}

	@Test
	@DisplayName("a save that fails says so and leaves the file")
	void aFailingSaveSaysSo(@TempDir File directory) throws Exception
	{
		AnsweringPanel panel = panelWithAnRsaKeyPair(true);
		panel.getModelObject().setPrivateKey(null);
		File existing = anExistingFile(directory);

		panel.savePrivateKeyTo(existing);

		assertEquals(List.of("Saving the private key failed"), panel.told);
		assertArrayEquals(WHAT_WAS_THERE, Files.readAllBytes(existing.toPath()));
	}

	/** The window with the question and the failure message answered by the test */
	static final class AnsweringPanel extends GenerateKeysPanel
	{
		private static final long serialVersionUID = 1L;

		private final boolean answer;

		final List<DataClass> asked = new ArrayList<>();

		final List<String> told = new ArrayList<>();

		AnsweringPanel(final boolean answer)
		{
			this.answer = answer;
		}

		@Override
		protected boolean allowsReplacing(final File file, final DataClass dataClass)
		{
			if (file.isFile())
			{
				asked.add(dataClass);
				return answer;
			}
			return true;
		}

		@Override
		protected void tellNotWritten(final String whatFailed, final String title,
			final Exception reason)
		{
			told.add(title);
		}
	}

	private static AnsweringPanel panelWithAnRsaKeyPair(final boolean answer)
	{
		AnsweringPanel panel = new AnsweringPanel(answer);
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

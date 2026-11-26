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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import io.github.astrapi69.crypt.api.algorithm.AesAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.compound.CompoundAlgorithm;
import io.github.astrapi69.crypt.data.factory.CipherFactory;
import io.github.astrapi69.crypt.data.factory.SecretKeyFactoryExtensions;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.file.write.StoreFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.algorithm.MysticSymmetricAlgorithm;
import io.github.astrapi69.mystic.crypt.file.PBEFileEncryptor;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyDecryptor;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyEncryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyGenericEncryptor;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.pw.PasswordStringDecryptor;
import io.github.astrapi69.mystic.crypt.pw.PasswordStringEncryptor;

/**
 * The databases whose decryption authenticates nothing, and passwords picked by how that decryption
 * answers them (#1188).
 * <p>
 * Two layers are decrypted without authentication: a database with the password alone in the format
 * before the marker, and the password layer of a database with a password and a key file, in this
 * version's format and in the one 8 wrote. Each decryption ends with a padding check and nothing
 * else, and a wrong key passes that check by chance. A test whose outcome depends on it cannot draw
 * a password and hope: it draws them at runtime, as every test here does, and keeps the first one
 * that gives the answer the test is about.
 */
final class UnauthenticatedVaults
{

	/** How many passwords a pick draws at most; one passes the padding check about once in 256 */
	private static final int MOST_DRAWS = 1 << 16;

	/**
	 * The salt the password layer of this version starts with, the length mystic-crypt's
	 * {@code PbeCipherSupport.SALT_LENGTH} gives it
	 */
	private static final int SALT_LENGTH_OF_THE_PASSWORD_LAYER = 8;

	private UnauthenticatedVaults()
	{
	}

	/**
	 * A decryption the way the reader does it: the text that comes out, or a
	 * {@link GeneralSecurityException} when the padding check refused the key
	 */
	@FunctionalInterface
	interface Decryption
	{
		String decrypt(String password) throws Exception;
	}

	/**
	 * Writes a database the way every release up to 8.2 wrote it: {@code PBEWithMD5AndDES} with the
	 * salt and the iteration count every installation shared, and no marker
	 *
	 * @param directory
	 *            where to write it
	 * @param xml
	 *            the content
	 * @param password
	 *            the master password
	 * @return the database
	 * @throws Exception
	 *             if it cannot be written
	 */
	static File writtenBeforeTheMarker(final File directory, final String xml,
		final String password) throws Exception
	{
		File plain = new File(directory, "legacy-plain.xml");
		StoreFileExtensions.toFile(plain, xml);
		File target = new File(directory, "legacy.mcrdb");
		CryptModel<Cipher, String, String> legacyModel = CryptModel
			.<Cipher, String, String> builder().key(password)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).salt(CompoundAlgorithm.SALT)
			.iterationCount(CompoundAlgorithm.ITERATIONCOUNT).build();
		return new PBEFileEncryptor(legacyModel, target).encrypt(plain);
	}

	/**
	 * Saves the sample vault with a password and a key file through this build's own save path
	 *
	 * @param database
	 *            the file to write
	 * @param keyFile
	 *            the key file
	 * @param password
	 *            the master password
	 * @return the database
	 */
	static File savedWithBoth(final File database, final File keyFile, final char[] password)
	{
		ApplicationModelBean model = SampleVaults.aVaultWith(entry -> {
		});
		model.setMasterPwFileModelBean(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(database))
				.selectedApplicationFilePath(database.getAbsolutePath())
				.keyFileInfo(FileInfo.toFileInfo(keyFile)).masterPw(password).withMasterPw(true)
				.withKeyFile(true).minPasswordLength(6).build());
		ApplicationXmlFileStoreWorker.saveToFileWithPasswordAndPrivateKey(model);
		return database;
	}

	/**
	 * Writes a database with a password and a key file in the two layers
	 * {@code ApplicationXmlFileStoreWorker.saveToFileWithPasswordAndPrivateKey} writes, around the
	 * given xml rather than around a model, so the xml can be one the writer would never produce
	 *
	 * @param database
	 *            the file to write
	 * @param xml
	 *            the content
	 * @param password
	 *            the master password
	 * @param publicKey
	 *            the public half of the key in the key file
	 * @return the database
	 * @throws Exception
	 *             if it cannot be written
	 */
	static File writtenWithBoth(final File database, final String xml, final String password,
		final PublicKey publicKey) throws Exception
	{
		String passwordLayer = new PasswordStringEncryptor(password).encrypt(xml);
		SecretKey symmetricKey = SecretKeyFactoryExtensions
			.newSecretKey(AesAlgorithm.AES.getAlgorithm(), 128);
		CryptModel<Cipher, SecretKey, String> symmetricKeyModel = CryptModel
			.<Cipher, SecretKey, String> builder().key(symmetricKey)
			.algorithm(MysticSymmetricAlgorithm.AES_GCM_NO_PADDING)
			.operationMode(Cipher.ENCRYPT_MODE).build();
		CryptModel<Cipher, PublicKey, byte[]> encryptionModel = CryptModel
			.<Cipher, PublicKey, byte[]> builder().key(publicKey).build();
		PublicKeyGenericEncryptor<String> encryptor = new PublicKeyGenericEncryptor<>(
			new PublicKeyEncryptor(encryptionModel, symmetricKeyModel));
		Files.write(database.toPath(), encryptor.encrypt(passwordLayer));
		return database;
	}

	/**
	 * The password layer of a database with a password and a key file, as the reader has it once
	 * the key file opened the layer around it: AES-GCM, which authenticates, around the password
	 * layer in Base64, which does not
	 *
	 * @param database
	 *            the database
	 * @param privateKey
	 *            the key in its key file
	 * @return the password layer
	 * @throws Exception
	 *             if the key does not open it
	 */
	static String passwordLayerOf(final File database, final PrivateKey privateKey) throws Exception
	{
		CryptModel<Cipher, PrivateKey, byte[]> decryptModel = CryptModel
			.<Cipher, PrivateKey, byte[]> builder().key(privateKey).build();
		PrivateKeyGenericDecryptor<String> decryptor = new PrivateKeyGenericDecryptor<>(
			new PrivateKeyDecryptor(decryptModel, MysticSymmetricAlgorithm.AES_GCM_NO_PADDING));
		return decryptor.decrypt(Files.readAllBytes(database.toPath()));
	}

	/**
	 * The reader's decryption of a database with the password alone; for a file without the marker
	 * that is the format before it
	 *
	 * @param database
	 *            the database
	 * @return the decryption
	 */
	static Decryption withThePasswordAlone(final File database)
	{
		return password -> new String(
			PasswordVaultFormat.decrypt(database, password.toCharArray()));
	}

	/**
	 * The reader's first try on the password layer: the format this version writes,
	 * {@code PBEWITHSHA1AND128BITAES-CBC-BC} keyed by 65536 iterations
	 *
	 * @param passwordLayer
	 *            the password layer in Base64
	 * @return the decryption
	 */
	static Decryption passwordLayerOfThisVersion(final String passwordLayer)
	{
		return password -> new PasswordStringDecryptor(password).decrypt(passwordLayer);
	}

	/**
	 * The same cipher on the same bytes as {@link #passwordLayerOfThisVersion(String)}, with keys
	 * derived in one iteration instead of 65536: the iteration count sets what deriving a key
	 * costs, not how the padding check answers a wrong one, and 65536 iterations per try make a
	 * count over thousands of wrong passwords take minutes. For measuring the rate, never for a
	 * sign-in
	 *
	 * @param passwordLayer
	 *            the password layer in Base64
	 * @return the decryption
	 */
	static Decryption passwordLayerOfThisVersionInOneIteration(final String passwordLayer)
	{
		byte[] saltAndCipherText = Base64.getDecoder().decode(passwordLayer);
		byte[] salt = Arrays.copyOf(saltAndCipherText, SALT_LENGTH_OF_THE_PASSWORD_LAYER);
		byte[] cipherText = Arrays.copyOfRange(saltAndCipherText, SALT_LENGTH_OF_THE_PASSWORD_LAYER,
			saltAndCipherText.length);
		return password -> new String(CipherFactory
			.newPBECipher(password.toCharArray(), Cipher.DECRYPT_MODE,
				CompoundAlgorithm.PBE_WITH_SHA1_AND_128BIT_AES_CBC_BC.getAlgorithm(), salt, 1)
			.doFinal(cipherText), StandardCharsets.UTF_8);
	}

	/**
	 * The reader's second try on the password layer: the format 8 wrote, {@code PBEWithMD5AndDES}
	 * with the shared salt
	 *
	 * @param passwordLayer
	 *            the password layer in Base64
	 * @return the decryption
	 */
	static Decryption passwordLayerOfEight(final String passwordLayer)
	{
		byte[] encryptedBytes = Base64.getDecoder().decode(passwordLayer);
		return password -> PasswordVaultFormat.decryptLegacy(encryptedBytes, password);
	}

	/**
	 * What the decryption gives back for the password
	 *
	 * @param decryption
	 *            the decryption
	 * @param password
	 *            the password
	 * @return the text, or null when the padding check refused the key
	 * @throws Exception
	 *             when the decryption fails for any other reason
	 */
	static String textOrNull(final Decryption decryption, final String password) throws Exception
	{
		try
		{
			return decryption.decrypt(password);
		}
		catch (GeneralSecurityException refused)
		{
			return null;
		}
	}

	/**
	 * Whether the text could begin an xml document: after white space and a byte order mark, a
	 * {@code <}. Text that cannot is refused by the parser before it reads anything else, with an
	 * {@code XmlPullParserException}; text that can might begin with a start tag that the reader
	 * then takes for a type it does not know, which is another refusal
	 *
	 * @param text
	 *            the text
	 * @return true if it starts the way a document can
	 */
	static boolean couldStartAnXmlDocument(final String text)
	{
		int index = 0;
		while (index < text.length() && " \t\r\n﻿".indexOf(text.charAt(index)) >= 0)
		{
			index++;
		}
		return index < text.length() && text.charAt(index) == '<';
	}

	/**
	 * A password drawn at runtime that every given decryption refuses at the padding check
	 *
	 * @param decryptions
	 *            the decryptions, the cheapest first: the draws stop asking at the first that lets
	 *            the password through
	 * @return the password
	 * @throws Exception
	 *             when a decryption fails for another reason than the padding check
	 */
	static String aPasswordRefusedBy(final Decryption... decryptions) throws Exception
	{
		for (int draw = 0; draw < MOST_DRAWS; draw++)
		{
			String password = TestPasswords.throwaway();
			if (refusedByAll(password, decryptions))
			{
				return password;
			}
		}
		return fail("no password of " + MOST_DRAWS + " was refused by every decryption");
	}

	/**
	 * A password drawn at runtime that passes the padding check of the one decryption by chance,
	 * with a text that cannot begin an xml document, and that every other given decryption refuses
	 *
	 * @param passing
	 *            the decryption whose padding check it passes, the cheaper one
	 * @param refusing
	 *            the decryptions that refuse it
	 * @return the password
	 * @throws Exception
	 *             when a decryption fails for another reason than the padding check
	 */
	static String aPasswordPassing(final Decryption passing, final Decryption... refusing)
		throws Exception
	{
		for (int draw = 0; draw < MOST_DRAWS; draw++)
		{
			String password = TestPasswords.throwaway();
			String text = textOrNull(passing, password);
			if (text != null && !couldStartAnXmlDocument(text) && refusedByAll(password, refusing))
			{
				return password;
			}
		}
		return fail("no password of " + MOST_DRAWS + " passed the padding check");
	}

	private static boolean refusedByAll(final String password, final Decryption... decryptions)
		throws Exception
	{
		for (Decryption decryption : decryptions)
		{
			if (textOrNull(decryption, password) != null)
			{
				return false;
			}
		}
		return true;
	}
}

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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Which key file formats opening and saving take, for a database with its key file alone and for
 * one with a master password and a key file (#937): the formats this application writes, from
 * "Create key file..." and from the key generator.
 * <p>
 * Measured, and kept as it was measured: a database with its key file alone takes PEM and DER, one
 * with a master password and a key file takes PEM only. Which files opening takes is not changed by
 * reading the key file in one place; this test fails if it is.
 */
class WhatOpeningAndSavingTakeAsAKeyFileTest
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

	static Stream<Arguments> everyFormatThisApplicationWrites()
	{
		return Stream.of(
			Arguments.of("key file alone, PEM as Create key file writes it", false,
				"writeInPemFormat", true),
			Arguments.of("key file alone, PEM PKCS#8", false, "PEM/PKCS_8", true),
			Arguments.of("key file alone, PEM PKCS#1", false, "PEM/PKCS_1", true),
			Arguments.of("key file alone, DER PKCS#8", false, "DER/PKCS_8", true),
			Arguments.of("key file alone, DER as the key generator writes PKCS#1", false,
				"DER/PKCS_1", true),
			Arguments.of("password and key file, PEM as Create key file writes it", true,
				"writeInPemFormat", true),
			Arguments.of("password and key file, PEM PKCS#8", true, "PEM/PKCS_8", true),
			Arguments.of("password and key file, PEM PKCS#1", true, "PEM/PKCS_1", true),
			Arguments.of("password and key file, DER PKCS#8", true, "DER/PKCS_8", false),
			Arguments.of("password and key file, DER as the key generator writes PKCS#1", true,
				"DER/PKCS_1", false));
	}

	@ParameterizedTest(name = "{0}: taken when opening and saving = {3}")
	@MethodSource("everyFormatThisApplicationWrites")
	void openingAndSaving_takeTheFormatsTheyAlwaysTook(final String caseName,
		final boolean withPassword, final String format, final boolean taken) throws Exception
	{
		PrivateKey key = KeyPairFactory.newKeyPair("RSA").getPrivate();
		char[] password = TestPasswords.throwaway().toCharArray();
		File vault = new File(directory, "database.mcrdb");
		File keyFile = new File(directory, "database.key");
		PrivateKeyWriter.writeInPemFormat(key, keyFile);
		ApplicationXmlFileStoreWorker
			.storeApplicationFile(model(credentials(vault, keyFile, withPassword, password)
				.toBuilder().privateKeyInfo(KeyModelExtensions.toKeyModel(key)).build()));
		write(key, keyFile, format);

		if (taken)
		{
			assertDoesNotThrow(() -> ApplicationXmlFileReader
				.read(credentials(vault, keyFile, withPassword, password)), "opening");
			assertDoesNotThrow(() -> ApplicationXmlFileStoreWorker
				.storeApplicationFile(model(credentials(vault, keyFile, withPassword, password))),
				"saving");
		}
		else
		{
			assertThrows(RuntimeException.class, () -> ApplicationXmlFileReader
				.read(credentials(vault, keyFile, withPassword, password)), "opening");
			assertThrows(RuntimeException.class, () -> ApplicationXmlFileStoreWorker
				.storeApplicationFile(model(credentials(vault, keyFile, withPassword, password))),
				"saving");
		}
	}

	private static MasterPwFileModelBean credentials(final File vault, final File keyFile,
		final boolean withPassword, final char[] password)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath())
			.keyFileInfo(FileInfo.toFileInfo(keyFile)).withKeyFile(true).withMasterPw(withPassword)
			.minPasswordLength(6).masterPw(withPassword ? password.clone() : null).build();
	}

	private static ApplicationModelBean model(final MasterPwFileModelBean credentials)
	{
		return ApplicationModelBean.builder().masterPwFileModelBean(credentials)
			.rootTreeAsMap(new LinkedHashMap<>()).dataOfNodes(new LinkedHashMap<>()).lastId(1L)
			.formatVersion(2).build();
	}

	/** Writes the key the way "Create key file..." or the key generator writes it */
	static void write(final PrivateKey key, final File keyFile, final String format)
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
}

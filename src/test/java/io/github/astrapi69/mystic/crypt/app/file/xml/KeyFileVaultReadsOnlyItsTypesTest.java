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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Opening a vault with a key file reads the file's bytes as the hybrid envelope of mystic-crypt.
 * The envelope is read through an allow-list from mystic-crypt 13.8 on, so a vault file that names
 * any other type - on disk, in a synchronised folder, sent by someone - is refused before that type
 * is built. Both ways to open with a key file are measured. The crafted files are harmless: a type
 * that notes whether it was built, and a nesting of empty lists; nothing in them executes code.
 */
class KeyFileVaultReadsOnlyItsTypesTest
{

	/** Set when an instance of {@link Tripwire} is read back; it must never be */
	static volatile boolean tripped;

	/** A type no vault holds, which notes that it was built */
	static class Tripwire implements Serializable
	{
		@Serial
		private static final long serialVersionUID = 1L;

		@Serial
		private void readObject(final ObjectInputStream in)
			throws IOException, ClassNotFoundException
		{
			in.defaultReadObject();
			tripped = true;
		}
	}

	@TempDir
	File directory;

	private static byte[] serialized(final Object object) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes))
		{
			out.writeObject(object);
		}
		return bytes.toByteArray();
	}

	static Stream<Arguments> craftedContents()
	{
		ArrayList<Object> nesting = new ArrayList<>();
		ArrayList<Object> current = nesting;
		for (int level = 0; level < 100; level++)
		{
			ArrayList<Object> next = new ArrayList<>();
			current.add(next);
			current = next;
		}
		return Stream.of(Arguments.of("a type no vault holds", new Tripwire()),
			Arguments.of("a nesting of empty lists", nesting));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("craftedContents")
	@DisplayName("a vault file that is not an envelope is refused when opened with the key alone")
	void openingWithTheKey_aCraftedVaultFile_isRefused(final String what, final Object content)
		throws Exception
	{
		tripped = false;
		File vault = new File(directory, "crafted.mcrdb");
		Files.write(vault.toPath(), serialized(content));
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);

		Exception refused = assertThrows(Exception.class,
			() -> ApplicationXmlFileReader.getApplicationModelBean(vault, keyPair.getPrivate()));

		assertTrue(causedByTheFilter(refused), "refused by the allow-list, not later: " + refused);
		assertFalse(tripped, "the foreign type was built");
	}

	@Test
	@DisplayName("a vault file that is not an envelope is refused when opened with a password and a key file")
	void openingWithPasswordAndKeyFile_aCraftedVaultFile_isRefused() throws Exception
	{
		tripped = false;
		File vault = new File(directory, "crafted.mcrdb");
		Files.write(vault.toPath(), serialized(new Tripwire()));
		File keyFile = new File(directory, "private-key.pem");
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), keyFile);

		Exception refused = assertThrows(Exception.class, () -> ApplicationXmlFileReader
			.getApplicationModelBean(vault, TestPasswords.throwaway().toCharArray(), keyFile));

		assertTrue(causedByTheFilter(refused), "refused by the allow-list, not later: " + refused);
		assertFalse(tripped, "the foreign type was built");
	}

	private static boolean causedByTheFilter(final Throwable thrown)
	{
		for (Throwable cause = thrown; cause != null; cause = cause.getCause())
		{
			if (cause instanceof InvalidClassException)
			{
				return true;
			}
		}
		return false;
	}
}

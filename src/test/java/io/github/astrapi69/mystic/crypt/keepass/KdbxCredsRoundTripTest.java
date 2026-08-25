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
package io.github.astrapi69.mystic.crypt.keepass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

public class KdbxCredsRoundTripTest
{

	/**
	 * Registers Bouncy Castle for this test class instead of relying on another test having done it
	 * already. The provider is global to the JVM, so in a full suite run some other class registers
	 * it first and this one passes by accident; run in isolation - as a mutation testing run does -
	 * it would fail with an InvalidKeySpecException.
	 */
	@BeforeAll
	public static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	public void testPasswordOnlyRoundTrip() throws Exception
	{
		byte[] kdbxBytes = createDatabase(
			new KdbxCreds("test-password".getBytes(StandardCharsets.UTF_8)));

		SimpleDatabase reloaded = SimpleDatabase.load(
			new KdbxCreds("test-password".getBytes(StandardCharsets.UTF_8)),
			new ByteArrayInputStream(kdbxBytes));

		assertEquals(1, reloaded.getRootGroup().getEntries().size());
	}

	@Test
	public void testPasswordAndRawKeyFileRoundTrip() throws Exception
	{
		File keyFile = Files.createTempFile("test-key", ".keyx").toFile();
		keyFile.deleteOnExit();
		// a raw 32-byte binary key file, the simplest legacy key file format KeePass accepts
		byte[] keyFileBytes = new byte[32];
		for (int i = 0; i < keyFileBytes.length; i++)
		{
			keyFileBytes[i] = (byte)i;
		}
		try (FileOutputStream fos = new FileOutputStream(keyFile))
		{
			fos.write(keyFileBytes);
		}

		roundTripWithKeyFile(keyFile);
	}

	@Test
	public void testPasswordAndXmlKeyFileRoundTrip() throws Exception
	{
		File keyFile = Files.createTempFile("test-key", ".keyx").toFile();
		keyFile.deleteOnExit();
		// the modern KeePass 2.x key file format: a 32-byte key, base64-encoded, wrapped in XML
		byte[] rawKey = new byte[32];
		for (int i = 0; i < rawKey.length; i++)
		{
			rawKey[i] = (byte)(i * 3);
		}
		String base64Key = java.util.Base64.getEncoder().encodeToString(rawKey);
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<KeyFile>\n" + "\t<Meta>\n"
			+ "\t\t<Version>1.00</Version>\n" + "\t</Meta>\n" + "\t<Key>\n" + "\t\t<Data>"
			+ base64Key + "</Data>\n" + "\t</Key>\n" + "</KeyFile>";
		try (FileOutputStream fos = new FileOutputStream(keyFile))
		{
			fos.write(xml.getBytes(StandardCharsets.UTF_8));
		}

		roundTripWithKeyFile(keyFile);
	}

	private void roundTripWithKeyFile(File keyFile) throws Exception
	{
		byte[] kdbxBytes;
		try (InputStream keyFileStream = Files.newInputStream(keyFile.toPath()))
		{
			KdbxCreds credentials = new KdbxCreds("test-password".getBytes(StandardCharsets.UTF_8),
				keyFileStream);
			kdbxBytes = createDatabase(credentials);
		}

		try (InputStream keyFileStream = Files.newInputStream(keyFile.toPath()))
		{
			KdbxCreds reloadCredentials = new KdbxCreds(
				"test-password".getBytes(StandardCharsets.UTF_8), keyFileStream);
			SimpleDatabase reloaded = SimpleDatabase.load(reloadCredentials,
				new ByteArrayInputStream(kdbxBytes));
			assertEquals(1, reloaded.getRootGroup().getEntries().size());
		}
	}

	private byte[] createDatabase(KdbxCreds credentials) throws Exception
	{
		SimpleDatabase database = new SimpleDatabase();
		database.getRootGroup().addEntry(database.newEntry("test-entry"));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		database.save(credentials, outputStream);
		return outputStream.toByteArray();
	}

}

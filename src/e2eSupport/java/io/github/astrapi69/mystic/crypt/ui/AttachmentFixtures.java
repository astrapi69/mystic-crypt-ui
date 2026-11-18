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
package io.github.astrapi69.mystic.crypt.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * The files the attachment tests attach and the way they read an attachment back: a UTF-8 text file
 * and a file of all 256 byte values, and an attachment as its name and the SHA-256 of its bytes
 */
final class AttachmentFixtures
{

	static final String TEXT_NAME = "gruesse.txt";

	static final String TEXT = "Grüße aus Köln, ß und €\n";

	static final String BINARY_NAME = "all-bytes.bin";

	private AttachmentFixtures()
	{
	}

	/** Writes the UTF-8 text file into the directory */
	static File writeText(final File directory) throws IOException
	{
		File text = new File(directory, TEXT_NAME);
		Files.writeString(text.toPath(), TEXT, StandardCharsets.UTF_8);
		return text;
	}

	/** Writes the file that holds each byte value from 0 to 255 once, in order */
	static File writeEveryByteValue(final File directory) throws IOException
	{
		File binary = new File(directory, BINARY_NAME);
		byte[] everyByte = new byte[256];
		for (int value = 0; value < everyByte.length; value++)
		{
			everyByte[value] = (byte)value;
		}
		Files.write(binary.toPath(), everyByte);
		return binary;
	}

	/** The SHA-256 of the bytes as lower case hex */
	static String sha256Hex(final byte[] bytes)
	{
		try
		{
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		}
		catch (NoSuchAlgorithmException missing)
		{
			throw new IllegalStateException("this Java runtime has no SHA-256", missing);
		}
	}

	/** The SHA-256 of the file's bytes as lower case hex */
	static String sha256Hex(final File file) throws IOException
	{
		return sha256Hex(Files.readAllBytes(file.toPath()));
	}

	/** The entry's attachments, each as its name and the SHA-256 of its bytes */
	static Map<String, String> byNameAndSha256(final MysticCryptEntryModelBean entry)
	{
		List<FileContentInfo> resources = entry.getResources() == null
			? List.of()
			: entry.getResources();
		Map<String, String> attachments = new TreeMap<>();
		for (FileContentInfo resource : resources)
		{
			attachments.put(resource.getName(), sha256Hex(resource.getContent()));
		}
		return attachments;
	}
}

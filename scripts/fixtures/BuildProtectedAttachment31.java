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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.format.KdbxHeader;
import org.linguafranca.pwdb.format.KdbxSerializer;
import org.linguafranca.pwdb.security.StreamEncryptor;

/**
 * Builds the fixture of #568: a KDBX 3.1 file whose attachment is protected. KeePassXC cannot write
 * one - it keeps a file at 3.1 when it adds an attachment, but writes the attachment compressed
 * and unprotected - so this takes such a file and protects its attachment the way KeePass 2 writes
 * a protected binary: {@code Protected="True"}, not compressed, XORed with the file's inner
 * Salsa20 stream in document order. Every protected value after it is encrypted again, because the
 * stream position moves; {@code HeaderHash} is dropped, because the header is written anew.
 * <p>
 * Run outside the build, see {@code src/e2eKdbx/resources/kdbx/README.md}:
 * {@code java -cp <KeePassJava2 and its libraries> BuildProtectedAttachment31.java in.kdbx password out.kdbx}
 */
public class BuildProtectedAttachment31
{

	private static final Pattern PROTECTED_VALUE = Pattern
		.compile("(<Value Protected=\"True\">)([^<]*)(</Value>)");

	private static final Pattern COMPRESSED_BINARY = Pattern
		.compile("<Binary ID=\"0\" Compressed=\"True\">([^<]*)</Binary>");

	public static void main(final String[] args) throws Exception
	{
		KdbxCredentials credentials = new KdbxCredentials(args[1].getBytes(StandardCharsets.UTF_8));
		KdbxHeader header = new KdbxHeader();
		String xml;
		try (InputStream in = new FileInputStream(args[0]);
			InputStream plain = KdbxSerializer.createUnencryptedInputStream(credentials, header, in))
		{
			xml = new String(plain.readAllBytes(), StandardCharsets.UTF_8);
		}
		if (header.getVersion() != 3)
		{
			throw new IllegalStateException("not a KDBX 3.1 file: version " + header.getVersion());
		}

		StreamEncryptor reading = new StreamEncryptor.Salsa20(header.getInnerRandomStreamKey());
		List<byte[]> plaintexts = new ArrayList<>();
		Matcher values = PROTECTED_VALUE.matcher(xml);
		while (values.find())
		{
			plaintexts.add(reading.decrypt(Base64.getDecoder().decode(values.group(2))));
		}

		Matcher binary = COMPRESSED_BINARY.matcher(xml);
		if (!binary.find())
		{
			throw new IllegalStateException("the file has no compressed binary with ID 0");
		}
		byte[] attachment;
		try (InputStream gzip = new GZIPInputStream(
			new ByteArrayInputStream(Base64.getDecoder().decode(binary.group(1)))))
		{
			attachment = gzip.readAllBytes();
		}

		StreamEncryptor writing = new StreamEncryptor.Salsa20(header.getInnerRandomStreamKey());
		String withProtectedBinary = xml.substring(0, binary.start())
			+ "<Binary ID=\"0\" Protected=\"True\">"
			+ Base64.getEncoder().encodeToString(writing.encrypt(attachment)) + "</Binary>"
			+ xml.substring(binary.end());
		StringBuilder rebuilt = new StringBuilder();
		Matcher again = PROTECTED_VALUE.matcher(withProtectedBinary);
		int index = 0;
		while (again.find())
		{
			again.appendReplacement(rebuilt,
				Matcher.quoteReplacement(again.group(1)
					+ Base64.getEncoder().encodeToString(writing.encrypt(plaintexts.get(index++)))
					+ again.group(3)));
		}
		again.appendTail(rebuilt);
		String result = rebuilt.toString().replaceAll("\\s*<HeaderHash>[^<]*</HeaderHash>", "");

		try (OutputStream file = new FileOutputStream(args[2]);
			OutputStream encrypted = KdbxSerializer.createEncryptedOutputStream(credentials, header,
				file))
		{
			encrypted.write(result.getBytes(StandardCharsets.UTF_8));
		}
		System.out.println("attachment " + attachment.length + " bytes protected, " + index
			+ " protected values encrypted again: " + args[2]);
	}
}

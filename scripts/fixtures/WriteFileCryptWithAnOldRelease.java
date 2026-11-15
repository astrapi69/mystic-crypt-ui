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
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Provider;
import java.security.Security;

/**
 * Writes a file and a text with the file encryption window of a published release, through that
 * release's own {@code FileCryptSupport.encryptFile} and {@code encryptText}, the calls the window
 * makes for a user. Reflection only, so nothing of this checkout is compiled against it.
 * <p>
 * Usage:
 * {@code java -cp <release app jar>:<the release's file-crypt plugin classes> WriteFileCryptWithAnOldRelease.java <version> <empty directory>},
 * the passphrase on standard input; {@code write-file-crypt-of-an-old-release.sh} assembles the
 * class path from the release's installer. It prints each file with its size, then WRITTEN.
 */
public class WriteFileCryptWithAnOldRelease
{

	private static final PrintStream OUT = new PrintStream(
		new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

	public static void main(final String[] arguments) throws Exception
	{
		String version = arguments[0];
		File directory = new File(arguments[1]).getAbsoluteFile();
		String passphrase = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).strip();
		Security.addProvider((Provider)Class
			.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").getDeclaredConstructor()
			.newInstance());
		Class<?> window = Class
			.forName("io.github.astrapi69.mystic.crypt.plugin.filecrypt.FileCryptSupport");
		Method encryptFile = window.getMethod("encryptFile", File.class, File.class, String.class);
		Method encryptText = window.getMethod("encryptText", String.class, String.class);

		File plain = File.createTempFile("window-" + version, ".txt");
		try
		{
			Files.writeString(plain.toPath(),
				"a file the file encryption window of " + version + " sealed\n",
				StandardCharsets.UTF_8);
			File sealed = new File(directory, "written-by-the-window-" + version + ".txt.mcenc");
			encryptFile.invoke(null, plain, sealed, passphrase);
			OUT.println(sealed.getName() + ": " + sealed.length() + " bytes");
		}
		finally
		{
			Files.delete(plain.toPath());
		}
		String text = (String)encryptText.invoke(null,
			"a text the file encryption window of " + version + " sealed: ä ö ü ß", passphrase);
		File textFile = new File(directory, "written-by-the-window-" + version + ".text");
		Files.writeString(textFile.toPath(), text, StandardCharsets.UTF_8);
		OUT.println(textFile.getName() + ": " + text.length() + " Base64 characters");
		OUT.println("WRITTEN");
	}
}

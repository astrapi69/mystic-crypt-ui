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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Map;

/**
 * Opens a vault with a release older than 8.6, through whichever
 * {@code ApplicationXmlFileReader.getApplicationModelBean(File, password)} that release has - a
 * String in 7.x, characters later - and prints its groups and entry titles. The check that a vault
 * in {@code src/testSupport/resources/compat/vaults} opens in the release that wrote it (#624).
 * <p>
 * Usage: {@code java -cp <release app jar> OpenWithAnOldRelease.java <vault file>}, the password on
 * standard input; with {@code --key-file <PEM file>} after the vault file, through the release's
 * key-file reader; with {@code --password-and-key-file <PEM file>}, through its reader for both
 * (#650). It prints OPENED and one line per entry ({@code EntryLines}), or REFUSED with the
 * root cause and exits 2. Nothing of this checkout is on the class path.
 */
public class OpenWithAnOldRelease
{
	public static void main(String[] arguments) throws Exception
	{
		Security.addProvider((Provider)Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").getDeclaredConstructor().newInstance());
		String password = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).strip();
		Class<?> reader = Class.forName("io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileReader");
		File vault = new File(arguments[0]).getAbsoluteFile();
		Object model = null;
		try
		{
			if (arguments.length >= 3 && "--password-and-key-file".equals(arguments[1]))
			{
				for (Method method : reader.getMethods())
				{
					if (method.getName().equals("getApplicationModelBean") && method.getParameterCount() == 3
						&& method.getParameterTypes()[2] == File.class)
					{
						Class<?> second = method.getParameterTypes()[1];
						Object secret = second == String.class ? password : password.toCharArray();
						model = method.invoke(null, vault, secret, new File(arguments[2]));
					}
				}
			}
			else if (arguments.length >= 3 && "--key-file".equals(arguments[1]))
			{
				Object privateKey = Class.forName("io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader")
					.getMethod("readPemPrivateKey", File.class).invoke(null, new File(arguments[2]));
				model = reader.getMethod("getApplicationModelBean", File.class, java.security.PrivateKey.class)
					.invoke(null, vault, privateKey);
			}
			else
			{
				for (Method method : reader.getMethods())
				{
					if (method.getName().equals("getApplicationModelBean") && method.getParameterCount() == 2
						&& method.getParameterTypes()[0] == File.class)
					{
						Class<?> second = method.getParameterTypes()[1];
						if (second == String.class) model = method.invoke(null, vault, password);
						else if (second == char[].class) model = method.invoke(null, vault, password.toCharArray());
					}
				}
			}
		}
		catch (java.lang.reflect.InvocationTargetException refused)
		{
			Throwable cause = refused;
			while (cause.getCause() != null) cause = cause.getCause();
			System.out.println("REFUSED " + cause.getClass().getSimpleName() + ": "
				+ String.valueOf(cause.getMessage()).lines().findFirst().orElse(""));
			System.exit(2);
		}
		Map<?, ?> tree = (Map<?, ?>)model.getClass().getMethod("getRootTreeAsMap").invoke(model);
		System.out.println("OPENED groups=" + (tree == null ? 0 : tree.size()));
		EntryLines.of(model).forEach(System.out::println);
	}
}

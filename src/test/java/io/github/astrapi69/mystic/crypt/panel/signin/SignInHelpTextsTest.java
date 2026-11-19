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
package io.github.astrapi69.mystic.crypt.panel.signin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The texts of the help fallback dialogs, shown when the wiki cannot be reached, come from the
 * message bundles in both languages, and the dialog of the create panel carries its own title (it
 * said "sign in" for creating a database).
 */
class SignInHelpTextsTest
{

	private static Properties bundle(final String name) throws IOException
	{
		Properties properties = new Properties();
		try (InputStream in = SignInHelpTextsTest.class.getResourceAsStream(name);
			InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			properties.load(reader);
		}
		return properties;
	}

	@ParameterizedTest(name = "{0} in {1}")
	@DisplayName("each help fallback text is in the bundle, in English and in German")
	@CsvSource({ "signin.help.open.title,/ui/messages.properties",
			"signin.help.open.dialog.title,/ui/messages.properties",
			"signin.help.open.content,/ui/messages.properties",
			"signin.help.create.title,/ui/messages.properties",
			"signin.help.create.dialog.title,/ui/messages.properties",
			"signin.help.create.content,/ui/messages.properties",
			"signin.help.open.title,/ui/messages_de.properties",
			"signin.help.open.dialog.title,/ui/messages_de.properties",
			"signin.help.open.content,/ui/messages_de.properties",
			"signin.help.create.title,/ui/messages_de.properties",
			"signin.help.create.dialog.title,/ui/messages_de.properties",
			"signin.help.create.content,/ui/messages_de.properties" })
	void helpText_isInTheBundle(final String key, final String resource) throws IOException
	{
		assertNotNull(bundle(resource).getProperty(key), key + " missing in " + resource);
	}

	@ParameterizedTest(name = "{0}")
	@DisplayName("the create dialog is titled for creating, not for signing in")
	@CsvSource({ "/ui/messages.properties", "/ui/messages_de.properties" })
	void createDialogTitle_isNotTheSignInTitle(final String resource) throws IOException
	{
		Properties bundle = bundle(resource);

		assertNotEquals(bundle.getProperty("signin.help.open.dialog.title"),
			bundle.getProperty("signin.help.create.dialog.title"));
		assertFalse(bundle.getProperty("signin.help.create.dialog.title").toLowerCase()
			.contains("sign in"));
	}
}

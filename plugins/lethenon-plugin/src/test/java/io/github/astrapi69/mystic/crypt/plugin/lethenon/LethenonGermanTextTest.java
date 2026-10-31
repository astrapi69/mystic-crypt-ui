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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The plugin's German text (#540) translates the "Before you start" view and nothing that the
 * English bundle does not have: a key only the German file knows is a translation of nothing, and a
 * view key missing from it shows that line in English inside a German window.
 */
class LethenonGermanTextTest
{

	private static Properties read(final String resource) throws IOException
	{
		try (InputStream stream = LethenonGermanTextTest.class.getResourceAsStream(resource))
		{
			assertNotNull(stream, resource);
			Properties properties = new Properties();
			properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
			return properties;
		}
	}

	@Test
	@DisplayName("the German file translates no key the English file does not have")
	void everyGermanKey_isAnEnglishKey() throws IOException
	{
		Set<String> english = read("/lethenon/messages.properties").stringPropertyNames();
		Set<String> orphans = new TreeSet<>(
			read("/lethenon/messages_de.properties").stringPropertyNames());
		orphans.removeAll(english);

		assertTrue(orphans.isEmpty(), "translated keys with no English original: " + orphans);
	}

	@Test
	@DisplayName("every text of the view \"Before you start\" and its menu item is in the German file")
	void everyKeyOfTheView_isTranslated() throws IOException
	{
		Properties english = read("/lethenon/messages.properties");
		Set<String> german = read("/lethenon/messages_de.properties").stringPropertyNames();
		List<String> viewKeys = english.stringPropertyNames().stream()
			.filter(key -> key.startsWith(LaunchChecklist.KEY_PREFIX)
				|| key.equals(LethenonMenuContribution.FOR_OPERATORS_KEY))
			.sorted().toList();

		assertFalse(viewKeys.isEmpty(), "the English file has the view's text");
		Set<String> missing = new TreeSet<>(viewKeys);
		missing.removeAll(german);
		assertEquals(Set.of(), missing, "view keys without a German text");
	}

	/**
	 * The windows that answer in fields (#604), each with the prefix of its own keys, and the keys
	 * they share: a window is German as a whole or not at all
	 */
	static Stream<Arguments> windowsInFields()
	{
		return Stream.of(Arguments.of("Show a Balance", List.of("lethenon.balance.")),
			Arguments.of("Mine a Pun", List.of("lethenon.mine.", "lethenon.chain.kind.")),
			Arguments.of("Send", List.of("lethenon.send.", "lethenon.recipient.kind.")),
			Arguments.of("Sweep", List.of("lethenon.sweep.")),
			Arguments.of("Synchronise with a Node", List.of("lethenon.sync.")),
			Arguments.of("Verify a Chain", List.of("lethenon.verify.")),
			Arguments.of("Run a Node", List.of("lethenon.node.")),
			Arguments.of("Wallet", List.of("lethenon.wallet.", "lethenon.backup.", "lethenon.network.", LethenonMenuContribution.WALLET_KEY)),
			Arguments.of("the menu", List.of("lethenon.menu.")),
			Arguments.of("what every window answers with",
				List.of("lethenon.answer.", "lethenon.input.", "lethenon.wallet.", "lethenon.failure.",
					"lethenon.value.")));
	}

	@ParameterizedTest(name = "every text of {0} is in the German file")
	@MethodSource("windowsInFields")
	@DisplayName("every text of a window that answers in fields is in the German file (#604)")
	void everyKeyOfAWindowInFields_isTranslated(final String window, final List<String> prefixes)
		throws IOException
	{
		Properties english = read("/lethenon/messages.properties");
		Set<String> german = read("/lethenon/messages_de.properties").stringPropertyNames();
		List<String> windowKeys = english.stringPropertyNames().stream()
			.filter(key -> prefixes.stream().anyMatch(key::startsWith)).sorted().toList();

		assertFalse(windowKeys.isEmpty(), "the English file has the text of " + window);
		Set<String> missing = new TreeSet<>(windowKeys);
		missing.removeAll(german);
		assertEquals(Set.of(), missing, "keys of " + window + " without a German text");
	}
}

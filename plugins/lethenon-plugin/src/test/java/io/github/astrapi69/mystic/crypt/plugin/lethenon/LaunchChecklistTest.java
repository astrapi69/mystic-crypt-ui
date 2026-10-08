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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.BinaryOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.resourcebundle.locale.ResourceBundleExtensions;

/**
 * The short form of lethenon's launch checklist that the "Before you start" view shows (#540): one
 * row per activity of lethenon's {@code docs/launch/launch-checklist.md}, each naming the provision
 * it rests on, and the five documents of lethenon#141 at their place on lethenon's develop branch.
 * <p>
 * The text is read from an explicit bundle in every test, never from the JVM's default locale: a
 * test that passes in English on one machine and in German on another proves neither.
 */
class LaunchChecklistTest
{

	private static final String DOCUMENTS = "https://github.com/astrapi69/lethenon/blob/develop/docs/launch/";

	/**
	 * The plugin's text in exactly one language: without the no-fallback control, asking for the
	 * base bundle on a German desktop returns the German one
	 */
	private static BinaryOperator<String> textIn(final Locale locale)
	{
		ResourceBundle bundle = ResourceBundle.getBundle("lethenon.messages", locale,
			ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
		return (key, defaultValue) -> ResourceBundleExtensions.getStringQuietly(bundle, key,
			defaultValue);
	}

	@ParameterizedTest(name = "in \"{0}\" (empty: English), every row names an activity, what it needs and its provision")
	@ValueSource(strings = { "", "de" })
	void everyCheck_isComplete(final String language)
	{
		List<LaunchCheck> checks = LaunchChecklist.checks(textIn(Locale.of(language)));

		assertFalse(checks.isEmpty());
		for (LaunchCheck check : checks)
		{
			assertFalse(check.activity().isBlank(), check.toString());
			assertFalse(check.authorisation().isBlank(), check.toString());
			assertFalse(check.provision().isBlank(), check.toString());
		}
	}

	@Test
	@DisplayName("the rows rest on the provisions the checklist names")
	void theRows_nameTheirProvisions()
	{
		List<String> provisions = LaunchChecklist.checks(textIn(Locale.ROOT)).stream()
			.map(LaunchCheck::provision).toList();

		for (String expected : List.of("Art. 4(3)(b)", "recital 93", "Art. 3(1)(17)", "Art. 4(5)",
			"Art. 59", "Art. 4(8)", "Art. 61"))
		{
			assertTrue(provisions.stream().anyMatch(provision -> provision.contains(expected)),
				expected + " in " + provisions);
		}
	}

	@Test
	@DisplayName("every row is translated into German, not shown in English through a fallback")
	void everyRow_isTranslatedIntoGerman()
	{
		List<LaunchCheck> english = LaunchChecklist.checks(textIn(Locale.ROOT));
		List<LaunchCheck> german = LaunchChecklist.checks(textIn(Locale.GERMAN));

		assertEquals(english.size(), german.size());
		for (int row = 0; row < english.size(); row++)
		{
			assertNotEquals(english.get(row).activity(), german.get(row).activity());
			assertNotEquals(english.get(row).authorisation(), german.get(row).authorisation());
		}
	}

	@Test
	@DisplayName("the notice says it is not legal advice, in English and in German")
	void theNotice_saysItIsNotLegalAdvice()
	{
		assertTrue(LaunchChecklist.notice(textIn(Locale.ROOT)).startsWith("Not legal advice."));
		assertTrue(LaunchChecklist.notice(textIn(Locale.GERMAN)).startsWith("Keine Rechtsberatung."));
	}

	@Test
	@DisplayName("the German text is read with its umlauts, so the file is read in the encoding it is written in")
	void theGermanText_keepsItsUmlauts()
	{
		String german = LaunchChecklist.notice(textIn(Locale.GERMAN));

		assertTrue(german.contains("ü"), german);
	}

	@Test
	@DisplayName("the five documents of lethenon#141, on lethenon's develop branch")
	void theDocuments_areTheFiveLaunchDocuments()
	{
		assertEquals(
			List.of(DOCUMENTS + "regulatory-overview.md", DOCUMENTS + "launch-checklist.md",
				DOCUMENTS + "messaging-guide.md", DOCUMENTS + "specification.md",
				DOCUMENTS + "infrastructure.md"),
			Arrays.stream(LaunchDocument.values()).map(LaunchDocument::url).toList());
	}

	@Test
	@DisplayName("every document has a title of its own, in English and in German")
	void everyDocument_hasATitle()
	{
		for (LaunchDocument document : LaunchDocument.values())
		{
			String english = document.title(textIn(Locale.ROOT));
			String german = document.title(textIn(Locale.GERMAN));

			assertFalse(english.isBlank(), document.name());
			assertNotEquals(english, german, document.name());
		}
	}
}

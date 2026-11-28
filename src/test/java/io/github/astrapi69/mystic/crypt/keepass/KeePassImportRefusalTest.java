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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.linguafranca.pwdb.format.KdbxCredentials;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The message a refused KeePass import shows (#568): words a person can act on for a KDBX 3.1 file
 * with protected attachments, the bundle's words for a refused import for anything else, from
 * whichever step it comes, and never the text of the exception (#1005). The texts are checked in
 * both languages; which one {@code Messages} returns depends on the JVM, so the choice is asserted
 * with what both languages share.
 */
class KeePassImportRefusalTest
{

	private static final File PROTECTED_KDBX31 = new File(
		"src/e2eKdbx/resources/kdbx/keepass2-kdbx31-protected-attachment.kdbx");

	private static final String KEY = "keepass.import.failed.kdbx31.protected.attachments";

	/** The key of the words for every other refused import */
	private static final String REFUSED_IMPORT = "keepass.import.failed.message";

	/** The text the messages of a refusal hold */
	private static final String TEXT = "text-" + UUID.randomUUID();

	@Test
	@DisplayName("a KDBX 3.1 file with protected attachments gets the readable message, not the library's")
	void aProtectedKdbx31File_getsTheReadableMessage()
	{
		String message = KeePassImportRefusal.messageFor(PROTECTED_KDBX31,
			credentials(TestPasswords.KEEPASS_FIXTURE), new IllegalArgumentException(
				"Unrecognized field \"Protected\"", new IllegalStateException(TEXT)));

		assertEquals(Messages.getString(KEY, ""), message);
		assertTrue(message.contains("KDBX 3.1") && message.contains("KDBX 4"), message);
		assertTrue(message.contains("KeePassXC"), message);
		assertFalse(message.contains("Unrecognized") || message.contains(TEXT), message);
	}

	@Test
	@DisplayName("any other refusal gets the bundle's words for a refused import, not the library's reason")
	void anyOtherRefusal_getsTheWordsOfTheBundle()
	{
		String message = KeePassImportRefusal.messageFor(PROTECTED_KDBX31,
			credentials(TestPasswords.throwaway()), new IllegalStateException("the reason"));

		assertEquals(wordsOfARefusedImport(), message);
		assertFalse(message.contains("KDBX 3.1"), message);
	}

	/**
	 * Refusals of the kinds an import can end with, from whichever step failed, each with the text
	 * in one place of it ({@link RefusalsOfEveryKind})
	 */
	static Stream<Arguments> refusalsOfEveryKind()
	{
		return RefusalsOfEveryKind.carrying(TEXT);
	}

	@ParameterizedTest(name = "{0}: the dialog shows the words of the bundle, and nothing of the refusal")
	@MethodSource("refusalsOfEveryKind")
	void aRefusal_isShownInTheWordsOfTheBundle(final String caseName, final Exception refusal)
	{
		String message = KeePassImportRefusal.messageFor(PROTECTED_KDBX31,
			credentials(TestPasswords.throwaway()), refusal);

		assertEquals(wordsOfARefusedImport(), message);
		assertFalse(message.contains(TEXT), "nothing of the refusal");
	}

	@Test
	@DisplayName("without credentials - a key file that could not be read - the bundle's words")
	void withoutCredentials_theWordsOfTheBundle()
	{
		String message = KeePassImportRefusal.messageFor(PROTECTED_KDBX31, null,
			new IllegalStateException("no key file"));

		assertEquals(wordsOfARefusedImport(), message);
	}

	/**
	 * The refusals the import gives before it reads a file - no vault is open, or no KeePass file
	 * was chosen - each with the key of its words and its words in English
	 */
	static Stream<Arguments> refusalsBeforeAFileIsRead()
	{
		return Stream.of(
			Arguments.of("no vault is open", "keepass.import.failed.not.signed.in",
				"Please sign in first.", (Supplier<String>)KeePassImportRefusal::notSignedIn),
			Arguments.of("no KeePass file is chosen", "keepass.import.failed.no.file",
				"No KeePass file selected.",
				(Supplier<String>)KeePassImportRefusal::noFileSelected));
	}

	@ParameterizedTest(name = "{0}: the dialog shows English words the bundle does not hold")
	@MethodSource("refusalsBeforeAFileIsRead")
	void aRefusalBeforeAFileIsRead_showsWordsTheBundleDoesNotHold(final String caseName,
		final String key, final String english, final Supplier<String> refusal)
	{
		assertEquals(english, refusal.get());
		assertFalse(ResourceBundle.getBundle("ui.messages", Locale.GERMAN).containsKey(key),
			"the bundle holds no words for it, in neither language");
	}

	@Test
	@DisplayName("the words of a refused import read in English and in German, with real umlauts")
	void theWordsOfARefusedImport_inBothLanguages()
	{
		String english = ResourceBundle.getBundle("ui.messages", Locale.ROOT)
			.getString(REFUSED_IMPORT);
		String german = ResourceBundle.getBundle("ui.messages", Locale.GERMAN)
			.getString(REFUSED_IMPORT);

		assertEquals("Could not import the KeePass file. The password or the key file may be "
			+ "wrong, or the file may be unreadable, damaged or in a format this version cannot "
			+ "read.", english);
		assertEquals("Die KeePass-Datei konnte nicht importiert werden. Möglicherweise ist das "
			+ "Passwort oder die Schlüsseldatei falsch, oder die Datei ist nicht lesbar, beschädigt "
			+ "oder in einem Format, das diese Version nicht unterstützt.", german);
	}

	/** What the bundle says for a refused import, in the language of this JVM */
	private static String wordsOfARefusedImport()
	{
		return Messages.getString(REFUSED_IMPORT, "");
	}

	@Test
	@DisplayName("the readable message reads the same way in English and in German, with the labels KeePassXC shows")
	void theReadableMessage_inBothLanguages()
	{
		String english = ResourceBundle.getBundle("ui.messages", Locale.ROOT).getString(KEY);
		String german = ResourceBundle.getBundle("ui.messages", Locale.GERMAN).getString(KEY);

		assertTrue(english.contains("Database > Database Settings... > Security")
			&& english.contains("Encryption Settings") && english.contains("KDBX 4 (recommended)"),
			english);
		assertTrue(german.contains("Datenbank > Datenbankeinstellungen... > Sicherheit")
			&& german.contains("Verschlüsselungs-Einstellungen")
			&& german.contains("KDBX 4 (empfohlen)"), german);
		assertTrue(german.contains("geschützte Anhänge") && german.contains("lässt"),
			"real umlauts, not a transliteration: " + german);
		for (String transliteration : new String[] { "geschuetzt", "Anhaenge", "laesst", "oeffnen",
				"Verschluesselung", "bestaetigen" })
		{
			assertFalse(german.contains(transliteration), german);
		}
	}

	@Test
	@DisplayName("every German key has its English original, so a typo in a key cannot hide a text")
	void everyGermanKey_hasItsEnglishOriginal()
	{
		ResourceBundle english = ResourceBundle.getBundle("ui.messages", Locale.ROOT);
		ResourceBundle german = ResourceBundle.getBundle("ui.messages", Locale.GERMAN);
		Set<String> englishKeys = english.keySet();

		for (String key : german.keySet())
		{
			assertTrue(englishKeys.contains(key), "German key without an English original: " + key);
		}
		assertEquals("Import fehlgeschlagen", german.getString("keepass.import.failed.title"));
	}

	private static KdbxCredentials credentials(String password)
	{
		return new KdbxCredentials(password.getBytes(StandardCharsets.UTF_8));
	}
}

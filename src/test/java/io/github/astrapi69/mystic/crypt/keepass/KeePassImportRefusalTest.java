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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.format.KdbxCredentials;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The message a refused KeePass import shows (#568): words a person can act on for a KDBX 3.1 file
 * with protected attachments, the library's reason for anything else. The texts are checked in both
 * languages; which one {@code Messages} returns depends on the JVM, so the choice is asserted with
 * what both languages share.
 */
class KeePassImportRefusalTest
{

	private static final File PROTECTED_KDBX31 = new File(
		"src/e2eKdbx/resources/kdbx/keepass2-kdbx31-protected-attachment.kdbx");

	private static final String KEY = "keepass.import.failed.kdbx31.protected.attachments";

	@Test
	@DisplayName("a KDBX 3.1 file with protected attachments gets the readable message, not the library's")
	void aProtectedKdbx31File_getsTheReadableMessage()
	{
		String message = KeePassImportRefusal.messageFor(PROTECTED_KDBX31,
			credentials(TestPasswords.KEEPASS_FIXTURE),
			new IllegalArgumentException("Unrecognized field \"Protected\""));

		assertTrue(message.contains("KDBX 3.1") && message.contains("KDBX 4"), message);
		assertTrue(message.contains("KeePassXC"), message);
		assertFalse(message.contains("Unrecognized"), message);
	}

	@Test
	@DisplayName("any other refusal keeps the library's reason")
	void anyOtherRefusal_keepsTheReason()
	{
		String message = KeePassImportRefusal.messageFor(PROTECTED_KDBX31,
			credentials("not the password"), new IllegalStateException("the reason"));

		assertTrue(message.endsWith(" the reason"), message);
		assertFalse(message.contains("KDBX 3.1"), message);
	}

	@Test
	@DisplayName("without credentials - a key file that could not be read - the reason is kept")
	void withoutCredentials_theReasonIsKept()
	{
		String message = KeePassImportRefusal.messageFor(PROTECTED_KDBX31, null,
			new IllegalStateException("no key file"));

		assertTrue(message.endsWith(" no key file"), message);
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

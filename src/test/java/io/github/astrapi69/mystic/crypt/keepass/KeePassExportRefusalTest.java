/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.Messages;

/**
 * The message a refused KeePass export shows, for the export of the whole tree and of chosen
 * entries alike: the bundle's words for a refused export, never the text of the exception (#1005)
 */
class KeePassExportRefusalTest
{

	/** The key of the words for a refused export */
	private static final String REFUSED_EXPORT = "keepass.export.failed.message";

	/** The key of the title a refused export of the whole tree shows */
	private static final String REFUSED_EXPORT_TITLE = "keepass.export.failed.title";

	/** The text the messages of a refusal hold */
	private static final String TEXT = "text-" + UUID.randomUUID();

	static Stream<Arguments> refusalsOfEveryKind()
	{
		return RefusalsOfEveryKind.carrying(TEXT);
	}

	@ParameterizedTest(name = "{0}: the dialog shows the words of the bundle, and nothing of the refusal")
	@MethodSource("refusalsOfEveryKind")
	void aRefusal_isShownInTheWordsOfTheBundle(final String caseName, final Exception refusal)
	{
		String message = KeePassExportRefusal.messageFor(refusal);

		assertEquals(Messages.getString(REFUSED_EXPORT, ""), message);
		assertFalse(message.contains(TEXT), "nothing of the refusal");
	}

	/**
	 * The refusals the export of the whole tree gives before it writes a file - no vault is open,
	 * or no destination file was chosen - each with the key of its words and its words in English
	 */
	static Stream<Arguments> refusalsBeforeAFileIsWritten()
	{
		return Stream.of(
			Arguments.of("no vault is open", "keepass.export.failed.not.signed.in",
				"Please sign in first.", (Supplier<String>)KeePassExportRefusal::notSignedIn),
			Arguments.of("no destination file is chosen", "keepass.export.failed.no.file",
				"No destination file selected.",
				(Supplier<String>)KeePassExportRefusal::noFileSelected));
	}

	@ParameterizedTest(name = "{0}: the dialog shows English words the bundle does not hold")
	@MethodSource("refusalsBeforeAFileIsWritten")
	void aRefusalBeforeAFileIsWritten_showsWordsTheBundleDoesNotHold(final String caseName,
		final String key, final String english, final Supplier<String> refusal)
	{
		assertEquals(english, refusal.get());
		assertFalse(ResourceBundle.getBundle("ui.messages", Locale.GERMAN).containsKey(key),
			"the bundle holds no words for it, in neither language");
	}

	@Test
	@DisplayName("the words of a refused export read in English and in German")
	void theWordsOfARefusedExport_inBothLanguages()
	{
		String english = ResourceBundle.getBundle("ui.messages", Locale.ROOT)
			.getString(REFUSED_EXPORT);
		String german = ResourceBundle.getBundle("ui.messages", Locale.GERMAN)
			.getString(REFUSED_EXPORT);

		assertEquals("Could not export the KeePass file.", english);
		assertEquals("Die KeePass-Datei konnte nicht exportiert werden.", german);
	}

	@Test
	@DisplayName("the title of a refused export of the whole tree reads in English and in German")
	void theTitleOfARefusedExport_inBothLanguages()
	{
		String english = ResourceBundle.getBundle("ui.messages", Locale.ROOT)
			.getString(REFUSED_EXPORT_TITLE);
		String german = ResourceBundle.getBundle("ui.messages", Locale.GERMAN)
			.getString(REFUSED_EXPORT_TITLE);

		assertEquals("Export failed", english);
		assertEquals("Export fehlgeschlagen", german);
	}
}

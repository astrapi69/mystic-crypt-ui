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

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The words of the KeePass import and export dialogs: each comes from the bundle, in English and in
 * German, and the English words are the ones the dialogs showed as literals before
 */
class KeePassDialogTextsTest
{

	static Stream<Arguments> everyText()
	{
		return Stream.of(
			Arguments.of("import dialog title", "keepass.import.dialog.title",
				"Import from KeePass", "Import aus KeePass",
				(Supplier<String>)KeePassDialogTexts::importTitle),
			Arguments.of("import done message", "keepass.import.done.message",
				"KeePass database imported successfully.",
				"KeePass-Datenbank erfolgreich importiert.",
				(Supplier<String>)KeePassDialogTexts::importDone),
			Arguments.of("import done title", "keepass.import.done.title", "Import successful",
				"Import erfolgreich", (Supplier<String>)KeePassDialogTexts::importDoneTitle),
			Arguments.of("export dialog title", "keepass.export.dialog.title", "Export to KeePass",
				"Export nach KeePass", (Supplier<String>)KeePassDialogTexts::exportTitle),
			Arguments.of("export done message", "keepass.export.done.message",
				"KeePass database exported successfully.",
				"KeePass-Datenbank erfolgreich exportiert.",
				(Supplier<String>)KeePassDialogTexts::exportDone),
			Arguments.of("export done title", "keepass.export.done.title", "Export successful",
				"Export erfolgreich", (Supplier<String>)KeePassDialogTexts::exportDoneTitle));
	}

	@ParameterizedTest(name = "{0}: the words read as the dialog showed them")
	@MethodSource("everyText")
	void everyText_isInTheBundleInBothLanguages(final String caseName, final String key,
		final String english, final String german, final Supplier<String> text)
	{
		assertEquals(english, text.get());
	}
}

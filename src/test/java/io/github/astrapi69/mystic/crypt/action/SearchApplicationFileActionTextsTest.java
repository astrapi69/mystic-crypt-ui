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
package io.github.astrapi69.mystic.crypt.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

/**
 * The words of the search dialogs that come from the bundle, in English and in German
 */
class SearchApplicationFileActionTextsTest
{

	private static String text(final Locale locale, final String key)
	{
		return ResourceBundle.getBundle("ui.messages", locale).getString(key);
	}

	@Test
	void theTitle_isInTheBundleInBothLanguages()
	{
		assertEquals("Search", text(Locale.ROOT, "search.dialog.title"));
		assertEquals("Suche", text(Locale.GERMAN, "search.dialog.title"));
		assertEquals("Search", SearchApplicationFileAction.dialogTitle());
	}

	@Test
	void theRefusalWithoutAnOpenVault_isInTheBundleInBothLanguages()
	{
		assertEquals("Please sign in first.", text(Locale.ROOT, "search.failed.not.signed.in"));
		assertEquals("Bitte zuerst anmelden.", text(Locale.GERMAN, "search.failed.not.signed.in"));
		assertEquals("Please sign in first.", SearchApplicationFileAction.notSignedIn());
	}
}

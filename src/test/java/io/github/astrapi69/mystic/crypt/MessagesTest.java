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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every host message comes back as the bundle holds it (#533). The lookup with a default ran each
 * value through MessageFormat, twice, without anything to format it with, and lost every apostrophe
 * on the way - "the certificate's" became "the certificates" (resourcebundle-core#21).
 */
class MessagesTest
{

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("ui.messages");

	@Test
	@DisplayName("every message looked up with a default comes back exactly as the bundle holds it")
	void getString_withADefault_returnsEveryValueAsTheBundleHoldsIt()
	{
		List<String> changed = Collections.list(BUNDLE.getKeys()).stream()
			.filter(key -> !BUNDLE.getString(key).equals(Messages.getString(key, "default")))
			.sorted().map(key -> key + ": '" + BUNDLE.getString(key) + "' came back as '"
				+ Messages.getString(key, "default") + "'")
			.toList();

		assertTrue(changed.isEmpty(),
			changed.size() + " message(s) changed on the way:\n" + String.join("\n", changed));
	}

	@Test
	@DisplayName("a tooltip with an apostrophe keeps it")
	void getString_keepsTheApostrophe()
	{
		assertEquals("the certificate's unique serial number, assigned by whoever issues it",
			Messages.getString("wizard.certificate.dates.tooltip.serial.number", "default"));
	}
}

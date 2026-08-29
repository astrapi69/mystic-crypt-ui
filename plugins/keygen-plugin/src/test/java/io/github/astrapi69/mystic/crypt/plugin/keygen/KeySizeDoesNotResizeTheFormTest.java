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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.Security;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.key.KeySize;

/**
 * Changing the key size moved the whole encrypt panel to the right: the combo box took its width
 * from whatever was selected, so picking a longer number made the column wider and everything to
 * the right of it moved. A form that shifts while it is being filled in is a form nobody can aim
 * at.
 */
class KeySizeDoesNotResizeTheFormTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	@DisplayName("the panels keep their place whatever key size is selected")
	void thePanelsKeepTheirPlaceWhateverKeySizeIsSelected()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.setSize(1200, 800);
		Set<String> places = new LinkedHashSet<>();

		for (KeySize keySize : KeySize.values())
		{
			panel.getCryptographyPanel().getCmbKeySize().setSelectedItem(keySize);
			panel.doLayout();
			panel.getCryptographyPanel().doLayout();
			places.add(keySize + " -> encrypt at x="
				+ panel.getEnDecryptPanel().getBounds().x + " generate at x="
				+ panel.getCryptographyPanel().getBtnGenerate().getBounds().x);
		}

		assertEquals(1, places.stream().map(place -> place.substring(place.indexOf("->"))).distinct()
			.count(), "the form moves while it is being filled in: " + places);
	}

	@Test
	@DisplayName("the key size box keeps its width whatever is selected in it")
	void theKeySizeBoxKeepsItsWidthWhateverIsSelectedInIt()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		Set<Integer> widths = new LinkedHashSet<>();

		for (KeySize keySize : KeySize.values())
		{
			panel.getCryptographyPanel().getCmbKeySize().setSelectedItem(keySize);
			widths.add(panel.getCryptographyPanel().getCmbKeySize().getPreferredSize().width);
		}

		assertEquals(1, widths.size(),
			"the box changes width with what is selected, which moves everything beside it: "
				+ widths);
	}

}

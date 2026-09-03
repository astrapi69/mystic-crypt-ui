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
package io.github.astrapi69.mystic.crypt.panel.certificate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

/**
 * The distinguished name fields the Issuer and Subject wizard steps share had no tooltip - a
 * first-time user has no way to tell "Organisation Unit" or "Location" apart from a caption alone
 * (#148)
 */
class NewCertificateAttributesPanelTest
{

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertNotNull(tooltip, fieldName + " must have a tooltip");
		assertFalse(tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		NewCertificateAttributesPanel panel = new NewCertificateAttributesPanel();

		assertHasTooltip(panel.getTxtCommonName(), "common name");
		assertHasTooltip(panel.getTxtOrganization(), "organisation");
		assertHasTooltip(panel.getTxtOrganizationUnit(), "organisation unit");
		assertHasTooltip(panel.getTxtCountryCode(), "country code");
		assertHasTooltip(panel.getTxtState(), "state");
		assertHasTooltip(panel.getTxtLocation(), "location");
	}
}

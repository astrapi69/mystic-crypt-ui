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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards {@link KeyStorePanel} against the collapse it showed when the window was narrower than the
 * panel wants (issue #44).
 * <p>
 * The input column of the GridBagLayout carried neither a fill nor a weight, so GridBagLayout fell
 * back to the minimum widths as soon as the container was narrower than the grid, and a text field
 * reports a minimum width of nearly zero. Measured before the fix:
 * <ul>
 * <li>at the preferred width of 768 px: key store file 444 px, store password 224 px, alias 224 px,
 * distinguished name 334 px, certificate file 444 px, private key file 444 px - correct</li>
 * <li>at 648 px, 120 px below the preferred width: every one of those six fields 5 px - the fields
 * did not shrink, they vanished</li>
 * </ul>
 * The panel that shows the details of a certificate was built from the same constraints and had the
 * same defect.
 */
class KeyStorePanelLayoutTest
{

	private static final int USABLE_WIDTH = 120;

	private static final int NARROWER_BY = 120;

	private static void layoutRecursively(final Component component)
	{
		if (component instanceof Container container)
		{
			container.doLayout();
			for (Component child : container.getComponents())
			{
				layoutRecursively(child);
			}
		}
	}

	private static void collectTextComponents(final Component component,
		final List<JTextComponent> collected)
	{
		if (component instanceof JTextComponent textComponent)
		{
			collected.add(textComponent);
		}
		if (component instanceof Container container)
		{
			for (Component child : container.getComponents())
			{
				collectTextComponents(child, collected);
			}
		}
	}

	private static JTextComponent narrowestTextComponent(final Component root)
	{
		List<JTextComponent> textComponents = new ArrayList<>();
		collectTextComponents(root, textComponents);
		assertTrue(!textComponents.isEmpty(), "the panel holds no text component at all");
		JTextComponent narrowest = textComponents.get(0);
		for (JTextComponent candidate : textComponents)
		{
			if (candidate.getWidth() < narrowest.getWidth())
			{
				narrowest = candidate;
			}
		}
		return narrowest;
	}

	/**
	 * Lays the component out at the given width, the way a window narrower than the component wants
	 * would, and answers the text component that came out of it narrowest
	 *
	 * @param component
	 *            the component to lay out
	 * @param width
	 *            the width to lay it out at
	 * @return the narrowest text component of the laid out tree
	 */
	private static JTextComponent layoutAt(final JComponent component, final int width)
	{
		Dimension preferred = component.getPreferredSize();
		component.setSize(width, Math.max(400, preferred.height));
		layoutRecursively(component);
		return narrowestTextComponent(component);
	}

	/**
	 * The details of a certificate, made up here, so the panel that shows them can be laid out
	 * without a key store on disk
	 *
	 * @return the made up details
	 */
	private static KeyStoreSupport.CertificateDetails someCertificateDetails()
	{
		return new KeyStoreSupport.CertificateDetails("narrow", "CN=narrow window",
			"CN=narrow window", "2026-01-01", "2027-01-01", false, "1", "SHA256withRSA", "RSA", 3,
			"AA:BB:CC:DD:EE:FF:00:11",
			"-----BEGIN CERTIFICATE-----\nMIIB\n-----END CERTIFICATE-----");
	}

	@Test
	@DisplayName("the key store panel keeps its fields readable in a window narrower than it wants")
	void everyTextComponentStaysUsable_whenTheWindowIsNarrowerThanPreferred()
	{
		KeyStorePanel panel = new KeyStorePanel();
		int preferredWidth = panel.getPreferredSize().width;

		JTextComponent narrowest = layoutAt(panel, preferredWidth - NARROWER_BY);

		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			"at " + (preferredWidth - NARROWER_BY) + " px, " + NARROWER_BY
				+ " px below the preferred " + preferredWidth
				+ " px, the narrowest text component '" + narrowest.getName() + "' is "
				+ narrowest.getWidth() + " px wide, expected at least " + USABLE_WIDTH);
	}

	@Test
	@DisplayName("the key store panel keeps its fields readable at the width it asks for")
	void everyTextComponentStaysUsable_whenTheWindowHasThePreferredWidth()
	{
		KeyStorePanel panel = new KeyStorePanel();
		int preferredWidth = panel.getPreferredSize().width;

		JTextComponent narrowest = layoutAt(panel, preferredWidth);

		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			"at the preferred width of " + preferredWidth + " px the narrowest text component '"
				+ narrowest.getName() + "' is " + narrowest.getWidth()
				+ " px wide, expected at least " + USABLE_WIDTH);
	}

	@Test
	@DisplayName("the certificate details keep their fields readable in a narrower dialog")
	void everyTextComponentOfTheDetailsStaysUsable_whenTheDialogIsNarrowerThanPreferred()
	{
		JComponent detailsPanel = new KeyStorePanel().newDetailsPanel(someCertificateDetails());
		int preferredWidth = detailsPanel.getPreferredSize().width;

		JTextComponent narrowest = layoutAt(detailsPanel, preferredWidth - NARROWER_BY);

		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			"at " + (preferredWidth - NARROWER_BY) + " px, " + NARROWER_BY
				+ " px below the preferred " + preferredWidth
				+ " px, the narrowest text component '" + narrowest.getName() + "' is "
				+ narrowest.getWidth() + " px wide, expected at least " + USABLE_WIDTH);
	}
}

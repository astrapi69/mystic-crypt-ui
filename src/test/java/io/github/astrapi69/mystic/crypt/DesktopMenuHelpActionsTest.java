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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.JFrame;

import org.junit.jupiter.api.Test;

/**
 * Two Help menu items that pointed at content unrelated to this project: Donate opened a dead
 * SourceForge URL from the library's own default (#113), and License showed the raw
 * {@code Copyright (C) ${year} ${owner}} template because the unqualified classpath resource name
 * it read collided with the same unfilled template bundled inside this app's own
 * swing-base-components dependency (#112).
 */
class DesktopMenuHelpActionsTest
{

	@Test
	void donateOffersEveryRealTargetNotTheLibrarysDeadDefault()
	{
		List<DesktopMenu.DonateTarget> targets = DesktopMenu.DONATE_TARGETS;

		assertEquals(4, targets.size());
		assertTrue(targets.stream()
			.anyMatch(target -> target.url().equals("https://github.com/sponsors/astrapi69")));
		assertTrue(targets.stream()
			.anyMatch(target -> target.url().equals("https://liberapay.com/astrapi69")));
		assertTrue(targets.stream()
			.anyMatch(target -> target.url().equals("https://ko-fi.com/astrapi69")));
		assertTrue(targets.stream().anyMatch(target -> target.url().equals(
			"https://www.paypal.com/donate/?cmd=_s-xclick&hosted_button_id=MJ7V43GU2H386")));
		assertFalse(targets.stream().anyMatch(target -> target.url().contains("sourceforge.net")),
			"the library's dead default must not survive alongside the real targets");
	}

	@Test
	void licenseTextHasNoUnresolvedTemplatePlaceholders()
	{
		DesktopMenu menu = new DesktopMenu(new JFrame());

		String license = menu.onNewLicenseText();

		assertFalse(license.contains("${year}") || license.contains("${owner}"),
			"the license text must not carry an unfilled template placeholder, was: " + license);
		assertTrue(license.contains("Asterios Raptis"),
			"the license text must actually say who holds the copyright, was: " + license);
	}

}

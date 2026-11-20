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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

	@Test
	void licenseText_namesNoticeAndKeepsTheMitText_afterTheFix() throws IOException
	{
		DesktopMenu menu = new DesktopMenu(new JFrame());
		String mitText;
		try (InputStream is = DesktopMenuHelpActionsTest.class.getClassLoader()
			.getResourceAsStream("legal/license.txt"))
		{
			mitText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}

		String license = menu.onNewLicenseText();

		assertTrue(license.contains("NOTICE.txt"),
			"the dialog must name NOTICE.txt, was: " + license);
		assertTrue(license.strip().endsWith(mitText.strip()),
			"the MIT text must stay in the dialog unchanged");
	}

	@Test
	void licenseNoticeBundles_nameNoticeInEnglishAndGerman()
	{
		for (String bundleName : List.of("ui/messages.properties", "ui/messages_de.properties"))
		{
			java.util.Properties bundle = new java.util.Properties();
			try (java.io.Reader reader = new java.io.InputStreamReader(
				DesktopMenuHelpActionsTest.class.getClassLoader().getResourceAsStream(bundleName),
				StandardCharsets.UTF_8))
			{
				bundle.load(reader);
			}
			catch (IOException e)
			{
				throw new AssertionError(bundleName, e);
			}
			assertTrue(
				bundle.getProperty(DesktopMenu.LICENSE_NOTICE_KEY, "").contains("NOTICE.txt"),
				bundleName + " must carry the licence notice naming NOTICE.txt");
		}
		assertTrue(DesktopMenu.LICENSE_NOTICE_DEFAULT.contains("NOTICE.txt")
			&& DesktopMenu.LICENSE_NOTICE_DEFAULT.contains("license texts")
			&& DesktopMenu.LICENSE_NOTICE_DEFAULT.contains("third-party"));
	}

}

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
package io.github.astrapi69.mystic.crypt.panel.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.util.Arrays;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

class ApplicationInfoPanelTest
{

	private static Component byName(final ApplicationInfoPanel panel, final String name)
	{
		return Arrays.stream(panel.getComponents())
			.filter(component -> name.equals(component.getName())).findFirst().orElse(null);
	}

	@Test
	void showsTheGivenApplicationNameVersionCopyrightAndLicense()
	{
		ApplicationInfo info = new ApplicationInfo("mystic-crypt-ui", "8.2-SNAPSHOT",
			"2016 Asterios Raptis", "This Software is licensed under the MIT License",
			"https://github.com/astrapi69/mystic-crypt-ui",
			"https://github.com/astrapi69/mystic-crypt-ui/wiki");

		ApplicationInfoPanel panel = new ApplicationInfoPanel(info);

		String allText = Arrays.stream(panel.getComponents()).map(
			component -> component instanceof javax.swing.JLabel label && label.getText() != null
				? label.getText()
				: "")
			.reduce("", String::concat);
		assertTrue(allText.contains("mystic-crypt-ui"));
		assertTrue(allText.contains("8.2-SNAPSHOT"));
		assertTrue(allText.contains("2016 Asterios Raptis"));
		assertTrue(allText.contains("MIT License"));
	}

	@Test
	void creditsTheSilkIconsWithTheirAuthorTheirPageAndTheirLicense()
	{
		ApplicationInfo info = new ApplicationInfo("mystic-crypt-ui", "8.15", "", "", "", "");

		ApplicationInfoPanel panel = new ApplicationInfoPanel(info);

		javax.swing.JLabel credit = (javax.swing.JLabel)byName(panel, "lblSilkIconsCredit");
		assertNotNull(credit, "the Info dialog credits the silk icons (#1007)");
		assertTrue(credit.getText().contains("Mark James"), credit.getText());
		JButton site = (JButton)byName(panel, "btnSilkIcons");
		JButton license = (JButton)byName(panel, "btnSilkIconsLicense");
		assertNotNull(site, "the link back the author asks for");
		assertNotNull(license, "the link to the license");
		assertEquals(ApplicationInfoPanel.SILK_ICONS_URL,
			site.getClientProperty(ApplicationInfoPanel.LINK_URL_PROPERTY));
		assertEquals("http://creativecommons.org/licenses/by/2.5/",
			license.getClientProperty(ApplicationInfoPanel.LINK_URL_PROPERTY));
		assertNotNull(byName(panel, "lblThirdPartyNotice"), "and where the rest is noted");
	}

	@Test
	void hasALinkButtonForGithubAndForTheWiki()
	{
		ApplicationInfo info = new ApplicationInfo("mystic-crypt-ui", "8.2-SNAPSHOT", "", "",
			"https://github.com/astrapi69/mystic-crypt-ui",
			"https://github.com/astrapi69/mystic-crypt-ui/wiki");

		ApplicationInfoPanel panel = new ApplicationInfoPanel(info);

		JButton github = (JButton)byName(panel, "btnProjectonGitHub");
		JButton wiki = (JButton)byName(panel, "btnWiki");
		assertNotNull(github, "the GitHub link must be reachable by name for a UI test");
		assertNotNull(wiki, "the wiki link must be reachable by name for a UI test");
	}

	@Test
	void theWikiButtonCarriesTheWikiAddressOfTheInfo_andTheDefaultOneIsTheWikiHome()
	{
		ApplicationInfo info = ApplicationInfo.current("mystic-crypt-ui", "", "");

		ApplicationInfoPanel panel = new ApplicationInfoPanel(info);

		JButton wiki = (JButton)byName(panel, "btnWiki");
		assertEquals("https://github.com/astrapi69/mystic-crypt-ui/wiki",
			wiki.getClientProperty(ApplicationInfoPanel.LINK_URL_PROPERTY),
			"the Wiki link of Help > Info opens the wiki's Home page, which docs/wiki/Home.md mirrors");
	}

	@Test
	void theWikiButtonCarriesTheAddressTheInfoGives_notAFixedOne()
	{
		ApplicationInfo info = new ApplicationInfo("x", "1", "", "", "",
			"https://example.invalid/wiki");

		ApplicationInfoPanel panel = new ApplicationInfoPanel(info);

		JButton wiki = (JButton)byName(panel, "btnWiki");
		assertEquals("https://example.invalid/wiki",
			wiki.getClientProperty(ApplicationInfoPanel.LINK_URL_PROPERTY));
	}

}

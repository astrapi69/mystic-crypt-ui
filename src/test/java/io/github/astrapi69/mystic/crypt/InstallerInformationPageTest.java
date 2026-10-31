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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The installer's information page is a page of its own, written for someone installing, and it
 * says what the installer does (#640).
 * <p>
 * It used to be the repository's README, shown as raw Markdown: badges, a table of contents,
 * building from source, contributing, donations. The page names the requirement the installer
 * checks and every place its packs write to; this test reads both sides from the files the
 * installer is built from, so the page cannot fall behind {@code install.xml} unnoticed.
 */
class InstallerInformationPageTest
{

	private static final Path INSTALL_XML = Path.of("src/main/izpack/install.xml");

	private static final Path PAGE = Path.of("src/main/izpack/installation.html");

	@Test
	@DisplayName("the information panel shows the installation page, and the README is no resource of the installer")
	void informationPanel_showsTheInstallationPage_notTheReadme() throws IOException
	{
		String installXml = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);

		assertTrue(
			installXml.contains(
				"<res src=\"../src/main/izpack/installation.html\" id=\"HTMLInfoPanel.info\" />"),
			"the HTMLInfoPanel does not show src/main/izpack/installation.html");
		assertTrue(Files.isRegularFile(PAGE), PAGE + " is missing");
		assertFalse(installXml.contains("README.md"),
			"install.xml still uses README.md, which is written for the repository");
	}

	@Test
	@DisplayName("the page names the Java version the installer checks")
	void informationPage_namesTheCheckedJavaVersion() throws IOException
	{
		Properties properties = new Properties();
		try (var reader = Files.newBufferedReader(Path.of("gradle.properties")))
		{
			properties.load(reader);
		}
		String javaVersion = properties.getProperty("projectSourceCompatibility");

		assertTrue(page().contains("Java " + javaVersion + " or later"),
			"the page does not name Java " + javaVersion
				+ ", which the installer checks (<javaversion>@{app.java.version}</javaversion>)");
	}

	@ParameterizedTest(name = "install.xml writes to {0}, and the page names {1}")
	@CsvSource({ "${USER_HOME}/.local/share/applications/, ~/.local/share/applications/",
			"${USER_HOME}/.config/mystic-crypt-ui/plugins/, ~/.config/mystic-crypt-ui/plugins",
			"${INSTALL_PATH}/legal/lethenon-plugin, legal/lethenon-plugin",
			"resources/main/doc/start.sh, start.sh", "resources/main/doc/start.bat, start.bat",
			"resources/main/doc/mystic-crypt\", mystic-crypt</code>",
			"resources/main/doc/mystic-crypt.bat, mystic-crypt.bat",
			"src=\"../CHANGELOG.md\", CHANGELOG.md", "src=\"../LICENSE\" targetdir, LICENSE",
			"src=\"../src/main/izpack/installation.html\" targetdir, installation.html" })
	void informationPage_namesWhereThePacksWrite(final String inInstallXml, final String onThePage)
		throws IOException
	{
		String installXml = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);

		assertTrue(installXml.contains(inInstallXml),
			"install.xml no longer contains '" + inInstallXml + "': update this test and the page");
		assertTrue(page().contains(onThePage),
			"the page does not name '" + onThePage + "', which install.xml installs");
	}

	private static String page() throws IOException
	{
		return Files.readString(PAGE, StandardCharsets.UTF_8);
	}
}

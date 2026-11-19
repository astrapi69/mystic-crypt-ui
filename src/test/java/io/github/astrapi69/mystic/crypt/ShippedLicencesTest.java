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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The licences of what the application ships (#1013, #1014): every module of the runtime class
 * path, with the licences its POM chain names, as the build's {@code runtimeLicences} task writes
 * them into {@code mystic.crypt.ui.runtime.licences}. The application is MIT licensed and ships as
 * a binary, so no module may come only under the AGPL or the GPL without the classpath exception,
 * and a module whose licence the POMs do not name is reported, not let through
 */
class ShippedLicencesTest
{

	/** Where the build names the file of the licences */
	private static final String RUNTIME_LICENCES = "mystic.crypt.ui.runtime.licences";

	private static final Pattern LESSER = Pattern.compile("lesser|lgpl|library general",
		Pattern.CASE_INSENSITIVE);

	private static final Pattern AFFERO = Pattern.compile("affero|agpl", Pattern.CASE_INSENSITIVE);

	private static final Pattern GPL = Pattern
		.compile("\\bgpl|gnu general public|general public license", Pattern.CASE_INSENSITIVE);

	private static final Pattern CLASSPATH_EXCEPTION = Pattern.compile("class ?path",
		Pattern.CASE_INSENSITIVE);

	@Test
	@DisplayName("every shipped module has a licence in its POM chain, and none comes only under the AGPL or the GPL without the classpath exception")
	void everyShippedModule_hasALicence_andNoneIsCopyleftOnly() throws IOException
	{
		List<String[]> modules = shippedModules();
		Set<String> unknown = new TreeSet<>();
		Set<String> copyleft = new TreeSet<>();
		for (String[] module : modules)
		{
			String groupAndArtifact = module[0].substring(0, module[0].lastIndexOf(':'));
			if (module[1].isBlank())
			{
				unknown.add(module[0] + " (" + module[2] + ")");
			}
			else if (List.of(module[1].split(" \\| ")).stream()
				.allMatch(ShippedLicencesTest::isCopyleft))
			{
				copyleft.add(groupAndArtifact);
			}
		}
		System.out.printf(
			"ShippedLicencesTest: %d modules, %d without a licence, %d copyleft only%n",
			modules.size(), unknown.size(), copyleft.size());

		assertTrue(unknown.isEmpty(),
			"no licence in the POM chain, to be checked by hand and not let through: " + unknown);
		// iText 5 under the AGPL-3 shipped through swing-components until #1013 removed it
		assertTrue(copyleft.isEmpty(),
			"modules that ship under the AGPL or the GPL without the classpath exception only: "
				+ copyleft);
	}

	@Test
	@DisplayName("a copyleft licence is told apart from the LGPL and from the GPL with the classpath exception")
	void isCopyleft_tellsTheLicencesApart()
	{
		assertTrue(isCopyleft(
			"GNU Affero General Public License v3 <http://www.fsf.org/licensing/licenses/agpl-3.0.html>"));
		assertTrue(isCopyleft(
			"GNU General Public License, version 2 <https://www.gnu.org/licenses/gpl-2.0.html>"));
		assertTrue(isCopyleft("GPL-3.0-only <>"));
		assertFalse(isCopyleft(
			"GNU General Public License - Version 2 with the class path exception <https://javahelp.dev.java.net/license.txt>"));
		assertFalse(isCopyleft(
			"GPL2 w/ CPE, the GNU General Public License with the Classpath Exception <https://openjdk.org/legal/gplv2+ce.html>"));
		assertFalse(isCopyleft(
			"Lesser General Public License (LGPL) <http://www.gnu.org/copyleft/lesser.html>"));
		assertFalse(isCopyleft("MIT License <https://opensource.org/licenses/MIT>"));
		assertFalse(isCopyleft(
			"Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0>"));
	}

	private static boolean isCopyleft(final String licence)
	{
		if (LESSER.matcher(licence).find())
		{
			return false;
		}
		if (AFFERO.matcher(licence).find())
		{
			return true;
		}
		return GPL.matcher(licence).find() && !CLASSPATH_EXCEPTION.matcher(licence).find()
			&& !licence.toLowerCase(Locale.ROOT).contains("cpe");
	}

	private static List<String[]> shippedModules() throws IOException
	{
		String file = System.getProperty(RUNTIME_LICENCES, "");
		assertFalse(file.isBlank(), "the build names no file of licences in " + RUNTIME_LICENCES);
		List<String[]> modules = Files.readAllLines(Path.of(file), StandardCharsets.UTF_8).stream()
			.filter(line -> !line.isBlank()).map(line -> line.split("\t", -1)).toList();
		assertFalse(modules.isEmpty(), "the file of licences names no module: " + file);
		modules.forEach(module -> assertEquals(3, module.length,
			"a line is module, licences and source: " + String.join("\t", module)));
		return modules;
	}
}

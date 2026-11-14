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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A database file that names a type no database holds is refused without costing data: the refusal
 * names the type, the file stays byte for byte as it was, and nothing is written beside it - no
 * backup, no temporary file, no save. Asked of a foreign type of this project and of standard types
 * the XML reader would build but no database holds
 */
class ARefusedTypeCostsNoDataTest
{

	/**
	 * A foreign type of this project, and standard types the XML library builds by default but no
	 * vault holds - each one measured as built by the reader before it started from no type
	 */
	static Stream<Arguments> foreignTypes()
	{
		return Stream.of(
			Arguments.of(NotAVaultType.class.getName(),
				"<" + NotAVaultType.class.getName() + "><text>not part of a vault</text></"
					+ NotAVaultType.class.getName() + ">"),
			Arguments.of("java.io.File", "<file>/tmp/not-part-of-a-vault</file>"),
			Arguments.of("java.net.URL", "<url>http://example.org/</url>"),
			Arguments.of("java.net.URI", "<uri>http://example.org/</uri>"),
			Arguments.of("java.lang.Class", "<java-class>java.lang.String</java-class>"),
			Arguments.of("java.lang.reflect.Method",
				"<method><class>java.lang.String</class><name>length</name><parameter-types/></method>"),
			Arguments.of("java.lang.RuntimeException", "<java.lang.RuntimeException/>"),
			Arguments.of("java.util.HashMap", "<java.util.HashMap/>"),
			Arguments.of("java.util.TreeMap", "<java.util.TreeMap/>"),
			Arguments.of("java.util.Properties", "<java.util.Properties/>"),
			Arguments.of("java.util.regex.Pattern",
				"<java.util.regex.Pattern><pattern>a</pattern><flags>0</flags></java.util.regex.Pattern>"),
			Arguments.of("javax.swing.UIDefaults", "<javax.swing.UIDefaults/>"),
			Arguments.of("javax.management.BadAttributeValueExpException",
				"<javax.management.BadAttributeValueExpException/>"));
	}

	@ParameterizedTest(name = "a file naming {0}")
	@MethodSource("foreignTypes")
	void aFileNamingAForeignType_isRefusedWithItsName_andStaysAsItWas(final String typeName,
		final String element, @TempDir final File directory) throws Exception
	{
		char[] password = TestPasswords.throwawayChars();
		String xml = new String(VaultXmlCodec.toXml(SampleVaults.aVaultWith(entry -> {
		}))).replaceFirst("<list>", "<list>" + element);
		assertTrue(xml.contains(element), "the foreign element is in the XML");
		File vault = new File(directory, "names-a-foreign-type.mcrdb");
		Files.write(vault.toPath(),
			PasswordVaultFormat.encrypt(xml.toCharArray(), password.clone()));
		byte[] before = Files.readAllBytes(vault.toPath());
		Set<String> filesBefore = namesIn(directory);

		RuntimeException refusal = assertThrows(RuntimeException.class,
			() -> SampleVaults.open(vault, password.clone()));

		assertTrue(String.valueOf(refusal.getMessage()).contains(typeName),
			"the refusal names the type: " + refusal.getMessage());
		assertArrayEquals(before, Files.readAllBytes(vault.toPath()),
			"the file is byte for byte as it was");
		assertEquals(filesBefore, namesIn(directory), "nothing was written beside it");
	}

	private static Set<String> namesIn(final File directory)
	{
		return new TreeSet<>(Arrays.asList(directory.list()));
	}
}

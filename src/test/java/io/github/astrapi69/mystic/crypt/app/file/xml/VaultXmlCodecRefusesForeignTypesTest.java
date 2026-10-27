/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The reader of a vault builds exactly the types a vault holds and refuses every other one before
 * it is built, also one of this project's own namespace.
 */
class VaultXmlCodecRefusesForeignTypesTest
{

	private static final String FOREIGN = "<" + NotAVaultType.class.getName()
		+ "><text>not part of a vault</text></" + NotAVaultType.class.getName() + ">";

	@BeforeEach
	void resetTheCount()
	{
		NotAVaultType.CREATED.set(0);
	}

	@Test
	@DisplayName("a vault holding a type of this project that no vault holds is refused, and the type is never built")
	void aVaultWithAForeignType_isRefused_andTheTypeIsNeverBuilt()
	{
		String vault = new String(VaultXmlCodec.toXml(SampleVaults.aVaultWith(entry -> {
		})));
		String withAForeignType = vault.replaceFirst("<list>", "<list>" + FOREIGN);
		assertTrue(withAForeignType.contains(NotAVaultType.class.getName()),
			"the foreign type is in the XML: " + withAForeignType);

		VaultHoldsAForeignTypeException refusal = assertThrows(
			VaultHoldsAForeignTypeException.class,
			() -> VaultXmlCodec.toModel(withAForeignType.toCharArray()));

		assertTrue(refusal.getMessage().contains(NotAVaultType.class.getName()),
			"the refusal names the type: " + refusal.getMessage());
		assertEquals(0, NotAVaultType.CREATED.get(), "instances of the foreign type built");
	}

	@Test
	@DisplayName("a vault file holding such a type is not taken for a wrong password: the sign-in says what it holds")
	void aVaultFileWithAForeignType_isNotTakenForAWrongPassword(@TempDir File directory)
		throws Exception
	{
		char[] password = TestPasswords.throwawayChars();
		String withAForeignType = new String(VaultXmlCodec.toXml(SampleVaults.aVaultWith(entry -> {
		}))).replaceFirst("<list>", "<list>" + FOREIGN);
		File vault = new File(directory, "names-a-foreign-type.mcrdb");
		Files.write(vault.toPath(),
			PasswordVaultFormat.encrypt(withAForeignType.toCharArray(), password.clone()));

		RuntimeException refusal = assertThrows(RuntimeException.class,
			() -> SampleVaults.open(vault, password));

		assertTrue(refusal.getMessage().contains(NotAVaultType.class.getName()),
			"the sign-in names the type: " + refusal.getMessage());
		assertFalse(refusal.getMessage().contains("Password is not valid"),
			"the sign-in does not blame the password: " + refusal.getMessage());
		assertEquals(0, NotAVaultType.CREATED.get(), "instances of the foreign type built");
	}

	@Test
	@DisplayName("a vault with every type a vault holds still opens, entries, attachments and properties as written")
	void aVaultWithEveryTypeItHolds_opens()
	{
		var model = SampleVaults.aVaultWith(entry -> {
		});

		var readBack = VaultXmlCodec.toModel(VaultXmlCodec.toXml(model));

		assertEquals(SampleVaults.entryLines(model), SampleVaults.entryLines(readBack));
	}
}

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A vault that decrypts with the right password but whose content this version cannot read is never
 * reported as a wrong password (#624, decided by the maintainer on 2026-10-27): the sign-in says
 * the database was not opened and names the reason. Up to 8.13 every such failure said "Password is
 * not valid" - which is how a 7.x vault looked to its owner.
 */
class AnUnreadableVaultIsNotAWrongPasswordTest
{

	/** A class name no version of this application has: what a vault of an unknown version holds */
	private static final String UNKNOWN_TYPE = "io.github.astrapi69.mystic.crypt.NoSuchTypeInAnyVersion";

	@Test
	@DisplayName("a vault whose content names a type this version does not know says so, and does not blame the password")
	void aVaultNamingAnUnknownType_isNotTakenForAWrongPassword(@TempDir File directory)
		throws Exception
	{
		char[] password = TestPasswords.throwawayChars();
		String unreadable = new String(VaultXmlCodec.toXml(SampleVaults.aVaultWith(entry -> {
		}))).replaceFirst("<list>", "<list><" + UNKNOWN_TYPE + "/>");
		File vault = new File(directory, "names-an-unknown-type.mcrdb");
		Files.write(vault.toPath(),
			PasswordVaultFormat.encrypt(unreadable.toCharArray(), password.clone()));

		RuntimeException refusal = assertThrows(RuntimeException.class,
			() -> SampleVaults.open(vault, password));

		assertFalse(refusal.getMessage().contains("Password is not valid"),
			"the sign-in does not blame the password: " + refusal.getMessage());
		assertTrue(refusal.getMessage().startsWith("Database not opened::"),
			"the sign-in says the database was not opened: " + refusal.getMessage());
		assertTrue(refusal.getMessage().contains(UNKNOWN_TYPE),
			"and names the reason, the type it does not know: " + refusal.getMessage());
	}

	@Test
	@DisplayName("a wrong password is still a wrong password")
	void aWrongPassword_isStillReportedAsOne(@TempDir File directory)
	{
		File vault = SampleVaults.save(SampleVaults.aVaultWith(entry -> {
		}), directory, TestPasswords.throwawayChars());

		RuntimeException refusal = assertThrows(RuntimeException.class,
			() -> SampleVaults.open(vault, TestPasswords.throwawayChars()));

		assertTrue(refusal.getMessage().contains("Password is not valid"),
			"a password that does not decrypt the file is what the sign-in names: "
				+ refusal.getMessage());
	}
}

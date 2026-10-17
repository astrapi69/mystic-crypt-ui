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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;

/**
 * The two password messages every window shows come from one place, say what to do next, and never
 * repeat the password (#604, rule 6).
 */
class LethenonWalletsTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	Path directory;

	@Test
	@DisplayName("an empty password is not tried: the window asks for it first")
	void anEmptyPassword_asksForThePassword() throws Exception
	{
		Path walletFile = aWallet();

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> LethenonWallets.open(walletFile, new char[0]));

		assertEquals("type the password of home.wallet first", refused.getMessage());
	}

	@Test
	@DisplayName("a wrong password says what to do, names the wallet, and does not repeat the password")
	void aWrongPassword_saysWhatToDo() throws Exception
	{
		Path walletFile = aWallet();
		String wrong = "not the password at all";

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> LethenonWallets.open(walletFile, wrong.toCharArray()));

		assertEquals("this password does not open home.wallet; type it again, and check that it "
			+ "is the wallet you meant (" + walletFile.toAbsolutePath() + ")", refused.getMessage());
		assertFalse(refused.getMessage().contains(wrong));
	}

	@Test
	@DisplayName("the right password opens the wallet")
	void theRightPassword_opensTheWallet() throws Exception
	{
		Path walletFile = aWallet();

		Wallet opened = LethenonWallets.open(walletFile, PASSWORD.toCharArray());

		assertEquals(WalletFile.read(walletFile, PASSWORD.toCharArray()).address().toText(),
			opened.address().toText());
	}

	private Path aWallet() throws Exception
	{
		Path walletFile = directory.resolve("home.wallet");
		WalletFile.write(walletFile, Wallet.create(), PASSWORD.toCharArray());
		return walletFile;
	}
}

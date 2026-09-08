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
package io.github.astrapi69.mystic.crypt.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The decision that #237 got wrong, now where it can be read and mutated: locking left the
 * decrypted vault on screen because the condition asked whether a view object existed, which is
 * true forever once a database has been opened, instead of asking whether the workspace is unlocked
 */
class WorkspaceLockDecisionTest
{

	@ParameterizedTest(name = "signedIn={0}, aVaultIsOpen={1} -> mayCreate={2}")
	@CsvSource({
			// unlocked: creating another vault is the user's business, not the lock's
			"true,  true,  true", "true,  false, true",
			// LOCKED with a vault open: the case of #270. Creating one here signed the workspace
			// back in without the locked vault's master password
			"false, true,  false",
			// nothing open at all: this is how a first vault is made, and it must stay possible
			"false, false, true" })
	@DisplayName("a vault may not be created while another one is locked")
	void aVaultMayNotBeCreatedWhileAnotherIsLocked(final boolean signedIn,
		final boolean aVaultIsOpen, final boolean expected)
	{
		assertEquals(expected, WorkspaceLockDecision.mayCreateAVault(signedIn, aVaultIsOpen));
	}

	@ParameterizedTest(name = "signedIn={0}, vaultViewBuilt={1} -> {2}")
	@CsvSource({
			// unlocked and there is a view to show: the one case that shows it
			"true,  true,  SHOW_VAULT",
			// unlocked but nothing was ever built - showing would mean showing an empty frame
			"true,  false, HIDE_VAULT",
			// LOCKED with a view that exists: the #237 defect. The view object outlives the lock,
			// so this is the case a null check answered wrongly
			"false, true,  HIDE_VAULT",
			// locked and nothing built
			"false, false, HIDE_VAULT" })
	void theVaultIsShownOnlyWhenUnlockedAndThereIsAViewToShow(boolean signedIn,
		boolean vaultViewBuilt, WorkspaceLockDecision expected)
	{
		assertEquals(expected,
			WorkspaceLockDecision.onSwitchToDesktopPane(signedIn, vaultViewBuilt));
	}
}

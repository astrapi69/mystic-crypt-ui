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

/**
 * Whether the vault view belongs on screen when the frame switches to the desktop pane.
 * <p>
 * The same switch serves two callers with opposite intentions: a plugin opening its own window
 * needs the database view to keep showing (#132), while locking the workspace needs it gone (#237).
 * Telling them apart is a security decision - answering it wrongly is what left a decrypted vault
 * readable and operable behind a locked-looking frame - so it lives here, as a function of state,
 * rather than inside a Swing method where it cannot be mutation tested (#252).
 */
public enum WorkspaceLockDecision
{

	/** The vault view is put on the desktop, or brought to the front if it is already there */
	SHOW_VAULT,

	/** The vault view is taken off the desktop */
	HIDE_VAULT;

	/**
	 * Whether a new vault may be created right now: only when none is open.
	 * <p>
	 * Creating a vault does not open one. It replaces the file the application saves to and leaves
	 * the model alone, so the open vault's entries end up inside the new file under the new master
	 * password, and a change made to the open vault never reaches its own file (#279). Measured
	 * through a restart, in both directions.
	 * <p>
	 * The signed-in state is deliberately NOT part of this decision. It used to be: the refusal
	 * only covered a LOCKED vault, because that was the lock bypass it was written for (#270).
	 * Locked or not, the vault that is open is the one whose content would travel, so the question
	 * is whether one is open at all - two cases instead of four.
	 * <p>
	 * What the caller does with a "no" has moved on, and this only answers the question. Since the
	 * close path was built (#281), {@code NewApplicationFileAction} closes an UNLOCKED vault and
	 * carries on, and shows the refusal only for a LOCKED one - whose master password is not in
	 * memory, so its pending changes could not be written even if somebody wanted to. This comment
	 * said the application could not close a vault at all, which was true when it was written and
	 * stopped being true with #287. A comment describing the application as it was is read as
	 * current, which makes it worse than none (#307).
	 * <p>
	 * Asked in the ACTION, not only at the button. The button follows the same decision (#269),
	 * which is the first line; this is the second, and it holds for a keyboard shortcut, a
	 * persisted menu layout carrying the item, or a caller added later.
	 *
	 * @param aVaultIsOpen
	 *            whether a vault is open at all, locked or not
	 * @return false exactly when a vault is open
	 */
	public static boolean mayCreateAVault(final boolean aVaultIsOpen)
	{
		return !aVaultIsOpen;
	}

	/**
	 * Decides what happens to the vault view when the frame switches to the desktop pane.
	 * <p>
	 * {@code signedIn} is the state, and it is asked first: locked means hidden, whatever else is
	 * true. {@code vaultViewBuilt} is not a second state carrier - it answers a different question,
	 * namely whether there is anything to show at all. The two are deliberately not
	 * interchangeable: the view object is created when a database is opened and never discarded, so
	 * it stays true across locking, which is exactly why a null check on it decided this wrongly
	 * before (#237).
	 * <p>
	 * This comment and its parameters used to sit above {@code mayCreateAVault}, documenting
	 * neither that method nor this one (#307).
	 *
	 * @param signedIn
	 *            whether the workspace is unlocked
	 * @param vaultViewBuilt
	 *            whether a vault view exists that could be shown
	 * @return {@link #SHOW_VAULT} only when the workspace is unlocked and there is a view to show
	 */
	public static WorkspaceLockDecision onSwitchToDesktopPane(final boolean signedIn,
		final boolean vaultViewBuilt)
	{
		if (!signedIn)
		{
			return HIDE_VAULT;
		}
		return vaultViewBuilt ? SHOW_VAULT : HIDE_VAULT;
	}
}

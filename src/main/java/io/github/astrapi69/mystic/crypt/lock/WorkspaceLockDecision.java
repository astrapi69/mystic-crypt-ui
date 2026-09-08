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
	 * Decides what happens to the vault view when the frame switches to the desktop pane.
	 * <p>
	 * {@code signedIn} is the state, and it is asked first: locked means hidden, whatever else is
	 * true. {@code vaultViewBuilt} is not a second state carrier - it answers a different question,
	 * namely whether there is anything to show at all. The two are deliberately not
	 * interchangeable: the view object is created when a database is opened and never discarded, so
	 * it stays true across locking, which is exactly why a null check on it decided this wrongly
	 * before (#237).
	 *
	 * @param signedIn
	 *            whether the workspace is unlocked
	 * @param vaultViewBuilt
	 *            whether a vault view exists that could be shown
	 * @return {@link #SHOW_VAULT} only when the workspace is unlocked and there is a view to show
	 */
	/**
	 * Whether a new vault may be created right now.
	 * <p>
	 * Not while another one is locked. Creating a vault ends in a sign-in that sets the signed-in
	 * flag and rebuilds the menu, which is how a locked workspace came back to life without its
	 * master password: the lock was lifted by a door that never asked about it (#270).
	 * <p>
	 * This is asked in the ACTION, not only at the button that triggers it. The button is disabled
	 * in the locked state (#269), and that is the first line - this is the second, and it holds for
	 * every other way the action can be reached: a keyboard shortcut, a persisted menu layout that
	 * carries the item, a caller added later. Without it, closing one door leaves the room open.
	 *
	 * @param signedIn
	 *            whether the workspace is unlocked
	 * @param aVaultIsOpen
	 *            whether a vault is open at all, locked or not
	 * @return false exactly when a vault is open and the workspace is locked
	 */
	public static boolean mayCreateAVault(final boolean signedIn, final boolean aVaultIsOpen)
	{
		return signedIn || !aVaultIsOpen;
	}

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

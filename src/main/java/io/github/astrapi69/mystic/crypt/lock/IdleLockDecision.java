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
package io.github.astrapi69.mystic.crypt.lock;

import java.util.concurrent.TimeUnit;

/**
 * Whether an idle workspace has been idle long enough to lock itself (#241).
 * <p>
 * Locking was a deliberate user action and nothing else: no idle timeout, no lock on screensaver,
 * no lock on suspend. #237 made locking actually lock, but only when somebody remembers to trigger
 * it - and the realistic case for a password manager is the one where nobody does, because the user
 * walked away.
 * <p>
 * The decision is a function of state rather than something a Swing timer decides for itself, for
 * the same reason {@link WorkspaceLockDecision} is: it can be unit tested and mutated here, and the
 * timer only wires its answer.
 */
public final class IdleLockDecision
{

	/** The timeout a fresh installation gets, in minutes */
	public static final int DEFAULT_TIMEOUT_MINUTES = 15;

	/**
	 * How long a fresh installation leaves a locked vault open before closing it, in minutes.
	 * <p>
	 * Fifteen again, so walking away costs at most half an hour of decrypted vault: fifteen minutes
	 * until it locks, fifteen more until it is gone.
	 */
	public static final int DEFAULT_CLOSE_LOCKED_MINUTES = 15;

	/** The value that turns the automatic lock off */
	public static final int OFF = 0;

	private IdleLockDecision()
	{
	}

	/**
	 * Whether the workspace should lock itself now.
	 * <p>
	 * A workspace that is not signed in is not locked again: it is already locked, or there is no
	 * vault at all, and locking either would put a second unlock prompt on screen over the first. A
	 * timeout of {@link #OFF} - or a negative one out of a hand-edited settings file - never locks,
	 * which is what "configurable and switchable off" has to mean at the deciding end rather than
	 * only in the dialog.
	 *
	 * @param signedIn
	 *            whether a vault is open and unlocked
	 * @param idleMillis
	 *            how long ago the last real user activity was
	 * @param timeoutMinutes
	 *            the configured timeout in minutes; {@link #OFF} or less turns it off
	 * @return true when the workspace should be locked now
	 */
	public static boolean shouldLock(final boolean signedIn, final long idleMillis,
		final int timeoutMinutes)
	{
		if (!signedIn || timeoutMinutes <= OFF)
		{
			return false;
		}
		return TimeUnit.MINUTES.toMillis(timeoutMinutes) <= idleMillis;
	}

	/**
	 * Whether a vault that has been locked for a while should now be closed altogether (#242).
	 * <p>
	 * WHY THIS EXISTS. Locking keeps the decrypted vault in memory so unlocking can rebuild the
	 * view without reading and decrypting the file again (#237), and nothing bounded that: a vault
	 * locked at five o'clock was still decrypted in the process the next morning. This turns an
	 * unbounded window into a named one, and the name is the two settings above.
	 * <p>
	 * It keeps the CONTENT, not the key material - the master password, its repeat and the private
	 * key are wiped by the lock itself, so what this bounds is the cost side rather than the attack
	 * surface. That split is recorded in {@code docs/decisions/} (#242). This Javadoc used to say
	 * wiping in place was unavailable because an entry's title, user name, URL and notes were
	 * {@code String}s; they have been character arrays since #294, and closing overwrites all six.
	 * <p>
	 * UNSAVED CHANGES REFUSE IT, and that is not a detail. A locked workspace has no master
	 * password - locking replaced it with a verifier - so pending changes cannot be written, and a
	 * close that discards them silently is worse than the memory it saves. Locking therefore saves
	 * first, while the password is still there, and this guard is what happens when that save
	 * failed: the vault stays open and decrypted rather than losing the work.
	 *
	 * @param aVaultIsOpen
	 *            whether a vault is open at all
	 * @param signedIn
	 *            whether the workspace is unlocked; an unlocked one is in use, not waiting
	 * @param unsavedChanges
	 *            whether the model holds changes that are not in the file
	 * @param lockedMillis
	 *            how long the workspace has been locked
	 * @param timeoutMinutes
	 *            the configured timeout in minutes; {@link #OFF} or less turns it off
	 * @return true when the locked vault should be closed now
	 */
	public static boolean shouldCloseLockedVault(final boolean aVaultIsOpen, final boolean signedIn,
		final boolean unsavedChanges, final long lockedMillis, final int timeoutMinutes)
	{
		if (!aVaultIsOpen || signedIn || unsavedChanges || timeoutMinutes <= OFF)
		{
			return false;
		}
		return TimeUnit.MINUTES.toMillis(timeoutMinutes) <= lockedMillis;
	}
}

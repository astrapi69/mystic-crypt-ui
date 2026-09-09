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

/**
 * What {@link IdleLockWatchdog} needs to know about the workspace, and what it may do to it.
 * <p>
 * One small interface rather than four suppliers and two runnables side by side: the watchdog reads
 * three pieces of state and takes two actions, and they belong to one thing. No Swing type appears
 * in it, so the watchdog stays testable without a display - the application frame is one
 * implementation, a test double is another.
 */
public interface LockableWorkspace
{

	/**
	 * Whether the workspace is unlocked. An unlocked workspace is in use; a locked one is waiting
	 *
	 * @return true if a vault is open and unlocked
	 */
	boolean isSignedIn();

	/**
	 * Whether a vault is open at all, locked or not. The credentials answer this rather than the
	 * signed-in flag: a locked vault is still an open one
	 *
	 * @return true if a vault is open
	 */
	boolean aVaultIsOpen();

	/**
	 * Whether the model holds changes that are not in the file
	 *
	 * @return true if there is something unsaved
	 */
	boolean hasUnsavedChanges();

	/** Locks the workspace, as the menu entry does */
	void lock();

	/** Closes the open vault, leaving no vault open and no decrypted content in memory */
	void closeVault();
}

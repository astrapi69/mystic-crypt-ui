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

import java.io.File;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.logging.Level;

import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import lombok.extern.java.Log;

/**
 * What unlocking asks of a locked workspace, without Swing (#880): the factors the database was
 * opened with. The master password where there is one, checked against the verifier the lock left
 * behind; the key file where there is one, read from the disk again and its key recognised by the
 * {@link KeyFileVerifier} the lock left behind. A database opened with both needs both.
 * <p>
 * Every refusal is the same answer, and both factors are checked whatever the first one came to: a
 * refusal that said which factor was wrong, or a check that stopped at the first wrong one, would
 * tell whoever tries which factor was right.
 */
@Log
public final class UnlockCheck
{

	private UnlockCheck()
	{
	}

	/** What an unlock attempt comes to */
	public enum Answer
	{
		/** every factor the database asks for is there */
		UNLOCKS,
		/** at least one factor is wrong or missing; which one is not said */
		REFUSED
	}

	/**
	 * The answer, and the key read from the key file when the attempt unlocks
	 *
	 * @param answer
	 *            what the attempt comes to
	 * @param privateKey
	 *            the key read from the key file of an unlocking attempt, null otherwise and for a
	 *            database without a key file
	 */
	public record Outcome(Answer answer, PrivateKey privateKey) {
	}

	/**
	 * Whether the database asks for its master password: it was opened with one, so the lock left a
	 * verifier of it, or kept the password where no verifier could be made
	 *
	 * @param credentials
	 *            the credentials of the locked database
	 * @return true when the prompt has to ask for the password
	 */
	public static boolean asksForThePassword(final MasterPwFileModelBean credentials)
	{
		return credentials.getLockVerifier() != null || credentials.getMasterPw() != null;
	}

	/**
	 * Whether the database asks for its key file: it was opened with one
	 *
	 * @param credentials
	 *            the credentials of the locked database
	 * @return true when the prompt has to ask for the key file
	 */
	public static boolean asksForTheKeyFile(final MasterPwFileModelBean credentials)
	{
		return credentials.isWithKeyFile();
	}

	/**
	 * Checks an unlock attempt: the password and the key file, each of them whatever the other came
	 * to
	 *
	 * @param credentials
	 *            the credentials of the locked database
	 * @param password
	 *            what was typed as the master password, null when the database has none
	 * @param keyFile
	 *            the key file chosen, null when the database has none
	 * @return the outcome
	 */
	public static Outcome check(final MasterPwFileModelBean credentials, final char[] password,
		final File keyFile)
	{
		boolean thePasswordHolds = !asksForThePassword(credentials)
			|| isTheMasterPassword(credentials, password);
		if (!asksForTheKeyFile(credentials))
		{
			return thePasswordHolds ? new Outcome(Answer.UNLOCKS, null) : refused();
		}
		PrivateKey key = readKey(keyFile);
		boolean theKeyHolds = key != null && isTheKey(credentials, key);
		return thePasswordHolds && theKeyHolds ? new Outcome(Answer.UNLOCKS, key) : refused();
	}

	private static Outcome refused()
	{
		return new Outcome(Answer.REFUSED, null);
	}

	private static boolean isTheKey(final MasterPwFileModelBean credentials, final PrivateKey key)
	{
		KeyFileVerifier verifier = credentials.getKeyFileLockVerifier();
		if (verifier == null)
		{
			// fail closed: a lock that could not keep a verifier must not turn into one that
			// takes any key - the file on the disk is safe either way
			log.log(Level.WARNING, "no key file verifier was left by the lock; refusing to unlock");
			return false;
		}
		byte[] encoded = key.getEncoded();
		try
		{
			return verifier.matches(encoded);
		}
		finally
		{
			Arrays.fill(encoded, (byte)0);
		}
	}

	private static boolean isTheMasterPassword(final MasterPwFileModelBean credentials,
		final char[] entered)
	{
		MasterPasswordVerifier verifier = credentials.getLockVerifier();
		if (verifier != null)
		{
			return verifier.matches(entered);
		}
		return SecretComparison.sameCharacters(entered, credentials.getMasterPw());
	}

	private static PrivateKey readKey(final File keyFile)
	{
		if (keyFile == null || !keyFile.isFile())
		{
			return null;
		}
		try
		{
			// the same reading opening uses, so the file that opens the database unlocks it:
			// PrivateKeyReader.readPrivateKey carries the one format branch - PEM with the PKCS#8
			// label, PEM with the traditional label, binary DER (#880, the review of #936)
			return PrivateKeyReader.readPrivateKey(keyFile);
		}
		catch (Exception notAKey)
		{
			log.log(Level.FINE, "the chosen key file holds no readable key", notAKey);
			return null;
		}
	}
}

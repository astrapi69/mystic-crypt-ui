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
package io.github.astrapi69.mystic.crypt.plugin.password;

import java.util.Base64;
import java.util.List;
import java.util.Locale;

import io.github.astrapi69.mystic.crypt.pw.PasswordEncryptor;
import io.github.astrapi69.mystic.crypt.sha.BcryptHasher;
import io.github.astrapi69.mystic.crypt.sha.ScryptHasher;

/**
 * Hashing a password so it can be stored, and checking a password against a stored hash, with the
 * four algorithms worth using.
 * <p>
 * All four are deliberately slow, which is the point: a password hash that can be computed quickly
 * can be guessed quickly. They differ in what they are slow with. Argon2id and scrypt want memory,
 * which is what makes them hard to attack with special hardware; bcrypt has been in use long enough
 * that it is everywhere; PBKDF2 is the one certifications ask for.
 * <p>
 * Everything comes back as one string that carries its own salt and its own cost, so a stored hash
 * says how it was made and can be checked without anything being remembered alongside it. For
 * scrypt that form has to be built here - the library hands out raw bytes and takes the parameters
 * separately, which cannot be stored in a single column.
 */
public final class PasswordHashSupport
{

	/** The memory hard algorithm to prefer */
	public static final String ARGON2ID = "Argon2id";

	/** The classical alternative, the one certifications ask for */
	public static final String PBKDF2 = "PBKDF2";

	/** Long established and everywhere */
	public static final String BCRYPT = "bcrypt";

	/** Memory hard, like Argon2id */
	public static final String SCRYPT = "scrypt";

	/** What the scrypt form starts with, so a stored value says what made it */
	static final String SCRYPT_PREFIX = "scrypt$";

	private PasswordHashSupport()
	{
	}

	/**
	 * The algorithms this tool offers, in the order they are shown
	 *
	 * @return the algorithms
	 */
	public static List<String> algorithms()
	{
		return List.of(ARGON2ID, PBKDF2, BCRYPT, SCRYPT);
	}

	/**
	 * Hashes a password
	 *
	 * @param algorithm
	 *            one of {@link #algorithms()}
	 * @param password
	 *            the password
	 * @return the hash, carrying its own salt and cost
	 * @throws Exception
	 *             if hashing fails
	 */
	public static String hash(final String algorithm, final char[] password) throws Exception
	{
		requirePassword(password);
		switch (algorithm)
		{
			case BCRYPT :
				// bcrypt truncates at 72 bytes without saying so - a longer password would be
				// accepted here and then silently only be checked up to that point
				requireBcryptLength(password);
				return BcryptHasher.hash(password.clone());
			case SCRYPT :
				return encodeScrypt(ScryptHasher.hash(password.clone(), ScryptHasher.DEFAULT_N,
					ScryptHasher.DEFAULT_R, ScryptHasher.DEFAULT_P), ScryptHasher.DEFAULT_N,
					ScryptHasher.DEFAULT_R, ScryptHasher.DEFAULT_P);
			case PBKDF2 :
				return PasswordEncryptor.getInstance().hashPasswordPbkdf2(new String(password));
			case ARGON2ID :
				return PasswordEncryptor.getInstance().hashPasswordArgon2id(new String(password));
			default :
				throw new IllegalArgumentException("'" + algorithm + "' is not one of "
					+ String.join(", ", algorithms()));
		}
	}

	/**
	 * Checks a password against a stored hash. Which algorithm made that hash is read out of the
	 * hash itself, so nothing has to be selected correctly for a check to work
	 *
	 * @param password
	 *            the password to check
	 * @param stored
	 *            the stored hash
	 * @return true if the password is the one the hash was made from
	 */
	public static boolean verify(final char[] password, final String stored)
	{
		if (password == null || password.length == 0 || stored == null || stored.isBlank())
		{
			return false;
		}
		try
		{
			String algorithm = algorithmOf(stored);
			if (algorithm == null)
			{
				return false;
			}
			return switch (algorithm)
			{
				case BCRYPT -> BcryptHasher.verify(password.clone(), stored.trim());
				case SCRYPT -> verifyScrypt(password.clone(), stored.trim());
				case PBKDF2 -> PasswordEncryptor.getInstance().matchPbkdf2(new String(password),
					stored.trim());
				case ARGON2ID -> PasswordEncryptor.getInstance().matchArgon2id(new String(password),
					stored.trim());
				default -> false;
			};
		}
		catch (Exception aHashThatCannotBeReadIsNotAMatch)
		{
			return false;
		}
	}

	/**
	 * Which algorithm made a stored hash, read out of the hash itself
	 *
	 * @param stored
	 *            the stored hash
	 * @return the algorithm, or {@code null} when it is none this tool knows
	 */
	public static String algorithmOf(final String stored)
	{
		if (stored == null)
		{
			return null;
		}
		String value = stored.trim();
		if (value.startsWith("$argon2id$") || value.startsWith("$argon2i$")
			|| value.startsWith("$argon2d$"))
		{
			return ARGON2ID;
		}
		if (value.startsWith(SCRYPT_PREFIX))
		{
			return SCRYPT;
		}
		// the bcrypt variants, all of them "$2" and a letter
		if (value.length() > 3 && value.startsWith("$2") && value.charAt(3) == '$')
		{
			return BCRYPT;
		}
		return value.contains(":") || value.startsWith("$pbkdf2") ? PBKDF2 : null;
	}

	/**
	 * What a stored hash cost to make, in the terms of its own algorithm
	 *
	 * @param stored
	 *            the stored hash
	 * @return the cost in words, or an empty string when it cannot be read
	 */
	public static String costOf(final String stored)
	{
		String algorithm = algorithmOf(stored);
		if (algorithm == null)
		{
			return "";
		}
		try
		{
			if (BCRYPT.equals(algorithm))
			{
				int logRounds = BcryptHasher.getLogRounds(stored.trim());
				return "2^" + logRounds + " rounds";
			}
			if (SCRYPT.equals(algorithm))
			{
				String[] parts = stored.trim().split("\\$");
				return "N=" + parts[1] + " r=" + parts[2] + " p=" + parts[3];
			}
		}
		catch (Exception theCostCannotBeRead)
		{
			return "";
		}
		return "";
	}

	/** The form a scrypt hash is stored in: its parameters and then salt and hash as Base64 */
	static String encodeScrypt(final byte[] saltAndHash, final int n, final int r, final int p)
	{
		return SCRYPT_PREFIX + n + "$" + r + "$" + p + "$"
			+ Base64.getEncoder().encodeToString(saltAndHash);
	}

	private static boolean verifyScrypt(final char[] password, final String stored)
	{
		String[] parts = stored.split("\\$");
		if (parts.length != 5)
		{
			return false;
		}
		int n = Integer.parseInt(parts[1]);
		int r = Integer.parseInt(parts[2]);
		int p = Integer.parseInt(parts[3]);
		return ScryptHasher.verify(password, Base64.getDecoder().decode(parts[4]), n, r, p);
	}

	private static void requirePassword(final char[] password)
	{
		if (password == null || password.length == 0)
		{
			throw new IllegalArgumentException("there is no password to hash");
		}
	}

	private static void requireBcryptLength(final char[] password)
	{
		int bytes = new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
		if (bytes > 72)
		{
			throw new IllegalArgumentException("bcrypt only looks at the first 72 bytes of a "
				+ "password, and this one is " + bytes + " - use " + ARGON2ID + " or " + SCRYPT
				+ " for a password this long");
		}
	}

	/** What an algorithm is worth knowing about, shown next to its name */
	public static String describe(final String algorithm)
	{
		return switch (algorithm == null ? "" : algorithm.toLowerCase(Locale.ROOT))
		{
			case "argon2id" -> "memory hard, the one to pick when nothing else is required";
			case "pbkdf2" -> "the one certifications ask for";
			case "bcrypt" -> "long established; looks at the first 72 bytes of a password only";
			case "scrypt" -> "memory hard, older than Argon2id and widely available";
			default -> "";
		};
	}
}

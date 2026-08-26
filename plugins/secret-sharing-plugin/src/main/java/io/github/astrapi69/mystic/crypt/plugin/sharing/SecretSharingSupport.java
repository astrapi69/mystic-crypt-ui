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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import io.github.astrapi69.crypt.data.factory.ShamirSecretSharingFactory;

/**
 * Splitting a secret into shares and putting it back together from some of them.
 * <p>
 * This is the answer to the question every password manager eventually raises: what happens to the
 * master password if its owner cannot use it any more. The secret is split into a number of shares,
 * of which a smaller number is enough to rebuild it - three of five, say. Fewer than that reveal
 * nothing at all, which is what makes it different from cutting a password into pieces.
 * <p>
 * A share is a line of text, so it can be written on paper, mailed, or kept in someone else's safe:
 *
 * <pre>
 * MCSS1$3$5$2$Base64OfTheShare$a1b2c3d4
 * </pre>
 *
 * It carries the threshold, the number of shares, its own number, its value, and a check value over
 * all of that. The check is what catches a share that was mistyped while being copied off paper -
 * without it, a wrong character would simply rebuild a wrong secret, silently.
 */
public final class SecretSharingSupport
{

	/** What every share starts with */
	public static final String PREFIX = "MCSS1";

	/** The most shares this can produce, a limit of the underlying splitter */
	public static final int MAX_SHARES = 255;

	private SecretSharingSupport()
	{
	}

	/**
	 * Splits a secret into shares
	 *
	 * @param secret
	 *            the secret
	 * @param threshold
	 *            how many shares are needed to rebuild it
	 * @param totalShares
	 *            how many shares to produce
	 * @return the shares, one line of text each
	 */
	public static List<String> split(final byte[] secret, final int threshold,
		final int totalShares)
	{
		requireUsable(secret, threshold, totalShares);
		List<String> lines = new ArrayList<>();
		for (ShamirSecretSharingFactory.Share share : ShamirSecretSharingFactory.split(secret,
			threshold, totalShares, new SecureRandom()))
		{
			lines.add(encode(threshold, totalShares, share.getIndex(), share.getValue()));
		}
		return lines;
	}

	/**
	 * Splits a piece of text, its UTF-8 bytes being the secret
	 *
	 * @param secret
	 *            the secret as text
	 * @param threshold
	 *            how many shares are needed to rebuild it
	 * @param totalShares
	 *            how many shares to produce
	 * @return the shares, one line of text each
	 */
	public static List<String> splitText(final String secret, final int threshold,
		final int totalShares)
	{
		if (secret == null || secret.isEmpty())
		{
			throw new IllegalArgumentException("there is no secret to split");
		}
		return split(secret.getBytes(StandardCharsets.UTF_8), threshold, totalShares);
	}

	/**
	 * Puts a secret back together from shares
	 *
	 * @param shares
	 *            the shares, in any order; more than the threshold is allowed
	 * @return the secret
	 * @throws IllegalArgumentException
	 *             if a share is damaged, the shares do not belong together, or there are too few
	 */
	public static byte[] combine(final List<String> shares)
	{
		List<String> usable = new ArrayList<>();
		for (String line : shares == null ? List.<String> of() : shares)
		{
			if (line != null && !line.isBlank())
			{
				usable.add(line.trim());
			}
		}
		if (usable.isEmpty())
		{
			throw new IllegalArgumentException("there are no shares to work with");
		}
		List<ShamirSecretSharingFactory.Share> parsed = new ArrayList<>();
		int threshold = -1;
		int totalShares = -1;
		for (String line : usable)
		{
			String[] parts = check(line);
			int shareThreshold = Integer.parseInt(parts[1]);
			int shareTotal = Integer.parseInt(parts[2]);
			if (threshold == -1)
			{
				threshold = shareThreshold;
				totalShares = shareTotal;
			}
			else if (threshold != shareThreshold || totalShares != shareTotal)
			{
				throw new IllegalArgumentException(
					"these shares are not from the same secret - one says " + threshold + " of "
						+ totalShares + ", another " + shareThreshold + " of " + shareTotal);
			}
			parsed.add(new ShamirSecretSharingFactory.Share(Integer.parseInt(parts[3]),
				Base64.getDecoder().decode(parts[4])));
		}
		if (parsed.stream().map(ShamirSecretSharingFactory.Share::getIndex).distinct()
			.count() != parsed.size())
		{
			throw new IllegalArgumentException("the same share was given twice");
		}
		if (parsed.size() < threshold)
		{
			throw new IllegalArgumentException("this needs " + threshold + " shares and there are "
				+ parsed.size());
		}
		return ShamirSecretSharingFactory.combine(parsed);
	}

	/**
	 * Puts a secret back together and reads it as text
	 *
	 * @param shares
	 *            the shares
	 * @return the secret as text
	 */
	public static String combineText(final List<String> shares)
	{
		return new String(combine(shares), StandardCharsets.UTF_8);
	}

	/**
	 * How many shares a line says are needed, without rebuilding anything
	 *
	 * @param share
	 *            one share
	 * @return the threshold
	 */
	public static int thresholdOf(final String share)
	{
		return Integer.parseInt(check(share.trim())[1]);
	}

	/**
	 * Whether a line is a share of this format and undamaged
	 *
	 * @param share
	 *            the line to look at
	 * @return true if it is one
	 */
	public static boolean isShare(final String share)
	{
		try
		{
			check(share == null ? "" : share.trim());
			return true;
		}
		catch (RuntimeException notAShare)
		{
			return false;
		}
	}

	/**
	 * The most shares a secret of this size can be split into. The splitter refuses to produce more
	 * shares than the secret has bytes
	 *
	 * @param secretLength
	 *            the length of the secret in bytes
	 * @return the largest usable number of shares
	 */
	public static int maxSharesFor(final int secretLength)
	{
		return Math.min(secretLength, MAX_SHARES);
	}

	static String encode(final int threshold, final int totalShares, final int index,
		final byte[] value)
	{
		String body = PREFIX + "$" + threshold + "$" + totalShares + "$" + index + "$"
			+ Base64.getEncoder().encodeToString(value);
		return body + "$" + checkValue(body);
	}

	/** Splits a share into its parts and refuses it when its check value does not fit */
	private static String[] check(final String share)
	{
		String[] parts = share.split("\\$");
		if (parts.length != 6 || !PREFIX.equals(parts[0]))
		{
			throw new IllegalArgumentException("'" + share + "' is not a share of this application");
		}
		String body = String.join("$", parts[0], parts[1], parts[2], parts[3], parts[4]);
		if (!checkValue(body).equals(parts[5]))
		{
			throw new IllegalArgumentException("share " + parts[3] + " is damaged - its check value "
				+ "does not fit, so something was mistyped or lost while copying it");
		}
		return parts;
	}

	/** A short value over the share, so a mistyped character is noticed rather than rebuilt into
	 * a wrong secret */
	private static String checkValue(final String body)
	{
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(body.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest).substring(0, 8);
		}
		catch (Exception everyMachineHasSha256)
		{
			throw new IllegalStateException(everyMachineHasSha256);
		}
	}

	private static void requireUsable(final byte[] secret, final int threshold,
		final int totalShares)
	{
		if (secret == null || secret.length == 0)
		{
			throw new IllegalArgumentException("there is no secret to split");
		}
		if (threshold < 2)
		{
			throw new IllegalArgumentException(
				"at least two shares have to be needed - with one, a share is the secret");
		}
		if (totalShares < threshold)
		{
			throw new IllegalArgumentException("there have to be at least as many shares ("
				+ totalShares + ") as are needed to rebuild the secret (" + threshold + ")");
		}
		if (totalShares > MAX_SHARES)
		{
			throw new IllegalArgumentException("at most " + MAX_SHARES + " shares");
		}
		if (totalShares > secret.length)
		{
			// a limit of the splitter, and a surprising one - so it is said in the terms of the
			// secret rather than as a number out of nowhere
			throw new IllegalArgumentException("a secret of " + secret.length
				+ " bytes cannot be split into " + totalShares + " shares - at most "
				+ maxSharesFor(secret.length) + ", so use a longer secret or fewer shares");
		}
	}
}

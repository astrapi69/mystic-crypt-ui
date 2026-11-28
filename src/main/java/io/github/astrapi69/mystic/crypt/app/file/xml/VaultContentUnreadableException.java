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

import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

/**
 * A vault decrypted, and its content could not be read: the password or key file was right, and the
 * file holds something this version cannot build. The sign-in says so and names the reason, instead
 * of calling the password or the key file wrong (#624, decided by the maintainer).
 * <p>
 * Where the decryption authenticates nothing, content that is no xml at all ({@link #isNoXml()}) or
 * whose root the reader fails on before it converts anything ({@link #isAtTheRoot()}) proves no key
 * right: the reader turns it into a {@link VaultNotDecryptedException} there (#1188).
 * <p>
 * The reason never quotes the content: the only text taken from the failure is a type name, which
 * is no secret. Anything else is named by the kind of failure alone, because a parser's message can
 * carry a piece of the decrypted document.
 * <p>
 * A type name is taken in the form {@link #isAClassNameOfThisFamily(String)} accepts; a type of any
 * other form is named as a type this version does not know. The cause is the reader's failure as a
 * {@link ReaderFailureTrace}; a vault that was read and holds no sign-in data has none
 * ({@link #withoutItsSignInData()}).
 */
public class VaultContentUnreadableException extends IllegalArgumentException
{

	private static final long serialVersionUID = 1L;

	/**
	 * The longest type name the reason names: 200 characters. The longest type a vault of this
	 * version holds, {@code MysticCryptEntryModelBean} with its package, has 71
	 */
	static final int LONGEST_NAMED_TYPE = 200;

	/** The package every class of this family lies in */
	private static final String FAMILY = "io.github.astrapi69.";

	/** What could not be read, without any of the content */
	private final String reason;

	/** Whether the parser refused the document itself, read off the reader's own failure */
	private final boolean noXml;

	/** Whether the reader failed on the root, before it converted anything */
	private final boolean atTheRoot;

	/**
	 * The failure of reading a decrypted vault, and where it happened
	 *
	 * @param cause
	 *            what the reader threw: an exception of XStream, or any other the reading ended
	 *            with before or after XStream converted anything
	 * @param atTheRoot
	 *            whether the reader failed on the root, before it converted anything: a root it
	 *            cannot read, decode or resolve, or a root of another type than the vault
	 */
	public VaultContentUnreadableException(final RuntimeException cause, final boolean atTheRoot)
	{
		this(reasonOf(cause), ReaderFailureTrace.of(cause), refusedByTheParser(cause), atTheRoot);
	}

	private VaultContentUnreadableException(final String reason, final ReaderFailureTrace cause,
		final boolean noXml, final boolean atTheRoot)
	{
		super("The database was decrypted, but its content could not be read: " + reason + ".",
			cause);
		this.reason = reason;
		this.noXml = noXml;
		this.atTheRoot = atTheRoot;
	}

	/**
	 * A vault that was read and holds no sign-in data: the content is a vault, and the sign-in has
	 * nothing to take over from it
	 *
	 * @return the failure, without a cause
	 */
	public static VaultContentUnreadableException withoutItsSignInData()
	{
		return new VaultContentUnreadableException("it holds no sign-in data", null, false, false);
	}

	/**
	 * What could not be read
	 *
	 * @return the reason, without any of the content
	 */
	public String getReason()
	{
		return reason;
	}

	/**
	 * Whether the content is no xml at all: the parser refused the document itself, which XStream
	 * reports as a {@link StreamException}, rather than the reader a type or a field of a
	 * well-formed one. After a decryption that authenticates nothing this is what a wrong key
	 * leaves, and such a database was not decrypted (#1188, {@link VaultNotDecryptedException})
	 *
	 * @return true if the parser refused the content
	 */
	public boolean isNoXml()
	{
		return noXml;
	}

	/**
	 * Whether the reader failed on the root, before it converted anything: a root it cannot read,
	 * decode or resolve, or a root of another type than the vault. After a decryption that
	 * authenticates nothing a wrong key can leave such a root as well as no xml, and such a
	 * database was not decrypted either (#1188)
	 *
	 * @return true if the reader failed on the root
	 */
	public boolean isAtTheRoot()
	{
		return atTheRoot;
	}

	/**
	 * Whether the given name, as the content gives it, is a binary class name of this family:
	 * {@code io.github.astrapi69.} followed by segments of Java identifier characters separated by
	 * dots, at most {@value #LONGEST_NAMED_TYPE} characters long
	 *
	 * @param name
	 *            the name
	 * @return true if the reason may name it
	 */
	static boolean isAClassNameOfThisFamily(final String name)
	{
		if (name == null || LONGEST_NAMED_TYPE < name.length() || !name.startsWith(FAMILY))
		{
			return false;
		}
		for (String segment : name.substring(FAMILY.length()).split("\\.", -1))
		{
			if (!isAJavaIdentifier(segment))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean isAJavaIdentifier(final String segment)
	{
		return !segment.isEmpty() && Character.isJavaIdentifierStart(segment.charAt(0))
			&& segment.chars().allMatch(character -> Character.isJavaIdentifierPart(character)
				&& !Character.isIdentifierIgnorable(character));
	}

	private static boolean refusedByTheParser(final Throwable failure)
	{
		for (Throwable cause = failure; cause != null; cause = cause.getCause())
		{
			if (cause instanceof StreamException)
			{
				return true;
			}
		}
		return false;
	}

	private static String reasonOf(final Throwable failure)
	{
		for (Throwable cause = failure; cause != null; cause = cause.getCause())
		{
			if (cause instanceof CannotResolveClassException unknown)
			{
				return isAClassNameOfThisFamily(unknown.getMessage())
					? "it names the type " + unknown.getMessage()
						+ ", which this version does not know"
					: "it names a type this version does not know";
			}
		}
		Throwable innermost = failure;
		while (innermost.getCause() != null)
		{
			innermost = innermost.getCause();
		}
		return "the reader failed with " + innermost.getClass().getSimpleName();
	}
}

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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation;

/**
 * A rule table to import names a type no rule table holds. The reader refuses it before that type
 * is built, so nothing is imported.
 */
public class RuleTableHoldsAForeignTypeException extends IllegalArgumentException
{

	private static final long serialVersionUID = 1L;

	/** The fully qualified name of the type the file names */
	private final String typeName;

	/**
	 * A refusal of the given type
	 *
	 * @param typeName
	 *            the fully qualified name of the type the file names
	 * @param cause
	 *            the reader's own refusal
	 */
	public RuleTableHoldsAForeignTypeException(final String typeName, final Throwable cause)
	{
		super("This file names the type " + typeName + ", which no rule table holds. Nothing was "
			+ "imported.", cause);
		this.typeName = typeName;
	}

	/**
	 * The type the file names
	 *
	 * @return its fully qualified name
	 */
	public String getTypeName()
	{
		return typeName;
	}
}

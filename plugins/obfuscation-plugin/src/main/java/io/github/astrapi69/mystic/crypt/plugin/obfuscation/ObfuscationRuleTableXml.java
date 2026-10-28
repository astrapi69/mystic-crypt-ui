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

import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.ForbiddenClassException;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.xstream.factory.XStreamFactory;

/**
 * Reads the rule tables the two obfuscation windows import, the XML their export wrote, once it is
 * decrypted. Each table is read with an XStream that builds XStream's own standard types and the
 * types of that table, and refuses every other type before it is built.
 */
public final class ObfuscationRuleTableXml
{

	/** The types of a table of the simple obfuscation window */
	private static final Class<?>[] SIMPLE_RULE_TABLE_TYPES = { KeyValuePair.class,
			ObfuscationRule.class };

	/** The types of a table of the character operation window */
	private static final Class<?>[] OPERATION_RULE_TABLE_TYPES = { KeyValuePair.class,
			ObfuscationOperationRule.class };

	private ObfuscationRuleTableXml()
	{
	}

	/**
	 * The rules of the simple obfuscation window
	 *
	 * @param xml
	 *            the decrypted XML of an exported table
	 * @return the rules
	 * @throws RuleTableHoldsAForeignTypeException
	 *             when the XML names a type no such table holds; that type is not built
	 */
	public static List<KeyValuePair<Character, ObfuscationRule<Character, Character>>> toSimpleRules(
		final String xml)
	{
		return read(xml, SIMPLE_RULE_TABLE_TYPES);
	}

	/**
	 * The rules of the character operation window
	 *
	 * @param xml
	 *            the decrypted XML of an exported table
	 * @return the rules
	 * @throws RuleTableHoldsAForeignTypeException
	 *             when the XML names a type no such table holds; that type is not built
	 */
	public static List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> toOperationRules(
		final String xml)
	{
		return read(xml, OPERATION_RULE_TABLE_TYPES);
	}

	/**
	 * The title of the dialog shown when an imported file names a type no rule table holds
	 *
	 * @return the dialog title
	 */
	public static String refusalTitle()
	{
		return ObfuscationMessages.getString("obfuscation.rule.table.refused.foreign.type.title",
			"Rule table not imported");
	}

	/**
	 * The message of that dialog
	 *
	 * @param refusal
	 *            the reader's refusal
	 * @return the message text, naming the type
	 */
	public static String refusalMessage(final RuleTableHoldsAForeignTypeException refusal)
	{
		// %1$s rather than {0}: the bundle lookup formats braces itself and fills them with null
		return String.format(
			ObfuscationMessages.getString("obfuscation.rule.table.refused.foreign.type",
				"This file names the type %1$s, which no rule table holds. Nothing was imported."),
			refusal.getTypeName());
	}

	/**
	 * Reads the table with XStream, on an instance from {@code XStreamFactory} of
	 * xstream-extensions, a dependency, that is allowed the table's types
	 *
	 * @param xml
	 *            the decrypted rule table
	 * @param types
	 *            the types the table holds, besides XStream's own standard types
	 * @return the rule table
	 * @throws RuleTableHoldsAForeignTypeException
	 *             when the xml names another type; that type is not built
	 */
	@SuppressWarnings("unchecked")
	private static <T> T read(final String xml, final Class<?>[] types)
	{
		XStream xStream = XStreamFactory.newXStream();
		xStream.allowTypes(types);
		try
		{
			return (T)xStream.fromXML(xml);
		}
		catch (RuntimeException failure)
		{
			for (Throwable cause = failure; cause != null; cause = cause.getCause())
			{
				if (cause instanceof ForbiddenClassException refused)
				{
					throw new RuleTableHoldsAForeignTypeException(refused.getMessage(), refused);
				}
			}
			throw failure;
		}
	}
}

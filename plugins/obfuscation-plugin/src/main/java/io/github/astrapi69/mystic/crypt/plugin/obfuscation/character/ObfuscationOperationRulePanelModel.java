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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation.character;

import io.github.astrapi69.crypt.api.obfuscation.rule.Operation;

/**
 * Everything the {@link ObfuscationOperationRulePanel} holds: the original character a rule
 * replaces, the character it is replaced with, the comma separated indexes the rule is limited to
 * and the operation that is applied.
 * <p>
 * The panel binds its components to this object, so what the user typed or chose is readable here
 * at any moment - the Add button reads the rule from here and never asks a widget for its content.
 */
public class ObfuscationOperationRulePanelModel
{

	/** The character a rule replaces, as typed; empty as long as nothing was typed */
	private String originalCharacter = "";

	/** The character the original one is replaced with, as typed */
	private String replaceWith = "";

	/** The indexes the rule is limited to, as the comma separated text the user typed */
	private String indexes = "";

	/** The operation the rule applies, as chosen in the combo box */
	private Operation operation;

	/**
	 * Gets the character a rule replaces
	 *
	 * @return the original character, empty when nothing was typed
	 */
	public String getOriginalCharacter()
	{
		return originalCharacter;
	}

	/**
	 * Sets the character a rule replaces
	 *
	 * @param originalCharacter
	 *            the original character
	 */
	public void setOriginalCharacter(final String originalCharacter)
	{
		this.originalCharacter = originalCharacter;
	}

	/**
	 * Gets the character the original one is replaced with
	 *
	 * @return the replacement character, empty when nothing was typed
	 */
	public String getReplaceWith()
	{
		return replaceWith;
	}

	/**
	 * Sets the character the original one is replaced with
	 *
	 * @param replaceWith
	 *            the replacement character
	 */
	public void setReplaceWith(final String replaceWith)
	{
		this.replaceWith = replaceWith;
	}

	/**
	 * Gets the indexes the rule is limited to
	 *
	 * @return the indexes as comma separated text, empty when the rule is not limited
	 */
	public String getIndexes()
	{
		return indexes;
	}

	/**
	 * Sets the indexes the rule is limited to
	 *
	 * @param indexes
	 *            the indexes as comma separated text
	 */
	public void setIndexes(final String indexes)
	{
		this.indexes = indexes;
	}

	/**
	 * Gets the operation the rule applies
	 *
	 * @return the operation
	 */
	public Operation getOperation()
	{
		return operation;
	}

	/**
	 * Sets the operation the rule applies
	 *
	 * @param operation
	 *            the operation
	 */
	public void setOperation(final Operation operation)
	{
		this.operation = operation;
	}
}

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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.api.obfuscation.rule.Operation;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationOperationRule;

/**
 * The window's obfuscate and disentangle buttons must be inverses of each other, through the same
 * library methods the command line uses
 */
class OperationRulePanelBindingTest
{

	/**
	 * Disentangle turns an obfuscated text back into the original: with the 13.9 library the
	 * deprecated path threw a {@link NullPointerException} for the plain {@code HashBiMap} the
	 * table builds, so the window could obfuscate and never reverse (#884)
	 */
	@Test
	void disentangleTurnsAnObfuscatedTextBack()
	{
		OperationRulePanel panel = new OperationRulePanel();
		ObfuscationOperationRule<Character, Character> rule = ObfuscationOperationRule
			.<Character, Character> builder().character('a').replaceWith('b')
			.operation(Operation.UPPERCASE).indexes(new TreeSet<>(java.util.List.of(2))).build();
		panel.getModelObject().getTableModel().getData()
			.add(KeyValuePair.<Character, ObfuscationOperationRule<Character, Character>> builder()
				.key('a').value(rule).build());
		panel.getEnDecryptPanel().getTxtToEncrypt().setText("acac");

		panel.onEncrypt(null);
		String obfuscated = panel.getEnDecryptPanel().getModelObject().getRightContent();
		assertNotEquals("acac", obfuscated, "obfuscation changed nothing");

		panel.onDecrypt(null);

		assertEquals("acac", panel.getEnDecryptPanel().getModelObject().getLeftContent(),
			"< Disentangle did not turn the obfuscated text back (#884)");
	}
}

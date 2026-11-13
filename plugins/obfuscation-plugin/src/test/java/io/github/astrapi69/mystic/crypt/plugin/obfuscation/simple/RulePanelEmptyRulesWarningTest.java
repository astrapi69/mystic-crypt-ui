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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation.simple;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;

/**
 * An empty rule table refuses Obfuscate and Disentangle with a message instead of letting the
 * library's "Given map 'rules' may not be empty" go to the console (#891). The guard is pure, so
 * the panel itself, which cannot be built outside the application frame, stays out of the test
 */
class RulePanelEmptyRulesWarningTest
{

	@Test
	@DisplayName("an empty rule table gets a warning that says what to do")
	void anEmptyRuleTable_getsAWarning()
	{
		String warning = RulePanel.emptyRulesWarning(Map.of());

		assertTrue(warning != null && !warning.isBlank(), "the warning says what to do");
		assertTrue(warning.toLowerCase().contains("rule"), "it names the rule table: " + warning);
	}

	@Test
	@DisplayName("a filled rule table gets no warning")
	void aFilledRuleTable_getsNoWarning()
	{
		ObfuscationRule<Character, Character> rule = ObfuscationRule
			.<Character, Character> builder().character('a').replaceWith('x').build();

		assertNull(RulePanel.emptyRulesWarning(Map.of('a', rule)));
	}

	@Test
	@DisplayName("a missing rule table counts as empty, not as a crash")
	void aMissingRuleTable_countsAsEmpty()
	{
		String warning = RulePanel.emptyRulesWarning(null);

		assertFalse(warning == null || warning.isBlank());
	}
}

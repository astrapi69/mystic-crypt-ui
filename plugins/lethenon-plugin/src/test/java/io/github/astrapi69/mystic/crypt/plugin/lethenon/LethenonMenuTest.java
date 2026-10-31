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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The plugin's menu (#708, README decision 3): the wallet is the way in, the launch checklist for
 * operators is under "More ...", not at the top
 */
class LethenonMenuTest
{

	@Test
	@DisplayName("\"Wallet\" comes first, \"More ...\" last, and it holds the checklist as \"For Operators\"")
	void theMenu_startsWithTheWallet_andKeepsTheChecklistForOperators()
	{
		List<JMenuItem> items = new LethenonMenuContribution().getMenuItems();

		assertEquals("Wallet", items.get(0).getText());
		JMenu more = assertInstanceOf(JMenu.class, items.get(items.size() - 1));
		assertEquals("More ...", more.getText());
		assertEquals(1, more.getItemCount());
		assertEquals("For Operators", more.getItem(0).getText());
		assertTrue(items.stream().noneMatch(item -> "Before You Start".equals(item.getText())),
			"the checklist is no longer at the top of the menu");
	}
}

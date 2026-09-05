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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import javax.swing.Icon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.keepass.KeePassIcons;

/**
 * An entry imported from KeePass carries the icon its owner picked there. The entry table is where
 * entries are listed, so that is where the icon has to appear (#206)
 */
class MysticCryptEntryTableModelTest
{

	private static MysticCryptEntryTableModel modelWith(final MysticCryptEntryModelBean entry)
	{
		MysticCryptEntryTableModel tableModel = new MysticCryptEntryTableModel();
		tableModel.setData(List.of(entry));
		return tableModel;
	}

	@Test
	@DisplayName("the icon column shows the icon of the entry's KeePass index")
	void theIconColumnShowsTheIconOfTheKeePassIndex()
	{
		MysticCryptEntryTableModel tableModel = modelWith(MysticCryptEntryModelBean.builder()
			.title("Mail").userName("me").url("https://example.com").keePassIconIndex(19).build());

		assertSame(KeePassIcons.of(19), tableModel.getValueAt(0, 0));
		assertEquals(Icon.class, tableModel.getColumnClass(0));
	}

	@Test
	@DisplayName("the other columns keep showing title, username and url")
	void theRemainingColumnsAreUnchanged()
	{
		MysticCryptEntryTableModel tableModel = modelWith(MysticCryptEntryModelBean.builder()
			.title("Mail").userName("me").url("https://example.com").keePassIconIndex(19).build());

		assertEquals("Mail", tableModel.getValueAt(0, 1));
		assertEquals("me", tableModel.getValueAt(0, 2));
		assertEquals("https://example.com", tableModel.getValueAt(0, 3));
	}

	@Test
	@DisplayName("an entry that was never imported from KeePass simply has no icon")
	void anEntryWithoutAKeePassIconHasNoIcon()
	{
		MysticCryptEntryTableModel tableModel = modelWith(
			MysticCryptEntryModelBean.builder().title("Handmade").build());

		assertNull(tableModel.getValueAt(0, 0));
		assertEquals("Handmade", tableModel.getValueAt(0, 1));
	}
}

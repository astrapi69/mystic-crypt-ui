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
package io.github.astrapi69.mystic.crypt.plugin.vault;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.plugin.api.EntryActionContribution;

/**
 * A plugin's action on one entry (#1120) reaches only the entry the user picked, only at the click,
 * and as a copy, the three limits of the maintainer's decision of 2026-11-24; a failing action is
 * named and ends nothing
 */
class EntryActionsTest
{

	private final List<MysticCryptEntryModelBean> handed = new ArrayList<>();

	private final JPanel parent = new JPanel();

	private final Icon icon = new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));

	private final char[] password = TestPasswords.throwaway().toCharArray();

	private final MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
		.id(UUID.randomUUID()).title("home network".toCharArray()).password(password.clone())
		.build();

	private EntryActions.Offered action(final EntryActionContribution contribution)
	{
		return new EntryActions.Offered(contribution, "wifi-qr-plugin");
	}

	private final EntryActionContribution recording = new EntryActionContribution()
	{
		@Override
		public String getActionName()
		{
			return "Show Wi-Fi QR code";
		}

		@Override
		public Icon getIcon()
		{
			return icon;
		}

		@Override
		public void act(final MysticCryptEntryModelBean picked, final Component where)
		{
			handed.add(picked);
		}
	};

	@Test
	@DisplayName("the item carries the action's name and icon and is enabled while one entry is selected")
	void theItem_isEnabledWhileOneEntryIsSelected()
	{
		List<JMenuItem> items = EntryActions.menuItems(List.of(action(recording)),
			Optional.of(entry), parent);

		assertEquals(1, items.size());
		assertEquals("Show Wi-Fi QR code", items.get(0).getText());
		assertEquals("mnuEntryAction-wifi-qr-plugin", items.get(0).getName());
		assertSame(icon, items.get(0).getIcon(), "the item shows the icon the plugin brings");
		assertTrue(items.get(0).isEnabled());
		assertTrue(handed.isEmpty(), "nothing is handed over before the click");
	}

	@Test
	@DisplayName("without one selected entry the item is disabled and a click hands over nothing")
	void withoutASelectedEntry_theItemIsDisabled()
	{
		JMenuItem item = EntryActions
			.menuItems(List.of(action(recording)), Optional.empty(), parent).get(0);

		assertFalse(item.isEnabled());
		item.doClick();
		assertTrue(handed.isEmpty());
	}

	@Test
	@DisplayName("the click hands the plugin a copy: changing it changes nothing of the vault's entry")
	void theClick_handsACopy()
	{
		EntryActions.menuItems(List.of(action(recording)), Optional.of(entry), parent).get(0)
			.doClick();

		assertEquals(1, handed.size());
		MysticCryptEntryModelBean copy = handed.get(0);
		assertNotSame(entry, copy);
		assertNotSame(entry.getPassword(), copy.getPassword());
		assertArrayEquals(password, copy.getPassword());
		Arrays.fill(copy.getPassword(), '\0');
		assertArrayEquals(password, entry.getPassword(), "the vault's password is untouched");
	}

	@Test
	@DisplayName("a failing action is named with its plugin and its reason, and nothing is thrown")
	void aFailingAction_isNamed()
	{
		EntryActionContribution failing = new EntryActionContribution()
		{
			@Override
			public String getActionName()
			{
				return "fails";
			}

			@Override
			public Icon getIcon()
			{
				return icon;
			}

			@Override
			public void act(final MysticCryptEntryModelBean picked, final Component where)
			{
				throw new IllegalStateException("no network name");
			}
		};
		List<String> told = new ArrayList<>();

		EntryActions.perform(action(failing), entry, parent, told::add);

		assertEquals(List.of("The action of the plugin wifi-qr-plugin failed: no network name"),
			told);
	}
}

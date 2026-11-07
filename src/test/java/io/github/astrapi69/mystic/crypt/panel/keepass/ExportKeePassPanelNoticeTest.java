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
package io.github.astrapi69.mystic.crypt.panel.keepass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The line a plugin gives the KDBX export dialog about what the file will contain, such as the
 * sealed lethenon wallets (#810, decision 4 of
 * docs/decisions/lethenon-wallet-in-vault-2026-10-30.md)
 */
class ExportKeePassPanelNoticeTest
{

	private static Component named(final Container container, final String name)
	{
		for (Component component : container.getComponents())
		{
			if (name.equals(component.getName()))
			{
				return component;
			}
			if (component instanceof Container inner && named(inner, name) != null)
			{
				return named(inner, name);
			}
		}
		return null;
	}

	@Test
	@DisplayName("the export dialog shows the lines the plugins give, above the fields")
	void theExportDialogShowsThePluginsLinesAboveTheFields()
	{
		ExportKeePassPanel panel = new ExportKeePassPanel(null, null,
			List.of("The file contains sealed lethenon wallets.", "A second plugin's line."));

		JTextComponent notices = (JTextComponent)named(panel, "txtExportNotices");

		assertEquals("The file contains sealed lethenon wallets.\nA second plugin's line.",
			notices.getText());
		assertTrue(notices.isVisible());
		assertFalse(notices.isEditable(), "the line can be changed in the dialog");
		assertSame(notices, panel.getComponent(0), "the line is not the first thing in the dialog");
	}

	@Test
	@DisplayName("without a plugin's line the export dialog has no place for one")
	void withoutAPluginsLine_theDialogHasNoPlaceForOne()
	{
		assertNull(named(new ExportKeePassPanel(), "txtExportNotices"));
		assertNull(named(new ExportKeePassPanel(null, null, List.of()), "txtExportNotices"));
	}
}

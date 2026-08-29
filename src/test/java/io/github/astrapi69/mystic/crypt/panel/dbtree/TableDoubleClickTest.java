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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JTable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesPanel;

/**
 * A row that answers a double click with nothing reads as broken, and both tables of the entry
 * editor did exactly that: attachments could only be looked at through "Save to", and a property
 * could only be changed through the Edit button.
 */
class TableDoubleClickTest
{

	private static IModel<MysticCryptEntryModelBean> anEntryWith(final String propertyKey,
		final byte[] attachment)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		entry.setProperties(List.of(
			KeyValuePair.<String, String> builder().key(propertyKey).value("the value").build()));
		entry.setResources(
			List.of(FileContentInfo.builder().name("notes.txt").content(attachment).build()));
		return BaseModel.of(entry);
	}

	private static void doubleClick(final JTable table)
	{
		table.setRowSelectionInterval(0, 0);
		for (var listener : table.getMouseListeners())
		{
			listener.mouseClicked(new MouseEvent(table, MouseEvent.MOUSE_CLICKED,
				System.currentTimeMillis(), 0, 5, 5, 2, false, MouseEvent.BUTTON1));
		}
	}

	@Test
	@DisplayName("a double click on a property opens it for editing")
	void aDoubleClickOnAPropertyOpensItForEditing()
	{
		AtomicInteger opened = new AtomicInteger();
		PropertiesPanel panel = new PropertiesPanel(anEntryWith("the key", new byte[] { 1 }))
		{
			@Override
			protected void onEditSelected()
			{
				opened.incrementAndGet();
			}
		};

		doubleClick(panel.getTblProperties());

		assertEquals(1, opened.get(), "the double click did not reach the editing");
	}

	@Test
	@DisplayName("a double click on an attachment shows what is in it")
	void aDoubleClickOnAnAttachmentShowsWhatIsInIt()
	{
		AtomicInteger shown = new AtomicInteger();
		AttachmentPanel panel = new AttachmentPanel(anEntryWith("the key", "notes".getBytes()))
		{
			@Override
			protected void onShowSelectedContent()
			{
				shown.incrementAndGet();
			}
		};

		doubleClick(panel.getTblFiles());

		assertEquals(1, shown.get(), "the double click did not reach the content");
	}

	@Test
	@DisplayName("a single click does not open anything")
	void aSingleClickDoesNotOpenAnything()
	{
		AtomicInteger opened = new AtomicInteger();
		PropertiesPanel panel = new PropertiesPanel(anEntryWith("the key", new byte[] { 1 }))
		{
			@Override
			protected void onEditSelected()
			{
				opened.incrementAndGet();
			}
		};
		JTable table = panel.getTblProperties();
		table.setRowSelectionInterval(0, 0);

		for (var listener : table.getMouseListeners())
		{
			listener.mouseClicked(new MouseEvent(table, MouseEvent.MOUSE_CLICKED,
				System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1));
		}

		assertTrue(0 == opened.get(), "one click already opened the editor");
	}

	@Test
	@DisplayName("saving an attachment proposes the name it already has")
	void savingAnAttachmentProposesTheNameItAlreadyHas()
	{
		AttachmentPanel panel = new AttachmentPanel(anEntryWith("the key", "notes".getBytes()));

		assertEquals("notes.txt",
			panel
				.proposedTargetFor(
					FileContentInfo.builder().name("notes.txt").content(new byte[] { 1 }).build())
				.getName(),
			"the save dialog would open with a different name than the attachment carries");
	}

	@Test
	@DisplayName("an attachment without a name still gets one proposed")
	void anAttachmentWithoutANameStillGetsOneProposed()
	{
		AttachmentPanel panel = new AttachmentPanel(anEntryWith("the key", "notes".getBytes()));

		assertEquals("attachment",
			panel.proposedTargetFor(FileContentInfo.builder().content(new byte[] { 1 }).build())
				.getName(),
			"the save dialog would open with an empty name again");
	}

}

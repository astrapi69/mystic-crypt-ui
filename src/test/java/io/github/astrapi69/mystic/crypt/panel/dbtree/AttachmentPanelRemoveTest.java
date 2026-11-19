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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.model.BaseModel;

/**
 * "Remove" on an attachment: the table lists the entry's own list of attachments, so a row taken
 * out of the table is taken out of the entry. Pinned for the table models of swing-table-components
 * 4.0 (#1047), whose {@code BaseTableModel} and {@code TableColumnsModel} the attachment table is
 * built on
 */
class AttachmentPanelRemoveTest
{

	private static MysticCryptEntryModelBean anEntryWithTwoAttachments()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		entry.setResources(new ArrayList<>(List.of(
			FileContentInfo.builder().name("first.txt").content(new byte[] { 1 }).build(),
			FileContentInfo.builder().name("second.bin").content(new byte[] { 2, 3 }).build())));
		return entry;
	}

	private static List<String> namesIn(final MysticCryptEntryModelBean entry)
	{
		return entry.getResources().stream().map(FileContentInfo::getName).toList();
	}

	private static List<String> namesInTheTable(final AttachmentPanel panel)
	{
		return panel.getTblFiles().getGenericTableModel().getData().stream()
			.map(FileContentInfo::getName).toList();
	}

	@Test
	@DisplayName("Remove takes the selected attachment out of the table and out of the entry, and leaves the other one")
	void remove_takesTheSelectedAttachmentOutOfTheEntry()
	{
		MysticCryptEntryModelBean entry = anEntryWithTwoAttachments();
		AttachmentPanel panel = new AttachmentPanel(BaseModel.of(entry));
		panel.getTblFiles().setRowSelectionInterval(0, 0);

		panel.onRemove(null);

		assertEquals(List.of("second.bin"), namesInTheTable(panel));
		assertEquals(List.of("second.bin"), namesIn(entry));
		assertEquals(2, entry.getResources().get(0).getContent().length,
			"the attachment that is left keeps its content");
	}

	@Test
	@DisplayName("Remove of the second row takes the second attachment and leaves the first")
	void remove_ofTheSecondRow_takesTheSecondAttachment()
	{
		MysticCryptEntryModelBean entry = anEntryWithTwoAttachments();
		AttachmentPanel panel = new AttachmentPanel(BaseModel.of(entry));
		panel.getTblFiles().setRowSelectionInterval(1, 1);

		panel.onRemove(null);

		assertEquals(List.of("first.txt"), namesInTheTable(panel));
		assertEquals(List.of("first.txt"), namesIn(entry));
	}

	@Test
	@DisplayName("Remove with no row selected changes neither the table nor the entry")
	void removeWithNoRowSelected_changesNothing()
	{
		MysticCryptEntryModelBean entry = anEntryWithTwoAttachments();
		AttachmentPanel panel = new AttachmentPanel(BaseModel.of(entry));

		panel.onRemove(null);

		assertEquals(List.of("first.txt", "second.bin"), namesInTheTable(panel));
		assertEquals(List.of("first.txt", "second.bin"), namesIn(entry));
	}
}

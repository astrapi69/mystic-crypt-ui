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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.model.BaseModel;

/**
 * "Save to" on an attachment: what it writes, and what it says when the attachment belongs to an
 * entry a plugin marks, such as a sealed lethenon wallet (#810)
 */
class AttachmentPanelSaveToTest
{

	private static final byte[] SEALED = { 7, 1, 8, 2, 8, 1, 8, 2, 8, 4, 5, 9 };

	private static MysticCryptEntryModelBean aWalletEntry()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		VaultEntryProperties.replace(entry, Map.of("lethenon.wallet", "testnet"), Set.of());
		entry.setResources(new ArrayList<>(List
			.of(FileContentInfo.builder().name("wallet.lethwf").content(SEALED.clone()).build())));
		return entry;
	}

	private static AttachmentPanel savingTo(final MysticCryptEntryModelBean entry,
		final File target)
	{
		AttachmentPanel panel = new AttachmentPanel(BaseModel.of(entry))
		{
			@Override
			protected File chooseTargetFor(final FileContentInfo fileContentInfo)
			{
				return target;
			}
		};
		panel.getTblFiles().setRowSelectionInterval(0, 0);
		return panel;
	}

	@Test
	@DisplayName("Save to writes the sealed wallet of a marked entry as it is")
	void saveTo_writesTheAttachmentAsItIs(@TempDir final File directory) throws IOException
	{
		File target = new File(directory, "wallet.lethwf");

		savingTo(aWalletEntry(), target).onSaveTo(null);

		assertArrayEquals(SEALED, Files.readAllBytes(target.toPath()));
	}

	@Test
	@DisplayName("Cancel in the save dialog writes nothing")
	void cancelInTheSaveDialog_writesNothing(@TempDir final File directory)
	{
		savingTo(aWalletEntry(), null).onSaveTo(null);

		assertFalse(new File(directory, "wallet.lethwf").exists());
		assertArrayEquals(new String[0], directory.list());
	}
}

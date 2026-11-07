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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

	private static final String PLUGINS_LINE = "This file is a sealed lethenon wallet.";

	private static MysticCryptEntryModelBean aWalletEntry()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		VaultEntryProperties.replace(entry, Map.of("lethenon.wallet", "testnet"), Set.of());
		entry.setResources(new ArrayList<>(List
			.of(FileContentInfo.builder().name("wallet.lethwf").content(SEALED.clone()).build())));
		return entry;
	}

	/** What the panel asked, chose and showed while saving */
	private static final class Recorded
	{

		private String asked;

		private boolean saveDialogOpened;

		private File shownFile;

		private String shownChecksum;
	}

	/**
	 * A panel whose plugin gives the line, whose user answers the line and picks the target, and
	 * which records what it asked and showed
	 */
	private static AttachmentPanel saving(final Optional<String> pluginsLine, final boolean goesOn,
		final File target, final Recorded recorded)
	{
		AttachmentPanel panel = new AttachmentPanel(BaseModel.of(aWalletEntry()))
		{
			@Override
			protected Optional<String> attachmentExportNotice()
			{
				return pluginsLine;
			}

			@Override
			protected boolean confirmLeavingTheVault(final String notice)
			{
				recorded.asked = notice;
				return goesOn;
			}

			@Override
			protected File chooseTargetFor(final FileContentInfo fileContentInfo)
			{
				recorded.saveDialogOpened = true;
				return target;
			}

			@Override
			protected void showWrittenFile(final File writtenFile, final String sha256)
			{
				recorded.shownFile = writtenFile;
				recorded.shownChecksum = sha256;
			}
		};
		panel.getTblFiles().setRowSelectionInterval(0, 0);
		return panel;
	}

	private static String sha256Hex(final byte[] content) throws NoSuchAlgorithmException
	{
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
	}

	@Test
	@DisplayName("Save to writes the sealed wallet of a marked entry as it is")
	void saveTo_writesTheAttachmentAsItIs(@TempDir final File directory) throws IOException
	{
		File target = new File(directory, "wallet.lethwf");

		saving(Optional.of(PLUGINS_LINE), true, target, new Recorded()).onSaveTo(null);

		assertArrayEquals(SEALED, Files.readAllBytes(target.toPath()));
	}

	@Test
	@DisplayName("Save to of a marked entry shows the plugin's line first, then the SHA-256 of the file on disk")
	void saveToOfAMarkedEntry_showsTheLineFirstAndTheChecksumAfter(@TempDir final File directory)
		throws NoSuchAlgorithmException
	{
		File target = new File(directory, "wallet.lethwf");
		Recorded recorded = new Recorded();

		saving(Optional.of(PLUGINS_LINE), true, target, recorded).onSaveTo(null);

		assertEquals(PLUGINS_LINE, recorded.asked);
		assertEquals(target, recorded.shownFile);
		assertTrue(sha256Hex(SEALED).equalsIgnoreCase(recorded.shownChecksum),
			"shown " + recorded.shownChecksum + ", the file has " + sha256Hex(SEALED));
	}

	@Test
	@DisplayName("Cancel at the plugin's line writes nothing and opens no save dialog")
	void cancelAtThePluginsLine_writesNothing(@TempDir final File directory)
	{
		Recorded recorded = new Recorded();

		saving(Optional.of(PLUGINS_LINE), false, new File(directory, "wallet.lethwf"), recorded)
			.onSaveTo(null);

		assertFalse(recorded.saveDialogOpened, "the save dialog opened after Cancel");
		assertArrayEquals(new String[0], directory.list());
		assertNull(recorded.shownChecksum);
	}

	@Test
	@DisplayName("Cancel in the save dialog writes nothing and shows no checksum")
	void cancelInTheSaveDialog_writesNothing(@TempDir final File directory)
	{
		Recorded recorded = new Recorded();

		saving(Optional.of(PLUGINS_LINE), true, null, recorded).onSaveTo(null);

		assertArrayEquals(new String[0], directory.list());
		assertNull(recorded.shownChecksum);
	}

	@Test
	@DisplayName("an attachment no plugin speaks for is saved without a question and without a checksum, as before")
	void anAttachmentNoPluginSpeaksFor_isSavedAsBefore(@TempDir final File directory)
		throws IOException
	{
		File target = new File(directory, "wallet.lethwf");
		Recorded recorded = new Recorded();

		saving(Optional.empty(), true, target, recorded).onSaveTo(null);

		assertArrayEquals(SEALED, Files.readAllBytes(target.toPath()));
		assertNull(recorded.asked, "a question came without a plugin's line");
		assertNull(recorded.shownChecksum, "a checksum was shown without a plugin's line");
	}
}

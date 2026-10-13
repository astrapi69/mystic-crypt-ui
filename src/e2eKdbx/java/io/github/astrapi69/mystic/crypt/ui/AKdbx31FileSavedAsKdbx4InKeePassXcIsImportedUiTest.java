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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * The way out that the refusal of a KDBX 3.1 file with protected attachments names, measured
 * (#568): the same file, opened in KeePassXC 2.7.10, switched to the KDBX 4 format in its database
 * settings and saved, is imported with its attachment byte for byte. The refusal message names this
 * way only because this test holds; if it fails, the message must stop naming it.
 * <p>
 * The fixture and how KeePassXC saved it: {@code src/e2eKdbx/resources/kdbx/README.md}.
 */
class AKdbx31FileSavedAsKdbx4InKeePassXcIsImportedUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String FIXTURE = "keepassxc-2.7.10-kdbx4-saved-from-kdbx31-protected-attachment.kdbx";

	/** The attachment the KDBX 3.1 fixture protects, as it was added */
	private static final byte[] ATTACHMENT = "secret attachment content, 2026-10-13\n"
		.getBytes(StandardCharsets.UTF_8);

	@Test
	@DisplayName("saved as KDBX 4 in KeePassXC, the file is imported with its attachment byte for byte")
	void savedAsKdbx4_theFileIsImportedWithItsAttachment() throws Exception
	{
		File vault = new File(tempHome, "imports-the-kdbx4-copy.mcrdb");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);

		application.importKeePassDatabase(KdbxFixtures.copy(FIXTURE, tempHome),
			TestPasswords.KEEPASS_FIXTURE);

		MysticCryptEntryModelBean entry = KdbxFixtures.liveEntryTitled("Sample Entry");
		List<FileContentInfo> attachments = entry.getResources();
		assertEquals(1, attachments.size(), "the entry carries the one attachment");
		assertEquals("protected.txt", attachments.get(0).getName());
		assertArrayEquals(ATTACHMENT, attachments.get(0).getContent(),
			"the attachment arrives byte for byte");
	}
}

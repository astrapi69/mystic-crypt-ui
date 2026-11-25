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
package io.github.astrapi69.mystic.crypt.panel.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.keepass.MemoizedKeePassModelBean;

/**
 * The copies of the remembered files that were kept aside as unreadable keep the key file paths
 * that were in them, and the notice that lists them can delete them (#1124). It deletes only those
 * copies: a file that is no longer such a copy in the configuration directory is refused and stays,
 * and a copy that cannot be deleted is reported with its reason
 */
class DeletingTheKeptAsideCopiesTest
{

	@TempDir
	File directory;

	private File signinCopy;

	private File keePassCopy;

	private File settingsCopy;

	private File signin;

	private void writeTheFiles() throws Exception
	{
		signin = new File(directory, MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		signinCopy = new File(directory, MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME
			+ ".unreadable-20261101T101010");
		keePassCopy = new File(directory,
			MemoizedKeePassModelBean.JSON_FILENAME + ".unreadable-20261102T111111");
		settingsCopy = new File(directory, "settings.json.unreadable-20261103T121212");
		for (File file : List.of(signin, signinCopy, keePassCopy, settingsCopy))
		{
			Files.writeString(file.toPath(), "{\"selectedKeyFilePath\": \"/keys/a-key.pem\"");
		}
	}

	@Test
	@DisplayName("the listed copies of both remembered files are deleted, and no other file")
	void theListedCopies_areDeleted_andNoOtherFile() throws Exception
	{
		writeTheFiles();
		List<File> listed = RememberedKeyFileSupport.keptAsideCopies(directory);

		RememberedKeyFileSupport.KeptAsideDeletion deletion = RememberedKeyFileSupport
			.deleteKeptAsideCopies(listed, directory);

		assertEquals(List.of(keePassCopy, signinCopy), deletion.deleted(),
			"in the order they were listed, sorted by path");
		assertTrue(deletion.notDeleted().isEmpty(),
			"nothing was refused: " + deletion.notDeleted());
		assertFalse(signinCopy.exists(), "the sign-in copy is still there");
		assertFalse(keePassCopy.exists(), "the KeePass copy is still there");
		assertTrue(settingsCopy.exists(),
			"a copy of the settings, which holds no key file path, went");
		assertTrue(signin.exists(), "the remembered sign-in itself went");
	}

	@Test
	@DisplayName("a listed file that is no kept-aside copy of the remembered files is refused and stays")
	void aListedFileThatIsNoCopy_isRefused_andStays() throws Exception
	{
		writeTheFiles();

		RememberedKeyFileSupport.KeptAsideDeletion deletion = RememberedKeyFileSupport
			.deleteKeptAsideCopies(List.of(signin, settingsCopy), directory);

		assertTrue(deletion.deleted().isEmpty(), "deleted: " + deletion.deleted());
		assertEquals(List.of(signin, settingsCopy),
			deletion.notDeleted().stream().map(RememberedKeyFileSupport.NotDeleted::file).toList());
		assertTrue(deletion.notDeleted().stream().allMatch(refused -> refused
			.reason() == RememberedKeyFileSupport.NotDeleted.Reason.NOT_A_KEPT_ASIDE_COPY));
		assertTrue(signin.exists() && settingsCopy.exists(), "a refused file went");
	}

	@Test
	@DisplayName("a copy that cannot be deleted is reported with its reason, and the other one still goes")
	void aCopyThatCannotBeDeleted_isReportedWithItsReason() throws Exception
	{
		writeTheFiles();
		List<File> listed = RememberedKeyFileSupport.keptAsideCopies(directory);

		RememberedKeyFileSupport.KeptAsideDeletion deletion = RememberedKeyFileSupport
			.deleteKeptAsideCopies(listed, directory, path -> {
				if (path.equals(signinCopy.toPath()))
				{
					throw new AccessDeniedException(path.toString());
				}
				Files.delete(path);
			});

		assertEquals(List.of(keePassCopy), deletion.deleted());
		assertEquals(1, deletion.notDeleted().size());
		RememberedKeyFileSupport.NotDeleted refused = deletion.notDeleted().getFirst();
		assertEquals(signinCopy, refused.file());
		assertEquals(RememberedKeyFileSupport.NotDeleted.Reason.NOT_DELETABLE, refused.reason());
		assertTrue(refused.detail().contains("AccessDeniedException"), refused.detail());
		assertTrue(signinCopy.exists(), "the copy that could not be deleted is gone anyway");
	}
}

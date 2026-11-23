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
package io.github.astrapi69.mystic.crypt.app.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.keepass.MemoizedKeePassModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.RememberedKeyFileSupport;

/**
 * The copies the file store keeps aside are the ones the notice about the setting "Remember key
 * files" lists (#997): the scan derives the naming from the file store, not from a copy of it. The
 * copy is made through the store's own call that sets a file aside, so a change of the naming there
 * fails here.
 */
class KeptAsideCopiesTest
{

	@Test
	@DisplayName("the copy the file store keeps aside of each remembered file is found, another file's copy is not")
	void keptAsideCopies_findsWhatTheFileStoreSetsAside(@TempDir final File directory)
		throws Exception
	{
		File signin = new File(directory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		File keePass = new File(directory, MemoizedKeePassModelBean.JSON_FILENAME);
		File settings = new File(directory, "settings.json");
		for (File file : List.of(signin, keePass, settings))
		{
			Files.writeString(file.toPath(), "{ not json");
		}

		File signinCopy = JsonFileStore.setAside(signin, Clock.systemDefaultZone());
		File keePassCopy = JsonFileStore.setAside(keePass, Clock.systemDefaultZone());
		JsonFileStore.setAside(settings, Clock.systemDefaultZone());

		List<File> found = RememberedKeyFileSupport.keptAsideCopies(directory);
		assertEquals(2, found.size(), "only the two remembered files' copies: " + found);
		assertTrue(found.contains(signinCopy) && found.contains(keePassCopy));
	}
}

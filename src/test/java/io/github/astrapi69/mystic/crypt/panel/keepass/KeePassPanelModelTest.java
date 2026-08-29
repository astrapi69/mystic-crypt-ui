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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the two forms start with when they remember where they were last pointed.
 * <p>
 * The two directions are not the same rule, which is why they are two methods: a source that has
 * been deleted since must not be offered again, because reading it would fail at once, while a
 * destination is where the file is going to be written and does not have to exist yet.
 */
class KeePassPanelModelTest
{

	@TempDir
	File directory;

	private File anExistingFile(final String name) throws Exception
	{
		File file = new File(directory, name);
		Files.writeString(file.toPath(), "not empty");
		return file;
	}

	@Test
	@DisplayName("a remembered source that is still there is offered again")
	void aRememberedSourceThatIsStillThereIsOfferedAgain() throws Exception
	{
		File keePassFile = anExistingFile("remembered.kdbx");

		KeePassPanelModel model = KeePassPanelModel.rememberingSource(keePassFile.getAbsolutePath(),
			null);

		assertEquals(keePassFile, model.getFile());
	}

	@Test
	@DisplayName("a remembered source that has been deleted is not offered again")
	void aRememberedSourceThatHasBeenDeletedIsNotOfferedAgain()
	{
		KeePassPanelModel model = KeePassPanelModel
			.rememberingSource(new File(directory, "gone.kdbx").getAbsolutePath(), null);

		assertEquals(null, model.getFile(),
			"a file that is no longer there was offered as the one to import from");
	}

	@Test
	@DisplayName("a remembered destination is taken even though nothing is there yet")
	void aRememberedDestinationIsTakenEvenThoughNothingIsThereYet()
	{
		File destination = new File(directory, "not-written-yet.kdbx");

		KeePassPanelModel model = KeePassPanelModel
			.rememberingDestination(destination.getAbsolutePath(), null);

		assertEquals(destination, model.getFile(),
			"the export forgot where it last wrote, because nothing is there yet");
	}

	@Test
	@DisplayName("a remembered key file that is still there switches the key file on")
	void aRememberedKeyFileThatIsStillThereSwitchesTheKeyFileOn() throws Exception
	{
		File keyFile = anExistingFile("remembered.key");

		KeePassPanelModel model = KeePassPanelModel.rememberingSource(null,
			keyFile.getAbsolutePath());

		assertTrue(model.isUseKeyFile(), "the key file was taken over but left switched off");
		assertEquals(keyFile, model.getKeyFile());
	}

	@Test
	@DisplayName("a remembered key file that has been deleted leaves the key file off")
	void aRememberedKeyFileThatHasBeenDeletedLeavesTheKeyFileOff()
	{
		KeePassPanelModel model = KeePassPanelModel.rememberingDestination(null,
			new File(directory, "gone.key").getAbsolutePath());

		assertFalse(model.isUseKeyFile(),
			"the form switched the key file on for a file that is not there");
		assertEquals(null, model.getKeyFile());
	}

	@Test
	@DisplayName("nothing remembered leaves the form empty in both directions")
	void nothingRememberedLeavesTheFormEmptyInBothDirections()
	{
		for (KeePassPanelModel model : new KeePassPanelModel[] {
				KeePassPanelModel.rememberingSource(null, null),
				KeePassPanelModel.rememberingDestination("   ", null) })
		{
			assertEquals(null, model.getFile());
			assertEquals(null, model.getKeyFile());
			assertFalse(model.isUseKeyFile());
			assertEquals(0, model.getPassword().length);
		}
	}

}

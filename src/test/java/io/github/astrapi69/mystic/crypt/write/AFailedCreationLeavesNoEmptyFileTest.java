/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.write;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * A first write of a new database that fails leaves no empty file behind, and never touches a file
 * with content (#944).
 * <p>
 * The store worker creates the database file before it reads the key, so a key file that went away
 * left a file with 0 bytes where the database was to be: the next sign-in offered it as one.
 */
class AFailedCreationLeavesNoEmptyFileTest
{

	@TempDir
	File directory;

	private final List<RuntimeException> handedOver = new ArrayList<>();

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static Stream<Arguments> targetsWithNothingToLose()
	{
		return Stream.of(Arguments.argumentSet("a file that is not there yet", false),
			Arguments.argumentSet("an empty file the chooser made", true));
	}

	@ParameterizedTest(name = "{argumentSetName}")
	@MethodSource("targetsWithNothingToLose")
	@DisplayName("a failed first write leaves no empty file and hands the reason over")
	void aFailedFirstWriteLeavesNoEmptyFile(final boolean thereBefore) throws Exception
	{
		File target = new File(directory, "new.mcrdb");
		if (thereBefore)
		{
			Files.createFile(target.toPath());
		}
		File goneKeyFile = new File(directory, "gone.pem");

		boolean written = GuardedCreate.createOrHandOver(aKeyFileDatabase(target, goneKeyFile),
			handedOver::add);

		assertFalse(written, "nothing was written, and that is what OK has to hear");
		assertFalse(target.exists(), "no empty file is left where the database was to be");
		assertEquals(1, handedOver.size(), "the failure is handed over once, to be shown");
		assertTrue(GuardedSave.reasonOf(handedOver.get(0)).contains(goneKeyFile.getName()),
			"and its reason names the key file that is not there: "
				+ GuardedSave.reasonOf(handedOver.get(0)));
	}

	@Test
	@DisplayName("a failed first write onto a file with content leaves that file byte for byte")
	void aFileWithContentIsNeverRemoved() throws Exception
	{
		File target = new File(directory, "already-there.mcrdb");
		GuardedCreate.createOrHandOver(aPasswordDatabase(target), handedOver::add);
		byte[] before = Files.readAllBytes(target.toPath());

		boolean written = GuardedCreate.createOrHandOver(
			aKeyFileDatabase(target, new File(directory, "gone.pem")), handedOver::add);

		assertFalse(written);
		assertArrayEquals(before, Files.readAllBytes(target.toPath()),
			"an empty file holds nothing and may go; a file with content stays as it was");
		assertEquals(1, handedOver.size());
	}

	@Test
	@DisplayName("a database file that cannot be created is reported, and nothing is made for it")
	void aFileThatCannotBeCreated() throws Exception
	{
		File notADirectory = new File(directory, "not-a-directory");
		Files.writeString(notADirectory.toPath(), "a file, so nothing can be created beneath it");
		File target = new File(notADirectory, "new.mcrdb");

		boolean written = GuardedCreate.createOrHandOver(aPasswordDatabase(target),
			handedOver::add);

		assertFalse(written);
		assertFalse(target.exists());
		assertEquals(1, handedOver.size(), "the failure reaches the user instead of the console");
	}

	@Test
	@DisplayName("a first write that works writes the database and hands nothing over")
	void aFirstWriteThatWorks() throws Exception
	{
		File target = new File(directory, "new.mcrdb");

		boolean written = GuardedCreate.createOrHandOver(aPasswordDatabase(target),
			handedOver::add);

		assertTrue(written);
		assertTrue(target.length() > 0, "the database is on disk");
		assertTrue(handedOver.isEmpty(), "and there is nothing to report");
	}

	private static MasterPwFileModelBean aPasswordDatabase(final File target)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(target))
			.selectedApplicationFilePath(target.getAbsolutePath())
			.masterPw(TestPasswords.throwaway().toCharArray()).withMasterPw(true).withKeyFile(false)
			.minPasswordLength(6).build();
	}

	private static MasterPwFileModelBean aKeyFileDatabase(final File target, final File keyFile)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(target))
			.selectedApplicationFilePath(target.getAbsolutePath())
			.masterPw(TestPasswords.throwaway().toCharArray()).withMasterPw(true).withKeyFile(true)
			.keyFileInfo(FileInfo.toFileInfo(keyFile))
			.selectedKeyFilePath(keyFile.getAbsolutePath()).minPasswordLength(6).build();
	}
}

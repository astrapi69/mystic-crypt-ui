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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.List;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.file.create.DirectoryFactory;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The application's own file paths below a directory whose name holds a dot, as
 * {@code ~/.config/mystic-crypt-ui} and {@code ~/.cache} are (#720): the vault is written and read
 * where the user put it, through {@link FileInfo#toFileInfo(File)}, {@code FileFactory} and
 * {@code ReadFileExtensions}, and the plugins directory is created inside the configuration
 * directory, as {@code MysticCryptApplicationFrame} does it. A dot in a directory name was once
 * taken for an extension in silly-io (astrapi69/silly-io#9, astrapi69/file-worker#37)
 */
class VaultBelowADottedDirectoryTest
{

	private static final char[] MASTER_PASSWORD = TestPasswords.throwawayChars();

	@TempDir
	File tempDir;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@ParameterizedTest(name = "{0}/{1}")
	@CsvSource({ ".config/mystic-crypt-ui, vault.mcrdb", ".config/mystic-crypt-ui, vault",
			".cache/x.y, first.db.mcrdb", "a.b/c, backup" })
	void aVault_isWrittenAndReadInItsOwnDirectory_belowADottedDirectory(final String directory,
		final String name) throws Exception
	{
		File parent = new File(tempDir, directory);
		Files.createDirectories(parent.toPath());
		File applicationFile = new File(parent, name);
		Files.createFile(applicationFile.toPath());
		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(applicationFile))
			.selectedApplicationFilePath(applicationFile.getAbsolutePath())
			.masterPw(MASTER_PASSWORD.clone()).withMasterPw(true).withKeyFile(false)
			.minPasswordLength(6).build();

		File savedFile = ApplicationXmlFileFactory
			.newApplicationFileWithPassword(masterPwFileModelBean);
		ApplicationModelBean readBack = ApplicationXmlFileReader
			.getApplicationModelBean(applicationFile, MASTER_PASSWORD.clone());

		assertEquals(applicationFile.getAbsolutePath(), savedFile.getAbsolutePath());
		assertTrue(applicationFile.length() > 0, "the vault was written in its own directory");
		assertEquals(applicationFile.getAbsolutePath(),
			readBack.getMasterPwFileModelBean().getSelectedApplicationFilePath());
		// the save replaced the empty file the test created, so its backup lies beside the vault
		// (#847) - and belongs in the same dotted directory, which is this test's property
		assertEquals(List.of(name, name + VaultFileWriter.BACKUP_SUFFIX), namesIn(parent.toPath()),
			"nothing else in its directory");
		assertEquals(List.of(directory.substring(0, directory.indexOf('/'))),
			namesIn(tempDir.toPath()), "nothing written above it");
	}

	@Test
	void thePluginsDirectory_isCreatedInsideTheConfigurationDirectory_belowADottedDirectory()
		throws Exception
	{
		File configurationDirectory = new File(tempDir, ".config/mystic-crypt-ui");
		Files.createDirectories(configurationDirectory.toPath());

		File plugins = DirectoryFactory.newDirectory(configurationDirectory, "plugins");

		assertEquals(new File(configurationDirectory, "plugins"), plugins);
		assertTrue(plugins.isDirectory());
		assertEquals(List.of("plugins"), namesIn(configurationDirectory.toPath()));
	}

	private static List<String> namesIn(final Path directory) throws Exception
	{
		try (Stream<Path> entries = Files.list(directory))
		{
			return entries.map(path -> path.getFileName().toString()).sorted().toList();
		}
	}
}

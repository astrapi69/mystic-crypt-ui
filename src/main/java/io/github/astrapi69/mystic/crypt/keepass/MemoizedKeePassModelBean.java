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
package io.github.astrapi69.mystic.crypt.keepass;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import io.github.astrapi69.mystic.crypt.app.file.JsonFileStore;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Remembers the last file paths used in the KeePass import/export dialogs, across app restarts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoizedKeePassModelBean
{
	public static final String JSON_FILENAME = "memoizedKeePass.json";

	String lastImportFilePath;
	String lastImportKeyFilePath;
	String lastExportFilePath;
	String lastExportKeyFilePath;

	/**
	 * Loads the memoized bean from the given configuration directory, or a fresh, empty bean if
	 * none exists yet or it could not be read
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return the loaded, or a fresh, {@link MemoizedKeePassModelBean}
	 */
	public static MemoizedKeePassModelBean load(File configurationDirectory)
	{
		return JsonFileStore
			.read(new File(configurationDirectory, JSON_FILENAME), MemoizedKeePassModelBean.class)
			.orElseGet(() -> MemoizedKeePassModelBean.builder().build());
	}

	/**
	 * Saves this bean into the given configuration directory. Failures are swallowed - remembering
	 * the last used file is a convenience only, not worth failing the calling import/export for.
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 */
	public void save(File configurationDirectory)
	{
		File file = new File(configurationDirectory, JSON_FILENAME);
		MemoizedKeePassModelBean toWrite = MysticCryptSettings.load(configurationDirectory)
			.isRememberKeyFiles() ? this : withoutKeyFilePaths();
		try
		{
			// a file that could not be read is kept aside first, not written over (#1082)
			JsonFileStore.write(toWrite, file, MemoizedKeePassModelBean.class);
		}
		catch (Exception exception)
		{
			// ignore
		}
	}

	/**
	 * A copy without the two key file paths, the paths of the import and export files stay: the
	 * setting "Remember key files" covers where a key file lies, not which file was imported (#997)
	 *
	 * @return the copy, this bean is not changed
	 */
	public MemoizedKeePassModelBean withoutKeyFilePaths()
	{
		return MemoizedKeePassModelBean.builder().lastImportFilePath(lastImportFilePath)
			.lastExportFilePath(lastExportFilePath).build();
	}

	/**
	 * Removes the two key file paths from the stored file, at once, when the setting "Remember key
	 * files" is switched off (#997). A file that is not there is left alone, one that is there and
	 * cannot be read may hold paths this code cannot name, so it is reported like a file that
	 * cannot be written.
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return true if the file was rewritten without the key file paths, false if there is none
	 * @throws IOException
	 *             if the file cannot be read or written
	 */
	public static boolean forgetKeyFiles(File configurationDirectory) throws IOException
	{
		File file = new File(configurationDirectory, JSON_FILENAME);
		Optional<MemoizedKeePassModelBean> stored = JsonFileStore.read(file,
			MemoizedKeePassModelBean.class);
		if (stored.isEmpty())
		{
			if (file.isFile())
			{
				throw new IOException("the remembered KeePass paths " + file
					+ " cannot be read, so the key file paths in it cannot be removed");
			}
			return false;
		}
		JsonFileStore.write(stored.get().withoutKeyFilePaths(), file,
			MemoizedKeePassModelBean.class);
		return true;
	}
}

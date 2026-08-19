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

import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.gson.ObjectToJsonFileExtensions;
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
		File file = new File(configurationDirectory, JSON_FILENAME);
		if (file.exists())
		{
			try
			{
				String json = ReadFileExtensions.fromFile(file);
				return JsonStringToObjectExtensions.toObject(json, MemoizedKeePassModelBean.class);
			}
			catch (Exception exception)
			{
				// ignore, fall through to a fresh, empty memoized bean
			}
		}
		return MemoizedKeePassModelBean.builder().build();
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
		try
		{
			ObjectToJsonFileExtensions.toJsonFile(this, file);
		}
		catch (Exception exception)
		{
			// ignore
		}
	}
}

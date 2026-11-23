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

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.app.file.JsonFileStore;

/**
 * Which key file a remembered sign-in preselects, for the database signed in to last and for each
 * database the dialog is asked to open
 */
public final class RememberedKeyFileSupport
{

	private RememberedKeyFileSupport()
	{
	}

	/**
	 * The key file a remembered path names, if there is one.
	 * <p>
	 * A blank path is no path, and only a file is a key file. Both are rules here, not accidents:
	 * the check used to be {@code new File(path).exists()} alone, and it refused the empty path
	 * only because that did not exist. Since JDK 24 the empty abstract pathname is the current
	 * directory, which exists, so a remembered {@code ""} was taken for a key file and the
	 * application ended before the sign-in dialog appeared (#499)
	 *
	 * @param rememberedPath
	 *            the key file path the last sign-in remembered, may be null
	 * @return the key file to preselect, or empty when the path names none
	 */
	public static Optional<FileInfo> keyFileOf(final String rememberedPath)
	{
		if (rememberedPath == null || rememberedPath.isBlank())
		{
			return Optional.empty();
		}
		File keyFile = new File(rememberedPath);
		if (!keyFile.isFile())
		{
			return Optional.empty();
		}
		return Optional.of(FileInfo.toFileInfo(keyFile));
	}

	private static Path normalized(final String databasePath)
	{
		if (databasePath == null || databasePath.isBlank())
		{
			return null;
		}
		try
		{
			return Path.of(databasePath).toAbsolutePath().normalize();
		}
		catch (InvalidPathException notAPath)
		{
			return null;
		}
	}

	/**
	 * What the remembered sign-in knows about the given database: the key file and the factors it
	 * was opened with. A key file that is no longer there is not offered, so the entry comes back
	 * with an empty key file path and the same factors, and the user chooses a key file anew.
	 *
	 * @param choicesByDatabase
	 *            the remembered choices by database path, may be null
	 * @param databasePath
	 *            the database that was chosen in the dialog, may be null
	 * @return the choice, or empty when the database has no entry
	 */
	public static Optional<DatabaseSignInChoice> choiceFor(
		final Map<String, DatabaseSignInChoice> choicesByDatabase, final String databasePath)
	{
		Path wanted = normalized(databasePath);
		if (wanted == null || choicesByDatabase == null)
		{
			return Optional.empty();
		}
		for (Map.Entry<String, DatabaseSignInChoice> entry : choicesByDatabase.entrySet())
		{
			if (entry.getValue() != null && wanted.equals(normalized(entry.getKey())))
			{
				DatabaseSignInChoice remembered = entry.getValue();
				String keyFilePath = keyFileOf(remembered.getKeyFilePath()).isPresent()
					? remembered.getKeyFilePath()
					: "";
				return Optional.of(remembered.toBuilder().keyFilePath(keyFilePath).build());
			}
		}
		return Optional.empty();
	}

	/**
	 * The remembered choices with the given database's entry set, the entries of the other
	 * databases untouched. An older entry of the same database, written under another spelling of
	 * its path, is replaced.
	 *
	 * @param choicesByDatabase
	 *            the choices remembered so far, may be null
	 * @param databasePath
	 *            the database that was signed in to
	 * @param choice
	 *            what the sign-in used
	 * @return a new map, the input is not changed
	 */
	public static Map<String, DatabaseSignInChoice> remember(
		final Map<String, DatabaseSignInChoice> choicesByDatabase, final String databasePath,
		final DatabaseSignInChoice choice)
	{
		Map<String, DatabaseSignInChoice> remembered = new HashMap<>();
		Path signedInTo = normalized(databasePath);
		if (choicesByDatabase != null)
		{
			choicesByDatabase.forEach((path, existing) -> {
				if (existing != null && !Objects.equals(signedInTo, normalized(path)))
				{
					remembered.put(path, existing);
				}
			});
		}
		if (signedInTo != null)
		{
			remembered.put(signedInTo.toString(), choice);
		}
		return remembered;
	}

	/**
	 * Removes every key file path from the remembered sign-in file, at once, when the setting that
	 * remembers key files is switched off (#997). A file that is not there is left alone. One that
	 * is there and cannot be read may hold paths this code cannot name, so it is reported like a
	 * file that cannot be written: the next write would keep it aside with the paths inside.
	 *
	 * @param memoizedSigninFile
	 *            the {@code memoizedSignin.json} of the configuration directory
	 * @return true if the file was rewritten without its key file paths, false if there is none
	 * @throws java.io.IOException
	 *             if the file cannot be read or written, so the caller can tell the user that key
	 *             file paths are still on disk
	 */
	public static boolean forgetKeyFiles(final File memoizedSigninFile) throws java.io.IOException
	{
		Optional<MemoizedSigninModelBean> remembered = JsonFileStore.read(memoizedSigninFile,
			MemoizedSigninModelBean.class);
		if (remembered.isEmpty())
		{
			if (memoizedSigninFile.isFile())
			{
				throw new java.io.IOException(memoizedSigninFile + ": the file cannot be read");
			}
			return false;
		}
		try
		{
			JsonFileStore.write(remembered.get().withoutKeyFilePaths(), memoizedSigninFile,
				MemoizedSigninModelBean.class);
		}
		catch (final java.io.IOException notWritable)
		{
			throw new java.io.IOException(memoizedSigninFile + ": the file cannot be written ("
				+ notWritable.getMessage() + ")", notWritable);
		}
		return true;
	}

	/**
	 * The copies of the remembered files that were kept aside because they could not be read
	 * ({@code memoizedSignin.json.unreadable-*} and {@code memoizedKeePass.json.unreadable-*}).
	 * They keep the paths that were in them, and the setting "Remember key files" does not change
	 * them, so the user is told where they are (#997).
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return the copies, sorted by path, empty when there are none
	 */
	public static java.util.List<File> keptAsideCopies(final File configurationDirectory)
	{
		File[] files = configurationDirectory.listFiles();
		if (files == null)
		{
			return java.util.List.of();
		}
		java.util.List<File> copies = new java.util.ArrayList<>();
		for (File file : files)
		{
			String name = file.getName();
			if (file.isFile() && (name.startsWith("memoizedSignin.json.unreadable-")
				|| name.startsWith("memoizedKeePass.json.unreadable-")))
			{
				copies.add(file);
			}
		}
		copies.sort(java.util.Comparator.comparing(File::getPath));
		return copies;
	}
}

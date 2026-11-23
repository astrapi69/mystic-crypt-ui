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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.gson.ObjectToJsonExtensions;

/**
 * Reads and writes the JSON files the application keeps beside its database: the settings, the
 * remembered sign-in and the KeePass memo (#1082).
 * <p>
 * A file that cannot be read, for whatever reason (cut short by a crash, edited by hand, written by
 * a release that knows other fields), reads as nothing, and the caller goes on with its defaults.
 * Writing those defaults over the file would destroy what the user had in it, so before a file that
 * cannot be read is written anew, it is kept beside it as
 * {@code <name>.unreadable-<yyyyMMdd'T'HHmmss>} and the user is told through
 * {@link UnreadableFileNotices}. A file is written to a sibling first and moved over the old one in
 * one step, so a reader at the same moment, a watchdog that reads the settings, sees the old file
 * or the new one and never half of it.
 */
public final class JsonFileStore
{

	/** The time in the name of a file that was set aside */
	static final DateTimeFormatter SET_ASIDE_TIME = DateTimeFormatter
		.ofPattern("yyyyMMdd'T'HHmmss");

	/** What a file's name is followed by when it could not be read and was kept aside */
	static final String SET_ASIDE_MARKER = ".unreadable-";

	private JsonFileStore()
	{
	}

	/**
	 * The object the given file holds, or nothing if the file is not there or cannot be read
	 *
	 * @param <T>
	 *            the type of the object
	 * @param file
	 *            the JSON file
	 * @param type
	 *            the class of the object
	 * @return the object, or empty
	 */
	public static <T> Optional<T> read(final File file, final Class<T> type)
	{
		if (!file.isFile())
		{
			return Optional.empty();
		}
		try
		{
			return Optional.ofNullable(
				JsonStringToObjectExtensions.toObject(TextFiles.readUtf8OrLatin1(file), type));
		}
		catch (IOException | RuntimeException unreadable)
		{
			return Optional.empty();
		}
	}

	/**
	 * Writes the given object to the file as JSON; a file there that cannot be read is set aside
	 * first and the user is told
	 *
	 * @param <T>
	 *            the type of the object
	 * @param value
	 *            the object to write
	 * @param file
	 *            the JSON file
	 * @param type
	 *            the class of the object, the one the file is read back with
	 * @throws IOException
	 *             if the file cannot be set aside or written
	 */
	public static <T> void write(final T value, final File file, final Class<T> type)
		throws IOException
	{
		write(value, file, type, Clock.systemDefaultZone());
	}

	static <T> void write(final T value, final File file, final Class<T> type, final Clock clock)
		throws IOException
	{
		if (file.isFile() && read(file, type).isEmpty())
		{
			File keptAs = setAside(file, clock);
			UnreadableFileNotices.publish(new UnreadableFileNotices.SetAside(file, keptAs));
		}
		writeInOneStep(file, ObjectToJsonExtensions.toJson(value));
	}

	/**
	 * Moves the file to a sibling whose name says that it could not be read and when
	 *
	 * @param file
	 *            the file that could not be read
	 * @param clock
	 *            the clock that names the time
	 * @return where the file lies now
	 * @throws IOException
	 *             if it cannot be moved
	 */
	/**
	 * Whether the file is a copy that was kept aside because the named file could not be read
	 *
	 * @param file
	 *            a file of the directory
	 * @param originalName
	 *            the name the unreadable file had
	 * @return true if {@link #setAside} made it from a file of that name
	 */
	public static boolean isSetAsideCopyOf(final File file, final String originalName)
	{
		return file.isFile() && file.getName().startsWith(originalName + SET_ASIDE_MARKER);
	}

	static File setAside(final File file, final Clock clock) throws IOException
	{
		String name = file.getName() + SET_ASIDE_MARKER
			+ LocalDateTime.now(clock).format(SET_ASIDE_TIME);
		File keptAs = new File(file.getParentFile(), name);
		for (int number = 2; keptAs.exists(); number++)
		{
			keptAs = new File(file.getParentFile(), name + "-" + number);
		}
		Files.move(file.toPath(), keptAs.toPath());
		return keptAs;
	}

	private static void writeInOneStep(final File file, final String json) throws IOException
	{
		Path target = file.toPath();
		Files.createDirectories(target.toAbsolutePath().getParent());
		Path written = target.resolveSibling(file.getName() + ".writing");
		Files.writeString(written, json, StandardCharsets.UTF_8);
		try
		{
			Files.move(written, target, StandardCopyOption.ATOMIC_MOVE,
				StandardCopyOption.REPLACE_EXISTING);
		}
		catch (AtomicMoveNotSupportedException notAtomic)
		{
			Files.move(written, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}

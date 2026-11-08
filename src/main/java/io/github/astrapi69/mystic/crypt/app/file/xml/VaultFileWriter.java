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

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Writes the database file so that a save which cannot finish does not take the previous database
 * with it.
 * <p>
 * Writing straight to the file empties it first and fills it afterwards. A full disk, a quota, a
 * stick pulled out or a power cut in between leaves neither the old database nor the new one: the
 * file that remains is half of something. For a password manager that is the whole of the data.
 * <p>
 * So the bytes go to a file next to it, are flushed to the disk rather than left in a cache, and
 * are then moved onto the database in one step. A failure before the move leaves the previous
 * database exactly as it was, and the move itself either happened or did not.
 */
public final class VaultFileWriter
{

	/** What the half written file is called while it is being written */
	static final String WORK_IN_PROGRESS_SUFFIX = ".part";

	/** What the kept previous database is called after a save (#847) */
	static final String BACKUP_SUFFIX = ".bak";

	private VaultFileWriter()
	{
	}

	/**
	 * Writes the content to the file, leaving the previous content in place if it cannot be written
	 * in full
	 *
	 * @param file
	 *            the database file
	 * @param content
	 *            the encrypted bytes to write
	 * @throws IOException
	 *             if the content could not be written or could not be moved into place
	 */
	public static void write(final File file, final byte[] content) throws IOException
	{
		Path target = file.toPath();
		// next to the database rather than in a temporary directory: a move is only atomic within
		// one file system, and a temporary directory is often on another one
		Path workInProgress = target.resolveSibling(file.getName() + WORK_IN_PROGRESS_SUFFIX);
		try
		{
			toDisk(workInProgress, content);
			keepThePrevious(target);
			moveOntoTheDatabase(workInProgress, target);
		}
		catch (IOException | RuntimeException failed)
		{
			discard(workInProgress, failed);
			throw failed;
		}
	}

	/**
	 * Keeps the previous database as the sibling backup before the move replaces it (#847): when
	 * the new file turns out unreadable later, the previous state is still there. A copy rather
	 * than a move, so the database itself stays in place until the one atomic replacement; the
	 * backup stays until the new file was read back successfully once, which is when
	 * {@link #discardBackup(File)} removes it
	 */
	private static void keepThePrevious(final Path target) throws IOException
	{
		if (Files.exists(target))
		{
			Files.copy(target, backupOf(target), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** The sibling file that keeps the previous database after a save (#847) */
	static Path backupOf(final Path target)
	{
		return target.resolveSibling(target.getFileName() + BACKUP_SUFFIX);
	}

	/**
	 * Removes the backup a save kept, once the database it belongs to was read back successfully: a
	 * read is the proof the new file is good, and a password manager keeps no second copy of the
	 * encrypted data longer than needed (#847). Without a backup there is nothing to do
	 *
	 * @param database
	 *            the database file whose backup is no longer needed
	 * @throws IOException
	 *             if the backup exists and could not be removed
	 */
	static void discardBackup(final File database) throws IOException
	{
		Files.deleteIfExists(backupOf(database.toPath()));
	}

	/**
	 * Writes the bytes and waits until the disk has them. Without this the move can complete while
	 * the content is still in a cache, which a power cut then loses
	 */
	private static void toDisk(final Path workInProgress, final byte[] content) throws IOException
	{
		try (FileChannel channel = FileChannel.open(workInProgress, StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))
		{
			channel.write(java.nio.ByteBuffer.wrap(content));
			channel.force(true);
		}
	}

	private static void moveOntoTheDatabase(final Path workInProgress, final Path target)
		throws IOException
	{
		try
		{
			Files.move(workInProgress, target, StandardCopyOption.ATOMIC_MOVE,
				StandardCopyOption.REPLACE_EXISTING);
		}
		catch (AtomicMoveNotSupportedException notOnThisFileSystem)
		{
			// some file systems cannot promise it; a plain replace is still better than writing
			// through the database itself, because the content is complete before it is moved
			Files.move(workInProgress, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void discard(final Path workInProgress, final Exception reason)
	{
		try
		{
			Files.deleteIfExists(workInProgress);
		}
		catch (IOException couldNotClean)
		{
			// the database is intact, which is what matters; say what was left behind
			reason.addSuppressed(couldNotClean);
		}
	}
}

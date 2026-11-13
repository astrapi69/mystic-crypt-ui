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

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import lombok.extern.java.Log;

/**
 * Writes a new database and, when that fails, says so (#944).
 * <p>
 * The first write of a new database did not go through {@link GuardedSave}: its exception left OK
 * on the event thread and reached standard error only, while "Create your master key" stayed open
 * with nothing on screen. The file the database was to be written to was left with 0 bytes - the
 * store worker creates it before it reads the key - and the next sign-in offered it as a database.
 */
@Log
public final class GuardedCreate
{

	private GuardedCreate()
	{
	}

	/**
	 * Writes the new database and reports a failure to the user, naming the file and the reason
	 *
	 * @param parent
	 *            the component the message belongs to
	 * @param masterPwFileModelBean
	 *            the new database's file and master key
	 * @return true when the database was written, false when it was not and the user has been told
	 */
	public static boolean createOrTell(final Component parent,
		final MasterPwFileModelBean masterPwFileModelBean)
	{
		return createOrHandOver(masterPwFileModelBean,
			failure -> tellThatTheCreationFailed(parent, masterPwFileModelBean, failure));
	}

	/**
	 * Writes the new database; when that fails, removes the file it was to be written to if that
	 * file is empty, and hands the failure over. An empty file holds no database, so removing it
	 * loses nothing; a file with content is never removed, whatever put it there
	 *
	 * @param masterPwFileModelBean
	 *            the new database's file and master key
	 * @param whenFailed
	 *            receives the failure, after the empty file is gone
	 * @return true when the database was written
	 */
	static boolean createOrHandOver(final MasterPwFileModelBean masterPwFileModelBean,
		final Consumer<RuntimeException> whenFailed)
	{
		try
		{
			ApplicationXmlFileFactory.newApplicationFile(masterPwFileModelBean);
			return true;
		}
		catch (RuntimeException failure)
		{
			removeWhenEmpty(masterPwFileModelBean, failure);
			log.log(Level.SEVERE, "the new database could not be written", failure);
			whenFailed.accept(failure);
			return false;
		}
	}

	/**
	 * Removes the file the database was to be written to when it is there and empty. A file that
	 * cannot be removed stays, and the reason travels with the failure rather than being lost
	 */
	private static void removeWhenEmpty(final MasterPwFileModelBean masterPwFileModelBean,
		final RuntimeException failure)
	{
		if (masterPwFileModelBean.getApplicationFileInfo() == null)
		{
			return;
		}
		File target = masterPwFileModelBean.getApplicationFileInfo().toFile();
		if (!target.isFile() || target.length() != 0)
		{
			return;
		}
		try
		{
			Files.deleteIfExists(target.toPath());
		}
		catch (IOException couldNotRemove)
		{
			failure.addSuppressed(couldNotRemove);
		}
	}

	private static void tellThatTheCreationFailed(final Component parent,
		final MasterPwFileModelBean masterPwFileModelBean, final RuntimeException failure)
	{
		String message = String.format(
			Messages.getString("dialog.create.failed.message",
				"<html><body><div>The database could not be created.</div>"
					+ "<div>File: %1$s</div><div>Reason: %2$s</div>"
					+ "<div>Nothing was written. Correct the input and press OK again, or cancel."
					+ "</div></body></html>"),
			GuardedSave.pathOf(masterPwFileModelBean), GuardedSave.reasonOf(failure));
		JOptionPane.showMessageDialog(parent, message,
			Messages.getString("dialog.create.failed.title", "The database could not be created"),
			JOptionPane.ERROR_MESSAGE);
	}
}

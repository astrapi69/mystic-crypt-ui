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
package io.github.astrapi69.mystic.crypt.write;

import java.awt.Component;
import java.io.File;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import lombok.extern.java.Log;

/**
 * Writes the open vault and, when that fails, says so (#424).
 * <p>
 * Every way of saving goes through here: the menu item, Save As, and the Yes of the question before
 * closing. A failed write used to reach the user as nothing at all - the exception left the action
 * on the event thread, Swing printed it to standard error, and the vault was marked saved on the
 * way in. What a user saw was a save that had happened. What was on disk was the previous version,
 * and the next ending discarded the rest without asking.
 */
@Log
public final class GuardedSave
{

	private GuardedSave()
	{
	}

	/**
	 * Writes the vault and reports a failure to the user, naming the file and the reason
	 *
	 * @param parent
	 *            the component the message belongs to
	 * @param applicationModelBean
	 *            the open vault
	 * @return true when the vault was written, false when it was not and the user has been told
	 */
	public static boolean writeOrTell(final Component parent,
		final ApplicationModelBean applicationModelBean)
	{
		try
		{
			ApplicationXmlFileStoreWorker.storeApplicationFile(applicationModelBean);
			return true;
		}
		catch (RuntimeException exception)
		{
			log.log(Level.SEVERE, "the vault could not be written", exception);
			tellThatTheWriteFailed(parent, applicationModelBean, exception);
			return false;
		}
	}

	/**
	 * The reason, so that the user can act on it: a full disk, a directory somebody else owns and a
	 * share that went away are three different problems, and "Saving failed" tells them apart for
	 * none of them
	 */
	private static void tellThatTheWriteFailed(final Component parent,
		final ApplicationModelBean applicationModelBean, final RuntimeException exception)
	{
		String message = String.format(
			Messages.getString("dialog.save.failed.message",
				"<html><body><div>The database could not be saved.</div>"
					+ "<div>File: %1$s</div><div>Reason: %2$s</div>"
					+ "<div>Your changes are still open and still unsaved.</div></body></html>"),
			whereItWouldHaveGone(applicationModelBean), reasonOf(exception));
		JOptionPane.showMessageDialog(parent, message,
			Messages.getString("dialog.save.failed.title", "The database could not be saved"),
			JOptionPane.ERROR_MESSAGE);
	}

	/** The file the write was aimed at, or a placeholder when the vault has no file yet */
	private static String whereItWouldHaveGone(final ApplicationModelBean applicationModelBean)
	{
		MasterPwFileModelBean masterPwFileModelBean = applicationModelBean == null
			? null
			: applicationModelBean.getMasterPwFileModelBean();
		if (masterPwFileModelBean == null || masterPwFileModelBean.getApplicationFileInfo() == null)
		{
			return "no file is set for this database";
		}
		File applicationFile = FileFactory
			.newFileQuietly(masterPwFileModelBean.getApplicationFileInfo());
		return applicationFile == null
			? "no file is set for this database"
			: applicationFile.getAbsolutePath();
	}

	/**
	 * The innermost message, because the decorated ones on top of it say what wrapped the failure
	 * rather than what it was
	 */
	private static String reasonOf(final RuntimeException exception)
	{
		Throwable cause = exception;
		while (cause.getCause() != null)
		{
			cause = cause.getCause();
		}
		String message = cause.getMessage();
		return message == null || message.isBlank()
			? cause.getClass().getSimpleName()
			: cause.getClass().getSimpleName() + ": " + message;
	}
}

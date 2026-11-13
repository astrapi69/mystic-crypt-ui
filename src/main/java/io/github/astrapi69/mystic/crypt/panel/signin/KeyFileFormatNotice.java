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

import java.awt.Component;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.app.file.xml.KeyFileOnDisk;

/**
 * Tells the user that a database with a master password and a key file does not take the chosen key
 * file, because it is not PEM (#943). The OK button of the dialog stays disabled for it on its own
 * ({@code BtnOkStateMachine}); this says why, once per choice.
 * <p>
 * The notice is shown after the event that caused it, not inside it: a modal dialog opened from
 * within the choice would hold up whatever made the choice until it is closed.
 */
final class KeyFileFormatNotice
{

	private final Component parent;

	private String pendingKeyFilePath;

	/**
	 * A notice for the dialog of the given component
	 *
	 * @param parent
	 *            the component the notice belongs to
	 */
	KeyFileFormatNotice(final Component parent)
	{
		this.parent = parent;
	}

	/**
	 * Tells the user when the database the dialog holds has a master password and a key file, and
	 * the chosen key file is one such a database cannot read
	 *
	 * @param modelObject
	 *            what the dialog holds
	 */
	void tellWhenNotTaken(final MasterPwFileModelBean modelObject)
	{
		if (!modelObject.isWithMasterPw() || !modelObject.isWithKeyFile()
			|| modelObject.getKeyFileInfo() == null)
		{
			return;
		}
		File keyFile = modelObject.getKeyFileInfo().toFile();
		if (!keyFile.isFile() || KeyFileOnDisk.takes(keyFile, true))
		{
			return;
		}
		String keyFilePath = keyFile.getAbsolutePath();
		if (keyFilePath.equals(pendingKeyFilePath))
		{
			return;
		}
		pendingKeyFilePath = keyFilePath;
		SwingUtilities.invokeLater(() -> {
			pendingKeyFilePath = null;
			// %1$s rather than {0}: the bundle lookup formats braces itself and fills them with
			// null
			JOptionPane.showMessageDialog(parent,
				String.format(Messages.getString("dialog.key.file.not.pem.message",
					"A database with a master password and a key file takes its key file as PEM. "
						+ "%1$s is not a PEM file: choose a PEM key file."),
					keyFilePath),
				Messages.getString("dialog.key.file.not.pem.title", "Not a PEM key file"),
				JOptionPane.WARNING_MESSAGE);
		});
	}
}

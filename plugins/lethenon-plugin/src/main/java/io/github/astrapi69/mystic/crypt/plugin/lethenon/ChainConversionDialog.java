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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.awt.Component;
import java.nio.file.Path;

import javax.swing.JOptionPane;

/**
 * The question before a chain file of lethenon 0.5.0 or earlier is converted (#581): what happens to
 * the file, what stays, and what older versions can still read. Nothing is converted unless the
 * person chooses to; the default button is the one that leaves the file as it is.
 */
final class ChainConversionDialog
{

	private ChainConversionDialog()
	{
	}

	/**
	 * Asks the person whether to convert the chain file
	 *
	 * @param parent
	 *            the window that wants to read the file, may be null
	 * @param chainFile
	 *            the chain file, whose chain verified
	 * @return whether the person chose to convert it
	 */
	static boolean ask(final Component parent, final Path chainFile)
	{
		String convert = LethenonMessages.getString("lethenon.convert.button.convert", "Convert");
		String cancel = LethenonMessages.getString("lethenon.convert.button.cancel", "Cancel");
		Object[] options = { convert, cancel };
		int chosen = JOptionPane.showOptionDialog(parent, message(chainFile),
			LethenonMessages.getString("lethenon.convert.title", "Convert the chain file?"),
			JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, cancel);
		return chosen == 0;
	}

	/**
	 * The question's text, with the file's path and the names of the file and its legacy copy
	 *
	 * @param chainFile
	 *            the chain file
	 * @return the text
	 */
	static String message(final Path chainFile)
	{
		return LethenonMessages.getString("lethenon.convert.message", "{path} was written by "
			+ "lethenon 0.5.0 or earlier, as mystic-crypt-ui 8.9 and earlier write chain files. "
			+ "lethenon 0.6.0 keeps the chain in a database and reads this file only once it is "
			+ "converted, which happens once:\n\n"
			+ "- the chain in the file was verified, and is converted as it is\n"
			+ "- the file as it is now stays beside it as {legacy}\n"
			+ "- mystic-crypt-ui 8.9 and earlier cannot read {file} after that; they can still "
			+ "read {legacy}\n\n" + "Convert it now?")
			.replace("{path}", chainFile.toAbsolutePath().toString())
			.replace("{file}", String.valueOf(chainFile.getFileName()))
			.replace("{legacy}", String.valueOf(ChainFiles.legacyOf(chainFile).getFileName()));
	}
}

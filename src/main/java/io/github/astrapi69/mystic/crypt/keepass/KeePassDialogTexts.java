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
package io.github.astrapi69.mystic.crypt.keepass;

import io.github.astrapi69.mystic.crypt.Messages;

/**
 * The words of the dialogs that ask for a KeePass file and that report a finished import or export,
 * from the bundle in English and in German. The e2e helpers that find these dialogs by title read
 * the same methods, so a title is written once.
 */
public final class KeePassDialogTexts
{

	private KeePassDialogTexts()
	{
	}

	/**
	 * The title of the dialog that asks for the KeePass file to import
	 *
	 * @return the title
	 */
	public static String importTitle()
	{
		return Messages.getString("keepass.import.dialog.title", "Import from KeePass");
	}

	/**
	 * The message of the dialog that reports a finished import
	 *
	 * @return the message for the person
	 */
	public static String importDone()
	{
		return Messages.getString("keepass.import.done.message",
			"KeePass database imported successfully.");
	}

	/**
	 * The title of the dialog that reports a finished import
	 *
	 * @return the title
	 */
	public static String importDoneTitle()
	{
		return Messages.getString("keepass.import.done.title", "Import successful");
	}

	/**
	 * The title of the dialog that asks for the KeePass file to export to
	 *
	 * @return the title
	 */
	public static String exportTitle()
	{
		return Messages.getString("keepass.export.dialog.title", "Export to KeePass");
	}

	/**
	 * The message of the dialog that reports a finished export of the whole tree
	 *
	 * @return the message for the person
	 */
	public static String exportDone()
	{
		return Messages.getString("keepass.export.done.message",
			"KeePass database exported successfully.");
	}

	/**
	 * The title of the dialog that reports a finished export of the whole tree
	 *
	 * @return the title
	 */
	public static String exportDoneTitle()
	{
		return Messages.getString("keepass.export.done.title", "Export successful");
	}
}

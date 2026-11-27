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
 * What a KeePass export tells a person when it cannot write the file: the export of the whole tree
 * and the export of chosen entries say it the same way, from here, in the bundle's words. The text
 * of the exception is never shown (#1005)
 */
public final class KeePassExportRefusal
{

	private KeePassExportRefusal()
	{
	}

	/**
	 * The title of the dialog that reports a refused export of the whole tree; the export of chosen
	 * entries keeps the title of its own dialog
	 *
	 * @return the title
	 */
	public static String title()
	{
		return Messages.getString("keepass.export.failed.title", "Export failed");
	}

	/**
	 * The message for a refused export
	 *
	 * @param refusal
	 *            what the export failed with; its text is not shown
	 * @return the message for the person
	 */
	public static String messageFor(final Exception refusal)
	{
		return Messages.getString("keepass.export.failed.message",
			"Could not export the KeePass file.");
	}
}

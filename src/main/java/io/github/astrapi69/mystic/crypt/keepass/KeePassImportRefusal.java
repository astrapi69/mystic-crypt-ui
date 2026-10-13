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

import org.linguafranca.pwdb.format.KdbxCredentials;

import io.github.astrapi69.mystic.crypt.Messages;

/**
 * What the KeePass import tells a person when it cannot import a file.
 * <p>
 * A KDBX 3.1 file with protected attachments gets words of its own (#568): KeePassJava2 3.1.0
 * refuses it with a Jackson exception about an unrecognized field, which says nothing a person can
 * act on. The message says what the file is, and the way out that was measured to work - saved as
 * KDBX 4 in KeePassXC, the file is imported with its attachments
 * ({@code AKdbx31FileSavedAsKdbx4InKeePassXcIsImportedUiTest}). Every other refusal keeps the
 * library's reason, as before.
 */
public final class KeePassImportRefusal
{

	private KeePassImportRefusal()
	{
	}

	/**
	 * The title of the dialog that reports a refused import
	 *
	 * @return the title
	 */
	public static String title()
	{
		return Messages.getString("keepass.import.failed.title", "Import failed");
	}

	/**
	 * The message for a refused import
	 *
	 * @param file
	 *            the KeePass file that was to be imported
	 * @param credentials
	 *            the credentials it was to be opened with, or null when they could not be made
	 * @param refusal
	 *            what the import failed with
	 * @return the message for the person
	 */
	public static String messageFor(final File file, final KdbxCredentials credentials,
		final Exception refusal)
	{
		if (credentials != null && Kdbx31ProtectedAttachments.in(file, credentials))
		{
			return Messages.getString("keepass.import.failed.kdbx31.protected.attachments",
				"This KeePass file is in the KDBX 3.1 format and has protected attachments,\n"
					+ "which the import cannot read yet. Nothing was imported.\n\n"
					+ "Saved as KDBX 4 in KeePassXC, the file can be imported:\n"
					+ "open it in KeePassXC, choose Database > Database Settings... > Security >\n"
					+ "Encryption Settings, set the database format to KDBX 4 (recommended),\n"
					+ "confirm with OK, save the database, and import it again.");
		}
		return Messages.getString("keepass.import.failed.message",
			"Could not import the KeePass file:") + " " + refusal.getMessage();
	}
}

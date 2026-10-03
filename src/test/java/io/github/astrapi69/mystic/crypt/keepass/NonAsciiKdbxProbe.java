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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.jackson.JacksonDatabase;
import org.linguafranca.pwdb.kdbx.jackson.JacksonEntry;

/**
 * Writes a KDBX holding non-ASCII text and reads it back, in THIS process, and says what the
 * process's default encoding was.
 * <p>
 * It is a main class rather than a test because the property it is about, {@code file.encoding}, is
 * read when a JVM starts and cannot be changed afterwards:
 * {@link NonAsciiTextSurvivesANonUtf8JvmTest} starts this one with a non-UTF-8 default and reads
 * its exit code.
 */
public final class NonAsciiKdbxProbe
{

	/** What a user's entry may well contain, and what the library used to lose */
	static final String TITLE = "Grüße aus München";

	/** The same question in several alphabets, because one umlaut is a weak sample */
	static final String NOTES = "é à ü ß 中文";

	private NonAsciiKdbxProbe()
	{
	}

	/**
	 * Runs the round trip
	 *
	 * @param arguments
	 *            none
	 * @throws Exception
	 *             when the database cannot be written or read, which is the defect this guards
	 */
	public static void main(final String[] arguments) throws Exception
	{
		System.out.println("file.encoding=" + System.getProperty("file.encoding"));
		JacksonDatabase database = new JacksonDatabase();
		JacksonEntry entry = database.newEntry();
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_TITLE, TITLE);
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_NOTES, NOTES);
		database.getRootGroup().addEntry(entry);
		KdbxCreds credentials = new KdbxCreds("probe".getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream written = new ByteArrayOutputStream();
		database.save(credentials, written);

		JacksonEntry read = JacksonDatabase
			.load(credentials, new ByteArrayInputStream(written.toByteArray())).getRootGroup()
			.getEntries().get(0);

		if (!TITLE.equals(read.getTitle()) || !NOTES.equals(read.getNotes()))
		{
			System.out.println("MANGLED title=" + read.getTitle() + " notes=" + read.getNotes());
			System.exit(2);
		}
		System.out.println("INTACT");
	}
}

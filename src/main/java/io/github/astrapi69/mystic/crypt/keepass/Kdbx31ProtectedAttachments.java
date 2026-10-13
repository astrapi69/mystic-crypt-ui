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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.format.KdbxHeader;
import org.linguafranca.pwdb.format.KdbxSerializer;

import lombok.extern.java.Log;

/**
 * Whether a KeePass file is KDBX 3.1 with protected attachments: the file KeePassJava2 3.1.0
 * refuses, because its model of {@code Meta/Binaries/Binary} has no {@code Protected} attribute
 * (#568, jorabin/KeePassJava2#114).
 * <p>
 * Asked of the file itself, not of the library's exception text, so that the import can say what
 * the file is when the library refuses it: the version from the outer header, then the decrypted
 * XML up to the end of {@code Meta}, where KDBX 3.1 keeps its attachments. Protected values stay
 * encrypted; only the attribute is read.
 */
@Log
public final class Kdbx31ProtectedAttachments
{

	private static final int KDBX_3 = 3;

	private Kdbx31ProtectedAttachments()
	{
	}

	/**
	 * Whether the given file is KDBX 3.1 and holds an attachment marked protected
	 *
	 * @param file
	 *            the KeePass file
	 * @param credentials
	 *            the credentials it was to be opened with
	 * @return true when it is; false when it is not, or when it cannot be read with these
	 *         credentials - a file that cannot be read is not known to be the case
	 */
	public static boolean in(final File file, final KdbxCredentials credentials)
	{
		try
		{
			return isKdbx31(file) && hasAProtectedAttachment(file, credentials);
		}
		catch (IOException | IllegalStateException | XMLStreamException unreadable)
		{
			log.log(Level.FINE, "not known to hold protected KDBX 3.1 attachments: " + file,
				unreadable);
			return false;
		}
	}

	private static boolean isKdbx31(final File file) throws IOException
	{
		try (InputStream in = new FileInputStream(file))
		{
			return KdbxSerializer.readOuterHeader(in, new KdbxHeader()).getVersion() == KDBX_3;
		}
	}

	private static boolean hasAProtectedAttachment(final File file,
		final KdbxCredentials credentials) throws IOException, XMLStreamException
	{
		try (InputStream in = new FileInputStream(file);
			InputStream xml = KdbxSerializer.createUnencryptedInputStream(credentials,
				new KdbxHeader(), in))
		{
			XMLStreamReader reader = newSafeFactory().createXMLStreamReader(xml);
			try
			{
				return aProtectedBinaryInMeta(reader);
			}
			finally
			{
				reader.close();
			}
		}
	}

	/**
	 * Reads up to the end of {@code Meta}: a {@code Binary} inside {@code Binaries} that says
	 * Protected="True"
	 */
	private static boolean aProtectedBinaryInMeta(final XMLStreamReader reader)
		throws XMLStreamException
	{
		boolean inBinaries = false;
		while (reader.hasNext())
		{
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT)
			{
				String name = reader.getLocalName();
				if ("Binaries".equals(name))
				{
					inBinaries = true;
				}
				else if (inBinaries && "Binary".equals(name)
					&& "True".equalsIgnoreCase(reader.getAttributeValue(null, "Protected")))
				{
					return true;
				}
			}
			else if (event == XMLStreamConstants.END_ELEMENT)
			{
				if ("Binaries".equals(reader.getLocalName()))
				{
					inBinaries = false;
				}
				else if ("Meta".equals(reader.getLocalName()))
				{
					return false;
				}
			}
		}
		return false;
	}

	/** No DTD and no external entities: the XML comes out of a file somebody else wrote */
	private static XMLInputFactory newSafeFactory()
	{
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		return factory;
	}
}

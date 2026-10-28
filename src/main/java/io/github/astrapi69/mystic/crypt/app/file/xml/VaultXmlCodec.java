/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.app.file.xml;

import java.io.CharArrayReader;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.ForbiddenClassException;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.secret.WipingCharWriter;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;
import io.github.astrapi69.xstream.factory.XStreamFactory;

/**
 * The vault between its object form and its xml, with the xml held as characters rather than as a
 * {@link String} (#294).
 * <p>
 * {@code ObjectToXmlExtensions.toXml(model)} returns a String holding every entry's every field in
 * the clear, and {@code XmlToObjectExtensions.toObject(xml)} takes one. On #294 that String was the
 * largest single reason the decrypted database was still lying in the heap after the vault had been
 * closed: it cannot be overwritten, and there was one of it per save and one per load.
 * <p>
 * The xml itself does not change. This goes through the same {@link XStreamFactory} configuration
 * the extensions go through - the same instance shape, the same allowed types - and writes into a
 * {@link java.io.Writer} instead of into a String, which is a difference in where the characters
 * land and in nothing else.
 * <p>
 * <b>Two additions to that configuration, both for the vault format (#402).</b> The version is
 * written as an attribute of the root element, and an element this build does not know is skipped
 * rather than failing the read. Both were measured against the 8.5 release jar: it passes over an
 * unknown attribute and refuses the whole vault for an unknown element, which the sign-in reports
 * as a wrong password. The attribute keeps a vault 8.6 writes readable by 8.5 as long as it holds
 * nothing 8.5 has no field for; the skipping makes sure no later version's addition is ever read as
 * a wrong password again. This is the one place the vault is converted, on all three ways a vault
 * is protected.
 * <p>
 * <b>What is still out of reach.</b> XStream's own writer buffers a chunk of what it writes, and
 * the pull parser reading the xml back buffers the document it is parsing. Neither is reachable
 * from here to be overwritten. What this removes is the full copy that was retained for the whole
 * of every save and load.
 */
public final class VaultXmlCodec
{

	/**
	 * How much room the writer starts with. A vault of a few dozen entries fits in this, so the
	 * usual save does not grow the buffer at all; growing is correct when it happens, just not free
	 */
	private static final int INITIAL_CAPACITY = 16 * 1024;

	/**
	 * The format this build writes. A vault without the attribute was written before it existed, up
	 * to and including 8.5.1; 2 is the first that can carry an entry's history and the names of its
	 * protected properties
	 */
	public static final int FORMAT_VERSION = 2;

	/** The field of the root element that is written as an attribute */
	private static final String FORMAT_VERSION_FIELD = "formatVersion";

	/**
	 * The types of this project's namespace that a vault holds, and the only ones the reader builds
	 * besides XStream's own standard types. Measured over every vault the test suites write and
	 * read, the end-to-end suites with every plugin and the KeePass import among them, and the
	 * vaults the published releases wrote. A type that is not here is refused before it is built.
	 */
	private static final Class<?>[] VAULT_TYPES = { ApplicationModelBean.class,
			MysticCryptEntryModelBean.class, TreeIdNode.class, GenericTreeElement.class,
			KeyValuePair.class, FileContentInfo.class };

	private VaultXmlCodec()
	{
	}

	/**
	 * Serializes the given application model to xml, in the format this build writes - which the
	 * model records, because from here on that is the format it is in
	 *
	 * @param applicationModelBean
	 *            the model
	 * @return the xml, in an array the caller owns and is expected to overwrite when it is done
	 */
	public static char[] toXml(final ApplicationModelBean applicationModelBean)
	{
		if (isNewerThanThisBuild(applicationModelBean))
		{
			throw new IllegalStateException(whyItIsReadOnly(applicationModelBean));
		}
		applicationModelBean.setFormatVersion(FORMAT_VERSION);
		WipingCharWriter writer = new WipingCharWriter(INITIAL_CAPACITY);
		try
		{
			newXStream().toXML(applicationModelBean, writer);
			return writer.toCharArray();
		}
		finally
		{
			writer.wipe();
		}
	}

	/**
	 * Whether the vault was written in a format newer than this build writes. Such a vault is read
	 * with the elements this build does not know skipped, so it is shown and never written: a write
	 * would drop from the file what was skipped while reading it (#402, decided by the maintainer)
	 *
	 * @param applicationModelBean
	 *            the model; null is accepted and is not newer
	 * @return true if the vault has to stay read-only
	 */
	public static boolean isNewerThanThisBuild(final ApplicationModelBean applicationModelBean)
	{
		return applicationModelBean != null && applicationModelBean.getFormatVersion() != null
			&& FORMAT_VERSION < applicationModelBean.getFormatVersion();
	}

	/**
	 * What the user is told when a vault opens read-only, and what a refused write says
	 *
	 * @param applicationModelBean
	 *            the model of a vault in a newer format
	 * @return the reason, naming the format version the vault needs
	 */
	public static String whyItIsReadOnly(final ApplicationModelBean applicationModelBean)
	{
		// %1$s rather than {0}: the bundle lookup formats braces itself and fills them with null
		return String.format(Messages.getString("dialog.read.only.newer.format.message",
			"This database is in format version %1$s, and this version of the application reads "
				+ "format version %2$s. It is open read-only: saving would remove from the file what "
				+ "this version cannot read. Open it with a version that reads format version %1$s "
				+ "to change it."),
			applicationModelBean.getFormatVersion(), FORMAT_VERSION);
	}

	/**
	 * Reads an application model back out of its xml
	 *
	 * @param xml
	 *            the xml; it is read, not modified, and stays the caller's to overwrite
	 * @return the model
	 * @throws VaultHoldsAForeignTypeException
	 *             when the xml names a type no vault holds; that type is not built
	 */
	public static ApplicationModelBean toModel(final char[] xml)
	{
		try
		{
			return (ApplicationModelBean)newXStream().fromXML(new CharArrayReader(xml));
		}
		catch (RuntimeException failure)
		{
			ForbiddenClassException refused = refusalIn(failure);
			if (refused == null)
			{
				throw failure;
			}
			throw new VaultHoldsAForeignTypeException(refused.getMessage(), refused);
		}
	}

	/**
	 * The reader's refusal of a type, wherever XStream wrapped it
	 *
	 * @param failure
	 *            what reading threw
	 * @return the refusal, or null when the failure is another one
	 */
	private static ForbiddenClassException refusalIn(final Throwable failure)
	{
		for (Throwable cause = failure; cause != null; cause = cause.getCause())
		{
			if (cause instanceof ForbiddenClassException refused)
			{
				return refused;
			}
		}
		return null;
	}

	/**
	 * One instance for reading and writing, so the two cannot drift apart: XStream with its own
	 * standard types and {@link #VAULT_TYPES}, nothing else. On top of it the format version as an
	 * attribute and the skipping of unknown elements - see the class Javadoc. The instance comes
	 * from {@link XStreamFactory} of xstream-extensions, a dependency; reading and writing call
	 * XStream on it directly
	 *
	 * @return the configured instance
	 */
	private static XStream newXStream()
	{
		XStream xStream = XStreamFactory.newXStream();
		xStream.allowTypes(VAULT_TYPES);
		xStream.useAttributeFor(ApplicationModelBean.class, FORMAT_VERSION_FIELD);
		xStream.ignoreUnknownElements();
		return xStream;
	}
}

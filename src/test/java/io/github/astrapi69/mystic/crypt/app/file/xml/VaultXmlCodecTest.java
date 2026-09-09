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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.xstream.ObjectToXmlExtensions;

/**
 * The vault's xml as characters instead of as a {@link String} (#294).
 * <p>
 * The point of the first test is the one that matters: the xml this writes is character for
 * character the xml the library extensions wrote. The vault format is the xml, so a codec that
 * produced anything else would be a format change wearing a memory fix as a disguise.
 */
class VaultXmlCodecTest
{

	private static final String ENTRY_TITLE = "an entry with & < > and Grüße";

	/** Made up per run rather than written into the source, as every test password is */
	private static final String ENTRY_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("the xml is character for character what the extensions produced")
	void theXml_isUnchanged_byBeingWrittenIntoAWriter()
	{
		ApplicationModelBean model = aModelWithOneEntry();

		char[] xml = VaultXmlCodec.toXml(model);

		assertArrayEquals(ObjectToXmlExtensions.toXml(model).toCharArray(), xml,
			"the format is the xml. If this differs, the vault format changed");
	}

	@Test
	@DisplayName("a model survives the trip out to xml and back")
	void aModel_comesBack_fromItsOwnXml()
	{
		ApplicationModelBean model = aModelWithOneEntry();

		ApplicationModelBean readBack = VaultXmlCodec.toModel(VaultXmlCodec.toXml(model));

		assertNotNull(readBack);
		assertArrayEquals(ENTRY_TITLE.toCharArray(),
			readBack.getDataOfNodes().get(1L).get(0).getTitle());
		assertArrayEquals(ENTRY_PASSWORD.toCharArray(),
			readBack.getDataOfNodes().get(1L).get(0).getPassword());
	}

	@Test
	@DisplayName("xml written by the extensions is read back by the codec")
	void xmlFromBeforeThisChange_isRead_bytheCodec()
	{
		ApplicationModelBean model = aModelWithOneEntry();

		ApplicationModelBean readBack = VaultXmlCodec
			.toModel(ObjectToXmlExtensions.toXml(model).toCharArray());

		assertArrayEquals(ENTRY_TITLE.toCharArray(),
			readBack.getDataOfNodes().get(1L).get(0).getTitle(),
			"every vault in existence was written by the extensions; the codec has to read them");
	}

	@Test
	@DisplayName("the array handed to the codec is read, not modified")
	void toModel_leaves_theCallersXmlAlone()
	{
		char[] xml = VaultXmlCodec.toXml(aModelWithOneEntry());
		char[] asHandedOver = xml.clone();

		VaultXmlCodec.toModel(xml);

		assertArrayEquals(asHandedOver, xml,
			"the caller overwrites it when it is done - a codec that wiped it first would make "
				+ "reading a vault twice impossible");
	}

	@Test
	@DisplayName("an empty vault serializes and comes back")
	void anEmptyModel_isNot_aSpecialCase()
	{
		char[] xml = VaultXmlCodec.toXml(ApplicationModelBean.builder().build());

		assertTrue(xml.length > 0);
		assertNotNull(VaultXmlCodec.toModel(xml));
	}

	private static ApplicationModelBean aModelWithOneEntry()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title(ENTRY_TITLE.toCharArray()).userName("someone".toCharArray())
			.password(ENTRY_PASSWORD.toCharArray()).build();
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		entries.add(entry);
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new HashMap<>();
		dataOfNodes.put(1L, entries);
		return ApplicationModelBean.builder().dataOfNodes(dataOfNodes).build();
	}
}

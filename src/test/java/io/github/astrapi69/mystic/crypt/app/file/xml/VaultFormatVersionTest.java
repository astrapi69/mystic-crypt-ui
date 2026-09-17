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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.KeyPair;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The vault says which format wrote it, in a place 8.5 passes over, and a reader skips what it does
 * not know instead of failing (#402).
 * <p>
 * The version is an ATTRIBUTE of the root element because that is the one shape the 8.5 reader was
 * measured to accept: the release jar reads {@code formatVersion="2"} on the root and opens the
 * vault, and fails with {@code UnknownFieldException} on a {@code <formatVersion>} element - which
 * the running application reports as "Password is not valid". The measurement is in #402.
 * <p>
 * Each of the three ways a vault is protected has its own writer and its own reader, and two of
 * them used the library extensions instead of the codec. A version written by one path and not the
 * others, or read by one and not the others, is how an unknown element would still read as a wrong
 * password on a key-file vault. So every path is written and read back here.
 */
class VaultFormatVersionTest
{

	private static final String ENTRY_TITLE = "an entry that has to come back";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	@DisplayName("the version is an attribute of the root element, never an element")
	void theVersion_isWrittenAsAnAttribute_whichThe85ReaderPassesOver()
	{
		String xml = new String(VaultXmlCodec.toXml(aModelWithOneEntry()));

		String rootStartTag = xml.substring(0, xml.indexOf('>') + 1);
		assertTrue(rootStartTag.contains(" formatVersion=\"" + VaultXmlCodec.FORMAT_VERSION + "\""),
			"the root element carries the version as an attribute: " + rootStartTag);
		assertFalse(xml.contains("<formatVersion"),
			"an element is what 8.5 cannot read, and it would refuse the whole vault for it");
	}

	@Test
	@DisplayName("an element this build does not know is skipped, not reported as a wrong password")
	void anUnknownElement_isSkipped_andTheRestIsRead()
	{
		String xml = new String(VaultXmlCodec.toXml(aModelWithOneEntry()));
		String fromANewerVersion = xml.replaceFirst("<title>",
			"<somethingANewerVersionWrites>whatever it is</somethingANewerVersionWrites><title>");

		ApplicationModelBean readBack = VaultXmlCodec.toModel(fromANewerVersion.toCharArray());

		assertArrayEquals(ENTRY_TITLE.toCharArray(),
			readBack.getDataOfNodes().get(1L).get(0).getTitle(),
			"the entry is read as it was. Without this, the reader throws, and the sign-in says the "
				+ "password is wrong to somebody who typed it correctly");
	}

	@Test
	@DisplayName("a vault written before the version existed reads as having none")
	void aVaultWithoutTheAttribute_readsAsNoVersion()
	{
		String xml = new String(VaultXmlCodec.toXml(aModelWithOneEntry()));
		String asEarlierVersionsWroteIt = xml
			.replaceFirst(" formatVersion=\"" + VaultXmlCodec.FORMAT_VERSION + "\"", "");

		ApplicationModelBean readBack = VaultXmlCodec
			.toModel(asEarlierVersionsWroteIt.toCharArray());

		assertNull(readBack.getFormatVersion(),
			"every vault up to 8.5.1 has no attribute, and must still open");
		assertArrayEquals(ENTRY_TITLE.toCharArray(),
			readBack.getDataOfNodes().get(1L).get(0).getTitle());
	}

	@Test
	@DisplayName("a vault protected by the master password carries the version, and it is read back")
	void thePasswordPath_writesAndReadsTheVersion(@TempDir File directory) throws Exception
	{
		File vault = new File(directory, "password.mcrdb");
		char[] password = TestPasswords.throwaway().toCharArray();
		ApplicationModelBean model = aModelWithOneEntry();
		model.setMasterPwFileModelBean(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
				.masterPw(password.clone()).withMasterPw(true).withKeyFile(false).build());

		ApplicationXmlFileStoreWorker.saveToFileWithPassword(model);
		ApplicationModelBean readBack = ApplicationXmlFileReader.getApplicationModelBean(vault,
			password);

		assertEquals(VaultXmlCodec.FORMAT_VERSION, readBack.getFormatVersion());
	}

	@Test
	@DisplayName("a vault protected by a key file alone carries the version, and it is read back")
	void theKeyFilePath_writesAndReadsTheVersion(@TempDir File directory) throws Exception
	{
		File vault = new File(directory, "key.mcrdb");
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		ApplicationModelBean model = aModelWithOneEntry();
		model.setMasterPwFileModelBean(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
				.privateKeyInfo(KeyModelExtensions.toKeyModel(keyPair.getPrivate()))
				.withMasterPw(false).withKeyFile(true).build());

		ApplicationXmlFileStoreWorker.saveToFileWithPrivateKey(model);
		ApplicationModelBean readBack = ApplicationXmlFileReader.getApplicationModelBean(vault,
			keyPair.getPrivate());

		assertEquals(VaultXmlCodec.FORMAT_VERSION, readBack.getFormatVersion(),
			"this path wrote and read through the library extensions, which know neither the "
				+ "attribute nor how to skip an unknown element");
	}

	@Test
	@DisplayName("a vault protected by a password and a key file carries the version, and it is read back")
	void thePasswordAndKeyFilePath_writesAndReadsTheVersion(@TempDir File directory)
		throws Exception
	{
		File vault = new File(directory, "password-and-key.mcrdb");
		File keyFile = new File(directory, "private-key.pem");
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), keyFile);
		char[] password = TestPasswords.throwaway().toCharArray();
		ApplicationModelBean model = aModelWithOneEntry();
		model.setMasterPwFileModelBean(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
				.keyFileInfo(FileInfo.toFileInfo(keyFile)).masterPw(password.clone())
				.withMasterPw(true).withKeyFile(true).build());

		ApplicationXmlFileStoreWorker.saveToFileWithPasswordAndPrivateKey(model);
		ApplicationModelBean readBack = ApplicationXmlFileReader.getApplicationModelBean(vault,
			password, keyFile);

		assertEquals(VaultXmlCodec.FORMAT_VERSION, readBack.getFormatVersion(),
			"the same for the path that needs both");
	}

	private static ApplicationModelBean aModelWithOneEntry()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title(ENTRY_TITLE.toCharArray()).password(TestPasswords.throwaway().toCharArray())
			.build();
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		entries.add(entry);
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new HashMap<>();
		dataOfNodes.put(1L, entries);
		return ApplicationModelBean.builder().dataOfNodes(dataOfNodes).build();
	}
}

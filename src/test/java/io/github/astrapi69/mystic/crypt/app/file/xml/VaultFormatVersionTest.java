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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

	/**
	 * The one place every write passes through refuses a vault from a newer format, whichever way
	 * of saving got there - the menu items are disabled as well, but a guard that only exists in
	 * the menu is one caller away from a silent loss (#402, the maintainer's Q1)
	 */
	@Test
	@DisplayName("a vault in a newer format is not written, and the refusal names the version it needs")
	void aVaultInANewerFormat_isNotWritten()
	{
		ApplicationModelBean fromANewerVersion = aModelWithOneEntry();
		fromANewerVersion.setFormatVersion(VaultXmlCodec.FORMAT_VERSION + 1);

		IllegalStateException refused = assertThrows(IllegalStateException.class,
			() -> VaultXmlCodec.toXml(fromANewerVersion),
			"the unknown elements were dropped while it was read; writing it drops them from the "
				+ "file");
		assertTrue(
			refused.getMessage().contains("format version " + (VaultXmlCodec.FORMAT_VERSION + 1)),
			"the refusal says which format the vault needs: " + refused.getMessage());
		assertEquals(VaultXmlCodec.FORMAT_VERSION + 1, fromANewerVersion.getFormatVersion(),
			"and the model keeps its version - stamping this build's over it would turn the next "
				+ "attempt into a write");
	}

	@Test
	@DisplayName("storing a vault in a newer format is refused before the unsaved changes are marked as saved")
	void storing_aVaultInANewerFormat_leavesItDirty(@TempDir File directory)
	{
		File vault = new File(directory, "newer.mcrdb");
		ApplicationModelBean fromANewerVersion = aModelWithOneEntry();
		fromANewerVersion.setFormatVersion(VaultXmlCodec.FORMAT_VERSION + 1);
		fromANewerVersion.setDirty(true);
		fromANewerVersion.setMasterPwFileModelBean(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
				.masterPw(TestPasswords.throwaway().toCharArray()).withMasterPw(true)
				.withKeyFile(false).build());

		assertThrows(IllegalStateException.class,
			() -> ApplicationXmlFileStoreWorker.storeApplicationFile(fromANewerVersion));

		assertTrue(fromANewerVersion.isDirty(),
			"a refusal is not a save: the flag falls only behind a write that returned (#424), "
				+ "and the close question reads exactly this flag");
		assertFalse(vault.exists(), "and nothing was written");
	}

	@Test
	@DisplayName("only a version above this build's makes a vault read-only")
	void isNewerThanThisBuild_isTrue_onlyAboveTheCurrentVersion()
	{
		ApplicationModelBean model = aModelWithOneEntry();

		assertFalse(VaultXmlCodec.isNewerThanThisBuild(model),
			"no version: written before it existed");
		model.setFormatVersion(VaultXmlCodec.FORMAT_VERSION);
		assertFalse(VaultXmlCodec.isNewerThanThisBuild(model), "this build's own");
		model.setFormatVersion(VaultXmlCodec.FORMAT_VERSION + 1);
		assertTrue(VaultXmlCodec.isNewerThanThisBuild(model), "one above");
		assertFalse(VaultXmlCodec.isNewerThanThisBuild(null), "no model, no vault");
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

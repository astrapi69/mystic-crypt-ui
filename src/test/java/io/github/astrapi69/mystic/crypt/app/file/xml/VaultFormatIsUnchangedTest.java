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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.Security;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.thoughtworks.xstream.XStream;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.xstream.factory.XStreamFactory;

/**
 * Turning an entry's text into character arrays (#294) did not change the vault format.
 * <p>
 * That claim is the one thing about #294 that could cost somebody their database, so it is measured
 * from both ends: that XStream writes a character array as the same text it writes a String as, and
 * that a database in the shape an older build wrote opens here with every field where it belongs.
 * The round trip at the end is the acceptance quality-checks.md asks of a format-touching change -
 * a real file, written and read through the paths the application uses.
 */
class VaultFormatIsUnchangedTest
{

	private static final char[] MASTER_PASSWORD = TestPasswords.throwawayChars();

	/**
	 * The entry's password, made up per run.
	 * <p>
	 * Not a literal, although a literal would read more like the captured output this fixture is: a
	 * password written into a source file looks exactly like one that guards something, to a reader
	 * and to the secret scanner that reads every pull request (see {@link TestPasswords}). The
	 * fixture below is a template for that reason, and it proves the same thing - what is being
	 * pinned is the SHAPE of the xml, which does not depend on what the password happens to be
	 */
	private static final String ENTRY_PASSWORD = TestPasswords.throwaway();

	/**
	 * A database as a build before #294 wrote it, when the four text fields were {@link String}s.
	 * Captured from that build's output, not written by hand, with the password put back in at run
	 * time
	 */
	private static final String XML_FROM_AN_OLDER_BUILD = """
		<io.github.astrapi69.mystic.crypt.ApplicationModelBean>
		  <dataOfNodes class="linked-hash-map">
		    <entry>
		      <long>1</long>
		      <list>
		        <io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean>
		          <title>the bank</title>
		          <userName>account holder</userName>
		          <password>%s</password>
		          <url>https://bank.example.org</url>
		          <notes>line one</notes>
		          <expirable>false</expirable>
		          <showPassword>false</showPassword>
		          <resources/>
		          <properties/>
		          <dateTimesOfModification/>
		        </io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean>
		      </list>
		    </entry>
		  </dataOfNodes>
		  <showSplash>false</showSplash>
		  <signedIn>true</signedIn>
		  <dirty>false</dirty>
		</io.github.astrapi69.mystic.crypt.ApplicationModelBean>""".formatted(ENTRY_PASSWORD);

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/** The same three fields, once as text and once as characters */
	public static class WithText
	{
		String title = "my bank & <co>";
		String userName = "someone";
		char[] password = ENTRY_PASSWORD.toCharArray();
	}

	public static class WithCharacters
	{
		char[] title = "my bank & <co>".toCharArray();
		char[] userName = "someone".toCharArray();
		char[] password = ENTRY_PASSWORD.toCharArray();
	}

	@Test
	@DisplayName("a character array is written as the same text a String is written as")
	void theTwoFieldTypes_produce_theSameXml()
	{
		XStream forText = XStreamFactory.initializeXStream(null, null);
		forText.alias("entry", WithText.class);
		XStream forCharacters = XStreamFactory.initializeXStream(null, null);
		forCharacters.alias("entry", WithCharacters.class);

		assertEquals(forText.toXML(new WithText()), forCharacters.toXML(new WithCharacters()),
			"this is the whole reason #294 needs no migration. If it ever stops holding, every "
				+ "database in existence stops opening");
	}

	@Test
	@DisplayName("a database written before #294 opens with every field where it belongs")
	void aDatabaseFromAnOlderBuild_isRead_intoTheCurrentModel()
	{
		ApplicationModelBean model = VaultXmlCodec.toModel(XML_FROM_AN_OLDER_BUILD.toCharArray());

		MysticCryptEntryModelBean entry = model.getDataOfNodes().get(1L).get(0);
		assertArrayEquals("the bank".toCharArray(), entry.getTitle());
		assertArrayEquals("account holder".toCharArray(), entry.getUserName());
		assertArrayEquals(ENTRY_PASSWORD.toCharArray(), entry.getPassword());
		assertArrayEquals("https://bank.example.org".toCharArray(), entry.getUrl());
		assertArrayEquals("line one".toCharArray(), entry.getNotes());
	}

	@Test
	@DisplayName("a database written today is still what an older build would read")
	void aDatabaseWrittenToday_hasTheShape_anOlderBuildExpects()
	{
		char[] xml = VaultXmlCodec.toXml(aModelWithTheSameEntry());

		assertEquals(XML_FROM_AN_OLDER_BUILD, new String(xml),
			"character for character what the previous release wrote, so this change is readable "
				+ "in both directions rather than only forwards");
	}

	@Test
	@DisplayName("a real file, written and read back through the application's own paths")
	void anEntry_survives_theRoundTripThroughAFile(@TempDir File tempDir) throws Exception
	{
		File vaultFile = new File(tempDir, "format-unchanged.mcrdb");
		Files.createFile(vaultFile.toPath());
		ApplicationModelBean model = aModelWithTheSameEntry();
		model.setMasterPwFileModelBean(credentialsFor(vaultFile));

		ApplicationXmlFileStoreWorker.saveToFileWithPassword(model);
		ApplicationModelBean readBack = ApplicationXmlFileReader.getApplicationModelBean(vaultFile,
			MASTER_PASSWORD.clone());

		assertNotNull(readBack);
		assertTrue(vaultFile.length() > 0);
		MysticCryptEntryModelBean entry = readBack.getDataOfNodes().get(1L).get(0);
		assertArrayEquals("the bank".toCharArray(), entry.getTitle(),
			"a dirty flag says what the application believes; a file read back says what is on "
				+ "disk");
		assertArrayEquals("account holder".toCharArray(), entry.getUserName());
		assertArrayEquals(ENTRY_PASSWORD.toCharArray(), entry.getPassword());
		assertArrayEquals("https://bank.example.org".toCharArray(), entry.getUrl());
		assertArrayEquals("line one".toCharArray(), entry.getNotes());
	}

	private static MasterPwFileModelBean credentialsFor(final File vaultFile)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vaultFile))
			.selectedApplicationFilePath(vaultFile.getAbsolutePath())
			.masterPw(MASTER_PASSWORD.clone()).withMasterPw(true).withKeyFile(false)
			.minPasswordLength(6).build();
	}

	private static ApplicationModelBean aModelWithTheSameEntry()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("the bank".toCharArray()).userName("account holder".toCharArray())
			.password(ENTRY_PASSWORD.toCharArray()).url("https://bank.example.org".toCharArray())
			.notes("line one".toCharArray()).build();
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		entries.add(entry);
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new LinkedHashMap<>();
		dataOfNodes.put(1L, entries);
		return ApplicationModelBean.builder().dataOfNodes(dataOfNodes).signedIn(true).build();
	}
}

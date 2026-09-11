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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.file.search.PathFinder;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The key that opens a database has no business being inside it (#350).
 * <p>
 * {@code masterPw}, {@code repeatPw} and {@code lockVerifier} are transient, and
 * {@link MasterPasswordIsNotStoredTest} pins that the password never reaches the file.
 * {@code privateKeyInfo} carried no such modifier, so the encoded private key was written into the
 * payload on every save.
 * <p>
 * Why that is worth removing even though whoever decrypts the payload of a key-only vault already
 * holds the key: a key file usually protects more than one database, and copying it inside spreads
 * a credential the user keeps deliberately separate. A vault opened with a key and later saved with
 * a password is the sharper case - the key then rests on the strength of a passphrase it was chosen
 * to be independent of.
 * <p>
 * Nothing needs the stored copy. The key always comes from the key file the user picks at sign-in,
 * and the reader puts it on the model from there - which is what the last two tests hold in place,
 * because a field going transient is only safe while every reader of it is fed from somewhere else.
 */
class PrivateKeyIsNotStoredTest
{

	private static final char[] MASTER_PASSWORD = TestPasswords.throwawayChars();

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static File keyFile()
	{
		return new File(new File(PathFinder.getSrcTestResourcesDir(), "pem"), "private.pem");
	}

	private static PrivateKey theKey() throws Exception
	{
		return PrivateKeyReader.readPemPrivateKey(keyFile());
	}

	private static MasterPwFileModelBean credentialsFor(final File vault, final boolean withKeyFile,
		final boolean withMasterPw)
	{
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath()).withKeyFile(withKeyFile)
			.withMasterPw(withMasterPw).minPasswordLength(6).build();
		if (withKeyFile)
		{
			credentials.setKeyFileInfo(FileInfo.toFileInfo(keyFile()));
			credentials.setSelectedKeyFilePath(keyFile().getAbsolutePath());
		}
		if (withMasterPw)
		{
			credentials.setMasterPw(MASTER_PASSWORD.clone());
			credentials.setRepeatPw(MASTER_PASSWORD.clone());
		}
		return credentials;
	}

	@Test
	@DisplayName("the key is not in what gets serialized, whichever path writes it")
	void theSerializedDatabase_carriesNoKey_whereverItCameFrom(@TempDir File directory)
		throws Exception
	{
		File vault = new File(directory, "with-a-key.mcrdb");
		MasterPwFileModelBean credentials = credentialsFor(vault, true, false);
		credentials.setPrivateKeyInfo(KeyModelExtensions.toKeyModel(theKey()));
		ApplicationModelBean model = ApplicationModelBean.builder()
			.masterPwFileModelBean(credentials).build();

		String xml = new String(VaultXmlCodec.toXml(model));

		assertFalse(xml.contains("<privateKeyInfo>"),
			"the private key is written into the database: " + xml);
		assertFalse(xml.contains(Base64.getEncoder().encodeToString(theKey().getEncoded())),
			"the encoded key bytes are in the database even without the element name");
	}

	@Test
	@DisplayName("a key-only database is written without its key and opens again")
	void aKeyOnlyDatabase_isWrittenWithoutItsKey_andStillOpens(@TempDir File directory)
		throws Exception
	{
		File vault = new File(directory, "key-only.mcrdb");
		Files.createFile(vault.toPath());
		MasterPwFileModelBean credentials = credentialsFor(vault, true, false);

		ApplicationXmlFileFactory.newApplicationFileWithPrivateKey(credentials);
		ApplicationModelBean opened = ApplicationXmlFileReader
			.readApplicationFileWithPrivateKey(credentialsFor(vault, true, false));

		assertNotNull(opened);
		assertNotNull(opened.getMasterPwFileModelBean().getPrivateKeyInfo(),
			"the key has to be on the model after opening - the obfuscation plugin reads it from "
				+ "there, and it now comes from the key file rather than from inside the vault");
		assertArrayEquals(theKey().getEncoded(),
			opened.getMasterPwFileModelBean().getPrivateKeyInfo().getEncoded(),
			"and it is the key of the file that was used to open, not a copy of an older one");
	}

	@Test
	@DisplayName("a database opened with a password AND a key carries that key too")
	void aPasswordAndKeyDatabase_carriesItsKey_afterOpening(@TempDir File directory)
		throws Exception
	{
		File vault = new File(directory, "password-and-key.mcrdb");
		Files.createFile(vault.toPath());
		ApplicationXmlFileFactory
			.newApplicationFileWithPasswordAndPrivateKey(credentialsFor(vault, true, true));

		ApplicationModelBean opened = ApplicationXmlFileReader
			.readApplicationFileWithPasswordAndPrivateKey(credentialsFor(vault, true, true));

		assertArrayEquals(theKey().getEncoded(),
			opened.getMasterPwFileModelBean().getPrivateKeyInfo().getEncoded(),
			"this path took the key from the FILE until #350. With the field transient there is "
				+ "nothing in the file to take, so the reader has to put the key it was given on "
				+ "the model - as the key-only path already did");
	}

	@Test
	@DisplayName("a database written before #350, with the key inside, still opens")
	void aDatabaseFromAnOlderBuild_stillOpens_withTheKeyElementInIt()
	{
		String xmlFromAnOlderBuild = """
			<io.github.astrapi69.mystic.crypt.ApplicationModelBean>
			  <masterPwFileModelBean>
			    <selectedApplicationFilePath>/home/someone/vault.mcrdb</selectedApplicationFilePath>
			    <privateKeyInfo>
			      <encoded>AQIDBA==</encoded>
			      <algorithm>RSA</algorithm>
			    </privateKeyInfo>
			    <withKeyFile>true</withKeyFile>
			    <withMasterPw>false</withMasterPw>
			  </masterPwFileModelBean>
			  <signedIn>true</signedIn>
			</io.github.astrapi69.mystic.crypt.ApplicationModelBean>""";

		ApplicationModelBean opened = VaultXmlCodec.toModel(xmlFromAnOlderBuild.toCharArray());

		assertEquals("/home/someone/vault.mcrdb",
			opened.getMasterPwFileModelBean().getSelectedApplicationFilePath(),
			"every database written before this change has the element in it. Reading must skip "
				+ "it, not fail on it - a reader that throws here is data loss, not a fix");
	}
}

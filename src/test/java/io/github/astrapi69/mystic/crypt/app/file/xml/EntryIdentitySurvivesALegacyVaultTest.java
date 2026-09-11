package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.Security;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.vault.EntryIdentitySupport;

/**
 * The round trip {@code quality-checks.md} requires for #272, against a vault file that really was
 * written without identifiers.
 * <p>
 * The fixture is not a hand-built xml string: the file is produced by the application's own writer
 * from entries that carry no id, which is byte for byte what a version before identifiers wrote -
 * the field simply was never set, and XStream writes nothing for it. A synthetic fixture is what
 * {@code lessons-learned.md} records as having hidden the 2026-08-25 migration bug through a green
 * unit suite.
 * <p>
 * What is proven here is the decision taken in the issue, including the part that is a limitation:
 * loading assigns identifiers but writes nothing, so the identifiers are stable only from the first
 * real save onwards. A test that asserted stability before that save would be asserting something
 * the application does not do.
 */
class EntryIdentitySurvivesALegacyVaultTest
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

	@Test
	@DisplayName("a vault written before identifiers gets them on load, and keeps them from the "
		+ "first save on")
	void identifiersAreAssignedOnLoadAndSurviveEveryLaterOpen(@TempDir File tempDir)
		throws Exception
	{
		File vaultFile = writeLegacyVault(tempDir, "before-identifiers.mcrdb");

		ApplicationModelBean asWritten = read(vaultFile);
		for (MysticCryptEntryModelBean entry : entriesOf(asWritten))
		{
			assertNull(entry.getId(), "the fixture has to BE a vault without identifiers - if the "
				+ "writer put one in, this test proves nothing about the legacy path");
		}

		ApplicationModelBean firstOpen = read(vaultFile);
		EntryIdentitySupport.migrateOnLoad(firstOpen);
		List<UUID> beforeTheSave = identifiersOf(firstOpen);
		assertEquals(3, beforeTheSave.size());
		beforeTheSave.forEach(id -> assertNotNull(id,
			"every entry of the old file gets an identifier, not only the ones created later"));

		ApplicationModelBean openedAgainWithoutSaving = read(vaultFile);
		EntryIdentitySupport.migrateOnLoad(openedAgainWithoutSaving);
		assertNotEquals(beforeTheSave, identifiersOf(openedAgainWithoutSaving),
			"before the first save the identifiers are fresh on every open. That is the known "
				+ "limitation of assigning without writing, and it is asserted so that a later "
				+ "change which makes them stable earlier cannot pass unnoticed");

		save(firstOpen);

		ApplicationModelBean firstReopen = read(vaultFile);
		assertEquals(beforeTheSave, identifiersOf(firstReopen),
			"the identifiers the save wrote are the ones that come back");
		ApplicationModelBean secondReopen = read(vaultFile);
		assertEquals(beforeTheSave, identifiersOf(secondReopen),
			"and they are the same on every further open - an identifier regenerated per session "
				+ "is not an identifier");
	}

	@Test
	@DisplayName("an identifier that came in with a KeePass import is never replaced")
	void anExistingIdentifierIsKeptThroughTheRoundTrip(@TempDir File tempDir) throws Exception
	{
		UUID imported = UUID.randomUUID();
		File vaultFile = writeVault(tempDir, "mixed.mcrdb",
			List.of(
				MysticCryptEntryModelBean.builder().id(imported)
					.title("imported from KeePass".toCharArray()).build(),
				MysticCryptEntryModelBean.builder().title("written here, long ago".toCharArray())
					.build()));

		ApplicationModelBean opened = read(vaultFile);
		assertEquals(imported, identifiersOf(opened).get(0),
			"the imported identifier has to survive being written and read back at all");

		EntryIdentitySupport.migrateOnLoad(opened);
		assertEquals(imported, identifiersOf(opened).get(0),
			"an identifier once assigned never changes and is never reused");
		assertNotNull(identifiersOf(opened).get(1));

		save(opened);
		assertEquals(imported, identifiersOf(read(vaultFile)).get(0));
	}

	@Test
	@DisplayName("loading an old vault does not mark it as having unsaved changes")
	void loadingAssignsIdentifiersWithoutMakingTheVaultDirty(@TempDir File tempDir) throws Exception
	{
		File vaultFile = writeLegacyVault(tempDir, "not-dirtied-by-opening.mcrdb");

		ApplicationModelBean opened = read(vaultFile);
		opened.setDirty(false);
		EntryIdentitySupport.migrateOnLoad(opened);

		assertFalse(opened.isDirty(),
			"opening a file is not a change the user made. Marking it dirty here puts an "
				+ "unsaved-changes question in front of somebody who only looked at their vault, "
				+ "which is what #304 decided against for the lock and what the decision on #272 "
				+ "decided against here");
	}

	private File writeLegacyVault(File tempDir, String name) throws Exception
	{
		return writeVault(tempDir, name,
			List.of(MysticCryptEntryModelBean.builder().title("bank".toCharArray()).build(),
				MysticCryptEntryModelBean.builder().title("mail".toCharArray()).build(),
				MysticCryptEntryModelBean.builder().title("router".toCharArray()).build()));
	}

	private File writeVault(File tempDir, String name, List<MysticCryptEntryModelBean> entries)
		throws Exception
	{
		File vaultFile = new File(tempDir, name);
		Files.createFile(vaultFile.toPath());
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new LinkedHashMap<>();
		dataOfNodes.put(1L, new ArrayList<>(entries));
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder()
			.masterPwFileModelBean(credentialsFor(vaultFile)).dataOfNodes(dataOfNodes).build();

		File written = ApplicationXmlFileStoreWorker.saveToFileWithPassword(applicationModelBean);

		assertTrue(written.length() > 0, "the fixture must be a real written vault file");
		return vaultFile;
	}

	private void save(ApplicationModelBean applicationModelBean)
	{
		applicationModelBean.getMasterPwFileModelBean().setMasterPw(MASTER_PASSWORD.clone());
		ApplicationXmlFileStoreWorker.saveToFileWithPassword(applicationModelBean);
	}

	private ApplicationModelBean read(File vaultFile) throws Exception
	{
		ApplicationModelBean applicationModelBean = ApplicationXmlFileReader
			.getApplicationModelBean(vaultFile, MASTER_PASSWORD.clone());
		assertNotNull(applicationModelBean);
		return applicationModelBean;
	}

	private MasterPwFileModelBean credentialsFor(File vaultFile)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vaultFile))
			.selectedApplicationFilePath(vaultFile.getAbsolutePath())
			.masterPw(MASTER_PASSWORD.clone()).withMasterPw(true).withKeyFile(false)
			.minPasswordLength(6).build();
	}

	private List<MysticCryptEntryModelBean> entriesOf(ApplicationModelBean applicationModelBean)
	{
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		applicationModelBean.getDataOfNodes().values().forEach(entries::addAll);
		return entries;
	}

	private List<UUID> identifiersOf(ApplicationModelBean applicationModelBean)
	{
		return entriesOf(applicationModelBean).stream().map(MysticCryptEntryModelBean::getId)
			.toList();
	}
}

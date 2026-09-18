package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Opening a vault written before entries had identifiers, through the real sign-in path (#272).
 * <p>
 * The headless round trip in {@code EntryIdentitySurvivesALegacyVaultTest} proves the migration
 * itself. What it cannot reach is the sign-in code that calls it, and that is where the decision
 * taken in the issue actually lives: the identifiers are assigned, and the vault is NOT marked as
 * having unsaved changes. A user who only opened their vault must not be asked whether to save it.
 */
class LegacyVaultGetsIdentifiersOnOpenUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("opening a vault without identifiers assigns them and asks nothing about saving")
	void openingALegacyVaultAssignsIdentifiersWithoutMakingItDirty() throws Exception
	{
		File databaseFile = new File(tempHome, "written-before-identifiers.mcrdb");
		writeVaultWithoutIdentifiers(databaseFile);

		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		assertTrue(everyEntryHasAnIdentifier(),
			"the oldest data is the data most worth referencing, and only assigning on load "
				+ "reaches it - an entry that came out of this file without an identifier would "
				+ "never get one");
		assertFalse(unsavedChangesAreReported(),
			"opening a file is not an edit the user made. Marking the vault dirty here puts an "
				+ "unsaved-changes question in front of somebody who only looked at it, which is "
				+ "the decision taken in the issue and the same rule #304 settled for locking");
	}

	/**
	 * Writes a vault the way a version before identifiers wrote one: through the application's own
	 * writer, from entries whose id was never set
	 *
	 * @param databaseFile
	 *            the file to write
	 */
	private void writeVaultWithoutIdentifiers(final File databaseFile) throws Exception
	{
		if (java.security.Security
			.getProvider(org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			java.security.Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		}
		Files.createFile(databaseFile.toPath());
		List<MysticCryptEntryModelBean> entries = new ArrayList<>(
			List.of(MysticCryptEntryModelBean.builder().title("bank".toCharArray()).build(),
				MysticCryptEntryModelBean.builder().title("mail".toCharArray()).build()));
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new LinkedHashMap<>();
		dataOfNodes.put(1L, entries);
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(databaseFile))
			.selectedApplicationFilePath(databaseFile.getAbsolutePath())
			.masterPw(MASTER_PASSWORD.toCharArray()).withMasterPw(true).withKeyFile(false)
			.minPasswordLength(6).build();
		ApplicationXmlFileStoreWorker.saveToFileWithPassword(ApplicationModelBean.builder()
			.masterPwFileModelBean(credentials).dataOfNodes(dataOfNodes).build());
	}

	private boolean everyEntryHasAnIdentifier()
	{
		return GuiActionRunner.execute(() -> {
			ApplicationModelBean applicationModelBean = MysticCryptApplicationFrame.getInstance()
				.getModelObject();
			int seen = 0;
			for (List<MysticCryptEntryModelBean> entries : applicationModelBean.getDataOfNodes()
				.values())
			{
				for (MysticCryptEntryModelBean entry : entries)
				{
					assertNotNull(entry.getId());
					seen++;
				}
			}
			// an empty model would pass the loop above without proving anything
			return seen == 2;
		});
	}

	private boolean unsavedChangesAreReported()
	{
		return GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.asLockableWorkspace().hasUnsavedChanges());
	}
}

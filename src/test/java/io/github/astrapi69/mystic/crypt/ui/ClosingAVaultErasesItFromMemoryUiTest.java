package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.List;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.CloseApplicationFileAction;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Closing a vault erases it, in the running application, asserted on the buffers themselves (#242).
 * <p>
 * Every end-to-end assertion about closing was a null check until this class: the credentials are
 * null, the panel is null, the maps are null. All of those pass on a plain dereference, and
 * dereferencing is what a garbage collector may or may not get around to - so they proved the
 * references were dropped and said nothing about the memory behind them. {@code quality-checks.md}
 * calls a stand-in assertion worse than no test, because it makes the gap look covered.
 * <p>
 * So the buffers are taken out of the live model BEFORE the close and asserted afterwards. They are
 * the same arrays the application holds; nothing here copies them.
 */
class ClosingAVaultErasesItFromMemoryUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_TITLE = "erased-on-close";

	private static final String ENTRY_PASSWORD = "the-entry-password";

	@Test
	@DisplayName("closing overwrites the entry and master password buffers and clears the clipboard")
	void closingOverwritesTheBuffersAndClearsTheClipboard() throws Exception
	{
		File databaseFile = new File(tempHome, "erased-on-close.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone", ENTRY_PASSWORD);
		application.saveDatabase();

		// the arrays the application is using, not copies of them
		char[] entryPassword = liveEntryPassword();
		char[] entryTitle = liveEntryTitle();
		char[] masterPassword = liveMasterPassword();
		assertNotNull(entryPassword, "the precondition: the model really holds the entry");
		assertArrayEquals(ENTRY_PASSWORD.toCharArray(), entryPassword,
			"and the buffer really holds the password, so a zero-filled one afterwards means it "
				+ "was overwritten rather than never written");

		application.selectEntryRowByTitle(frame, ENTRY_TITLE).copyPasswordOfSelectedEntry(frame);
		assertEquals(ENTRY_PASSWORD, application.clipboardText(),
			"the precondition for the clipboard half: the password is really on it");

		closeTheOpenVault();

		assertArrayEquals(new char[entryPassword.length], entryPassword,
			"the entry's password is overwritten, not dropped. A null check on the field would "
				+ "pass here whether or not this happened");
		assertArrayEquals(new char[entryTitle.length], entryTitle,
			"and its text with it - all six fields are buffers since #294 for exactly this");
		assertArrayEquals(new char[masterPassword.length], masterPassword,
			"the master password opens the file again; leaving it in memory after the file is "
				+ "closed keeps the vault reachable");
		assertEquals("", application.clipboardText(),
			"a password copied out of the vault must not stay pasteable after the vault is "
				+ "closed. Locking has cleared the clipboard since #237; closing, the stronger of "
				+ "the two, did not (#242)");
	}

	private char[] liveEntryPassword()
	{
		return GuiActionRunner.execute(() -> firstEntry().getPassword());
	}

	private char[] liveEntryTitle()
	{
		return GuiActionRunner.execute(() -> firstEntry().getTitle());
	}

	private char[] liveMasterPassword()
	{
		return GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.getModelObject().getMasterPwFileModelBean().getMasterPw());
	}

	/**
	 * The first entry the running application holds, looked for in BOTH places an entry can live -
	 * the map of a node's entries and the tree the nodes hang in. The running application fills the
	 * tree and leaves the map null, which a test that reads only the map finds out as a
	 * NullPointerException rather than as a finding
	 *
	 * @return the first entry
	 */
	private static MysticCryptEntryModelBean firstEntry()
	{
		ApplicationModelBean applicationModelBean = MysticCryptApplicationFrame.getInstance()
			.getModelObject();
		if (applicationModelBean.getDataOfNodes() != null)
		{
			MysticCryptEntryModelBean found = firstOf(
				applicationModelBean.getDataOfNodes().values());
			if (found != null)
			{
				return found;
			}
		}
		if (applicationModelBean.getRootTreeAsMap() != null)
		{
			for (TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : applicationModelBean
				.getRootTreeAsMap().values())
			{
				if (node == null || node.getValue() == null)
				{
					continue;
				}
				MysticCryptEntryModelBean found = firstOf(
					List.of(node.getValue().getDefaultContent()));
				if (found != null)
				{
					return found;
				}
			}
		}
		throw new IllegalStateException("the vault under test holds no entry at all");
	}

	private static MysticCryptEntryModelBean firstOf(
		final Collection<List<MysticCryptEntryModelBean>> lists)
	{
		for (List<MysticCryptEntryModelBean> entries : lists)
		{
			if (entries == null)
			{
				continue;
			}
			for (MysticCryptEntryModelBean entry : entries)
			{
				if (entry != null)
				{
					return entry;
				}
			}
		}
		return null;
	}

	private void closeTheOpenVault()
	{
		GuiActionRunner.execute(() -> new CloseApplicationFileAction("Close Database")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		robot.waitForIdle();
	}
}

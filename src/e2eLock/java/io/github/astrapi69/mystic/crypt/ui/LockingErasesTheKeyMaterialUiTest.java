package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.key.KeyType;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Locking clears the key material and keeps the decrypted content, in the running application
 * (#242).
 * <p>
 * The two halves carry different weight, which is why they get different answers. The key material
 * - the master password, its repeat, the private key of a key-file vault - is the attack surface: a
 * process that sits locked for hours keeps it reachable in a core dump, in swap, or in a
 * hibernation image long after the user walked away. The decrypted content is the cost side:
 * rebuilding it means reading and decrypting the file again, and the 600,000 PBKDF2 iterations
 * behind that are what #237 promised the user would not pay on every short lock.
 * <p>
 * So this asserts both directions at once, on the buffers themselves rather than on their fields. A
 * null check would pass whether or not anything was overwritten, and half of what is checked here
 * is that something was NOT overwritten - which a null check cannot express at all.
 * <p>
 * What bounds the half that stays is the idle watchdog, and
 * {@link AutomaticLockHoldsTheInvariantUiTest} plus {@link ClosingAVaultErasesItFromMemoryUiTest}
 * hold the other end of it: when the timer closes the vault, the content goes too.
 */
class LockingErasesTheKeyMaterialUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_TITLE = "still-there-while-locked";

	private static final String ENTRY_PASSWORD = "the-entry-password";

	/** Not a real key: bytes with a recognisable pattern, so a zero-filled array means erased */
	private static final byte[] PRIVATE_KEY_BYTES = "not-a-real-private-key-just-bytes".getBytes();

	@Test
	@DisplayName("locking overwrites the master password and its repeat, and keeps the entries")
	void lockingErasesTheKeyMaterialAndKeepsTheContent() throws Exception
	{
		File databaseFile = new File(tempHome, "key-material-on-lock.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone", ENTRY_PASSWORD);
		application.saveDatabase();

		// the repeat is only filled when a database is created or its password changed, so this
		// test puts it there the way those flows do - it is the field, not the flow, that is at
		// stake, and a workspace locked in the session that created its vault really carries it
		char[] repeat = MASTER_PASSWORD.toCharArray();
		GuiActionRunner.execute(() -> credentials().setRepeatPw(repeat));

		char[] masterPassword = GuiActionRunner.execute(() -> credentials().getMasterPw());
		char[] entryPassword = GuiActionRunner.execute(() -> firstEntry().getPassword());
		char[] entryTitle = GuiActionRunner.execute(() -> firstEntry().getTitle());
		assertArrayEquals(MASTER_PASSWORD.toCharArray(), masterPassword,
			"the precondition: these are the arrays the application is using, so a zero-filled one "
				+ "afterwards means overwritten rather than never written");

		application.lockWorkspace();

		assertArrayEquals(new char[masterPassword.length], masterPassword,
			"the master password is what the database key is derived from. It is overwritten, not "
				+ "dropped - a reference the collector may or may not get around to says nothing "
				+ "about a core dump taken while the process sat locked");
		assertNull(GuiActionRunner.execute(() -> credentials().getMasterPw()));
		assertArrayEquals(new char[repeat.length], repeat,
			"and the repeat with it: it is the same secret typed twice, it is read by nothing "
				+ "after the vault exists, and it was quietly left behind while the first field "
				+ "was being carefully wiped");
		assertNull(GuiActionRunner.execute(() -> credentials().getRepeatPw()));
		assertNotNull(GuiActionRunner.execute(() -> credentials().getLockVerifier()),
			"what stays behind is the verifier, which can recognise the password and cannot "
				+ "produce it - that is what makes taking the password away possible at all");

		assertArrayEquals(ENTRY_PASSWORD.toCharArray(), entryPassword,
			"and the other half of the decision, which is the half a null check cannot even "
				+ "express: the decrypted content STAYS. Rebuilding it costs 600,000 PBKDF2 "
				+ "iterations, and #237 promised the user would not pay that for a short lock");
		assertArrayEquals(ENTRY_TITLE.toCharArray(), entryTitle);
		assertTrue(GuiActionRunner.execute(() -> firstEntry() != null),
			"the entry is still in the model, which is what makes unlocking cheap");
	}

	@Test
	@DisplayName("locking overwrites the private key, which for a key-file vault is the only way in")
	void lockingErasesThePrivateKey() throws Exception
	{
		File databaseFile = new File(tempHome, "private-key-on-lock.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone", ENTRY_PASSWORD);

		byte[] encoded = PRIVATE_KEY_BYTES.clone();
		GuiActionRunner.execute(() -> credentials().setPrivateKeyInfo(KeyModel.builder()
			.encoded(encoded).algorithm("RSA").keyType(KeyType.PRIVATE_KEY).build()));
		assertArrayEquals(PRIVATE_KEY_BYTES, encoded, "the precondition, again");

		application.lockWorkspace();

		assertArrayEquals(new byte[encoded.length], encoded,
			"for a key-only vault no password is involved at all, so this is not the second way "
				+ "in, it is the only one. Forgetting the password and keeping the key forgets the "
				+ "wrong door");
		assertNull(GuiActionRunner.execute(() -> credentials().getPrivateKeyInfo()),
			"and the holder goes with it - KeyModel declares its fields final, so overwriting the "
				+ "array and dropping the holder is what this layer can reach");
	}

	@Test
	@DisplayName("unlocking still works, and erases the verifier it no longer needs")
	void unlockingWorksAndLeavesNoVerifierBehind() throws Exception
	{
		File databaseFile = new File(tempHome, "unlock-after-erasing.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone", ENTRY_PASSWORD);
		application.lockWorkspace();

		application.unlockWorkspace(MASTER_PASSWORD);

		assertTrue(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"taking more away at lock must not make the way back in stop working - the password is "
				+ "typed again either way, which is the whole reason unlocking costs no more than "
				+ "it did");
		assertNull(GuiActionRunner.execute(() -> credentials().getLockVerifier()),
			"and what locking left behind is gone once it is no longer what stands between the "
				+ "user and the vault");
		assertEquals(ENTRY_TITLE,
			String.valueOf(GuiActionRunner.execute(() -> firstEntry().getTitle())),
			"with the content never having left, which is what the whole arrangement is for");
	}

	private static MasterPwFileModelBean credentials()
	{
		return MysticCryptApplicationFrame.getInstance().getModelObject()
			.getMasterPwFileModelBean();
	}

	/**
	 * The first entry the running application holds, from BOTH places an entry can live - the map
	 * of a node's entries and the tree the nodes hang in. The running application fills the tree
	 * and leaves the map null
	 *
	 * @return the first entry, or null when there is none
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
		if (applicationModelBean.getRootTreeAsMap() == null)
		{
			return null;
		}
		for (TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : applicationModelBean
			.getRootTreeAsMap().values())
		{
			if (node == null || node.getValue() == null)
			{
				continue;
			}
			MysticCryptEntryModelBean found = firstOf(List.of(node.getValue().getDefaultContent()));
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	private static MysticCryptEntryModelBean firstOf(
		final java.util.Collection<List<MysticCryptEntryModelBean>> lists)
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
}

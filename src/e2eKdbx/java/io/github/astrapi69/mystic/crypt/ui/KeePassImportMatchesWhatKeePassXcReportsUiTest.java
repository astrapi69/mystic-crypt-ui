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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.keepass.KdbxFacts;
import io.github.astrapi69.mystic.crypt.keepass.KeePassXcDump;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The spot check of a KeePass import, done by a second reader: three entries in three places, in a
 * file KeePassXC wrote, are imported, and what the vault holds for each is compared with what
 * {@code keepassxc-cli} reads from the SAME file (TC-0061).
 * <p>
 * The yardstick is KeePassXC, not this application's model, so a field the import drops cannot
 * compare equal to itself. The fixture is {@code keepassxc-2.7.10-kdbx31-three-entries.kdbx}, its
 * content and password are recorded in {@code src/e2eKdbx/resources/kdbx/README.md}. A failure
 * names the entry and the field, never the value: the values are passwords.
 */
class KeePassImportMatchesWhatKeePassXcReportsUiTest extends AbstractUiTest
{

	private static final String FIXTURE = "keepassxc-2.7.10-kdbx31-three-entries.kdbx";

	private static final String FIXTURE_PASSWORD = TestPasswords.KEEPASSXC_FIXTURE;

	/** The node every new vault starts with, which is not part of what is imported */
	private static final String STARTER_NODE = "mykeys";

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("every imported entry equals what keepassxc-cli reads from the same file, in the same group")
	void everyImportedEntryEqualsWhatKeePassXcReadsFromTheSameFile() throws Exception
	{
		File original = KdbxFixtures.copy(FIXTURE, tempHome);
		KdbxFacts reportedByKeePassXc = KdbxFacts
			.of(KeePassXcDump.xmlOf(original, FIXTURE_PASSWORD));
		assertEquals(3, reportedByKeePassXc.entries().size(),
			"the fixture holds three entries, as keepassxc-cli reads it");
		String readWith = " [read with keepassxc-cli " + KeePassXcDump.version() + "]";

		File vault = new File(tempHome, "kdbx-import-spot-check.mcrdb");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		application.deleteNode(application.showMainFrame(), STARTER_NODE);
		application.importKeePassDatabase(original, FIXTURE_PASSWORD);

		Map<String, MysticCryptEntryModelBean> liveByPath = liveEntriesByGroupPathAndTitle();
		assertEquals(reportedByKeePassXc.entries().size(), liveByPath.size(),
			"the vault holds as many entries as the file has" + readWith);
		for (KdbxFacts.Entry reported : reportedByKeePassXc.entries())
		{
			String where = reported.groupPath() + "/" + reported.title();
			MysticCryptEntryModelBean live = liveByPath.get(where);
			assertTrue(live != null,
				"the entry '" + where + "' is in the vault under the same group" + readWith);
			assertField(where, "user name", reported.userName(), live.getUserName(), readWith);
			assertField(where, "password", reported.password(), live.getPassword(), readWith);
			assertField(where, "URL", reported.url(), live.getUrl(), readWith);
			assertField(where, "notes", reported.notes(), live.getNotes(), readWith);
		}
	}

	private static void assertField(final String entry, final String field, final String reported,
		final char[] imported, final String readWith)
	{
		String expected = reported == null ? "" : reported;
		String actual = imported == null ? "" : new String(imported);
		assertTrue(expected.equals(actual), "the " + field + " of '" + entry
			+ "' is what KeePassXC reads (values not printed)" + readWith);
	}

	/**
	 * Every entry of the running vault, keyed by the names of the groups from the imported root
	 * down, then the entry's title
	 */
	private static Map<String, MysticCryptEntryModelBean> liveEntriesByGroupPathAndTitle()
	{
		return GuiActionRunner.execute(() -> {
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> vaultRoot = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			Map<String, MysticCryptEntryModelBean> found = new TreeMap<>();
			for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : childrenOf(
				vaultRoot))
			{
				collect(child, "", found);
			}
			return found;
		});
	}

	private static void collect(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node,
		final String parentPath, final Map<String, MysticCryptEntryModelBean> found)
	{
		String path = parentPath.isEmpty()
			? node.getValue().getName()
			: parentPath + "/" + node.getValue().getName();
		if (node.getValue().getDefaultContent() != null)
		{
			for (MysticCryptEntryModelBean entry : node.getValue().getDefaultContent())
			{
				found.put(path + "/" + new String(entry.getTitle()), entry);
			}
		}
		for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : childrenOf(
			node))
		{
			collect(child, path, found);
		}
	}

	private static List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> childrenOf(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node)
	{
		return node.getChildren() == null ? new ArrayList<>() : new ArrayList<>(node.getChildren());
	}
}

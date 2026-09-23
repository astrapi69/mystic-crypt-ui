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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.CloseApplicationFileAction;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Closing a vault overwrites every buffer of an entry, in the running application, for an entry
 * that is not in the first node and carries everything an entry can carry (#392).
 * <p>
 * {@code ClosingAVaultErasesItFromMemoryUiTest} holds the password and the title of the first entry
 * of the first node; {@code VaultCloseSupportTest} holds the rest, on models it builds by hand. A
 * close path that bypassed {@code VaultCloseSupport} - as ending the application did until #387 -
 * would pass both. This one takes the entry the way a user gets it: imported from the KeePassXC
 * fixture through File > Import, so it sits in {@code Root/Team} below the vault's own first node,
 * with a URL, notes, an attachment and a history version; the repeated password is typed in through
 * the entry editor. Then every buffer is held, the vault is closed through the menu action, and
 * each one is asserted zero-filled.
 */
class ClosingAVaultErasesAnImportedEntryUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String FIXTURE = "keepassxc-2.7.10-kdbx4.kdbx";

	/** Test material, see the README beside the fixture */
	private static final String FIXTURE_PASSWORD = "fixture";

	/** The fixture's one entry, in {@code Root/Team} - see the README beside the fixture */
	private static final String TITLE = "Titel test foo";

	private static final String GROUP = "Team";

	@Test
	@DisplayName("closing overwrites all six fields, the attachment and the history of a nested imported entry")
	void closingOverwritesEveryBufferOfANestedEntry() throws Exception
	{
		File vault = new File(tempHome, "erases-an-imported-entry.mcrdb");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.importKeePassDatabase(copyTheFixture(), FIXTURE_PASSWORD);
		application.selectTreeRowByName(frame, GROUP).selectEntryRowByTitle(frame, TITLE)
			.editSelectedEntryRepeat(frame, "the repeated password");
		application.saveDatabase();

		MysticCryptEntryModelBean entry = liveEntryTitled(TITLE);
		assertTrue(entry.getHistory() != null && !entry.getHistory().isEmpty(),
			"the precondition: the imported entry carries a history version");
		char[] title = entry.getTitle();
		char[] userName = entry.getUserName();
		char[] password = entry.getPassword();
		char[] repeat = entry.getRepeat();
		char[] url = entry.getUrl();
		char[] notes = entry.getNotes();
		byte[] attachment = entry.getResources().get(0).getContent();
		MysticCryptEntryModelBean previous = entry.getHistory().get(0);
		char[] previousTitle = previous.getTitle();
		char[] previousPassword = previous.getPassword();
		assertArrayEquals("the repeated password".toCharArray(), repeat,
			"the precondition: the editor wrote the repeat into the live entry");
		for (Object buffer : List.of(title, userName, password, url, notes, attachment,
			previousTitle, previousPassword))
		{
			assertNotNull(buffer, "the precondition: every buffer held here has content");
		}

		GuiActionRunner.execute(() -> new CloseApplicationFileAction("Close Database")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		robot.waitForIdle();

		assertZeroFilled(title, "the title");
		assertZeroFilled(userName, "the user name");
		assertZeroFilled(password, "the password");
		assertZeroFilled(repeat, "the repeated password");
		assertZeroFilled(url, "the URL");
		assertZeroFilled(notes, "the notes");
		assertArrayEquals(new byte[attachment.length], attachment,
			"the attachment's bytes are overwritten, not dropped");
		assertZeroFilled(previousTitle, "the history version's title");
		assertZeroFilled(previousPassword,
			"the history version's password - often one the user still uses elsewhere (#402)");
	}

	private static void assertZeroFilled(final char[] buffer, final String what)
	{
		assertArrayEquals(new char[buffer.length], buffer,
			what + " of the nested, imported entry is overwritten by the close, in the running "
				+ "application");
	}

	private static MysticCryptEntryModelBean liveEntryTitled(final String title)
	{
		return GuiActionRunner.execute(() -> {
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			return root.traverse().stream()
				.filter(
					node -> node.getValue() != null && node.getValue().getDefaultContent() != null)
				.flatMap(node -> node.getValue().getDefaultContent().stream())
				.filter(
					entry -> entry.getTitle() != null && title.equals(new String(entry.getTitle())))
				.findFirst().orElseThrow();
		});
	}

	private File copyTheFixture() throws Exception
	{
		File copy = new File(tempHome, FIXTURE);
		try (InputStream fixture = getClass().getResourceAsStream("/kdbx/" + FIXTURE))
		{
			assertNotNull(fixture, "the fixture is on the e2eKdbx class path");
			Files.copy(fixture, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		assertEquals(true, copy.length() > 0);
		return copy;
	}
}

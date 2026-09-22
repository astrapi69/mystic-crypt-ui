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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
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
 * An entry created in this application keeps its identifier when it is exported to KDBX (#422).
 * <p>
 * The round trip test measures identity for entries KeePassXC wrote; the FLOSS/fund milestone also
 * promises it "including entries created in the application". Measured the same way: the export is
 * read with {@code keepassxc-cli}, not with this application's own reader, so a reader that happens
 * to agree with the writer cannot make it pass.
 */
class AnEntryCreatedHereKeepsItsIdentifierUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String KDBX_PASSWORD = TestPasswords.throwaway();

	private static final String TITLE = "created in the application";

	@Test
	@DisplayName("an entry created here carries the identifier it was given into the exported KDBX file")
	void anEntryCreatedHere_keepsItsIdentifier_inTheExport() throws Exception
	{
		File vault = new File(tempHome, "identity.mcrdb");
		File exported = new File(tempHome, "identity.kdbx");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRowByName(frame, "mykeys").addEntry(frame, TITLE, "someone",
			TestPasswords.throwaway());
		UUID given = identifierOfTheEntryTitled(TITLE);
		assertNotNull(given,
			"the precondition: an entry gets an identifier when it is created (#272)");

		application.exportKeePassDatabase(exported, KDBX_PASSWORD);

		KdbxFacts.Entry written = KdbxFacts.of(KeePassXcDump.xmlOf(exported, KDBX_PASSWORD))
			.onlyEntry();
		assertEquals(TITLE, written.title(), "the precondition: this is the entry created above");
		assertEquals(asKeePassUuid(given), written.identifier(),
			"KeePass would otherwise see a different entry on every export [read with keepassxc-cli "
				+ KeePassXcDump.version() + "]");
	}

	private static UUID identifierOfTheEntryTitled(final String title)
	{
		return GuiActionRunner.execute(() -> {
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			return root.traverse().stream()
				.filter(
					node -> node.getValue() != null && node.getValue().getDefaultContent() != null)
				.flatMap(node -> node.getValue().getDefaultContent().stream())
				.filter(entry -> title.equals(new String(entry.getTitle()))).findFirst()
				.map(MysticCryptEntryModelBean::getId).orElse(null);
		});
	}

	/** KeePass writes a UUID as the base64 of its sixteen bytes, most significant first */
	private static String asKeePassUuid(final UUID identifier)
	{
		ByteBuffer bytes = ByteBuffer.allocate(16);
		bytes.putLong(identifier.getMostSignificantBits());
		bytes.putLong(identifier.getLeastSignificantBits());
		return Base64.getEncoder().encodeToString(bytes.array());
	}
}

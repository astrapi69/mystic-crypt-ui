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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.assertj.swing.edt.GuiActionRunner;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * What the KDBX end-to-end tests share: a fixture from {@code src/e2eKdbx/resources/kdbx}, copied
 * where a test may import it, and an entry of the running application's vault, found by its title
 */
final class KdbxFixtures
{

	private KdbxFixtures()
	{
	}

	/**
	 * Copies a fixture of the e2eKdbx class path into the given directory
	 *
	 * @param fixture
	 *            the fixture's file name in {@code /kdbx/}
	 * @param directory
	 *            where the copy goes
	 * @return the copy
	 * @throws IOException
	 *             if the copy cannot be written
	 */
	static File copy(final String fixture, final File directory) throws IOException
	{
		File copy = new File(directory, fixture);
		try (InputStream in = KdbxFixtures.class.getResourceAsStream("/kdbx/" + fixture))
		{
			assertNotNull(in, "the fixture " + fixture + " is on the e2eKdbx class path");
			Files.copy(in, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		assertTrue(copy.length() > 0, "the fixture was copied");
		return copy;
	}

	/**
	 * The entry with the given title in the running application's vault, as the tree holds it
	 *
	 * @param title
	 *            the entry's title
	 * @return the live entry
	 */
	static MysticCryptEntryModelBean liveEntryTitled(final String title)
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
}

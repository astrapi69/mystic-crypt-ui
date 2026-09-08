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

import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * The inventory of what the application offers while no vault is open, compared against the list
 * that says what it should offer (#232).
 * <p>
 * This is the test the whole exercise was for. The public state used to be decided by a blacklist,
 * so every entry added since - both KeePass entries, every plugin item - became public without
 * anyone deciding it, and nothing said so. This test fails on any difference in either direction:
 * an entry that became public without being listed, and an entry that was listed but is not there.
 * A new menu item therefore has to be decided rather than inherited.
 * <p>
 * It deliberately compares the WHOLE set rather than checking a few known entries: a test that
 * looks only for what it already knows cannot catch what nobody thought of, which is exactly the
 * class of defect this replaces.
 */
class PublicMenuInventoryUiTest extends AbstractUiTest
{

	/**
	 * The entries expected to be enabled without a vault: the host list, plus the entries of the
	 * one plugin that declares itself usable without one. The plugin's items are named by their
	 * TEXT, since a plugin's menu items need not carry a component name
	 */
	private static final Set<String> EXPECTED_PUBLIC_TEXTS = new LinkedHashSet<>(List.of("File",
		"Settings...", "Exit", "View", "Look and Feel", "GTK", "Metal", "Ocean", "Motif", "Nimbus",
		"System", "FlatLaf Dark", "FlatLaf IntelliJ", "FlatLaf Darcula", "Plugins", "Checksum",
		"Verify Checksum", "Checksum and MAC", "Help", "Donate", "Licence", "Info"));

	/**
	 * "FlatLaf Light" is public and still not in the list above: it is the theme the application
	 * starts with, and a theme item greys itself out while it is the active one
	 * (DesktopMenu.newFlatLafMenuItem overrides isEnabled). That is deliberate behaviour of the
	 * item, not the whitelist, and this test must not push against it
	 */
	private static final String ACTIVE_THEME_IS_NOT_OFFERED = "FlatLaf Light";

	@Test
	@DisplayName("without a vault, exactly the entries on the list are offered - no more, no less")
	void thePublicStateOffersExactlyWhatItIsAllowedTo() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();

		Set<String> enabled = enabledEntryTexts();

		assertTrue(
			GuiActionRunner.execute(
				() -> !MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"precondition: this is the state without a vault");
		assertEquals(EXPECTED_PUBLIC_TEXTS, enabled,
			"the entries offered without a vault must be exactly the ones decided on. An entry on "
				+ "the left that is not on the right became public without being listed; one on "
				+ "the right that is not on the left was listed but is not there");
	}

	/**
	 * The texts of every enabled menu entry, containers included, as the user would find them by
	 * walking the menu bar
	 *
	 * @return the enabled entries
	 */
	private static Set<String> enabledEntryTexts()
	{
		return GuiActionRunner.execute(() -> {
			JMenuBar menubar = MysticCryptApplicationFrame.getInstance().getJMenuBar();
			List<String> texts = new ArrayList<>();
			collectEnabled(menubar, texts);
			return new LinkedHashSet<>(texts);
		});
	}

	private static void collectEnabled(final MenuElement element, final List<String> texts)
	{
		for (MenuElement child : element.getSubElements())
		{
			Component component = child.getComponent();
			if (component instanceof JMenuItem item && item.isEnabled() && item.getText() != null
				&& !item.getText().isBlank())
			{
				texts.add(item.getText());
			}
			if (!(component instanceof JMenu menu) || menu.isEnabled())
			{
				collectEnabled(child, texts);
			}
		}
	}


	private static void collectElements(final MenuElement element,
		final java.util.List<MenuElement> collected)
	{
		for (MenuElement child : element.getSubElements())
		{
			collected.add(child);
			collectElements(child, collected);
		}
	}

}

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.io.File;
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
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The inventory of what the application offers while no vault is open, compared against the list
 * that says what it should offer (#232).
 * <p>
 * This is the test the whole exercise was for. The public state used to be decided by a blacklist,
 * so every entry added since - both KeePass entries, every plugin item - became public without
 * anyone deciding it, and nothing said so.
 * <p>
 * It checks a SUBSET, not equality: every entry that is offered has to be on the list, and nothing
 * else. That is the dangerous direction - something reachable that nobody allowed. The other
 * direction is deliberately not asserted, because an entry may legitimately be missing from the
 * offered set without anything being wrong: a FlatLaf theme item greys itself out while it is the
 * active one, so equality would break whenever the default theme changes. A test that breaks for
 * reasons nobody cares about gets loosened rather than investigated, and then it protects nothing.
 * <p>
 * It deliberately looks at the WHOLE offered set rather than at a few known entries: a test that
 * looks only for what it already knows cannot catch what nobody thought of, which is exactly the
 * class of defect this replaces.
 * <p>
 * SCOPE, and it is narrower than the name suggests: this covers MENU entries only. The toolbar in
 * the same method still decides by a blacklist, so its buttons are not checked here and the "new
 * database" button is active without a vault (#269). Until that is closed, a green run here does
 * not mean the public state is covered - only that no menu entry is offered unlisted.
 * <p>
 * Locked and public are the same menu state - {@code LockWorkspaceAction} calls the same
 * {@code onEnableByPublic} after clearing the signed-in flag (#237). That is asserted here rather
 * than written in a comment somewhere.
 */
class PublicMenuInventoryUiTest extends AbstractUiTest
{

	/**
	 * The entries that MAY be offered without a vault: the host list, plus the entries of the one
	 * plugin that declares itself usable without one. Named by their TEXT, since a plugin's menu
	 * items need not carry a component name. Anything offered that is not in here fails the test;
	 * something in here that is not offered does not, see the class comment
	 */
	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final Set<String> ALLOWED_PUBLIC_TEXTS = new LinkedHashSet<>(List.of("File",
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
	@DisplayName("without a vault, nothing is offered that is not on the list")
	void thePublicStateOffersNothingItIsNotAllowedTo() throws Exception
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
		assertOffersNothingUnlisted(enabled);
		assertTrue(enabled.contains("Verify Checksum"),
			"the tool this whole issue was raised for has to be reachable without a vault, or the "
				+ "list is right and useless");
	}

	@Test
	@DisplayName("locked, nothing is offered that is not on the list either")
	void theLockedStateOffersNothingItIsNotAllowedTo() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);
		File databaseFile = new File(tempHome, "inventory-locked-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		awaitApplicationInitialized();
		application.showMainFrame();

		application.lockWorkspace();

		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"precondition: the workspace is locked");
		assertOffersNothingUnlisted(enabledEntryTexts());
	}

	/**
	 * Fails naming the entries that are offered although nothing allows them - the direction that
	 * matters, and the message a future reader needs: which entry, not just that the sets differ
	 *
	 * @param offered
	 *            the entries currently enabled
	 */
	private static void assertOffersNothingUnlisted(final Set<String> offered)
	{
		Set<String> unlisted = new LinkedHashSet<>(offered);
		unlisted.removeAll(ALLOWED_PUBLIC_TEXTS);
		assertTrue(unlisted.isEmpty(),
			"offered without a vault, but on no list: " + unlisted + ". Either the entry belongs "
				+ "in PublicAccess, decided and written down, or it must not be offered here");
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

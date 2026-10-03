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
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.LockWorkspaceAction;

/**
 * The plugin half of {@link LockInvariantUiTest}: while the workspace is locked, nothing a plugin
 * offers puts the vault back on screen, lifts the locked state, writes the vault's file or shows an
 * entry of the locked vault (#301).
 * <p>
 * The core invariant fires action objects out of the host's action package and cannot reach a
 * plugin: a plugin's menu items and buttons are inline listeners in another module. This one drives
 * them the way a user would. Every shipped plugin is installed, a vault holding a known entry is
 * locked, and then every plugin menu item that is ENABLED in that state is clicked, and in every
 * window it opens, every named and enabled button. The four properties are asserted after each
 * click.
 * <p>
 * Which plugins are clicked is decided by the application, not by this class: whatever declares
 * {@code isUsableWithoutAVault()} is enabled while locked, so a plugin that opts in later is
 * measured here by opting in. And the plugins installed are read from the {@code plugins/}
 * directory, so a new plugin joins by existing, the same way a new action joins the core invariant.
 * <p>
 * <b>What this does not cover yet: a file chooser that is approved.</b> A chooser a button opens is
 * cancelled, never answered with a file, so whatever a plugin does with a chosen file while locked
 * is outside this run. For the two plugins that opt in today that is a small gap: checksum reads
 * and writes checksum files, password-hash opens no chooser at all, which is why it was the second
 * opt-in (#301). It stops being small with the first opt-in whose tools work on files that matter,
 * the Lethenon read-only tools (#450): extending this harness to approve a chooser with a prepared
 * file is part of that opt-in, not a follow-up to it.
 */
class PluginsUsableWhileLockedHoldTheLockUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String SECRET_TITLE = "TheSecret";

	private static final String SECRET_PASSWORD = "the-password";

	/**
	 * The plugin directories that are NOT installed, each with the reason. Every other directory
	 * under {@code plugins/} is installed from its built zip
	 */
	private static final Map<String, String> NOT_INSTALLED = Map.of("menu-designer-plugin",
		"development tooling, not shipped by the installer, and its menu entry exists only with "
			+ "-Dmystic.crypt.ui.menu.designer=true (architecture.md)");

	/**
	 * The menu items that have to be among the clicked ones, so a run that clicks nothing cannot
	 * pass: the two plugins that declare themselves usable without a vault today
	 */
	private static final Set<String> MUST_BE_CLICKED = Set.of("Verify Checksum", "Checksum and MAC",
		"Password Hashing");

	@Test
	@DisplayName("a locked workspace survives everything a plugin offers while locked")
	void aLockedWorkspaceSurvivesEveryPluginEntry() throws Exception
	{
		installEveryShippedPlugin();
		File databaseFile = new File(tempHome, "plugin-invariant.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, SECRET_TITLE, "the-user", SECRET_PASSWORD);
		application.saveDatabase();
		application.lockWorkspace();
		// the unlock prompt is cancelled so the plugins are driven against the locked application
		// itself rather than against a modal dialog
		application.cancelUnlock();

		LockedState locked = new LockedState(application, databaseFile);
		Set<String> clickedItems = new LinkedHashSet<>();
		Set<String> clickedButtons = new LinkedHashSet<>();
		for (JMenuItem item : enabledPluginMenuItems())
		{
			String itemText = GuiActionRunner.execute(item::getText);
			List<JInternalFrame> before = internalFrames();
			SwingUtilities.invokeLater(item::doClick);
			UiTestSpeed.windowManagerSettle();
			OpenedWindows.dismissWhateverOpened();
			locked.assertHolds("menu item '" + itemText + "'");
			clickedItems.add(itemText);

			for (JInternalFrame opened : openedSince(before))
			{
				for (JButton button : namedEnabledButtonsIn(opened))
				{
					String what = "button '" + GuiActionRunner.execute(button::getName) + "' in '"
						+ itemText + "'";
					SwingUtilities.invokeLater(button::doClick);
					UiTestSpeed.windowManagerSettle();
					OpenedWindows.dismissWhateverOpened();
					locked.assertHolds(what);
					clickedButtons.add(itemText + "/" + GuiActionRunner.execute(button::getName));
				}
				GuiActionRunner.execute(opened::dispose);
			}
		}

		assertTrue(clickedItems.containsAll(MUST_BE_CLICKED),
			"the plugins that opt in were not all reached while locked. Clicked: " + clickedItems
				+ ", required: " + MUST_BE_CLICKED);
		assertTrue(
			clickedButtons
				.containsAll(Set.of("Password Hashing/btnHash", "Password Hashing/btnVerify")),
			"the buttons of the second opt-in were not clicked, so its window was not driven. "
				+ "Clicked: " + clickedButtons);

		// the one way out, and the reason this is not a test that passes on a dead application
		SwingUtilities.invokeLater(() -> new LockWorkspaceAction("unlock").actionPerformed(
			new java.awt.event.ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		UiTestSpeed.windowManagerSettle();
		application.unlockWorkspace(MASTER_PASSWORD);
		assertTrue(signedIn(),
			"unlocking with the master password is the single way out of the "
				+ "locked state, after " + clickedItems.size() + " menu items and "
				+ clickedButtons.size() + " buttons were clicked");
	}

	/**
	 * The four properties of the core invariant, measured against the state right after locking
	 */
	private static final class LockedState
	{
		private final ApplicationSteps application;
		private final File databaseFile;
		private final long length;
		private final long lastModified;

		LockedState(final ApplicationSteps application, final File databaseFile)
		{
			this.application = application;
			this.databaseFile = databaseFile;
			this.length = databaseFile.length();
			this.lastModified = databaseFile.lastModified();
		}

		void assertHolds(final String what)
		{
			assertFalse(signedIn(),
				what + " lifted the locked state. Only entering the master password may do that");
			assertFalse(application.vaultIsOnScreen(),
				what + " put the vault back on screen while the workspace is locked");
			assertTrue(
				length == databaseFile.length() && lastModified == databaseFile.lastModified(),
				what + " wrote the vault's file while the workspace is locked");
			assertTrue(ScreenText.nothingOnScreenShows(SECRET_TITLE, SECRET_PASSWORD),
				what + " showed an entry of the locked vault");
		}
	}

	/**
	 * Installs the built zip of every directory under {@code plugins/} except the ones named in
	 * {@link #NOT_INSTALLED}. A missing zip fails the test (TestPrerequisites): a plugin that is
	 * not installed here is a plugin this harness silently does not measure
	 */
	private void installEveryShippedPlugin() throws IOException
	{
		List<Path> zips = new ArrayList<>();
		try (Stream<Path> directories = Files.list(Path.of("plugins")))
		{
			for (Path directory : directories.filter(Files::isDirectory)
				.collect(Collectors.toCollection(TreeSet::new)))
			{
				String name = directory.getFileName().toString();
				if (!NOT_INSTALLED.containsKey(name))
				{
					zips.add(directory.resolve("build/plugin-dist/" + name + "-1.0.0.zip"));
				}
			}
		}
		TestPrerequisites.requireBuiltPluginZips(zips.toArray(Path[]::new));
		File pluginsDir = new File(tempHome, ".config/mystic-crypt-ui/plugins");
		if (!pluginsDir.mkdirs() && !pluginsDir.isDirectory())
		{
			throw new IOException("could not create plugins dir " + pluginsDir);
		}
		for (Path zip : zips)
		{
			Files.copy(zip, pluginsDir.toPath().resolve(zip.getFileName()),
				StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * The leaf items of the "Plugins" menu that are enabled right now, under submenus that are
	 * enabled too - what a user can reach by walking the menu
	 */
	private static List<JMenuItem> enabledPluginMenuItems()
	{
		return GuiActionRunner.execute(() -> {
			JMenuBar menubar = MysticCryptApplicationFrame.getInstance().getJMenuBar();
			List<JMenuItem> items = new ArrayList<>();
			for (MenuElement top : menubar.getSubElements())
			{
				if (top.getComponent()instanceof JMenu menu
					&& MenuId.PLUGINS.propertiesKey().equals(menu.getName()) && menu.isEnabled())
				{
					collectEnabledLeaves(menu, items);
				}
			}
			return items;
		});
	}

	private static void collectEnabledLeaves(final JMenu menu, final List<JMenuItem> items)
	{
		for (Component child : menu.getMenuComponents())
		{
			if (child instanceof JMenu submenu)
			{
				if (submenu.isEnabled())
				{
					collectEnabledLeaves(submenu, items);
				}
			}
			else if (child instanceof JMenuItem item && item.isEnabled())
			{
				items.add(item);
			}
		}
	}

	private static List<JInternalFrame> internalFrames()
	{
		return GuiActionRunner.execute(() -> {
			MysticCryptApplicationFrame applicationFrame = MysticCryptApplicationFrame
				.getInstance();
			if (applicationFrame.getDesktopPanePanel() == null)
			{
				return List.<JInternalFrame> of();
			}
			return List.of(applicationFrame.getDesktopPanePanel().getDesktopPane().getAllFrames());
		});
	}

	private static List<JInternalFrame> openedSince(final List<JInternalFrame> before)
	{
		List<JInternalFrame> opened = new ArrayList<>(internalFrames());
		opened.removeAll(before);
		return opened;
	}

	private static List<JButton> namedEnabledButtonsIn(final Container container)
	{
		return GuiActionRunner.execute(() -> {
			List<JButton> buttons = new ArrayList<>();
			collectNamedEnabledButtons(container, buttons);
			return buttons;
		});
	}

	private static void collectNamedEnabledButtons(final Container container,
		final List<JButton> buttons)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JButton button && button.getName() != null && button.isEnabled()
				&& button.isShowing())
			{
				buttons.add(button);
			}
			if (child instanceof Container nested)
			{
				collectNamedEnabledButtons(nested, buttons);
			}
		}
	}

	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}
}

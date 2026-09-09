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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.ApplicationToggleFullScreenAction;
import io.github.astrapi69.mystic.crypt.action.CloseApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.ExportKeePassDatabaseAction;
import io.github.astrapi69.mystic.crypt.action.ImportKeePassDatabaseAction;
import io.github.astrapi69.mystic.crypt.action.LockWorkspaceAction;
import io.github.astrapi69.mystic.crypt.action.NewApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.NewSettingsFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenDatabaseTreeFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenExistingDatabaseAction;
import io.github.astrapi69.mystic.crypt.action.OpenPrivateKeyAction;
import io.github.astrapi69.mystic.crypt.action.SaveApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.SaveAsApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.SearchApplicationFileAction;

/**
 * The rule the single regression tests belong to: while the workspace is locked, no action puts the
 * vault back on screen, lifts the locked state, or writes the vault's file (#284).
 * <p>
 * Two bypasses were found by asking one action at a time - the vault window that survived locking
 * (#237) and the vault creation that signed the workspace back in (#270). Each has its own test,
 * and each proves one door is shut. This one states the rule, so an action added later is measured
 * against it by existing.
 * <p>
 * The actions are fired through their action OBJECTS, not through the menu. A menu-driven test only
 * says the menu is wired the way the menu is wired; the assertion has to hold for a keyboard
 * shortcut, a persisted menu layout carrying the item, and a caller written next month - which is
 * the same reason the #270 refusal sits in the action rather than at the button.
 * <p>
 * <b>Exactly one exception: unlocking with the correct master password.</b> A second exception is a
 * decision, not a detail: it needs a comment here naming the action and the reason, and it is not
 * added without asking.
 * <p>
 * This is the counterpart of {@link PublicMenuInventoryUiTest}, which answers what MAY be offered
 * without a sign-in. A subset check over names cannot say whether something is refused at the right
 * moment; that question is answered here.
 */
class LockInvariantUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/**
	 * The action classes this test does NOT fire, each with the reason. Everything else in
	 * {@code io.github.astrapi69.mystic.crypt.action} is fired, and
	 * {@link #everyActionInThePackageIsEitherFiredOrExcluded()} fails when a class is added to the
	 * package without landing in one of the two sets - so a new action cannot quietly stay outside
	 * the invariant
	 */
	private static final Map<String, String> NOT_FIRED = Map.of("LockWorkspaceAction",
		"it IS the exception: fired at the end of the invariant to bring the unlock prompt back",
		"SaveBeforeCloseConfirmation",
		"not an action at all: the save-if-dirty question, asked BY the actions that close a vault "
			+ "(#281). It has no actionPerformed to fire");
	// Two entries today, and the second one is a class in the directory that is not an action.
	// This is the second exception list in this class, and it is treated like the first: every
	// further entry needs a reason written here and the maintainer's agreement, not a quiet
	// addition

	/**
	 * Every action in the package, fired through its action object.
	 * <p>
	 * This is deliberately NOT "the actions offered while locked". Those are the first line, and
	 * the menu already disables them; firing only those would measure the first line twice and the
	 * second not at all. The set includes actions whose menu item is disabled in this state - "new
	 * database" and "open the vault window" among them - because that is the whole question: what
	 * happens when something reaches the action anyway
	 */
	private static Map<Class<? extends AbstractAction>, Supplier<AbstractAction>> actionsUnderTest()
	{
		Map<Class<? extends AbstractAction>, Supplier<AbstractAction>> actions = new LinkedHashMap<>();
		actions.put(NewApplicationFileAction.class, () -> new NewApplicationFileAction("new"));
		actions.put(OpenDatabaseTreeFrameAction.class,
			() -> new OpenDatabaseTreeFrameAction("open"));
		actions.put(SaveApplicationFileAction.class, () -> new SaveApplicationFileAction("save"));
		actions.put(SaveAsApplicationFileAction.class,
			() -> new SaveAsApplicationFileAction("save as"));
		actions.put(SearchApplicationFileAction.class,
			() -> new SearchApplicationFileAction("search"));
		actions.put(ExportKeePassDatabaseAction.class,
			() -> new ExportKeePassDatabaseAction("export"));
		actions.put(ImportKeePassDatabaseAction.class,
			() -> new ImportKeePassDatabaseAction("import"));
		actions.put(NewSettingsFrameAction.class, () -> new NewSettingsFrameAction("settings"));
		actions.put(OpenPrivateKeyAction.class,
			() -> new OpenPrivateKeyAction("key", MysticCryptApplicationFrame.getInstance()));
		actions.put(ApplicationToggleFullScreenAction.class,
			() -> new ApplicationToggleFullScreenAction("full screen",
				MysticCryptApplicationFrame.getInstance()));
		// closing a vault and opening another one, both added with the close path (#281, #266).
		// Neither is an exception: closing a LOCKED vault is refused, because saving its pending
		// changes needs the master password locking cleared, and opening another one closes the
		// current one first, so it is refused for the same reason
		actions.put(CloseApplicationFileAction.class,
			() -> new CloseApplicationFileAction("close"));
		actions.put(OpenExistingDatabaseAction.class,
			() -> new OpenExistingDatabaseAction("open a database file"));
		return actions;
	}

	@Test
	@DisplayName("a locked workspace survives every action that can reach the vault")
	void aLockedWorkspaceSurvivesEveryAction() throws Exception
	{
		File databaseFile = new File(tempHome, "invariant.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "TheSecret", "the-user", "the-password");
		application.saveDatabase();
		application.lockWorkspace();
		// locking puts the unlock prompt on screen right away. It is cancelled here so the actions
		// below are fired against the locked application itself rather than against a modal dialog
		application.cancelUnlock();

		long lengthWhileLocked = databaseFile.length();
		long modifiedWhileLocked = databaseFile.lastModified();

		for (Map.Entry<Class<? extends AbstractAction>, Supplier<AbstractAction>> underTest : actionsUnderTest()
			.entrySet())
		{
			String what = underTest.getKey().getSimpleName();
			fireOnTheEventThread(underTest.getValue().get());
			dismissWhateverOpened();

			assertFalse(signedIn(), "'" + what
				+ "' lifted the locked state. Only entering the master password may do that");
			assertFalse(application.vaultIsOnScreen(),
				"'" + what + "' put the vault back on screen while the workspace is locked");
			assertTrue(
				lengthWhileLocked == databaseFile.length()
					&& modifiedWhileLocked == databaseFile.lastModified(),
				"'" + what + "' wrote the vault's file while the workspace is locked");
			assertTrue(nothingOnScreenShows("TheSecret", "the-password"),
				"'" + what + "' showed an entry of the locked vault. The vault panel being gone is "
					+ "one way for the content to be out of reach; a dialog that lists it is a "
					+ "second door to the same content");
		}

		// the one exception, and the reason the rest of this test is not "nothing works".
		// The lock action is a toggle: fired while locked it brings the unlock prompt back
		fireOnTheEventThread(new LockWorkspaceAction("unlock"));
		application.unlockWorkspace(MASTER_PASSWORD);
		assertTrue(signedIn(),
			"unlocking with the master password is the single way out of the locked state - "
				+ "a test that only proves refusals would pass on an application that does "
				+ "nothing at all");
	}

	/**
	 * The set comes from the package directory rather than from a list somebody maintains, so an
	 * action added later joins the invariant by existing.
	 * <p>
	 * <b>Its limit: the directory holds the CORE actions only.</b> A plugin's actions live outside
	 * it and are covered today by the opt-in default - {@code isUsableWithoutAVault()} returns
	 * false, so no plugin is offered without a vault - together with
	 * {@link PublicMenuInventoryUiTest}. The first plugin that opts in has to bring its actions
	 * into this invariant with it; the checksum plugin is the candidate, since it needs no vault to
	 * do its work. Without that step the invariant has a hole exactly where #232 started: a
	 * mechanism that decides on its own what it offers without a sign-in.
	 */
	@Test
	@DisplayName("every action in the package is either fired by the invariant or excluded by name")
	void everyActionInThePackageIsEitherFiredOrExcluded() throws Exception
	{
		java.util.Set<String> inThePackage;
		try (java.util.stream.Stream<java.nio.file.Path> sources = java.nio.file.Files
			.list(java.nio.file.Path.of("src/main/java/io/github/astrapi69/mystic/crypt/action")))
		{
			inThePackage = sources.map(path -> path.getFileName().toString())
				.filter(name -> name.endsWith(".java")).map(name -> name.replace(".java", ""))
				.collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
		}

		java.util.Set<String> fired = actionsUnderTest().keySet().stream().map(Class::getSimpleName)
			.collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));

		java.util.Set<String> uncovered = new java.util.TreeSet<>(inThePackage);
		uncovered.removeAll(fired);
		uncovered.removeAll(NOT_FIRED.keySet());

		assertTrue(uncovered.isEmpty(),
			"these actions are neither fired by the invariant nor excluded by name: " + uncovered
				+ ". An action added to the package joins this test by existing - either fire it "
				+ "in actionsUnderTest(), or name it in NOT_FIRED with the reason");
	}

	/**
	 * Whether none of the given secrets can be read anywhere on screen right now: the labels, text
	 * fields, table cells and tree rows of every showing window.
	 * <p>
	 * The vault panel being off screen is one way for the content to be out of reach. It is not the
	 * only one - a dialog that lists entries reaches the same content through another door - so the
	 * invariant asks the question directly rather than through the panel
	 *
	 * @param secrets
	 *            the entry's title and password, put into the vault before it was locked
	 * @return true if none of them is readable
	 */
	private static boolean nothingOnScreenShows(final String... secrets)
	{
		List<String> onScreen = GuiActionRunner.execute(() -> {
			List<String> texts = new java.util.ArrayList<>();
			for (Window window : Window.getWindows())
			{
				if (window.isShowing())
				{
					collectText(window, texts);
				}
			}
			return texts;
		});
		return java.util.Arrays.stream(secrets)
			.noneMatch(secret -> onScreen.stream().anyMatch(text -> text.contains(secret)));
	}

	private static void collectText(final java.awt.Component component, final List<String> texts)
	{
		switch (component)
		{
			case javax.swing.JLabel label -> texts.add(String.valueOf(label.getText()));
			case javax.swing.text.JTextComponent field -> texts.add(String.valueOf(field.getText()));
			case javax.swing.JTable table -> collectTableText(table, texts);
			case javax.swing.JTree tree -> collectTreeText(tree, texts);
			default ->
			{
			}
		}
		if (component instanceof java.awt.Container container)
		{
			for (java.awt.Component child : container.getComponents())
			{
				collectText(child, texts);
			}
		}
	}

	private static void collectTableText(final javax.swing.JTable table, final List<String> texts)
	{
		for (int row = 0; row < table.getRowCount(); row++)
		{
			for (int column = 0; column < table.getColumnCount(); column++)
			{
				texts.add(String.valueOf(table.getValueAt(row, column)));
			}
		}
	}

	private static void collectTreeText(final javax.swing.JTree tree, final List<String> texts)
	{
		for (int row = 0; row < tree.getRowCount(); row++)
		{
			texts.add(String.valueOf(tree.getPathForRow(row).getLastPathComponent()));
		}
	}

	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}

	private void fireOnTheEventThread(final AbstractAction action)
	{
		SwingUtilities.invokeLater(() -> action
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		UiTestSpeed.windowManagerSettle();
	}

	/**
	 * Closes whatever the action opened - a refusal, a file chooser, a dialog - so the next action
	 * starts from the same state. Cancelling a chooser is deliberate: approving one would measure
	 * what the following action does with a target file, which is a different question
	 */
	private void dismissWhateverOpened()
	{
		GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (!window.isShowing())
				{
					continue;
				}
				JFileChooser fileChooser = fileChooserIn(window);
				if (fileChooser != null)
				{
					fileChooser.cancelSelection();
				}
				if (window instanceof JDialog dialog)
				{
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		});
		UiTestSpeed.windowManagerSettle();
	}

	private static JFileChooser fileChooserIn(final java.awt.Container container)
	{
		for (java.awt.Component component : container.getComponents())
		{
			if (component instanceof JFileChooser fileChooser)
			{
				return fileChooser;
			}
			if (component instanceof java.awt.Container nested)
			{
				JFileChooser found = fileChooserIn(nested);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}

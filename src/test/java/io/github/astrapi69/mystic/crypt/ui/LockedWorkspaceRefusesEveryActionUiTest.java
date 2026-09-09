/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.ApplicationToggleFullScreenAction;
import io.github.astrapi69.mystic.crypt.action.ExportKeePassDatabaseAction;
import io.github.astrapi69.mystic.crypt.action.ImportKeePassDatabaseAction;
import io.github.astrapi69.mystic.crypt.action.LockWorkspaceAction;
import io.github.astrapi69.mystic.crypt.action.NewApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.NewSettingsFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenDatabaseTreeFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenPrivateKeyAction;
import io.github.astrapi69.mystic.crypt.action.SaveApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.SaveAsApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.SearchApplicationFileAction;

/**
 * The rule the locked state has, stated once instead of one door at a time: <b>while the workspace
 * is locked, no action may reach the vault's content or lift the locked state - with exactly one
 * exception, unlocking with the master password</b> (#284).
 * <p>
 * Two bypasses were found by asking one action at a time. #237 left the vault window on screen with
 * a context menu that still copied passwords; #270 let creating a vault set the signed-in state
 * back to true without the master password. Each got a regression test, and each of those proves
 * one door is shut. This one states the rule those doors belong to, so an action written next year
 * is measured against it on the day it is written.
 * <p>
 * WHAT THIS ANSWERS, AND WHAT {@link PublicMenuInventoryUiTest} ANSWERS: that test walks the menu
 * bar and checks that nothing is OFFERED which is not on a list - "may this be offered". A subset
 * check over names is blind to state, and its own Javadoc says so. This test asks the other half -
 * "is it REFUSED at the right moment" - and it asks it of the action objects rather than of the
 * menu, because a disabled menu item is only the first line. The second line has to hold for a
 * keyboard shortcut, a persisted menu layout carrying the item, or a caller added later, which is
 * the same reason the #270 refusal sits in the action instead of at the button.
 * <p>
 * THE ONE EXCEPTION is unlocking with the correct master password. {@link LockWorkspaceAction} is
 * fired here like every other action, and it is not treated as a special case: fired while locked
 * it offers the unlock prompt, and a prompt this test never answers lifts nothing. Any FURTHER
 * exception is a decision, not a detail - it needs a comment here naming the action and the reason,
 * and it is not added without asking.
 */
class LockedWorkspaceRefusesEveryActionUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/**
	 * How long an action gets to do its damage before the state is read. An action that opens a
	 * modal dialog blocks the dispatch thread for as long as the dialog is up, so this cannot wait
	 * for idle - it waits for a fixed moment and then reads the state through the dispatch thread,
	 * which a modal dialog keeps pumping
	 */
	private static final long ACTION_SETTLE_MILLIS = TimeUnit.SECONDS.toMillis(3);

	/**
	 * Every action this application has, by the name it is known under. Built from the action
	 * package rather than from the menu: the menu is one caller among several, and the whole point
	 * of asking the action objects is to cover the callers the menu does not represent.
	 * <p>
	 * A new action belongs in here. That is the maintenance this test asks for, and it is the
	 * reason it catches what a list of menu names cannot
	 *
	 * @return the actions, each behind a supplier so it is built inside the test, after the
	 *         application frame exists
	 */
	static Stream<Arguments> everyApplicationAction()
	{
		Map<String, Supplier<Action>> actions = new LinkedHashMap<>();
		actions.put("New database", () -> new NewApplicationFileAction("New Application"));
		actions.put("Open database view", () -> new OpenDatabaseTreeFrameAction("Open database"));
		actions.put("Save", () -> new SaveApplicationFileAction("Save"));
		actions.put("Save as", () -> new SaveAsApplicationFileAction("Save As"));
		actions.put("Search", () -> new SearchApplicationFileAction("Search"));
		actions.put("Import from KeePass", () -> new ImportKeePassDatabaseAction("Import KeePass"));
		actions.put("Export to KeePass", () -> new ExportKeePassDatabaseAction("Export KeePass"));
		actions.put("Open private key", () -> new OpenPrivateKeyAction("Open private key",
			MysticCryptApplicationFrame.getInstance()));
		actions.put("Settings", () -> new NewSettingsFrameAction("Settings"));
		actions.put("Toggle fullscreen",
			() -> new ApplicationToggleFullScreenAction("Toggle Fullscreen",
				MysticCryptApplicationFrame.getInstance()));
		actions.put("Lock workspace", () -> new LockWorkspaceAction("Lock workspace"));
		return actions.entrySet().stream()
			.map(action -> Arguments.of(action.getKey(), action.getValue()));
	}

	/**
	 * The title of the entry put into the vault before it is locked. Every window on screen is
	 * searched for it afterwards: a panel that is not showing is one way for content to be out of
	 * reach, and a dialog that lists the entry is another way for it not to be
	 */
	private static final String SECRET_ENTRY_TITLE = "locked-invariant-secret-title";

	private static final String SECRET_ENTRY_PASSWORD = "locked-invariant-secret-password";

	@ParameterizedTest(name = "{0} is refused while the workspace is locked")
	@MethodSource("everyApplicationAction")
	@DisplayName("locked, no action reaches the vault, lifts the lock, or writes the file")
	void aLockedWorkspaceRefusesTheAction(final String actionName,
		final Supplier<Action> actionSupplier) throws IOException
	{
		File databaseFile = new File(tempHome, "lock-invariant.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, SECRET_ENTRY_TITLE, "someone",
			SECRET_ENTRY_PASSWORD);
		application.saveDatabase();
		application.lockWorkspace();
		byte[] vaultFileBeforeTheAction = Files.readAllBytes(databaseFile.toPath());

		fireOffTheDispatchThread(actionSupplier.get());

		assertFalse(signedIn(),
			actionName + " lifted the locked state. Only unlocking with the master password may "
				+ "do that, and no password was entered");
		assertFalse(application.vaultIsOnScreen(),
			actionName + " put the vault back on screen while the workspace is locked. The entries "
				+ "are readable and their context menu still copies passwords - that is #237 "
				+ "through another door");
		assertTrue(
			whatIsOnScreen().stream().noneMatch(
				text -> text.contains(SECRET_ENTRY_TITLE) || text.contains(SECRET_ENTRY_PASSWORD)),
			actionName + " showed an entry of the locked vault. The vault panel being gone is one "
				+ "way for the content to be out of reach; a dialog that lists it is a second door "
				+ "to the same content");
		assertArrayEquals(vaultFileBeforeTheAction, Files.readAllBytes(databaseFile.toPath()),
			actionName + " wrote the vault's file while the workspace is locked. A locked "
				+ "workspace has no master password in memory, so what it writes is not what the "
				+ "file held");
	}

	/**
	 * Every piece of text a person could read right now: the labels, text fields, table cells and
	 * tree rows of every window that is showing.
	 * <p>
	 * Read through the dispatch thread, which a modal dialog keeps pumping, so this also sees the
	 * dialog an action has just put up and is still blocked on
	 *
	 * @return the readable text of everything on screen
	 */
	private static List<String> whatIsOnScreen()
	{
		return GuiActionRunner.execute(() -> {
			List<String> texts = new ArrayList<>();
			for (Window window : Window.getWindows())
			{
				if (window.isShowing())
				{
					collectText(window, texts);
				}
			}
			return texts;
		});
	}

	private static void collectText(final Component component, final List<String> texts)
	{
		switch (component)
		{
			case JLabel label -> texts.add(String.valueOf(label.getText()));
			case JTextComponent textComponent -> texts.add(String.valueOf(textComponent.getText()));
			case JTable table -> collectTableText(table, texts);
			case JTree tree -> collectTreeText(tree, texts);
			default ->
			{
			}
		}
		if (component instanceof Container container)
		{
			for (Component child : container.getComponents())
			{
				collectText(child, texts);
			}
		}
	}

	private static void collectTableText(final JTable table, final List<String> texts)
	{
		for (int row = 0; row < table.getRowCount(); row++)
		{
			for (int column = 0; column < table.getColumnCount(); column++)
			{
				texts.add(String.valueOf(table.getValueAt(row, column)));
			}
		}
	}

	private static void collectTreeText(final JTree tree, final List<String> texts)
	{
		for (int row = 0; row < tree.getRowCount(); row++)
		{
			texts.add(String.valueOf(tree.getPathForRow(row).getLastPathComponent()));
		}
	}

	/**
	 * Fires the action the way a keyboard shortcut would, off the test thread, and gives it a
	 * moment to act.
	 * <p>
	 * {@code invokeLater} rather than {@code GuiActionRunner.execute}: an action that opens a modal
	 * dialog does not return until the dialog is dismissed, and a test thread waiting for it would
	 * never reach its assertions
	 *
	 * @param action
	 *            the action to fire
	 */
	private void fireOffTheDispatchThread(final Action action)
	{
		SwingUtilities.invokeLater(() -> action
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		org.assertj.swing.timing.Pause.pause(ACTION_SETTLE_MILLIS);
	}

	/**
	 * The signed-in flag, read through the dispatch thread
	 *
	 * @return whether the workspace is unlocked
	 */
	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}
}

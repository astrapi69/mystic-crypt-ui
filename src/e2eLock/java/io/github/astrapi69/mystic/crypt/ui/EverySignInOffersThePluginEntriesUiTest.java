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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.CloseApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.OpenExistingDatabaseAction;
import io.github.astrapi69.mystic.crypt.lock.IdleLockWatchdog;

/**
 * Every way back into a sign-in offers the plugins again (#1006).
 * <p>
 * The lock, the close of a database and the sign-in dialog of an open put the menu into the state
 * of no vault being open, which disables every plugin entry that needs one. Only the first sign-in
 * after the start used to get them back, because the plugins are loaded after it and their menu is
 * built afresh; an unlock, a second database and a database opened again after a close kept them
 * disabled until the application was restarted. The reference here is the state of the plugin
 * entries after the first sign-in, so the test asks nothing of a plugin entry but what that first
 * sign-in already gave it.
 */
class EverySignInOffersThePluginEntriesUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** One way out of the signed-in state and back into it */
	@FunctionalInterface
	private interface WayBack
	{
		void run(EverySignInOffersThePluginEntriesUiTest test, ApplicationSteps application,
			File firstDatabase, File secondDatabase) throws Exception;
	}

	static Stream<Arguments> waysBackIntoASignIn()
	{
		return Stream.of(
			Arguments.of("Lock workspace, then unlock",
				(WayBack)(test, application, first, second) -> application.lockWorkspace()
					.unlockWorkspace(MASTER_PASSWORD)),
			Arguments.of("the automatic lock, then unlock",
				(WayBack)(test, application, first, second) -> test
					.theAutomaticLockRuns(application)),
			Arguments.of("open a second database while one is open",
				(WayBack)(test, application, first, second) -> test
					.openInTheRunningApplication(second)),
			Arguments.of("close the database, then open it again",
				(WayBack)(test, application, first, second) -> {
					test.closeTheDatabase();
					test.openInTheRunningApplication(first);
				}));
	}

	@ParameterizedTest(name = "{0}: today the plugin entries that need a vault stay disabled")
	@MethodSource("waysBackIntoASignIn")
	void everyWayBackOffersThePluginEntriesAgain(final String wayBack, final WayBack theWayBack)
		throws Exception
	{
		installPluginRequiringItBuilt(CERTIFICATE_ZIP);
		installPluginRequiringItBuilt(CONVERSION_ZIP);
		installPluginRequiringItBuilt(KEYSTORE_ZIP);
		installPluginRequiringItBuilt(CHECKSUM_ZIP);
		File firstDatabase = new File(tempHome, "every-sign-in-first.mcrdb");
		File secondDatabase = new File(tempHome, "every-sign-in-second.mcrdb");
		createDatabaseFileHeadless(firstDatabase, MASTER_PASSWORD);
		createDatabaseFileHeadless(secondDatabase, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(firstDatabase, MASTER_PASSWORD);
		application.showMainFrame();
		Map<String, Boolean> afterTheFirstSignIn = pluginEntries();
		assertTrue(afterTheFirstSignIn.size() >= 4,
			"the precondition: the four installed plugins have entries in the Plugins menu, "
				+ "found " + afterTheFirstSignIn);
		assertTrue(afterTheFirstSignIn.values().stream().allMatch(Boolean::booleanValue),
			"the precondition: the first sign-in offers every plugin entry, "
				+ afterTheFirstSignIn);

		theWayBack.run(this, application, firstDatabase, secondDatabase);

		// pins today's behaviour: the entries that need a vault stay disabled (#1006)
		Map<String, Boolean> afterTheWayBack = pluginEntries();
		assertEquals(Boolean.FALSE,
			afterTheWayBack.get("Plugins > Certificate > Create Certificate..."),
			wayBack + ": today the certificate entry stays disabled (#1006)");
		assertEquals(Boolean.TRUE, afterTheWayBack.get("Plugins > Checksum > Verify Checksum"),
			wayBack + ": the checksum plugin, offered without a vault, stays enabled");
	}

	/**
	 * The idle lock the frame runs, with a clock of its own so the timeout passes at once, and the
	 * unlock after it; the same watchdog AutomaticLockHoldsTheInvariantUiTest drives
	 */
	private void theAutomaticLockRuns(final ApplicationSteps application)
	{
		AtomicLong clock = new AtomicLong();
		IdleLockWatchdog watchdog = new IdleLockWatchdog(
			MysticCryptApplicationFrame.getInstance().asLockableWorkspace(), () -> 15, () -> 15,
			clock::get);
		GuiActionRunner.execute(() -> {
			watchdog.checkNow();
			clock.set(TimeUnit.MINUTES.toMillis(15));
			watchdog.checkNow();
		});
		robot.waitForIdle();
		assertFalse(signedIn(), "the precondition: the idle lock locked the workspace");
		application.unlockWorkspace(MASTER_PASSWORD);
	}

	/** Opens a database file through "Open Database..." of the running application */
	private void openInTheRunningApplication(final File databaseFile)
	{
		SwingUtilities.invokeLater(() -> new OpenExistingDatabaseAction("Open Database File")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		awaitSignInDialog().checkMasterPassword().typeMasterPassword(MASTER_PASSWORD)
			.browseApplicationFile(databaseFile).requireOkEnabled().okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();
		robot.waitForIdle();
	}

	/** Closes the open database the way File > Close does */
	private void closeTheDatabase()
	{
		GuiActionRunner.execute(() -> new CloseApplicationFileAction("Close")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		robot.waitForIdle();
		assertFalse(signedIn(), "the precondition: no database is open after the close");
	}

	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}

	/**
	 * Every entry under the Plugins menu, as its path of texts, and whether it is enabled
	 *
	 * @return the entries in menu order
	 */
	private static Map<String, Boolean> pluginEntries()
	{
		return GuiActionRunner.execute(() -> {
			Map<String, Boolean> entries = new LinkedHashMap<>();
			JMenuBar menuBar = MysticCryptApplicationFrame.getInstance().getJMenuBar();
			for (int index = 0; index < menuBar.getMenuCount(); index++)
			{
				JMenu menu = menuBar.getMenu(index);
				if (menu != null && "Plugins".equals(menu.getText()))
				{
					for (Component child : menu.getMenuComponents())
					{
						collect(child, "Plugins", entries);
					}
				}
			}
			return entries;
		});
	}

	private static void collect(final Component component, final String parentPath,
		final Map<String, Boolean> entries)
	{
		if (!(component instanceof JMenuItem item))
		{
			return;
		}
		String path = parentPath + " > " + item.getText();
		entries.put(path, item.isEnabled());
		if (item instanceof JMenu submenu)
		{
			for (Component child : submenu.getMenuComponents())
			{
				collect(child, path, entries);
			}
		}
	}
}

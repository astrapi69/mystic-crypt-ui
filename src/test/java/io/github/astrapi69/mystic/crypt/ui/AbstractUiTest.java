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

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

import io.github.astrapi69.awt.window.adapter.CloseWindow;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * Base class for AssertJ-Swing end-to-end UI tests of this application.
 * <p>
 * Provides per-test isolation and the shared plumbing every UI test here needs:
 * <ul>
 * <li>redirects {@code user.home} to a fresh temp directory per test, so the app under test never
 * touches the real {@code ~/.config/mystic-crypt-ui} (memoized sign-in, plugins, config)
 * <li>manages the AssertJ-Swing {@link Robot} lifecycle
 * <li>disposes every AWT window in teardown, so no test UI is left behind on screen - this includes
 * the app frame the test never showed, and any dialog a failed test left open
 * <li>resets the {@link MysticCryptApplicationFrame} singleton between tests, since each test boots
 * its own application instance
 * </ul>
 * Timing/pacing of all interactions goes through {@link UiTestSpeed} (fast mode default, demo mode
 * via {@code -Dmystic.crypt.ui.test.mode=demo})
 */
abstract class AbstractUiTest
{

	private String originalUserHome;

	/** Per-test isolated home directory that {@code user.home} points to during the test */
	protected File tempHome;

	protected Robot robot;

	private Thread appThread;

	@BeforeEach
	void setUpUiTest() throws IOException
	{
		// no display, no UI test: skips cleanly on headless CI runners instead of failing there
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"UI tests need a graphical display and are skipped in headless environments");
		originalUserHome = System.getProperty("user.home");
		tempHome = Files.createTempDirectory("mystic-crypt-ui-test-home").toFile();
		System.setProperty("user.home", tempHome.getAbsolutePath());
		resetApplicationFrameSingleton();
		// no FailOnThreadViolationRepaintManager: StartMysticCryptApplication constructs the frame
		// off the EDT (tests mirror that), so enforcing EDT-only construction would fail every test
		// for a pre-existing app characteristic unrelated to what the tests check
		robot = BasicRobot.robotWithNewAwtHierarchy();
	}

	@AfterEach
	void tearDownUiTest() throws InterruptedException
	{
		if (robot == null)
		{
			// setup was skipped by the headless assumption - nothing to tear down
			return;
		}
		try
		{
			// let the application thread finish its frame construction first: after the sign-in
			// dialog closes, the MysticCryptApplicationFrame constructor keeps building the full
			// UI on that thread - disposing windows underneath it (or starting the next test
			// while it still runs) causes cross-test flakiness in a shared JVM
			if (appThread != null)
			{
				appThread.join(15000);
			}
			disposeAllWindows();
		}
		finally
		{
			// NOT robot.cleanUp(): the plain cleanUp also disposes windows via a window-closing
			// path, and the application frame is configured with EXIT_ON_CLOSE - that kills the
			// whole test JVM mid-teardown (the test then shows up as "skipped"). All windows are
			// already disposed above, with the close operation defused in launch
			robot.cleanUpWithoutDisposingWindows();
			resetApplicationFrameSingleton();
			System.setProperty("user.home", originalUserHome);
		}
	}

	/**
	 * Launches the application the same way {@code StartMysticCryptApplication.main} does, on a
	 * background thread (the constructor blocks on the modal sign-in dialog), and returns the steps
	 * object for the sign-in dialog once it is showing
	 *
	 * @return steps for the "Enter your credentials" sign-in dialog
	 */
	protected SignInDialogSteps launchApplication()
	{
		return new SignInDialogSteps(robot, launchApplicationAndFindSignInDialog());
	}

	private DialogFixture launchApplicationAndFindSignInDialog()
	{
		appThread = new Thread(MysticCryptApplicationFrame::new, "mystic-crypt-app-under-test");
		appThread.setDaemon(true);
		appThread.start();

		DialogFixture signInDialog = WindowFinder
			.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
			{
				@Override
				protected boolean isMatching(Dialog dialog)
				{
					// dialog.getTitle() is set by the JDialog constructor before the app thread
					// finishes assembling and showing the content, so also require isShowing() -
					// otherwise this can match a dialog whose content pane is still empty
					return "Enter your credentials".equals(dialog.getTitle()) && dialog.isShowing();
				}
			}).withTimeout(15, TimeUnit.SECONDS).using(robot);
		// the application frame is configured with EXIT_ON_CLOSE - defuse it for the test, so no
		// window-closing during teardown can call System.exit and kill the test JVM
		GuiActionRunner.execute(() -> {
			MysticCryptApplicationFrame applicationFrame = MysticCryptApplicationFrame
				.getInstance();
			if (applicationFrame != null)
			{
				applicationFrame
					.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
			}
		});
		raiseAndFocus(signInDialog);
		return signInDialog;
	}

	/**
	 * Creates a password-only application database file directly through the persistence layer (no
	 * UI), for tests whose use case STARTS from an already existing database - the UI creation flow
	 * itself is covered by {@code CreateNewDatabaseUiTest}
	 *
	 * @param databaseFile
	 *            the database file to create
	 * @param masterPassword
	 *            the master password
	 */
	protected void createDatabaseFileHeadless(File databaseFile, String masterPassword)
		throws IOException
	{
		if (java.security.Security
			.getProvider(org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			java.security.Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		}
		Files.createFile(databaseFile.toPath());
		io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean masterPwFileModelBean = io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean
			.builder()
			.applicationFileInfo(
				io.github.astrapi69.file.create.model.FileInfo.toFileInfo(databaseFile))
			.selectedApplicationFilePath(databaseFile.getAbsolutePath())
			.masterPw(masterPassword.toCharArray()).withMasterPw(true).withKeyFile(false)
			.minPasswordLength(6).build();
		io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory
			.newApplicationFileWithPassword(masterPwFileModelBean);
	}

	/**
	 * Complete "open an existing database" use case through the UI: launch, check the master
	 * password box, type the password, browse to the database file, click OK - and wait until the
	 * application is signed in
	 *
	 * @return steps for the signed-in application
	 */
	protected ApplicationSteps signInWithExistingDatabase(File databaseFile, String masterPassword)
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword(masterPassword)
			.browseApplicationFile(databaseFile).requireOkEnabled().okAndAwaitSignIn();
		return new ApplicationSteps(robot).awaitSignedIn();
	}

	/**
	 * Raises and focuses the given dialog. On this shared, live desktop display (no isolated Xvfb
	 * in this environment) a window manager focus race can otherwise leave it without OS focus
	 */
	private void raiseAndFocus(DialogFixture dialogFixture)
	{
		dialogFixture.moveToFront();
		dialogFixture.focus();
		UiTestSpeed.windowManagerSettle();
	}

	/** Disposes every AWT window still alive, on the EDT, so nothing stays on screen */
	private void disposeAllWindows()
	{
		GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				// the application frame registers a CloseWindow adapter whose windowClosing AND
				// windowClosed both call System.exit(0) - triggered by dispose(), that kills the
				// test JVM mid-teardown and the finished test gets reported as "skipped"
				for (WindowListener windowListener : window.getWindowListeners())
				{
					if (windowListener instanceof CloseWindow)
					{
						window.removeWindowListener(windowListener);
					}
				}
				window.dispose();
			}
		});
	}

	/**
	 * Nulls {@link MysticCryptApplicationFrame}'s private static {@code instance} field so the next
	 * test boots a completely fresh application (the singleton is only assigned once per JVM
	 * otherwise, and several panels resolve their configuration directory through it)
	 */
	private static void resetApplicationFrameSingleton()
	{
		try
		{
			Field instanceField = MysticCryptApplicationFrame.class.getDeclaredField("instance");
			instanceField.setAccessible(true);
			instanceField.set(null, null);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new IllegalStateException(
				"could not reset MysticCryptApplicationFrame.instance for test isolation",
				exception);
		}
	}
}

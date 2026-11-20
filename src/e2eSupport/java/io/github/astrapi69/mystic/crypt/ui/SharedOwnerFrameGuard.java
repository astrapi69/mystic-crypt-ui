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

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fails a UI test in which a window opened with Swing's shared, invisible owner frame as its owner
 * (#63).
 * <p>
 * A dialog created without a parent belongs to {@code SwingUtilities$SharedOwnerFrame}. Such a
 * dialog opens on the first screen, not on the one the application is on, which is #63. It also
 * makes the UI tests random (#1070): the JDK disposes the shared frame when its last window closes
 * ({@code SharedOwnerFrame.windowClosed}), AssertJ-Swing answers that close by ignoring every
 * window the frame owns at the moment its task runs, and a new ownerless dialog that came in
 * between stays hidden from the robot. A window with the application frame as its owner has neither
 * problem. The guard records every such window as it opens, and the test fails at its end naming
 * each one by class and title, so the dialogs to fix are listed by the suite itself.
 */
final class SharedOwnerFrameGuard implements AWTEventListener
{

	/** The class name of Swing's shared owner frame, package-private in the JDK */
	static final String SHARED_OWNER_FRAME = "javax.swing.SwingUtilities$SharedOwnerFrame";

	private final List<String> windowsOwnedByTheSharedFrame = new CopyOnWriteArrayList<>();

	/** Starts recording, with nothing recorded yet */
	void switchOn()
	{
		windowsOwnedByTheSharedFrame.clear();
		Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.WINDOW_EVENT_MASK);
	}

	/** Stops recording; what was recorded stays for {@link #requireNone()} */
	void switchOff()
	{
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
	}

	@Override
	public void eventDispatched(final AWTEvent event)
	{
		if (event.getID() == WindowEvent.WINDOW_OPENED && event instanceof WindowEvent windowEvent)
		{
			Window window = windowEvent.getWindow();
			if (isOwnedByTheSharedFrame(window))
			{
				windowsOwnedByTheSharedFrame.add(describe(window));
			}
		}
	}

	/**
	 * Whether the given window has Swing's shared owner frame as its owner
	 *
	 * @param window
	 *            the window
	 * @return true if its owner is the shared owner frame
	 */
	static boolean isOwnedByTheSharedFrame(final Window window)
	{
		Window owner = window.getOwner();
		return owner != null && SHARED_OWNER_FRAME.equals(owner.getClass().getName());
	}

	/**
	 * The window by its class and its title, the way the failure names it
	 *
	 * @param window
	 *            the window
	 * @return for example {@code MasterPwFileDialog 'Enter your credentials'}
	 */
	static String describe(final Window window)
	{
		String title = window instanceof Dialog dialog
			? dialog.getTitle()
			: window instanceof Frame frame ? frame.getTitle() : "";
		return window.getClass().getName() + " '" + title + "'";
	}

	/**
	 * The windows recorded so far, each once, in the order they first opened
	 *
	 * @return the descriptions of the recorded windows
	 */
	List<String> recorded()
	{
		return new ArrayList<>(new LinkedHashSet<>(windowsOwnedByTheSharedFrame));
	}

	/**
	 * Prints every recorded window as an {@code [on-screen]} line, which the test result XML keeps
	 * and the CI step "Print what was on screen" puts into the log, without failing the test: the
	 * measurement mode of #63, so one run of all suites lists every dialog to fix
	 *
	 * @param test
	 *            the test the windows opened in
	 */
	void report(final String test)
	{
		for (String window : recorded())
		{
			System.out.println("[on-screen] shared owner frame owns: " + window + " in " + test);
		}
	}

	/**
	 * Fails if any window opened with the shared owner frame as its owner
	 *
	 * @throws AssertionError
	 *             naming every such window
	 */
	void requireNone()
	{
		List<String> recorded = recorded();
		if (!recorded.isEmpty())
		{
			throw new AssertionError("windows opened with Swing's shared owner frame as owner, "
				+ "give each the application frame as parent (#63): " + recorded);
		}
	}
}

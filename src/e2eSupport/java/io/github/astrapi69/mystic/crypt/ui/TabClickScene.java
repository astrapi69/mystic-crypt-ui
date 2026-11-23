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

import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;

/**
 * Where a click on a tab went, read at one moment on the event dispatch thread: the window of the
 * tabbed pane, the tab, the pointer and the active window.
 * <p>
 * A click on a settings tab was lost once in CI after #1074 and #1131, and 70 rounds here did not
 * lose it again (#962). The step that clicks reports two of these when the tab stays unselected,
 * one read right after the click and one when it gives up, so the next lost click says whether the
 * window moved, the pointer missed the tab or another window was active.
 *
 * @param windowName
 *            the name of the window that holds the tabbed pane
 * @param window
 *            its bounds on screen
 * @param tabTitle
 *            the title of the tab that was clicked
 * @param tab
 *            the bounds of that tab on screen
 * @param pointer
 *            where the pointer is on screen
 * @param activeWindowName
 *            the name of the active window, or {@code null} when no window of this application is
 *            active
 */
record TabClickScene(String windowName, Rectangle window, String tabTitle, Rectangle tab,
	Point pointer, String activeWindowName) {

	/**
	 * Reads the scene of a click on the tab of the given title
	 *
	 * @param tabs
	 *            the tabbed pane, showing
	 * @param tabTitle
	 *            the title of the clicked tab
	 * @return the scene as it is now
	 */
	static TabClickScene read(final JTabbedPane tabs, final String tabTitle)
	{
		return GuiActionRunner.execute(() -> {
			Window window = SwingUtilities.getWindowAncestor(tabs);
			Rectangle tabInPane = tabs.getUI().getTabBounds(tabs, tabs.indexOfTab(tabTitle));
			Point paneOnScreen = tabs.getLocationOnScreen();
			Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
			return new TabClickScene(window.getName(),
				new Rectangle(window.getLocationOnScreen(), window.getSize()), tabTitle,
				new Rectangle(paneOnScreen.x + tabInPane.x, paneOnScreen.y + tabInPane.y,
					tabInPane.width, tabInPane.height),
				MouseInfo.getPointerInfo().getLocation(), active == null ? null : active.getName());
		});
	}

	/**
	 * Bounds on screen as the failure messages write them
	 *
	 * @param bounds
	 *            the bounds
	 * @return {@code (x,y) widthxheight}
	 */
	static String where(final Rectangle bounds)
	{
		return "(" + bounds.x + "," + bounds.y + ") " + bounds.width + "x" + bounds.height;
	}

	@Override
	public String toString()
	{
		return "dialog '" + windowName + "' at " + where(window) + ", tab '" + tabTitle + "' at "
			+ where(tab) + ", pointer at (" + pointer.x + "," + pointer.y + "), active window "
			+ (activeWindowName == null ? "none" : "'" + activeWindowName + "'");
	}
}

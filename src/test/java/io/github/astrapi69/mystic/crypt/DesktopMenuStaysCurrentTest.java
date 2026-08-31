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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertSame;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code DesktopMenu.getMenubar()} used to answer with the menu bar from the moment it was
 * constructed - a {@code final} field in the library base class that {@code setJMenuBar(...)} never
 * updates (#98). Every persisted layout swaps in a different {@code JMenuBar}, so the field goes
 * stale the first time that happens; this pins that {@link DesktopMenu#getMenubar()} instead
 * follows whatever the frame is actually showing.
 */
class DesktopMenuStaysCurrentTest
{

	@Test
	@DisplayName("getMenubar answers with the frame's current menu bar, not the one from construction")
	void getMenubarAnswersWithTheFramesCurrentMenuBarNotTheOneFromConstruction()
	{
		JFrame frame = new JFrame();
		DesktopMenu menu = new DesktopMenu(frame);
		frame.setJMenuBar(menu.getMenubar());
		JMenuBar constructedWith = menu.getMenubar();

		JMenuBar replacement = new JMenuBar();
		frame.setJMenuBar(replacement);

		assertSame(replacement, menu.getMenubar(),
			"getMenubar still answers with the bar from construction, not the one the frame shows");
		assertSame(frame.getJMenuBar(), menu.getMenubar(),
			"getMenubar and the frame disagree on which menu bar is current");
	}

	@Test
	@DisplayName("before the frame has any menu bar, the constructor's own bar is still answered")
	void beforeTheFrameHasAnyMenuBarTheConstructorsOwnBarIsStillAnswered()
	{
		JFrame frameWithNoMenuBarYet = new JFrame();
		DesktopMenu menu = new DesktopMenu(frameWithNoMenuBarYet);

		assertSame(menu.getMenubar(), menu.getMenubar(),
			"with no menu bar on the frame yet, getMenubar must still answer consistently");
	}

}

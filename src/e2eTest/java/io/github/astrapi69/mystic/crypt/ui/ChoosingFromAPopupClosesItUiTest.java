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

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The step that chooses an item from a context menu leaves no menu behind, as a click does (#1097).
 * <p>
 * A click on a menu item closes the menu before the item's action runs. The step used to fire the
 * item's action only, so the menu stayed open until a window lost the focus, which on a display
 * where the next dialog is not activated in time it never did: the next step then found the old
 * menu's item, clicked it a second time, and its next click came within the double click interval
 * of the tree's click listener and counted as the second half of a double click.
 */
class ChoosingFromAPopupClosesItUiTest extends AbstractUiTest
{

	@Test
	@DisplayName("after the step chose an item its action ran once and the menu is gone")
	void afterChoosingAnItem_theActionRanOnce_andTheMenuIsGone()
	{
		AtomicInteger actions = new AtomicInteger();
		JPopupMenu popup = new JPopupMenu();
		JMenuItem item = new JMenuItem("choose me");
		item.addActionListener(event -> actions.incrementAndGet());
		popup.add(item);
		JPanel content = new JPanel();
		JFrame frame = GuiActionRunner.execute(() -> {
			JFrame window = new JFrame("popup host");
			window.setContentPane(content);
			window.setSize(300, 200);
			window.setVisible(true);
			popup.show(content, 10, 10);
			return window;
		});
		robot.waitForIdle();

		new ApplicationSteps(robot).chooseFromShowingPopup("choose me");
		robot.waitForIdle();

		boolean showing = GuiActionRunner.execute(item::isShowing);
		GuiActionRunner.execute(frame::dispose);
		assertEquals(1, actions.get(), "the action of the item ran once");
		assertFalse(showing, "the menu is still showing after one of its items was chosen");
	}
}

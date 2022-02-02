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
package io.github.astrapi69.mystic.crypt.action;

import java.awt.event.ActionEvent;

import javax.swing.*;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.layout.ScreenSizeExtensions;
import io.github.astrapi69.swing.panel.output.ConsolePanel;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;

/**
 * The class {@link OpenConsoleFrameAction}.
 */
public class OpenConsoleFrameAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new new action.
	 *
	 * @param name
	 *            the name
	 */
	public OpenConsoleFrameAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		// create internal frame
		final JInternalFrame consoleInternalFrame = JComponentFactory.newInternalFrame("Console",
			true, true, true, true);
		ConsolePanel consolePanel = new ConsolePanel();
		int screenHeight = ScreenSizeExtensions
			.getScreenHeight(MysticCryptApplicationFrame.getInstance());
		int screenWidth = ScreenSizeExtensions
			.getScreenWidth(MysticCryptApplicationFrame.getInstance());
		JInternalFrameExtensions.addComponentToFrame(consoleInternalFrame, consolePanel);
		JInternalFrameExtensions.addJInternalFrame(
			MysticCryptApplicationFrame.getInstance().getMainComponent(), consoleInternalFrame);
		consoleInternalFrame.setSize(screenWidth, (screenHeight / 4));
		consoleInternalFrame.setLocation(0, (screenHeight / 4) * 3);
		consoleInternalFrame.setResizable(false);
		consoleInternalFrame.putClientProperty("dragMode", "fixed");
	}

}

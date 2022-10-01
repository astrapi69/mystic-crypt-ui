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

import javax.swing.AbstractAction;
import javax.swing.JInternalFrame;

import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.ApplicationPanel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.enumtype.FrameMode;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.panel.desktoppane.JDesktopPanePanel;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;

/**
 * The class {@link OpenDatabaseTreeFrameAction}.
 */
public class OpenDatabaseTreeFrameAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new new action.
	 *
	 * @param name
	 *            the name
	 */
	public OpenDatabaseTreeFrameAction(final String name)
	{
		super(name);
	}

	public static void openDatabaseTreeFrame()
	{
		MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
		if (FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
		{
			final ApplicationPanel component = instance.getApplicationPanel();
			// create internal frame
			final JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Key database",
				true, true, true, true);
			JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
			JDesktopPanePanel<ApplicationModelBean> desktopPanePanel = instance
				.getDesktopPanePanel();
			int screenHeight = desktopPanePanel.getDesktopPane().getHeight();
			int screenWidth = desktopPanePanel.getDesktopPane().getWidth();
			internalFrame.setSize(screenWidth, screenHeight);
			internalFrame.setLocation(0, 0);
			internalFrame.setResizable(true);
			JInternalFrameExtensions
				.addJInternalFrame(instance.getDesktopPanePanel().getDesktopPane(), internalFrame);
		}
		else
		{
			instance.switchToApplicationPanel();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		openDatabaseTreeFrame();
	}

}

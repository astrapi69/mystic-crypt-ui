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
import java.beans.PropertyVetoException;
import java.io.Serial;

import javax.swing.*;

import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.ApplicationPanel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.panel.desktoppane.JDesktopPanePanel;
import io.github.astrapi69.swing.util.JInternalFrameExtensions;

/**
 * The class {@link OpenDatabaseTreeFrameAction}.
 */
public class OpenDatabaseTreeFrameAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	@Serial
	private static final long serialVersionUID = 1L;

	/** The title of the database tree's internal frame - also used to find it again */
	private static final String DATABASE_TREE_FRAME_TITLE = "Key database";

	/**
	 * Instantiates a new action
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
			ensureDatabaseTreeFrameOpen(instance);
		}
		else
		{
			instance.switchToApplicationPanel();
		}

	}

	/**
	 * Makes sure the database tree - the whole {@link ApplicationPanel}, tree and content table
	 * alike - is showing as its own internal frame in the desktop pane, bringing an already open
	 * one to the front instead of creating a duplicate.
	 * <p>
	 * Called from {@link MysticCryptApplicationFrame#switchToDesktopPane()} every time the frame
	 * switches into Desktop mode, including when a plugin tool triggers the switch to have
	 * somewhere to put its own window - so the database view never just vanishes, it stays
	 * reachable as its own widget instead of requiring a mode switch back to Panel view
	 *
	 * @param instance
	 *            the application frame
	 */
	public static void ensureDatabaseTreeFrameOpen(final MysticCryptApplicationFrame instance)
	{
		JDesktopPanePanel<ApplicationModelBean> desktopPanePanel = instance.getDesktopPanePanel();
		for (JInternalFrame existing : desktopPanePanel.getDesktopPane().getAllFrames())
		{
			if (DATABASE_TREE_FRAME_TITLE.equals(existing.getTitle()))
			{
				bringToFront(existing);
				return;
			}
		}
		newDatabaseTreeFrame(instance, desktopPanePanel);
	}

	private static void newDatabaseTreeFrame(final MysticCryptApplicationFrame instance,
		final JDesktopPanePanel<ApplicationModelBean> desktopPanePanel)
	{
		final ApplicationPanel component = instance.getApplicationPanel();
		final JInternalFrame internalFrame = JComponentFactory
			.newInternalFrame(DATABASE_TREE_FRAME_TITLE, true, true, true, true);
		JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
		int screenHeight = desktopPanePanel.getDesktopPane().getHeight();
		int screenWidth = desktopPanePanel.getDesktopPane().getWidth();
		internalFrame.setSize(screenWidth, screenHeight);
		internalFrame.setLocation(0, 0);
		internalFrame.setResizable(true);
		JInternalFrameExtensions.addJInternalFrame(desktopPanePanel.getDesktopPane(),
			internalFrame);
	}

	private static void bringToFront(final JInternalFrame internalFrame)
	{
		internalFrame.toFront();
		try
		{
			internalFrame.setSelected(true);
		}
		catch (PropertyVetoException exception)
		{
			// a vetoed selection change is not worth failing this for - the frame is still on top
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

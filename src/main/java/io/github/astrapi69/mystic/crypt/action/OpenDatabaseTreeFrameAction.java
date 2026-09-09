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
import java.util.logging.Level;

import javax.swing.*;

import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.ApplicationPanel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.lock.WorkspaceLockDecision;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.panel.desktoppane.JDesktopPanePanel;
import io.github.astrapi69.swing.util.JInternalFrameExtensions;
import lombok.extern.java.Log;

/**
 * The class {@link OpenDatabaseTreeFrameAction}.
 */
@Log
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

	/**
	 * Puts the vault view back on screen - unless the workspace is locked.
	 * <p>
	 * The lock decision is asked HERE and not only where the menu item is enabled. The menu item is
	 * disabled while locked, which is the first line; measured, clicking it then does nothing. This
	 * is the second, and it holds for a keyboard shortcut, a persisted menu layout carrying the
	 * item, or a caller added later (#285). {@code switchToDesktopPane()} has asked the same
	 * decision since #237 - this is the other path to the same view, and it did not
	 */
	public static void openDatabaseTreeFrame()
	{
		MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
		WorkspaceLockDecision decision = WorkspaceLockDecision.onSwitchToDesktopPane(
			instance.getModelObject().isSignedIn(), instance.getApplicationPanel() != null);
		if (!WorkspaceLockDecision.SHOW_VAULT.equals(decision))
		{
			// No dialog: the menu item is disabled while locked, so a user who did nothing gets no
			// message. No silence either: this branch is only reached when the first line has
			// failed, which is exactly when someone should learn of it. So it goes to the log at
			// warning level, with what triggered the call as far as it is available (#285)
			log.log(Level.WARNING,
				"refused to open the vault view while the workspace is locked, triggered by: "
					+ callerOf(new Throwable().getStackTrace()));
			return;
		}
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

	/**
	 * Takes the database view off the desktop, so that locking removes it rather than leaving it
	 * behind the desktop pane where it stays readable and operable (#237).
	 * <p>
	 * The panel itself is only detached from the internal frame, not discarded: unlocking builds
	 * the view again from the same panel, which is what keeps a node added before locking there
	 * afterwards.
	 *
	 * @param instance
	 *            the application frame
	 */
	public static void closeDatabaseTreeFrame(final MysticCryptApplicationFrame instance)
	{
		JDesktopPanePanel<ApplicationModelBean> desktopPanePanel = instance.getDesktopPanePanel();
		if (desktopPanePanel == null || desktopPanePanel.getDesktopPane() == null)
		{
			return;
		}
		for (JInternalFrame existing : desktopPanePanel.getDesktopPane().getAllFrames())
		{
			if (DATABASE_TREE_FRAME_TITLE.equals(existing.getTitle()))
			{
				existing.getContentPane().removeAll();
				existing.setVisible(false);
				existing.dispose();
				desktopPanePanel.getDesktopPane().remove(existing);
			}
		}
		desktopPanePanel.getDesktopPane().revalidate();
		desktopPanePanel.getDesktopPane().repaint();
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

	/**
	 * The first frame outside this class, as a hint at what asked for the vault view. Only ever
	 * used for the refusal's log line - a caller reaching this branch bypassed the disabled menu
	 * item, and the stack is the only thing that says which one
	 *
	 * @param stack
	 *            the captured stack trace
	 * @return a readable caller, or "unknown" when the stack holds nothing outside this class
	 */
	private static String callerOf(final StackTraceElement[] stack)
	{
		for (StackTraceElement frame : stack)
		{
			if (!OpenDatabaseTreeFrameAction.class.getName().equals(frame.getClassName()))
			{
				return frame.toString();
			}
		}
		return "unknown";
	}

}

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
package io.github.astrapi69.mystic.crypt.action;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.Arrays;

import javax.swing.*;

import io.github.astrapi69.awt.extension.ClipboardExtensions;
import io.github.astrapi69.mystic.crypt.DesktopMenu;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;

/**
 * Locks the workspace: hides the open database behind the neutral desktop pane and disables the
 * editing menus and toolbar, then requires the master password to be re-entered before the content
 * is shown again. The open database stays in memory, so unlocking restores it without reopening the
 * file. Clicking the action again while locked re-opens the unlock prompt.
 */
public class LockWorkspaceAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	@Serial
	private static final long serialVersionUID = 1L;

	public LockWorkspaceAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		if (frame.getModelObject().isSignedIn())
		{
			// The flag goes first, where it used to go last. switchToDesktopPane reads it to tell
			// locking apart from a plugin switching the view, so a frame that does not know yet
			// that it is locked puts the database view back instead of taking it away (#237).
			//
			// What the old order bought, and what this costs: it was ordered the other way so that
			// an observer would never see "still signed in" while the view was already gone. Now an
			// observer could see "locked" while the view is still there. That window is between two
			// statements of one dispatch on the event dispatch thread, so no reader on that thread
			// can fall into it, and the only production reads off it - onEnableMenu from
			// onAfterInitialize - happen while the frame is being constructed, before locking is
			// reachable at all.
			//
			// One observer of the kind the old comment meant does exist, in the test harness:
			// ApplicationSteps.lockWorkspace polls this flag from the test thread. It therefore
			// waits for the event dispatch thread afterwards, the way unlockWorkspace already had
			// to for the mirror image of this order.
			frame.getModelObject().setSignedIn(false);
			frame.switchToDesktopPane();
			((DesktopMenu)frame.getMenu()).onEnableByPublic();
			// a password copied before locking would otherwise still be there to paste
			ClipboardExtensions.copyToClipboard("");
			// prompt asynchronously so the locked state is fully in effect before the modal blocks
			SwingUtilities.invokeLater(() -> promptForUnlock(frame));
		}
		else
		{
			// already locked - offer the unlock prompt again. Whether there is anything to unlock
			// is promptForUnlock's own guard, not a second state carrier next to signedIn
			promptForUnlock(frame);
		}
	}

	private void promptForUnlock(MysticCryptApplicationFrame frame)
	{
		MasterPwFileModelBean credentials = frame.getModelObject().getMasterPwFileModelBean();
		if (credentials == null)
		{
			return;
		}
		JPasswordField passwordField = new JPasswordField(20);
		passwordField.setName("txtUnlockPassword");
		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(new JLabel("Enter the master password to unlock the workspace:"));
		panel.add(passwordField);

		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, frame, "Unlock workspace", passwordField);
		if (option != JOptionPane.OK_OPTION)
		{
			// cancelled: stay locked
			return;
		}
		char[] entered = passwordField.getPassword();
		if (Arrays.equals(entered, credentials.getMasterPw()))
		{
			frame.getModelObject().setSignedIn(true);
			// back into the view the user chose, not always into the panel view: locking switched
			// to the desktop pane to hide the content, and unlocking has to undo exactly that
			frame.applyViewMode(
				MysticCryptSettings.load(frame.getConfigurationDirectory()).getViewMode());
			((DesktopMenu)frame.getMenu()).onEnableBySignin();
		}
		else
		{
			JOptionPane.showMessageDialog(frame, "Wrong master password.", "Unlock failed",
				JOptionPane.ERROR_MESSAGE);
			SwingUtilities.invokeLater(() -> promptForUnlock(frame));
		}
	}
}

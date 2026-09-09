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
import java.io.Serial;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.vault.VaultCloseSupport;

/**
 * Closes the open vault: asks about unsaved changes, then leaves the application with no vault open
 * (#281).
 * <p>
 * Until this existed, "the vault is closed" was a state the application could not reach while
 * running - the save-if-dirty question was reachable only by ending it.
 */
public class CloseApplicationFileAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new action
	 *
	 * @param name
	 *            the name
	 */
	public CloseApplicationFileAction(final String name)
	{
		super(name);
	}

	/**
	 * Closes the open vault after asking about unsaved changes.
	 * <p>
	 * A cancelled answer leaves everything as it was, which is the point of offering CANCEL at all.
	 * Asked in the ACTION rather than only at the menu item, for the reason every refusal in this
	 * application is: a disabled menu item is the first line, and the second one has to hold for a
	 * keyboard shortcut or a caller added later (#284)
	 *
	 * @param actionEvent
	 *            the event
	 * @return whether the vault was closed
	 */
	public static boolean closeOpenVault(final ActionEvent actionEvent)
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		if (!VaultCloseSupport.aVaultIsOpen(frame.getModelObject()))
		{
			return false;
		}
		if (!frame.getModelObject().isSignedIn())
		{
			// A LOCKED vault is refused rather than closed. Closing asks about unsaved changes and
			// saving them needs the master password, which locking cleared from memory on purpose
			// (#242) - so the question could only ever be answered by discarding, and discarding
			// someone's unsaved changes without a usable alternative is not a choice to put in
			// front of them. Unlocking first makes both answers available again.
			JOptionPane.showMessageDialog(frame,
				Messages.getString("closedatabase.refused.vault.locked",
					"The database is locked. Closing it would have to discard any unsaved changes, "
						+ "because saving them needs the master password and a locked workspace "
						+ "does not keep it. Unlock it first, then close it."),
				Messages.getString("closedatabase.refused.vault.locked.title",
					"The database is locked"),
				JOptionPane.WARNING_MESSAGE);
			return false;
		}
		SaveBeforeCloseConfirmation.Choice choice = SaveBeforeCloseConfirmation.askAndApply(frame,
			frame.getModelObject());
		if (SaveBeforeCloseConfirmation.Choice.CANCELLED.equals(choice))
		{
			return false;
		}
		frame.closeOpenVault();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent actionEvent)
	{
		closeOpenVault(actionEvent);
	}
}

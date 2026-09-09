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

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.vault.VaultCloseSupport;

/**
 * Opens an existing database file: the way back into a vault from the state with none open (#266).
 * <p>
 * After cancelling the sign-in the window is there, the menu bar is there, and every way into a
 * vault was disabled - Exit and restarting the application were the only moves left. Cancelling is
 * not an error state, it is "I do not want to unlock anything right now", and from there opening a
 * database is exactly the action that should be available.
 * <p>
 * The entry called "Open Database" is NOT this: it re-shows a vault that is already open, and
 * clicked with none it used to throw. This is a separate entry with its own name, which is what
 * #266 asks for - building a path into a vault, not enabling that one.
 * <p>
 * With a vault already open this is also the "open another database" caller of the close path
 * (#281): close the current one first, asking about unsaved changes, and only then ask for the next
 * one's credentials. Opening a second vault over the first is what put one vault's entries into
 * another vault's file (#279).
 */
public class OpenExistingDatabaseAction extends AbstractAction
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
	public OpenExistingDatabaseAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent actionEvent)
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		if (VaultCloseSupport.aVaultIsOpen(frame.getModelObject())
			&& !CloseApplicationFileAction.closeOpenVault(actionEvent))
		{
			// the user cancelled the question about the open vault's unsaved changes, so the open
			// vault stays open and nothing is asked about a second one
			return;
		}
		frame.showMasterPwDialog();
		// a cancelled sign-in leaves the model without credentials, and the state stays the public
		// one it already was - the dialog is the only thing that closed
		frame.onEnableMenu();
	}
}

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
import java.util.logging.Level;

import javax.swing.*;

import io.github.astrapi69.awt.extension.ClipboardExtensions;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.DesktopMenu;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;
import io.github.astrapi69.mystic.crypt.lock.MasterPasswordVerifier;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import io.github.astrapi69.mystic.crypt.vault.SecretBuffers;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import lombok.extern.java.Log;

/**
 * Locks the workspace: hides the open database behind the neutral desktop pane and disables the
 * editing menus and toolbar, then requires the master password to be re-entered before the content
 * is shown again. The open database stays in memory, so unlocking restores it without reopening the
 * file. Clicking the action again while locked re-opens the unlock prompt.
 */
@Log
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
			// before the password goes, because afterwards there is nothing to encrypt with.
			// A locked vault is closed again after a while so its decrypted content leaves memory
			// (#242), and a close cannot ask about unsaved changes when it has no way to write
			// them - so locking writes them here, while it still can
			persistPendingChanges(frame.getModelObject());
			forgetTheKeyMaterial(frame.getModelObject().getMasterPwFileModelBean());
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

	/**
	 * Whether the user asked for locking to write pending changes. Off by default (#304): read from
	 * the settings on each lock rather than cached, so a change in the settings dialog takes effect
	 * without a restart
	 *
	 * @return true if locking may write
	 */
	private static boolean savingWhenLockingIsAskedFor()
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		if (frame == null)
		{
			return false;
		}
		return MysticCryptSettings.load(frame.getConfigurationDirectory()).isSaveWhenLocking();
	}

	/** The title of the unlock prompt, also used to find it again when it has to be dismissed */
	private static final String UNLOCK_PROMPT_TITLE = "Unlock workspace";

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
		if (frame.getModelObject().isDirty())
		{
			// says it rather than letting the save-before-close question be the first hint the
			// user ever gets: locking kept the changes on purpose, and that is worth one line
			// (#304). A line, not a dialog - nobody who just typed a master password needs
			// something else to acknowledge
			JLabel waiting = new JLabel(Messages.getString("unlock.unsaved.changes.waiting",
				"Unsaved changes are waiting in this database."));
			waiting.setName("lblUnsavedChangesWaiting");
			panel.add(waiting);
		}
		panel.add(passwordField);

		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, frame, UNLOCK_PROMPT_TITLE, passwordField);
		if (option != JOptionPane.OK_OPTION)
		{
			// cancelled: stay locked
			return;
		}
		if (frame.getModelObject().getMasterPwFileModelBean() != credentials)
		{
			// the vault this prompt belongs to was closed while the prompt was up - the timed close
			// of #242 does that. Accepting the password now would sign the application in over an
			// empty model, so it is refused here as well as dismissed there: the second line, for
			// whatever closes a vault next
			return;
		}
		char[] entered = passwordField.getPassword();
		if (isTheMasterPassword(credentials, entered))
		{
			rememberTheMasterPassword(credentials, entered);
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
		Arrays.fill(entered, '\0');
	}

	/**
	 * Writes pending changes to the vault's file before locking takes the master password away.
	 * <p>
	 * Locking used to leave them pending, which was harmless while a locked vault stayed open
	 * forever. It stopped being harmless when a locked vault started closing itself after a while
	 * (#242): closing drops the model, and a locked workspace has no master password to write the
	 * model with, so anything still pending at that moment would be lost. Saving here is the moment
	 * where writing is still possible at all.
	 * <p>
	 * A failed write leaves the model marked as changed, which is what stops the timed close from
	 * running later - the vault then stays open and decrypted rather than losing the work. That is
	 * the trade this makes on purpose: memory hygiene never costs somebody their entries.
	 *
	 * @param applicationModelBean
	 *            the model being locked
	 */
	private static void persistPendingChanges(final ApplicationModelBean applicationModelBean)
	{
		if (applicationModelBean == null || !applicationModelBean.isDirty()
			|| applicationModelBean.getMasterPwFileModelBean() == null)
		{
			return;
		}
		if (!savingWhenLockingIsAskedFor())
		{
			// the default since #304: a timer does not commit a change the user has not decided
			// about. The change stays in memory, the timed close leaves a dirty vault alone, and
			// the unlock prompt says that something is waiting
			return;
		}
		try
		{
			ApplicationXmlFileStoreWorker.storeApplicationFile(applicationModelBean);
		}
		catch (RuntimeException exception)
		{
			// storeApplicationFile clears the flag before it writes, so a failed write has to put
			// it back: it is what the timed close asks before dropping the model
			applicationModelBean.setDirty(true);
			log.log(Level.WARNING,
				"the pending changes could not be written while locking, so the vault stays open",
				exception);
		}
	}

	/**
	 * Takes the unlock prompt off the screen.
	 * <p>
	 * Called when the vault it belongs to is closed underneath it (#242). A prompt left standing
	 * would ask for the master password of a database that is no longer open, and answering it
	 * correctly would put the application into the signed-in state over an empty model - the
	 * improvised state move #270 was
	 */
	public static void dismissUnlockPrompt()
	{
		for (java.awt.Window window : java.awt.Window.getWindows())
		{
			if (window instanceof java.awt.Dialog dialog && dialog.isShowing()
				&& UNLOCK_PROMPT_TITLE.equals(dialog.getTitle()))
			{
				dialog.setVisible(false);
				dialog.dispose();
			}
		}
	}

	/**
	 * Takes the KEY MATERIAL out of memory: the master password, the repeat of it, and the private
	 * key of a key-file vault. What stays behind is a verifier that can recognise the password and
	 * cannot produce it (#242).
	 * <p>
	 * The decrypted content is deliberately NOT touched here - see {@code docs/decisions/} for why
	 * the two halves are answered differently, and
	 * {@link io.github.astrapi69.mystic.crypt.lock.IdleLockWatchdog} for what bounds how long the
	 * content stays.
	 * <p>
	 * If the verifier cannot be derived the password is left where it is: locking the workspace
	 * still has to work, and a lock nobody can open is worse than a lock that keeps holding the
	 * secret it used to hold. The other two are wiped in that case anyway - neither is needed to
	 * unlock, so neither has a reason to survive the failure.
	 *
	 * @param credentials
	 *            the credentials of the open database, null when none is open
	 */
	private static void forgetTheKeyMaterial(final MasterPwFileModelBean credentials)
	{
		if (credentials == null)
		{
			return;
		}
		forgetTheRepeatedPassword(credentials);
		forgetThePrivateKey(credentials);
		if (credentials.getMasterPw() == null)
		{
			return;
		}
		try
		{
			credentials.setLockVerifier(MasterPasswordVerifier.of(credentials.getMasterPw()));
		}
		catch (Exception exception)
		{
			log.log(Level.WARNING,
				"the master password stays in memory: its verifier could not be derived",
				exception);
			return;
		}
		Arrays.fill(credentials.getMasterPw(), '\0');
		credentials.setMasterPw(null);
	}

	/**
	 * Overwrites the repeated master password, which is the same secret typed twice.
	 * <p>
	 * It is filled when a database is created or its password changed, and nothing reads it after
	 * that - so a workspace locked in the session that created it carried the master password in a
	 * second field while the first one was being carefully wiped (#242)
	 *
	 * @param credentials
	 *            the credentials of the open database
	 */
	private static void forgetTheRepeatedPassword(final MasterPwFileModelBean credentials)
	{
		if (credentials.getRepeatPw() == null)
		{
			return;
		}
		Arrays.fill(credentials.getRepeatPw(), '\0');
		credentials.setRepeatPw(null);
	}

	/**
	 * Overwrites the encoded private key of a key-file vault.
	 * <p>
	 * This is the other way in, and for a key-only vault it is the ONLY way in - no password is
	 * involved at all. Forgetting the password and keeping the key forgets one of two doors, and
	 * for one kind of vault it forgets the wrong one. Unlocking does not need it: the file is read
	 * again from the key file on the next open, and while the workspace is locked nothing decrypts.
	 * <p>
	 * {@code KeyModel} declares its fields final and {@code @NonNull}, so the array is overwritten
	 * in place and the holder is dropped afterwards - the same shape the close path uses.
	 *
	 * @param credentials
	 *            the credentials of the open database
	 */
	private static void forgetThePrivateKey(final MasterPwFileModelBean credentials)
	{
		KeyModel privateKeyInfo = credentials.getPrivateKeyInfo();
		if (privateKeyInfo == null)
		{
			return;
		}
		SecretBuffers.wipe(privateKeyInfo.getEncoded());
		credentials.setPrivateKeyInfo(null);
	}

	/**
	 * Whether the typed characters open this database. Asks the verifier that locking left behind;
	 * only where there is none - locking could not derive one - does it fall back to comparing
	 * against the password itself
	 *
	 * @param credentials
	 *            the credentials of the open database
	 * @param entered
	 *            what was typed into the unlock dialog
	 * @return true if the workspace may be unlocked
	 */
	private static boolean isTheMasterPassword(final MasterPwFileModelBean credentials,
		final char[] entered)
	{
		MasterPasswordVerifier verifier = credentials.getLockVerifier();
		if (verifier != null)
		{
			return verifier.matches(entered);
		}
		return Arrays.equals(entered, credentials.getMasterPw());
	}

	/**
	 * Puts the master password back where the rest of the application expects it. Saving the
	 * database re-encrypts it with exactly this array (ApplicationXmlFileStoreWorker:145), so
	 * unlocking has to restore what locking took away
	 *
	 * @param credentials
	 *            the credentials of the open database
	 * @param entered
	 *            the password that was just accepted
	 */
	private static void rememberTheMasterPassword(final MasterPwFileModelBean credentials,
		final char[] entered)
	{
		if (credentials.getLockVerifier() == null)
		{
			return;
		}
		credentials.setMasterPw(entered.clone());
		// the verifier is PBKDF2 output over the master password; dropping the reference leaves it
		// in the heap for the collector to get to eventually, which is not the same as erasing it
		credentials.getLockVerifier().wipe();
		credentials.setLockVerifier(null);
	}
}

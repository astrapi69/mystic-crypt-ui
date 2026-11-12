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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.Serial;
import java.util.Arrays;
import java.util.logging.Level;

import javax.swing.*;

import io.github.astrapi69.awt.extension.ClipboardExtensions;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.DesktopMenu;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;
import io.github.astrapi69.mystic.crypt.lock.KeyFileVerifier;
import io.github.astrapi69.mystic.crypt.lock.MasterPasswordVerifier;
import io.github.astrapi69.mystic.crypt.lock.UnlockCheck;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.secret.SecretBuffers;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import lombok.extern.java.Log;

/**
 * Locks the workspace: hides the open database behind the neutral desktop pane and disables the
 * editing menus and toolbar, then requires the factors the database was opened with - its master
 * password, its key file read from the disk again, or both (#880) - before the content is shown
 * again. The open database stays in memory, so unlocking restores it without reopening the file.
 * Clicking the action again while locked re-opens the unlock prompt.
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
			// the plugins that keep entries in the vault drop what they hold first, while the
			// vault is still unlocked (#675)
			frame.announceVaultClosing();
			persistPendingChanges(frame.getModelObject());
			forgetTheKeyMaterial(frame.getModelObject().getMasterPwFileModelBean());
			frame.getModelObject().setSignedIn(false);
			closeTheWindowsOnTheDesktop(frame);
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
		UnlockFields fields = unlockFields(frame, credentials);
		int option = JOptionPaneExtensions.getSelectedOption(fields.panel(),
			JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, frame, UNLOCK_PROMPT_TITLE,
			fields.password() != null ? fields.password() : fields.keyFile());
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
		char[] entered = fields.password() == null ? null : fields.password().getPassword();
		File keyFile = fields.keyFile() == null || fields.keyFile().getText().isBlank()
			? null
			: new File(fields.keyFile().getText().trim());
		try
		{
			UnlockCheck.Outcome outcome = UnlockCheck.check(credentials, entered, keyFile);
			if (outcome.answer() == UnlockCheck.Answer.UNLOCKS)
			{
				completeTheUnlock(frame, credentials, entered, outcome);
				return;
			}
			JOptionPane.showMessageDialog(frame, refusalOf(outcome, fields),
				Messages.getString("unlock.failed.title", "Unlock failed"),
				JOptionPane.ERROR_MESSAGE);
			SwingUtilities.invokeLater(() -> promptForUnlock(frame));
		}
		finally
		{
			if (entered != null)
			{
				Arrays.fill(entered, '\0');
			}
		}
	}

	/**
	 * The fields of the unlock prompt: a password field when the database was opened with a master
	 * password, a key file field with a chooser when it was opened with a key file, both for a
	 * database opened with both (#880)
	 *
	 * @param panel
	 *            the panel the prompt shows
	 * @param password
	 *            the password field, null for a database without a master password
	 * @param keyFile
	 *            the key file field, null for a database without a key file
	 */
	private record UnlockFields(JPanel panel, JPasswordField password, JTextField keyFile) {
	}

	private static UnlockFields unlockFields(final MysticCryptApplicationFrame frame,
		final MasterPwFileModelBean credentials)
	{
		boolean asksForThePassword = UnlockCheck.asksForThePassword(credentials);
		boolean asksForTheKeyFile = UnlockCheck.asksForTheKeyFile(credentials);
		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(new JLabel(promptOf(asksForThePassword, asksForTheKeyFile)));
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
		JPasswordField passwordField = null;
		if (asksForThePassword)
		{
			passwordField = new JPasswordField(20);
			passwordField.setName("txtUnlockPassword");
			panel.add(passwordField);
		}
		JTextField keyFileField = null;
		if (asksForTheKeyFile)
		{
			keyFileField = keyFileField(credentials);
			panel.add(keyFileRow(frame, keyFileField));
		}
		return new UnlockFields(panel, passwordField, keyFileField);
	}

	private static String promptOf(final boolean asksForThePassword,
		final boolean asksForTheKeyFile)
	{
		if (asksForThePassword && asksForTheKeyFile)
		{
			return Messages.getString("unlock.prompt.both",
				"Enter the master password and choose the key file to unlock the workspace:");
		}
		if (asksForTheKeyFile)
		{
			return Messages.getString("unlock.prompt.key.file",
				"Choose the key file to unlock the workspace:");
		}
		return Messages.getString("unlock.prompt.password",
			"Enter the master password to unlock the workspace:");
	}

	/**
	 * The key file field, preset with the key file the database was opened with, as the sign-in
	 * dialog presets the one chosen last: the file is read from the disk again when the prompt is
	 * confirmed, so a key file that is gone does not unlock. {@code FileInfo.toFile()} rather than
	 * {@code FileFactory.newFileQuietly}, which creates the file when it is missing and would leave
	 * an empty one where the key file was
	 */
	private static JTextField keyFileField(final MasterPwFileModelBean credentials)
	{
		JTextField keyFileField = new JTextField(30);
		keyFileField.setName("txtUnlockKeyFile");
		if (credentials.getKeyFileInfo() != null)
		{
			keyFileField.setText(credentials.getKeyFileInfo().toFile().getAbsolutePath());
		}
		return keyFileField;
	}

	private static JPanel keyFileRow(final MysticCryptApplicationFrame frame,
		final JTextField keyFileField)
	{
		JButton choose = new JButton(
			Messages.getString("unlock.key.file.choose", "Choose key file..."));
		choose.setName("btnUnlockKeyFile");
		choose.addActionListener(event -> {
			JFileChooser chooser = new JFileChooser(
				new File(keyFileField.getText()).getParentFile());
			if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
			{
				keyFileField.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		});
		JPanel row = new JPanel(new BorderLayout(4, 4));
		row.add(new JLabel(Messages.getString("unlock.key.file.label", "Key file:")),
			BorderLayout.WEST);
		row.add(keyFileField, BorderLayout.CENTER);
		row.add(choose, BorderLayout.EAST);
		return row;
	}

	/**
	 * The refusal of the prompt, one and the same for every wrong factor of a database: it names
	 * the factors the prompt asked for, never which of them was wrong (#880). A key file that is
	 * not there is named with the words opening and saving use for it (#937)
	 *
	 * @param outcome
	 *            what the attempt came to
	 * @param fields
	 *            the fields the prompt showed
	 * @return the refusal
	 */
	private static String refusalOf(final UnlockCheck.Outcome outcome, final UnlockFields fields)
	{
		if (outcome.answer() == UnlockCheck.Answer.KEY_FILE_MISSING)
		{
			return outcome.reason();
		}
		if (fields.password() != null && fields.keyFile() != null)
		{
			return Messages.getString("unlock.failed.both",
				"The master password or the key file is wrong.");
		}
		if (fields.keyFile() != null)
		{
			return Messages.getString("unlock.failed.key.file",
				"This key file does not unlock this database.");
		}
		return Messages.getString("unlock.failed.password", "Wrong master password.");
	}

	/**
	 * Puts back what locking took away and shows the content again: the master password where the
	 * database has one, the key just read from the key file where it has one, so the next save and
	 * the plugins find the key where opening put it (#880)
	 */
	private static void completeTheUnlock(final MysticCryptApplicationFrame frame,
		final MasterPwFileModelBean credentials, final char[] entered,
		final UnlockCheck.Outcome outcome)
	{
		if (entered != null)
		{
			rememberTheMasterPassword(credentials, entered);
		}
		if (outcome.privateKey() != null)
		{
			credentials.setPrivateKeyInfo(KeyModelExtensions.toKeyModel(outcome.privateKey()));
		}
		forgetTheKeyFileVerifier(credentials);
		frame.getModelObject().setSignedIn(true);
		// back into the view the user chose, not always into the panel view: locking switched
		// to the desktop pane to hide the content, and unlocking has to undo exactly that
		frame.applyViewMode(
			MysticCryptSettings.load(frame.getConfigurationDirectory()).getViewMode());
		((DesktopMenu)frame.getMenu()).onEnableBySignin();
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
			// the flag needs no repair since #424: storeApplicationFile clears it only after the
			// write returned, so a failed write leaves the vault dirty by itself. The lock is
			// silent about it on purpose - it runs on a timer, with nobody at the screen
			log.log(Level.WARNING,
				"the pending changes could not be written while locking, so the vault stays open",
				exception);
		}
	}

	/**
	 * Closes every window standing on the desktop, because a locked workspace is one whose content
	 * is off the screen.
	 * <p>
	 * A plugin window outlived the lock: the console was measured after locking with its whole
	 * scrollback still on screen, vault paths in it, and its buffer was not reachable to be erased
	 * either, because the streams still pointed into the text area of a window nobody could see
	 * (#375). Closing is the host's half - what a window has to let go of when it closes is its
	 * own, and the console erases its buffer on exactly this event.
	 *
	 * @param frame
	 *            the application frame being locked
	 */
	private static void closeTheWindowsOnTheDesktop(final MysticCryptApplicationFrame frame)
	{
		if (frame.getDesktopPanePanel() == null
			|| frame.getDesktopPanePanel().getDesktopPane() == null)
		{
			return;
		}
		for (javax.swing.JInternalFrame internalFrame : frame.getDesktopPanePanel().getDesktopPane()
			.getAllFrames())
		{
			internalFrame.dispose();
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
		keepAVerifierOfTheKey(credentials);
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
	 * Keeps a verifier of the private key of a database opened with a key file, made before the key
	 * is overwritten: unlocking reads the key file from the disk again and asks this verifier
	 * whether it holds the same key, so the key itself does not stay in memory while the workspace
	 * is locked (#880)
	 *
	 * @param credentials
	 *            the credentials of the open database
	 */
	private static void keepAVerifierOfTheKey(final MasterPwFileModelBean credentials)
	{
		KeyModel privateKeyInfo = credentials.getPrivateKeyInfo();
		if (!credentials.isWithKeyFile() || privateKeyInfo == null)
		{
			return;
		}
		credentials.setKeyFileLockVerifier(KeyFileVerifier.of(privateKeyInfo.getEncoded()));
	}

	/**
	 * Overwrites and drops the key file verifier once the key is back in memory
	 *
	 * @param credentials
	 *            the credentials of the unlocked database
	 */
	private static void forgetTheKeyFileVerifier(final MasterPwFileModelBean credentials)
	{
		if (credentials.getKeyFileLockVerifier() == null)
		{
			return;
		}
		credentials.getKeyFileLockVerifier().wipe();
		credentials.setKeyFileLockVerifier(null);
	}

	/**
	 * Overwrites the encoded private key of a key-file vault.
	 * <p>
	 * This is the other way in, and for a key-only vault it is the ONLY way in - no password is
	 * involved at all. Forgetting the password and keeping the key forgets one of two doors, and
	 * for one kind of vault it forgets the wrong one. Unlocking does not need it: it reads the key
	 * file from the disk again and recognises the key by the verifier kept before this (#880), and
	 * while the workspace is locked nothing decrypts.
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

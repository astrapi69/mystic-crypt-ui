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

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.lock.WorkspaceLockDecision;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.NewMasterPwFileDialog;
import io.github.astrapi69.mystic.crypt.vault.VaultCloseSupport;
import io.github.astrapi69.mystic.crypt.write.DataClass;
import io.github.astrapi69.mystic.crypt.write.OverwriteConfirmation;
import io.github.astrapi69.swing.filechooser.JFileChooserExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

public class NewApplicationFileAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	public NewApplicationFileAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		MysticCryptApplicationFrame mysticCryptApplicationFrame = MysticCryptApplicationFrame
			.getInstance();
		ApplicationModelBean applicationModelBean = mysticCryptApplicationFrame.getModelObject();
		if (!WorkspaceLockDecision
			.mayCreateAVault(VaultCloseSupport.aVaultIsOpen(applicationModelBean)))
		{
			// A LOCKED vault is refused outright, and that is the #270 protection: its master
			// password is not in memory, so there is nothing to save its pending changes with, and
			// carrying on would set the signed-in flag over a vault nobody unlocked.
			//
			// An UNLOCKED one is closed first instead of refused, which is what #281 built the
			// close path for. The refusal message stays for the locked case, and it is shown
			// rather than silently doing nothing - that was the defect in "Lock workspace"
			if (!applicationModelBean.isSignedIn())
			{
				JOptionPane.showMessageDialog(mysticCryptApplicationFrame,
					Messages.getString("newdatabase.refused.vault.locked",
						"A database is open and locked. Creating another one here would put its "
							+ "entries into the new file without its master password ever being "
							+ "entered. Unlock it first, then close it."),
					Messages.getString("newdatabase.refused.vault.open.title",
						"A database is already open"),
					JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (!CloseApplicationFileAction.closeOpenVault(e))
			{
				// the user cancelled the question about the open vault's unsaved changes
				return;
			}
		}
		JFileChooser fileChooser = new JFileChooser(
			mysticCryptApplicationFrame.getConfigurationDirectory());
		fileChooser.setDialogTitle("Specify the database file to save");
		FileNameExtensionFilter fileNameExtensionFilter = new FileNameExtensionFilter(
			"Mystic crypt files (*.mcrdb)", "mcrdb");
		fileChooser.setFileFilter(fileNameExtensionFilter);

		final int returnVal = fileChooser.showSaveDialog(mysticCryptApplicationFrame);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedApplicationFile = JFileChooserExtensions
				.getSelectedFileWithFirstExtension(fileChooser);
			if (!OverwriteConfirmation.allowsWriting(mysticCryptApplicationFrame,
				selectedApplicationFile, DataClass.IRREPLACEABLE))
			{
				// the check below only decided whether to create an empty file first; it stopped
				// nothing, so creating a database onto an existing one took it over (#300)
				return;
			}
			if (!selectedApplicationFile.exists())
			{
				RuntimeExceptionDecorator
					.decorate(() -> FileFactory.newFile(selectedApplicationFile));
			}
			String selectedApplicationFilePath = selectedApplicationFile.getAbsolutePath();
			IModel<MasterPwFileModelBean> model = BaseModel.of(MasterPwFileModelBean.builder()
				.applicationFileInfo(FileInfo.toFileInfo(selectedApplicationFile))
				.selectedApplicationFilePath(selectedApplicationFilePath).minPasswordLength(6)
				.withKeyFile(false).withMasterPw(false).showMasterPw(false).build());
			NewMasterPwFileDialog dialog = new NewMasterPwFileDialog(mysticCryptApplicationFrame,
				"Create your master key", true, model)
			{
				@Override
				protected void onOk(ActionEvent actionEvent)
				{
					super.onOk(actionEvent);
					MasterPwFileModelBean dialogModelObject = this.getModelObject();
					mysticCryptApplicationFrame.getModelObject()
						.setMasterPwFileModelBean(dialogModelObject);
					mysticCryptApplicationFrame.getModelObject().setSignedIn(true);
					mysticCryptApplicationFrame.onEnableMenu();
				}
			};
			dialog.setSize(840, 520);
			dialog.setVisible(true);
		}
	}
}

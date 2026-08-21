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
import java.io.Serial;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.astrapi69.component.model.enumeration.visibility.RenderMode;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;
import io.github.astrapi69.mystic.crypt.eventbus.ApplicationEventBus;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.swing.filechooser.JFileChooserExtensions;

/**
 * Saves the currently open database to a newly chosen {@code .mcrdb} file. The chosen file becomes
 * the model's application file, so it is what the plain "Save" action targets afterwards, and the
 * database is written there with the current master credentials.
 */
public class SaveAsApplicationFileAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	@Serial
	private static final long serialVersionUID = 1L;

	public SaveAsApplicationFileAction(final String name)
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
		JFileChooser fileChooser = new JFileChooser(frame.getConfigurationDirectory());
		fileChooser.setDialogTitle("Save the database as");
		fileChooser
			.setFileFilter(new FileNameExtensionFilter("Mystic crypt files (*.mcrdb)", "mcrdb"));

		if (fileChooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		File selectedFile = JFileChooserExtensions.getSelectedFileWithFirstExtension(fileChooser);
		// retarget the open model to the chosen file, then store there with the current credentials
		MasterPwFileModelBean masterPwFileModelBean = frame.getModelObject()
			.getMasterPwFileModelBean();
		masterPwFileModelBean.setApplicationFileInfo(FileInfo.toFileInfo(selectedFile));
		ApplicationXmlFileStoreWorker.storeApplicationFile(frame.getModelObject());
		ApplicationEventBus.getSaveState().fireEvent(new EventObject<>(RenderMode.VIEWABLE));
	}
}

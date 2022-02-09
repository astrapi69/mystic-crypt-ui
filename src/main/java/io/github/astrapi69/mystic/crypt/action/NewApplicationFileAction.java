package io.github.astrapi69.mystic.crypt.action;

import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.NewMasterPwFileDialog;
import io.github.astrapi69.swing.filechooser.JFileChooserExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

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
			if (!selectedApplicationFile.exists())
			{
				RuntimeExceptionDecorator
					.decorate(() -> FileFactory.newFile(selectedApplicationFile));
			}
			String selectedApplicationFilePath = selectedApplicationFile.getAbsolutePath();
			IModel<MasterPwFileModelBean> model = BaseModel
				.of(MasterPwFileModelBean.builder().applicationFile(selectedApplicationFile)
					.selectedApplicationFilePath(selectedApplicationFilePath).minPasswordLength(6)
					.withKeyFile(false).withMasterPw(false).showMasterPw(false).build());
			NewMasterPwFileDialog dialog = new NewMasterPwFileDialog(mysticCryptApplicationFrame,
				"Create your master key", true, model);
			dialog.setSize(840, 520);
			dialog.setVisible(true);
		}
		else if (returnVal == JFileChooser.CANCEL_OPTION)
		{
			mysticCryptApplicationFrame.getModelObject().setSignedIn(false);
		}
	}
}

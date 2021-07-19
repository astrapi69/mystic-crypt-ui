package io.github.astrapi69.mystic.crypt.panels.privatekey;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;

import io.github.astrapi69.crypto.key.KeySize;
import io.github.astrapi69.layout.ScreenSizeExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.base.PanelDialog;
import io.github.astrapi69.window.adapter.CloseWindow;

public class NewPrivateKeyFileDialog extends PanelDialog<NewPrivateKeyModelBean>
{
	/**
	 * The main method for test this dialog
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args)
	{
		Model<NewPrivateKeyModelBean> model = BaseModel
			.of(NewPrivateKeyModelBean.builder().keySize(KeySize.KEYSIZE_4096).build());
		NewPrivateKeyFileDialog dialog = new NewPrivateKeyFileDialog(null,
			"NewPrivateKeyFileDialog", true, model);
		dialog.addWindowListener(new CloseWindow());
		ScreenSizeExtensions.centralize(dialog, 3, 3);
		dialog.setSize(840, 520);

		dialog.setVisible(true);
	}

	public NewPrivateKeyFileDialog(Frame owner, String title, boolean modal,
		Model<NewPrivateKeyModelBean> model)
	{
		super(owner, title, modal, model);
		ScreenSizeExtensions.centralize(this, 3, 3);
	}

	protected JPanel newContent(Model<NewPrivateKeyModelBean> model)
	{
		return new NewPrivateKeyPanel(model)
		{

			@Override
			protected void onSave(ActionEvent actionEvent)
			{
				super.onSave(actionEvent);
				NewPrivateKeyFileDialog.this.dispose();
			}

			@Override
			protected void onCancel(ActionEvent actionEvent)
			{
				super.onCancel(actionEvent);
				NewPrivateKeyFileDialog.this.dispose();
			}
		};
	}
}

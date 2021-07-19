package io.github.astrapi69.mystic.crypt.panels.pw;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;

import io.github.astrapi69.layout.ScreenSizeExtensions;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.base.PanelDialog;

public class GeneratePasswordDialog extends PanelDialog<GeneratePasswordModelBean>
{
	public GeneratePasswordDialog(Frame owner, String title, boolean modal,
		Model<GeneratePasswordModelBean> model)
	{
		super(owner, title, modal, model);
		ScreenSizeExtensions.centralize(this, 3, 3);
	}

	protected JPanel newContent(Model<GeneratePasswordModelBean> model)
	{
		return new GeneratePasswordPanel(model)
		{
			@Override
			protected void onOk(ActionEvent actionEvent)
			{
				super.onOk(actionEvent);
				GeneratePasswordDialog.this.onOk();
			}

			@Override
			protected void onCancel(ActionEvent actionEvent)
			{
				super.onCancel(actionEvent);
				GeneratePasswordDialog.this.dispose();
			}
		};
	}

	protected void onOk()
	{
		GeneratePasswordDialog.this.dispose();
	}
}


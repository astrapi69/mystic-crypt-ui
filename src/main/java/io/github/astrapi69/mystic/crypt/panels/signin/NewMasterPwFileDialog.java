package io.github.astrapi69.mystic.crypt.panels.signin;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;

import io.github.astrapi69.layout.ScreenSizeExtensions;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.base.PanelDialog;

public class NewMasterPwFileDialog extends PanelDialog<MasterPwFileModelBean>
{
	public NewMasterPwFileDialog(Frame owner, String title, boolean modal,
		Model<MasterPwFileModelBean> model)
	{
		super(owner, title, modal, model);
		ScreenSizeExtensions.centralize(this, 3, 3);
	}

	protected JPanel newContent(Model<MasterPwFileModelBean> model)
	{
		return new NewMasterPwFilePanel(model)
		{
			@Override protected void onOk(ActionEvent actionEvent)
			{
				super.onOk(actionEvent);
				NewMasterPwFileDialog.this.dispose();
			}

			@Override protected void onCancel(ActionEvent actionEvent)
			{
				super.onCancel(actionEvent);
				NewMasterPwFileDialog.this.dispose();
			}
		};
	}
}

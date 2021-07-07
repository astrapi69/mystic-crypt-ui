package io.github.astrapi69.mystic.crypt.panels.signin;

import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.layout.ScreenSizeExtensions;
import io.github.astrapi69.swing.base.PanelDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MasterPwFileDialog extends PanelDialog<MasterPwFileModelBean>
{
	public MasterPwFileDialog(Frame owner, String title, boolean modal,
		Model<MasterPwFileModelBean> model)
	{
		super(owner, title, modal, model);
		ScreenSizeExtensions.centralize(this, 3, 3);
	}

	protected JPanel newContent(Model<MasterPwFileModelBean> model)
	{
		return new MasterPwWithApplicationFilePanel(model)
		{
			@Override protected void onOk(ActionEvent actionEvent)
			{
				super.onOk(actionEvent);
				MasterPwFileDialog.this.dispose();
			}

			@Override protected void onCancel(ActionEvent actionEvent)
			{
				super.onCancel(actionEvent);
				MasterPwFileDialog.this.dispose();
			}
		};
	}
}

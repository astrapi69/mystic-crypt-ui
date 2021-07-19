package io.github.astrapi69.mystic.crypt.panels.privatekey;

import java.awt.*;

import javax.swing.*;

import io.github.astrapi69.layout.ScreenSizeExtensions;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.mystic.crypt.panels.privatekey.NewPrivateKeyFormPanel;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileModelBean;
import io.github.astrapi69.swing.base.PanelDialog;

public class NewPrivateKeyFileDialog extends PanelDialog<NewPrivateKeyModelBean>
{
	public NewPrivateKeyFileDialog(Frame owner, String title, boolean modal,
		Model<NewPrivateKeyModelBean> model)
	{
		super(owner, title, modal, model);
		ScreenSizeExtensions.centralize(this, 3, 3);
	}

	protected JPanel newContent(Model<NewPrivateKeyModelBean> model)
	{
		return new NewPrivateKeyFormPanel();
	}
}

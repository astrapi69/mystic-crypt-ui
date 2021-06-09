package io.github.astrapi69.mystic.crypt.panels.signin;

import de.alpharogroup.layout.ScreenSizeExtensions;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.PanelDialog;

import javax.swing.*;
import java.awt.*;

public class MasterPwFileDialog extends PanelDialog<MasterPwFileModelBean>
{
	public MasterPwFileDialog(Frame owner, String title, boolean modal,
		Model<MasterPwFileModelBean> model)
	{
		super(owner, title, modal, model);
		ScreenSizeExtensions.centralize(this, 3,3);
	}

	protected JPanel newContent(Model<MasterPwFileModelBean> model)
	{
		return new MasterPwFilePanel();
	}
}

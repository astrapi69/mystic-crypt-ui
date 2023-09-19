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
package io.github.astrapi69.mystic.crypt.panel.signin;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;

import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.PanelDialog;
import io.github.astrapi69.awt.screen.ScreenSizeExtensions;

public class MasterPwFileDialog extends PanelDialog<MasterPwFileModelBean>
{
	public MasterPwFileDialog(Frame owner, String title, boolean modal,
		IModel<MasterPwFileModelBean> model)
	{
		super(owner, title, modal, model);
		ScreenSizeExtensions.centralize(this, 3, 3);
	}

	protected JPanel newContent(IModel<MasterPwFileModelBean> model)
	{
		return new MasterPwWithApplicationFilePanel(model)
		{
			@Override
			protected void onOk(ActionEvent actionEvent)
			{
				super.onOk(actionEvent);
				MasterPwFileDialog.this.dispose();
			}

			@Override
			protected void onCancel(ActionEvent actionEvent)
			{
				super.onCancel(actionEvent);
				MasterPwFileDialog.this.dispose();
			}
		};
	}
}

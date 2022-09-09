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
package io.github.astrapi69.mystic.crypt.panel.privatekey;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.JPanel;

import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.PanelDialog;
import io.github.astrapi69.swing.layout.ScreenSizeExtensions;
import io.github.astrapi69.window.adapter.CloseWindow;

public class NewPrivateKeyFileDialog extends PanelDialog<NewPrivateKeyModelBean>
{
	public NewPrivateKeyFileDialog(Frame owner, String title, boolean modal,
		IModel<NewPrivateKeyModelBean> model)
	{
		super(owner, title, modal, model);
		ScreenSizeExtensions.centralize(this, 3, 3);
	}

	/**
	 * The main method for test this dialog
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args)
	{
		IModel<NewPrivateKeyModelBean> model = BaseModel
			.of(NewPrivateKeyModelBean.builder().keySize(KeySize.KEYSIZE_2048).build());
		NewPrivateKeyFileDialog dialog = new NewPrivateKeyFileDialog(null,
			"NewPrivateKeyFileDialog", true, model);
		dialog.addWindowListener(new CloseWindow());
		ScreenSizeExtensions.centralize(dialog, 3, 3);
		dialog.setSize(950, 560);
		dialog.setVisible(true);
	}

	protected JPanel newContent(IModel<NewPrivateKeyModelBean> model)
	{
		return new NewPrivateKeyPanel(model)
		{

			@Override
			protected void onSave(ActionEvent actionEvent)
			{
				super.onSave(actionEvent);
				NewPrivateKeyFileDialog.this.onSave();
			}

			@Override
			protected void onCancel(ActionEvent actionEvent)
			{
				super.onCancel(actionEvent);
				NewPrivateKeyFileDialog.this.dispose();
			}
		};
	}

	protected void onSave()
	{
	}
}

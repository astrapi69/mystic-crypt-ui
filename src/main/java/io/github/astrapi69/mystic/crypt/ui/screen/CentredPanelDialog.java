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
package io.github.astrapi69.mystic.crypt.ui.screen;

import java.awt.Frame;

import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.PanelDialog;

/**
 * A {@link PanelDialog} that centres itself over its owner when it is shown, from the size it has
 * then ({@link ScreenPlacement#centerOverOwner}).
 * <p>
 * The dialogs built on it used to centre themselves in their constructor, while they were still 0
 * by 0 pixels, and their callers gave them their size only afterwards: what sat in the middle of
 * the screen was their upper left corner. Placed when it is shown, a dialog is centred whatever
 * size its caller gave it and however, with {@code setSize} or {@code pack}, so a caller cannot get
 * the order wrong.
 *
 * @param <T>
 *            the type of the model object
 */
public class CentredPanelDialog<T> extends PanelDialog<T>
{

	private static final long serialVersionUID = 1L;

	/**
	 * Creates the dialog; it is placed when it is shown
	 *
	 * @param owner
	 *            the window the dialog belongs to and is centred over
	 * @param title
	 *            the title
	 * @param modal
	 *            whether the dialog is modal
	 * @param model
	 *            the model
	 */
	public CentredPanelDialog(final Frame owner, final String title, final boolean modal,
		final IModel<T> model)
	{
		super(owner, title, modal, model);
	}

	/**
	 * Centres the dialog over its owner before it appears, from the size it has now; a dialog that
	 * is already showing stays where it is
	 *
	 * @param visible
	 *            whether the dialog is to be shown
	 */
	@Override
	public void setVisible(final boolean visible)
	{
		if (visible && !isVisible())
		{
			ScreenPlacement.centerOverOwner(this);
		}
		super.setVisible(visible);
	}
}

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
package de.alpharogroup.mystic.crypt.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JInternalFrame;

import de.alpharogroup.swing.actions.OpenFileAction;
import de.alpharogroup.swing.components.factories.JComponentFactory;

/**
 * The class {@link OpenPrivateKeyAction}.
 */
public class OpenPrivateKeyAction extends OpenFileAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new {@link OpenPrivateKeyAction} object.
	 *
	 * @param name
	 *            the name
	 */
	public OpenPrivateKeyAction(final String name, Component parent)
	{
		super(name, parent);
	}

	@Override
	protected void onApproveOption(File file, ActionEvent actionEvent)
	{
		// create internal frame
		final JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Private key view", true, true,
				true, true);
	}

	@Override
	protected void onCancel(ActionEvent actionEvent)
	{
	}

}
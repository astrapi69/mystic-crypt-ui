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

import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import de.alpharogroup.mystic.crypt.SwingApplication;
import de.alpharogroup.swing.plaf.LookAndFeels;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class ShowHelpDialogAction.
 */
@Slf4j
public class ShowHelpDialogAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new show help dialog action.
	 *
	 * @param name
	 *            the name
	 */
	public ShowHelpDialogAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent event)
	{
		final LookAndFeels currentLaf = SwingApplication.getInstance().getCurrentLookAndFeels();
		final Window helpWindow = SwingApplication.getInstance().getMenu().getHelpWindow();
		helpWindow.setLocationRelativeTo(null);
		try
		{
			UIManager.setLookAndFeel(currentLaf.getLookAndFeelName());
		}
		catch (final Exception e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>"
				+ "<p>" + e.getMessage();
			JOptionPane.showMessageDialog(SwingApplication.getInstance(), htmlMessage, title, JOptionPane.ERROR_MESSAGE);
			log.error(e.getMessage(), e);
		}
		SwingUtilities.updateComponentTreeUI(helpWindow);
	}
}
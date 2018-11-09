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

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.springframework.core.io.Resource;

import de.alpharogroup.mystic.crypt.SpringBootSwingApplication;
import de.alpharogroup.mystic.crypt.help.HelpJFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class ShowLicenseFrameAction.
 */
@Slf4j
public class ShowLicenseFrameAction extends AbstractAction
{

	/** The Constant LICENCE_TITLE. */
	private static final String LICENCE_TITLE = "Licence";

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new show license frame action.
	 *
	 * @param name
	 *            the name
	 */
	public ShowLicenseFrameAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		final String licence = loadLicense();
		final HelpJFrame frame = new HelpJFrame(LICENCE_TITLE, licence);
		frame.setVisible(true);
	}

	/**
	 * Load license.
	 *
	 * @return the string
	 */
	private String loadLicense()
	{
		final Resource resource = de.alpharogroup.mystic.crypt.SpringBootSwingApplication.ctx.getResource("classpath:LICENSE.txt");
		final StringBuilder license = new StringBuilder();
		try(InputStream is = resource.getInputStream())
		{
			String thisLine;
			final BufferedReader br = new BufferedReader(new InputStreamReader(is));
			while ((thisLine = br.readLine()) != null)
			{
				license.append(thisLine);
				license.append("\n");
			}
		}
		catch (final IOException e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>"
				+ "<p>" + e.getMessage();
			JOptionPane.showMessageDialog(SpringBootSwingApplication.getInstance(), htmlMessage, title, JOptionPane.ERROR_MESSAGE);
			log.error(e.getMessage(), e);
		}
		return license.toString();
	}

}
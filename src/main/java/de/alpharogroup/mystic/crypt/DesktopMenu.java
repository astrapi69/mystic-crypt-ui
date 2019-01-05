/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.alpharogroup.mystic.crypt;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.springframework.core.io.Resource;

import de.alpharogroup.mystic.crypt.actions.NewFileConversionInternalFrameAction;
import de.alpharogroup.mystic.crypt.actions.NewKeyGenerationInternalFrameAction;
import de.alpharogroup.mystic.crypt.actions.NewObfuscationInternalFrameAction;
import de.alpharogroup.mystic.crypt.actions.NewObfuscationOperationInternalFrameAction;
import de.alpharogroup.mystic.crypt.actions.OpenPrivateKeyAction;
import de.alpharogroup.swing.actions.ExitApplicationAction;
import de.alpharogroup.swing.base.BaseDesktopMenu;
import de.alpharogroup.swing.menu.MenuExtensions;
import lombok.NonNull;
import lombok.extern.java.Log;

/**
 * The class {@link DesktopMenu}
 */
@SuppressWarnings("serial")
@Log
public class DesktopMenu extends BaseDesktopMenu
{

	/**
	 * Instantiates a new desktop menu.
	 */
	public DesktopMenu(@NonNull Component applicationFrame)
	{
		super(applicationFrame);
	}

	/**
	 * Creates the file menu.
	 *
	 * @param listener
	 *            the listener
	 *
	 * @return the j menu
	 */
	@Override
	protected JMenu newFileMenu(final ActionListener listener)
	{
		final JMenu fileMenu = super.newFileMenu(listener);

		JMenuItem jmi;

		final JMenu keyMenu = new JMenu("Key");
		keyMenu.setMnemonic('K');
		fileMenu.add(keyMenu);

		// New key generation
		jmi = new JMenuItem("New key generation", 'K');
		jmi.addActionListener(new NewKeyGenerationInternalFrameAction("New key generation"));
		MenuExtensions.setCtrlAccelerator(jmi, 'K');
		keyMenu.add(jmi);

		// Open private key
		jmi = new JMenuItem("Open private key", 'e');
		jmi.addActionListener(new OpenPrivateKeyAction("Open private key", getApplicationFrame()));
		MenuExtensions.setCtrlAccelerator(jmi, 'e');
		keyMenu.add(jmi);

		// Separator
		fileMenu.addSeparator();

		final JMenu obfuscationMenu = new JMenu("Obfuscation");
		obfuscationMenu.setMnemonic('O');
		fileMenu.add(obfuscationMenu);

		// New simple obfuscation
		jmi = new JMenuItem("Simple obfuscation", 'S');
		jmi.addActionListener(new NewObfuscationInternalFrameAction("Simple Obfuscation"));
		MenuExtensions.setCtrlAccelerator(jmi, 'S');
		obfuscationMenu.add(jmi);

		// New operated obfuscation
		jmi = new JMenuItem("Operated obfuscation", 'N');
		jmi.addActionListener(
			new NewObfuscationOperationInternalFrameAction("Operated Obfuscation"));
		MenuExtensions.setCtrlAccelerator(jmi, 'N');
		obfuscationMenu.add(jmi);

		// Separator
		fileMenu.addSeparator();

		// Convert der to pem file
		JMenuItem jmiConvert;
		jmiConvert = new JMenuItem("Convert...", 'C');
		jmiConvert.addActionListener(
			new NewFileConversionInternalFrameAction("Convert *.der-file to *.pem-file"));
		MenuExtensions.setCtrlAccelerator(jmiConvert, 'C');
		jmiConvert.setEnabled(true);
		fileMenu.add(jmiConvert);

		// Exit
		JMenuItem jmiExit;
		jmiExit = new JMenuItem("Exit", 'E');
		jmiExit.addActionListener(new ExitApplicationAction("Exit"));
		jmiExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
		fileMenu.add(jmiExit);

		return fileMenu;
	}

	@Override
	protected String newLabelTextApplicationName()
	{
		return Messages.getString("InfoJPanel.application.name.value");
	}

	@Override
	protected String newLabelTextCopyright()
	{
		return Messages.getString("InfoJPanel.copyright.value");
	}

	@Override
	protected String newLabelTextLabelApplicationName()
	{
		return Messages.getString("InfoJPanel.application.name.key");
	}

	@Override
	protected String newLabelTextLabelCopyright()
	{
		return Messages.getString("InfoJPanel.copyright.key");
	}

	@Override
	protected String newLabelTextLabelVersion()
	{
		return Messages.getString("InfoJPanel.version.key");
	}

	@Override
	protected String newLabelTextVersion()
	{
		return Messages.getString("InfoJPanel.version.value");
	}

	@Override
	protected String newTextWarning()
	{
		return Messages.getString("InfoJPanel.warning");
	}

	@Override
	protected String onNewLicenseText()
	{
		final Resource resource = de.alpharogroup.mystic.crypt.SpringBootSwingApplication.ctx
			.getResource("classpath:LICENSE.txt");
		final StringBuilder license = new StringBuilder();
		try (InputStream is = resource.getInputStream())
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
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>" + "<p>"
				+ e.getMessage();
			JOptionPane.showMessageDialog(SpringBootSwingApplication.getInstance(), htmlMessage,
				title, JOptionPane.ERROR_MESSAGE);
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return license.toString();
	}

}
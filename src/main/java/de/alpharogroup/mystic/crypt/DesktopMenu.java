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
package de.alpharogroup.mystic.crypt;

import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.help.CSH;
import javax.help.DefaultHelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.WindowPresentation;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

import de.alpharogroup.lang.ClassExtensions;
import de.alpharogroup.mystic.crypt.actions.NewFileConversionInternalFrameAction;
import de.alpharogroup.mystic.crypt.actions.NewKeyGenerationInternalFrameAction;
import de.alpharogroup.mystic.crypt.actions.NewObfuscationInternalFrameAction;
import de.alpharogroup.mystic.crypt.actions.NewObfuscationOperationInternalFrameAction;
import de.alpharogroup.mystic.crypt.actions.OpenBrowserToDonateAction;
import de.alpharogroup.mystic.crypt.actions.OpenPrivateKeyAction;
import de.alpharogroup.mystic.crypt.actions.ShowHelpDialogAction;
import de.alpharogroup.mystic.crypt.actions.ShowInfoDialogAction;
import de.alpharogroup.mystic.crypt.actions.ShowLicenseFrameAction;
import de.alpharogroup.swing.actions.ExitApplicationAction;
import de.alpharogroup.swing.menu.MenuExtensions;
import de.alpharogroup.swing.plaf.actions.LookAndFeelGTKAction;
import de.alpharogroup.swing.plaf.actions.LookAndFeelMetalAction;
import de.alpharogroup.swing.plaf.actions.LookAndFeelMotifAction;
import de.alpharogroup.swing.plaf.actions.LookAndFeelNimbusAction;
import de.alpharogroup.swing.plaf.actions.LookAndFeelSystemAction;
import lombok.Getter;
import lombok.Setter;

/**
 * The Class DesktopMenu.
 */
public class DesktopMenu extends JMenu
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(DesktopMenu.class.getName());

	private static final long serialVersionUID = 1L;

	/** The file menu. */
	@Getter
	private final JMenu fileMenu;

	/** The help menu. */
	@Getter
	private final JMenu helpMenu;

	/** The help window. */
	@Getter
	@Setter
	private Window helpWindow;

	/** The look and feel menu. */
	@Getter
	private final JMenu lookAndFeelMenu;

	/** The JMenuBar from the DesktopMenu. */
	@Getter
	private final JMenuBar menubar;

	SwingApplication swingApplication;

	/**
	 * Instantiates a new desktop menu.
	 */
	public DesktopMenu()
	{
		menubar = newJMenuBar();
		menubar.add(fileMenu = newFileMenu(e -> logger.debug("filemenu")));
		menubar.add(lookAndFeelMenu = newLookAndFeelMenu(e -> logger.debug("Look and Feel menu")));
		menubar.add(helpMenu = newHelpMenu(e -> logger.debug("Help menu")));
	}

	/**
	 * Creates a new {@link JMenuBar}
	 *
	 * @return the new {@link JMenuBar}
	 */
	protected JMenuBar newJMenuBar() {
		return  new JMenuBar();
	}

	/**
	 * Creates the help menu.
	 *
	 * @param listener
	 *            the listener
	 * @return the j menu
	 */
	private JMenu newHelpMenu(final ActionListener listener)
	{
		// Help menu
		final JMenu menuHelp = new JMenu("Help"); //$NON-NLS-1$
		menuHelp.setMnemonic('H');

		// Help JMenuItems
		// Help content
		final JMenuItem mihHelpContent = new JMenuItem("Content", 'c'); //$NON-NLS-1$
		MenuExtensions.setCtrlAccelerator(mihHelpContent, 'H');

		menuHelp.add(mihHelpContent);
		// found bug with the javax.help
		// Exception in thread "main" java.lang.SecurityException: no manifiest
		// section for signature file entry
		// com/sun/java/help/impl/TagProperties.class
		// Solution is to remove the rsa files from the jar

		final HelpSet hs = getHelpSet();
		final DefaultHelpBroker helpBroker = (DefaultHelpBroker)hs.createHelpBroker();
		final WindowPresentation pres = helpBroker.getWindowPresentation();
		pres.createHelpWindow();
		helpWindow = pres.getHelpWindow();

		helpWindow.setLocationRelativeTo(null);

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (final Exception e1)
		{
			e1.printStackTrace();
		}
		SwingUtilities.updateComponentTreeUI(helpWindow);

		// 2. assign help to components
		CSH.setHelpIDString(mihHelpContent, "Overview");
		// 3. handle events
		final CSH.DisplayHelpFromSource displayHelpFromSource = new CSH.DisplayHelpFromSource(
			helpBroker);
		mihHelpContent.addActionListener(displayHelpFromSource);

		mihHelpContent.addActionListener(new ShowHelpDialogAction("Content"));

		// Donate
		final JMenuItem mihDonate = new JMenuItem(
			Messages.getString("com.find.duplicate.files.menu.item.donate")); //$NON-NLS-1$

		mihDonate.addActionListener(new OpenBrowserToDonateAction("Donate"));
		menuHelp.add(mihDonate);

		// Licence
		final JMenuItem mihLicence = new JMenuItem("Licence"); //$NON-NLS-1$
		mihLicence.addActionListener(new ShowLicenseFrameAction("Licence"));
		menuHelp.add(mihLicence);
		// Info
		final JMenuItem mihInfo = new JMenuItem("Info", 'i'); //$NON-NLS-1$
		MenuExtensions.setCtrlAccelerator(mihInfo, 'I');

		mihInfo.addActionListener(new ShowInfoDialogAction("Info"));
		menuHelp.add(mihInfo);

		return menuHelp;
	}

	/**
	 * Creates the look and feel menu.
	 *
	 * @param listener
	 *            the listener
	 * @return the j menu
	 */
	private JMenu newLookAndFeelMenu(final ActionListener listener)
	{

		final JMenu menuLookAndFeel = new JMenu("Look and Feel");
		menuLookAndFeel.setMnemonic('L');

		// Look and Feel JMenuItems
		// GTK
		JMenuItem jmiPlafGTK;
		jmiPlafGTK = new JMenuItem("GTK", 'g'); //$NON-NLS-1$
		MenuExtensions.setCtrlAccelerator(jmiPlafGTK, 'G');
		jmiPlafGTK
			.addActionListener(new LookAndFeelGTKAction("GTK", SwingApplication.getInstance()));
		menuLookAndFeel.add(jmiPlafGTK);
		// Metal default Metal theme
		JMenuItem jmiPlafMetal;
		jmiPlafMetal = new JMenuItem("Metal", 'm'); //$NON-NLS-1$
		MenuExtensions.setCtrlAccelerator(jmiPlafMetal, 'M');
		jmiPlafMetal.addActionListener(new LookAndFeelMetalAction("Metal", SwingApplication.getInstance()));
		menuLookAndFeel.add(jmiPlafMetal);
		// Metal Ocean theme
		JMenuItem jmiPlafOcean;
		jmiPlafOcean = new JMenuItem("Ocean", 'o'); //$NON-NLS-1$
		MenuExtensions.setCtrlAccelerator(jmiPlafOcean, 'O');
		jmiPlafOcean.addActionListener(new LookAndFeelMetalAction("Ocean", SwingApplication.getInstance()));
		menuLookAndFeel.add(jmiPlafOcean);
		// Motif
		JMenuItem jmiPlafMotiv;
		jmiPlafMotiv = new JMenuItem("Motif", 't'); //$NON-NLS-1$
		MenuExtensions.setCtrlAccelerator(jmiPlafMotiv, 'T');
		jmiPlafMotiv.addActionListener(new LookAndFeelMotifAction("Motif", SwingApplication.getInstance()));
		menuLookAndFeel.add(jmiPlafMotiv);	
		// Nimbus
		JMenuItem jmiPlafNimbus;
		jmiPlafNimbus = new JMenuItem("Nimbus", 'n'); //$NON-NLS-1$
		MenuExtensions.setCtrlAccelerator(jmiPlafNimbus, 'N');
		jmiPlafNimbus
			.addActionListener(new LookAndFeelNimbusAction("Nimbus", SwingApplication.getInstance()));
		menuLookAndFeel.add(jmiPlafNimbus);				
		// Windows
		JMenuItem jmiPlafSystem;
		jmiPlafSystem = new JMenuItem("System", 'd'); //$NON-NLS-1$
		MenuExtensions.setCtrlAccelerator(jmiPlafSystem, 'W');
		jmiPlafSystem
			.addActionListener(new LookAndFeelSystemAction("System", SwingApplication.getInstance()));
		menuLookAndFeel.add(jmiPlafSystem);

		return menuLookAndFeel;

	}

	/**
	 * Gets the help set.
	 *
	 * @return the help set
	 */
	public HelpSet getHelpSet()
	{
		HelpSet hs = null;
		final String filename = "simple-hs.xml";
		final String path = "help/" + filename;
		URL hsURL;
			hsURL = ClassExtensions.getResource(path);
			try
			{
				hs = new HelpSet(ClassExtensions.getClassLoader(), hsURL);
			}
			catch (final HelpSetException e)
			{
				e.printStackTrace();
			}
		return hs;
	}

	/**
	 * Creates the file menu.
	 *
	 * @param listener
	 *            the listener
	 *
	 * @return the j menu
	 */
	private JMenu newFileMenu(final ActionListener listener)
	{
		final JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
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
		jmi.addActionListener(
			new OpenPrivateKeyAction("Open private key", SwingApplication.getInstance()));
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

}
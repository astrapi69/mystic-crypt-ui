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
package io.github.astrapi69.mystic.crypt;

import io.github.astrapi69.mystic.crypt.actions.*;
import io.github.astrapi69.swing.actions.ExitApplicationAction;
import io.github.astrapi69.swing.actions.ToggleFullScreenAction;
import io.github.astrapi69.swing.base.BaseDesktopMenu;
import io.github.astrapi69.swing.layout.ScreenSizeExtensions;
import io.github.astrapi69.swing.menu.MenuFactory;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.springframework.core.io.Resource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

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

	@Override
	protected JMenu newEditMenu(final ActionListener listener) {
		final JMenu editMenu = super.newEditMenu(listener);
		editMenu.add(MenuFactory.newJMenuItem("Verify checksum",
				'V',
				KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.ALT_DOWN_MASK),
				new NewChecksumFrameAction("ChecksumVerifier")));
		return editMenu;
	}
	/**
	 * Creates the file menu.
	 *
	 * @return the j menu
	 */
	@Override
	protected JMenu newFileMenu() {
		final JMenu fileMenu = super.newFileMenu();

		// Open Database
		fileMenu.add(MenuFactory.newJMenuItem("Open Database", 'D', 'D'
				, new OpenDatabaseTreeFrameAction("Open Database")));
		// Main key menu
		final JMenu keyMenu = MenuFactory.newJMenu("Key", 'K');
		fileMenu.add(keyMenu);
		// New key generation
		keyMenu.add(MenuFactory.newJMenuItem("New key generation", 'K', 'K'
				, new NewKeyGenerationInternalFrameAction("New key generation")));
		// Open private key
		keyMenu.add(MenuFactory.newJMenuItem("Open private key", 'e', 'e'
				, new OpenPrivateKeyAction("Open private key", getApplicationFrame())));
		// Separator
		fileMenu.addSeparator();

		final JMenu obfuscationMenu = MenuFactory.newJMenu("Obfuscation", 'O');
		fileMenu.add(obfuscationMenu);

		// New simple obfuscation
		obfuscationMenu.add(MenuFactory.newJMenuItem("Simple obfuscation", 'S', 'S'
				, new NewObfuscationInternalFrameAction("Simple Obfuscation")));
		// New operated obfuscation
		obfuscationMenu.add(MenuFactory.newJMenuItem("Operated obfuscation", 'N', 'N'
				, new NewObfuscationOperationInternalFrameAction("Operated Obfuscation")));

		// Separator
		fileMenu.addSeparator();
		// Convert der to pem file
		JMenuItem jmiConvert = MenuFactory.newJMenuItem("Convert...", 'C', 'C'
				, new NewFileConversionInternalFrameAction("Convert *.der-file to *.pem-file"));
		jmiConvert.setEnabled(true);
		fileMenu.add(jmiConvert);

		// Fullscreen
		fileMenu.add(MenuFactory.newJMenuItem("Toggle Fullscreen",
				'F',
				KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.ALT_DOWN_MASK)
				, new ToggleFullScreenAction("Fullscreen", MysticCryptApplicationFrame.getInstance()) {

					/**
					 * {@inheritDoc}
					 */
					@Override
					public void actionPerformed(final ActionEvent e) {
						ScreenSizeExtensions.toggleFullScreen(MysticCryptApplicationFrame.getInstance());
					}
				}));

		// Exit
		fileMenu.add(MenuFactory.newJMenuItem("Exit", 'E'
				, KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK)
				, new ExitApplicationAction("Exit")));

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
		final Resource resource = SpringBootSwingApplication.ctx
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
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return license.toString();
	}

}

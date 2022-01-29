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

import java.awt.Component;
import java.awt.Event;
import java.awt.Frame;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import io.github.astrapi69.swing.base.BaseMenuId;
import lombok.NonNull;
import lombok.extern.java.Log;

import org.springframework.core.io.Resource;

import io.github.astrapi69.mystic.crypt.action.ApplicationToggleFullScreenAction;
import io.github.astrapi69.mystic.crypt.action.NewChecksumFrameAction;
import io.github.astrapi69.mystic.crypt.action.NewKeyGenerationInternalFrameAction;
import io.github.astrapi69.mystic.crypt.action.NewObfuscationInternalFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenConsoleFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenDatabaseTreeFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenPrivateKeyAction;
import io.github.astrapi69.swing.action.ExitApplicationAction;
import io.github.astrapi69.swing.base.BaseDesktopMenu;
import io.github.astrapi69.swing.dialog.info.InfoDialog;
import io.github.astrapi69.swing.dialog.info.InfoPanel;
import io.github.astrapi69.swing.menu.MenuExtensions;
import io.github.astrapi69.swing.menu.builder.JMenuItemInfo;

/**
 * The class {@link DesktopMenu}
 */
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
	protected JMenu newEditMenu()
	{
		// @formatter:on
		final JMenu editMenu = JMenuItemInfo.builder().text("Edit")
			.mnemonic(MenuExtensions.toMnemonic('E')).name(BaseMenuId.EDIT.propertiesKey()).build()
			.toJMenu();
		JMenuItem verifyChecksum = JMenuItemInfo.builder().text("Verify checksum")
			.mnemonic(MenuExtensions.toMnemonic('V'))
			.keyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.ALT_DOWN_MASK))
			.actionListener(new NewChecksumFrameAction("ChecksumVerifier"))
			.name(MenuId.VERIFY_CHECKSUM.propertiesKey()).build().toJMenuItem();
		// @formatter:off
		editMenu.add(verifyChecksum);
		return editMenu;
	}

	/**
	 * Creates the file menu.
	 *
	 * @return the j menu
	 */
	@Override
	protected JMenu newFileMenu()
	{
		// @formatter:on
		// File
		final JMenu fileMenu = JMenuItemInfo.builder().text("File")
			.mnemonic(MenuExtensions.toMnemonic('F')).name(BaseMenuId.FILE.propertiesKey()).build()
			.toJMenu();
		super.newFileMenu();

		// Open Database
		JMenuItem openDatabaseMenuItem = JMenuItemInfo.builder().text("Open Database")
			.mnemonic(MenuExtensions.toMnemonic('D'))
			.keyStroke(KeyStroke.getKeyStroke('D', Event.CTRL_MASK))
			.actionListener(new OpenDatabaseTreeFrameAction("Open Database"))
			.name(MenuId.OPEN_DATABASE.propertiesKey()).build().toJMenuItem();
		fileMenu.add(openDatabaseMenuItem);
		// @formatter:off
		// Main secret key menu
		// @formatter:on
		final JMenu keyMenu = JMenuItemInfo.builder().text("Secret key")
			.mnemonic(MenuExtensions.toMnemonic('K')).name(MenuId.SECRET_KEY.propertiesKey())
			.build().toJMenu();
		fileMenu.add(keyMenu);
		// @formatter:off
		// @formatter:on
		// New key generation
		JMenuItem newSecretKeyJMenuItem = JMenuItemInfo.builder().text("New key generation")
			.mnemonic(MenuExtensions.toMnemonic('K'))
			.keyStroke(KeyStroke.getKeyStroke('K', Event.CTRL_MASK))
			.actionListener(new NewKeyGenerationInternalFrameAction("New key generation"))
			.name(MenuId.SECRET_KEY_NEW.propertiesKey()).build().toJMenuItem();
		keyMenu.add(newSecretKeyJMenuItem);
		// Open private key
		JMenuItem openPrivateKeyMenuItem = JMenuItemInfo.builder().text("Open private key")
			.mnemonic(MenuExtensions.toMnemonic('e'))
			.keyStroke(KeyStroke.getKeyStroke('e', Event.CTRL_MASK))
			.actionListener(new OpenPrivateKeyAction("Open private key", getApplicationFrame()))
			.name(MenuId.OPEN_PRIVATE_KEY.propertiesKey()).build().toJMenuItem();
		keyMenu.add(openPrivateKeyMenuItem);
		// Separator
		fileMenu.addSeparator();

		final JMenu obfuscationMenu = JMenuItemInfo.builder().text("Obfuscation")
			.mnemonic(MenuExtensions.toMnemonic('O')).name(MenuId.OBFUSCATION.propertiesKey())
			.build().toJMenu();
		fileMenu.add(obfuscationMenu);

		// New simple obfuscation
		JMenuItem simpleObfuscationMenuItem = JMenuItemInfo.builder().text("Simple obfuscation")
			.mnemonic(MenuExtensions.toMnemonic('S'))
			.keyStroke(KeyStroke.getKeyStroke('S', Event.CTRL_MASK))
			.actionListener(new NewObfuscationInternalFrameAction("Simple Obfuscation"))
			.name(MenuId.SIMPLE_OBFUSCATION.propertiesKey()).build().toJMenuItem();

		obfuscationMenu.add(simpleObfuscationMenuItem);
		// New operated obfuscation
		JMenuItem operatedObfuscationMenuItem = JMenuItemInfo.builder().text("Operated obfuscation")
			.mnemonic(MenuExtensions.toMnemonic('N'))
			.keyStroke(KeyStroke.getKeyStroke('N', Event.CTRL_MASK))
			.actionListener(new NewObfuscationInternalFrameAction("Operated Obfuscation"))
			.name(MenuId.OPERATED_OBFUSCATION.propertiesKey()).build().toJMenuItem();
		obfuscationMenu.add(operatedObfuscationMenuItem);

		// Separator
		fileMenu.addSeparator();
		// Convert der to pem file
		JMenuItem convertMenuItem = JMenuItemInfo.builder().text("Convert...")
			.mnemonic(MenuExtensions.toMnemonic('C'))
			.keyStroke(KeyStroke.getKeyStroke('C', Event.CTRL_MASK))
			.actionListener(
				new NewObfuscationInternalFrameAction("Convert *.der-file to *.pem-file"))
			.name(MenuId.CONVERT.propertiesKey()).build().toJMenuItem();
		convertMenuItem.setEnabled(true);
		fileMenu.add(convertMenuItem);

		// Fullscreen
		JMenuItem toggleFullscreenMenuItem = JMenuItemInfo.builder().text("Toggle Fullscreen")
			.mnemonic(MenuExtensions.toMnemonic('T'))
			.keyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.ALT_DOWN_MASK))
			.actionListener(new ApplicationToggleFullScreenAction("Fullscreen",
				MysticCryptApplicationFrame.getInstance()))
			.name(MenuId.TOGGLE_FULLSCREEN.propertiesKey()).build().toJMenuItem();
		fileMenu.add(toggleFullscreenMenuItem);

		// Console
		JMenuItem consoleMenuItem = JMenuItemInfo.builder().text("Console")
			.mnemonic(MenuExtensions.toMnemonic('L'))
			.keyStroke(KeyStroke.getKeyStroke('L', InputEvent.ALT_DOWN_MASK))
			.actionListener(new OpenConsoleFrameAction("Console"))
			.name(MenuId.CONSOLE.propertiesKey()).build().toJMenuItem();
		fileMenu.add(consoleMenuItem);

		// Exit
		JMenuItem exitMenuItem = JMenuItemInfo.builder().text("Exit")
			.mnemonic(MenuExtensions.toMnemonic('E'))
			.keyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK))
			.actionListener(new ExitApplicationAction("Exit"))
				.name(MenuId.EXIT.propertiesKey())
				.build().toJMenuItem();
		fileMenu.add(exitMenuItem);
		// @formatter:off
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

	protected InfoDialog onNewInfoDialog(Frame owner, String title)
	{
		return new InfoDialog(owner, title)
		{

			@Override
			protected InfoPanel newInfoPanel()
			{
				return new InfoPanel()
				{

					@Override
					protected String newLabelTextApplicationName()
					{
						return DesktopMenu.this.newLabelTextApplicationName();
					}

					@Override
					protected String newLabelTextCopyright()
					{
						return DesktopMenu.this.newLabelTextCopyright();
					}

					@Override
					protected String newLabelTextLabelApplicationName()
					{
						return DesktopMenu.this.newLabelTextLabelApplicationName();
					}

					@Override
					protected String newLabelTextLabelCopyright()
					{
						return DesktopMenu.this.newLabelTextLabelCopyright();
					}

					@Override
					protected String newLabelTextLabelVersion()
					{
						return DesktopMenu.this.newLabelTextLabelVersion();
					}

					@Override
					protected String newLabelTextVersion()
					{
						return DesktopMenu.this.newLabelTextVersion();
					}

					@Override
					protected String newTextWarning()
					{
						return DesktopMenu.this.newTextWarning();
					}

				};
			}

			@Override
			protected String newLabelTextPlaceholder()
			{
				return "";
			}

		};
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

	public Map<String, Boolean> getEnabledMenuIdsWithEmptyModel() {
		Map<String, Boolean> menuIds = new LinkedHashMap<>();
		menuIds.put(BaseMenuId.EDIT.propertiesKey(), false);
		menuIds.put(BaseMenuId.FILE.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP_CONTENT.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP_DONATE.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP_LICENSE.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP_INFO.propertiesKey(), true);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL.propertiesKey(), false);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_GTK.propertiesKey(), false);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_METAL.propertiesKey(), false);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_OCEAN.propertiesKey(), false);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_MOTIF.propertiesKey(), false);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_NIMBUS.propertiesKey(), false);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_SYSTEM.propertiesKey(), false);
		menuIds.put(MenuId.VERIFY_CHECKSUM.propertiesKey(), false);
		menuIds.put(MenuId.OPEN_DATABASE.propertiesKey(), true);
		menuIds.put(MenuId.OPEN_DATABASE_TOOL_BAR.propertiesKey(), true);
		menuIds.put(MenuId.SECRET_KEY.propertiesKey(), false);
		menuIds.put(MenuId.SECRET_KEY_NEW.propertiesKey(), false);
		menuIds.put(MenuId.OPEN_PRIVATE_KEY.propertiesKey(), false);
		menuIds.put(MenuId.OBFUSCATION.propertiesKey(), false);
		menuIds.put(MenuId.SIMPLE_OBFUSCATION.propertiesKey(), false);
		menuIds.put(MenuId.OPERATED_OBFUSCATION.propertiesKey(), false);
		menuIds.put(MenuId.CONVERT.propertiesKey(), false);
		menuIds.put(MenuId.TOGGLE_FULLSCREEN.propertiesKey(), false);
		menuIds.put(MenuId.CONSOLE.propertiesKey(), false);
		menuIds.put(MenuId.EXIT.propertiesKey(), true);
		return menuIds;
	}

	public Map<String, Boolean> getEnabledMenuIdsWithExistingModel() {
		Map<String, Boolean> menuIds = new LinkedHashMap<>();
		menuIds.put(BaseMenuId.EDIT.propertiesKey(), true);
		menuIds.put(BaseMenuId.FILE.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP_CONTENT.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP_DONATE.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP_LICENSE.propertiesKey(), true);
		menuIds.put(BaseMenuId.HELP_INFO.propertiesKey(), true);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL.propertiesKey(), true);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_GTK.propertiesKey(), true);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_METAL.propertiesKey(), true);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_OCEAN.propertiesKey(), true);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_MOTIF.propertiesKey(), true);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_NIMBUS.propertiesKey(), true);
		menuIds.put(BaseMenuId.LOOK_AND_FEEL_SYSTEM.propertiesKey(), true);
		menuIds.put(MenuId.VERIFY_CHECKSUM.propertiesKey(), true);
		menuIds.put(MenuId.OPEN_DATABASE.propertiesKey(), true);
		menuIds.put(MenuId.OPEN_DATABASE_TOOL_BAR.propertiesKey(), true);
		menuIds.put(MenuId.SECRET_KEY.propertiesKey(), true);
		menuIds.put(MenuId.SECRET_KEY_NEW.propertiesKey(), true);
		menuIds.put(MenuId.OPEN_PRIVATE_KEY.propertiesKey(), true);
		menuIds.put(MenuId.OBFUSCATION.propertiesKey(), true);
		menuIds.put(MenuId.SIMPLE_OBFUSCATION.propertiesKey(), true);
		menuIds.put(MenuId.OPERATED_OBFUSCATION.propertiesKey(), true);
		menuIds.put(MenuId.CONVERT.propertiesKey(), true);
		menuIds.put(MenuId.TOGGLE_FULLSCREEN.propertiesKey(), true);
		menuIds.put(MenuId.CONSOLE.propertiesKey(), true);
		menuIds.put(MenuId.EXIT.propertiesKey(), true);
		return menuIds;
	}
}

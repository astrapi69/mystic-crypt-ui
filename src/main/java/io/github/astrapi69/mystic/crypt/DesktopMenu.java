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
import java.awt.Frame;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;

import io.github.astrapi69.model.enumtype.visibity.RenderMode;
import io.github.astrapi69.swing.menu.model.KeyStrokeInfo;
import io.github.astrapi69.swing.menu.model.MenuInfo;
import io.github.astrapi69.swing.panel.info.InfoModelBean;
import lombok.NonNull;
import lombok.extern.java.Log;

import io.github.astrapi69.collection.set.SetFactory;
import io.github.astrapi69.design.pattern.observer.event.EventListener;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.design.pattern.observer.event.EventSource;
import io.github.astrapi69.lang.ClassExtensions;
import io.github.astrapi69.mystic.crypt.action.ApplicationToggleFullScreenAction;
import io.github.astrapi69.mystic.crypt.action.NewChecksumFrameAction;
import io.github.astrapi69.mystic.crypt.action.NewFileConversionInternalFrameAction;
import io.github.astrapi69.mystic.crypt.action.NewKeyGenerationInternalFrameAction;
import io.github.astrapi69.mystic.crypt.action.NewObfuscationInternalFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenConsoleFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenDatabaseTreeFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenPrivateKeyAction;
import io.github.astrapi69.mystic.crypt.action.SaveApplicationFileAction;
import io.github.astrapi69.mystic.crypt.eventbus.ApplicationEventBus;
import io.github.astrapi69.swing.action.ExitApplicationAction;
import io.github.astrapi69.swing.base.BaseDesktopMenu;
import io.github.astrapi69.swing.base.BaseMenuId;
import io.github.astrapi69.swing.dialog.info.InfoDialog;
import io.github.astrapi69.swing.dialog.info.InfoPanel;
import io.github.astrapi69.swing.menu.KeyStrokeExtensions;
import io.github.astrapi69.swing.menu.MenuExtensions;
import io.github.astrapi69.swing.menu.ParentMenuResolver;
import io.github.astrapi69.swing.menu.model.MenuItemInfo;

/**
 * The class {@link DesktopMenu}
 */
@Log
public class DesktopMenu extends BaseDesktopMenu implements EventListener<EventObject<RenderMode>>
{
	private Map<String, Boolean> enabledMenuIdsWithExistingModel;

	private Map<String, Boolean> enabledMenuIdsWithEmptyModel;

	/**
	 * Instantiates a new desktop menu.
	 */
	public DesktopMenu(@NonNull Component applicationFrame)
	{
		super(applicationFrame);
		// register as listener...
		final EventSource<EventObject<RenderMode>> eventSource = ApplicationEventBus.getSaveState();
		eventSource.add(this);
	}

	@Override
	protected JMenu newEditMenu()
	{
		// @formatter:on
		final JMenu editMenu = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text(Messages.getString(BaseMenuId.EDIT.propertiesKey()))
				.mnemonic(MenuExtensions.toMnemonic('E')).name(BaseMenuId.EDIT.propertiesKey())
				.build())
			.build().toJMenu();

		JMenuItem verifyChecksum = MenuItemInfo.builder()

			.menuInfo(
				MenuInfo.builder().text(Messages.getString(MenuId.VERIFY_CHECKSUM.propertiesKey()))
					.name(MenuId.VERIFY_CHECKSUM.propertiesKey())
					.mnemonic(MenuExtensions.toMnemonic('V'))
					.keyStrokeInfo(KeyStrokeInfo.toKeyStrokeInfo(
						KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.ALT_DOWN_MASK)))
					.build())

			.actionListener(new NewChecksumFrameAction("ChecksumVerifier"))

			.build().toJMenuItem();

		final JMenu viewModeMenu = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text(Messages.getString(MenuId.VIEW_MODE.propertiesKey()))
				.name(MenuId.VIEW_MODE.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('W'))
				.build())

			.build().toJMenu();
		editMenu.add(viewModeMenu);
		JMenuItem switchDesktopMode = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder()
				.text(Messages.getString(MenuId.VIEW_DESKTOP_MODE.propertiesKey()))
				.name(MenuId.VIEW_DESKTOP_MODE.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('M'))
				.keyStrokeInfo(KeyStrokeInfo.toKeyStrokeInfo(
					KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.ALT_DOWN_MASK)))
				.build())

			.actionListener(event -> {
				MysticCryptApplicationFrame.getInstance().switchToDesktopPane();
			}).build().toJMenuItem();
		viewModeMenu.add(switchDesktopMode);
		JMenuItem switchPanelMode = MenuItemInfo.builder()

			.menuInfo(
				MenuInfo.builder().text(Messages.getString(MenuId.VIEW_PANEL_MODE.propertiesKey()))
					.name(MenuId.VIEW_PANEL_MODE.propertiesKey())
					.mnemonic(MenuExtensions.toMnemonic('Q'))
					.keyStrokeInfo(KeyStrokeInfo.toKeyStrokeInfo(
						KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.ALT_DOWN_MASK)))
					.build())

			.actionListener(event -> {
				MysticCryptApplicationFrame.getInstance().switchToApplicationPanel();
			}).build().toJMenuItem();
		viewModeMenu.add(switchPanelMode);
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
		final JMenu fileMenu = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("File").name(BaseMenuId.FILE.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('F')).build())

			.build().toJMenu();
		// Open Database

		JMenuItem openDatabaseMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Open Database")
				.name(MenuId.OPEN_DATABASE.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('D'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed D")))
				.build())

			.actionListener(new OpenDatabaseTreeFrameAction("Open Database")).build().toJMenuItem();
		fileMenu.add(openDatabaseMenuItem);

		// Save application file
		JMenuItem saveApplicationFileMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Save")
				.name(MenuId.SAVE_APPLICATION_FILE.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('S'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed S")))
				.build())

			.actionListener(new SaveApplicationFileAction("Save")).build().toJMenuItem();
		fileMenu.add(saveApplicationFileMenuItem);

		// @formatter:off
		// Main secret key menu
		// @formatter:on
		final JMenu keyMenu = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Secret key").name(MenuId.SECRET_KEY.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('K')).build())

			.build().toJMenu();
		fileMenu.add(keyMenu);
		// @formatter:off
		// @formatter:on
		// New key generation
		JMenuItem newSecretKeyJMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("New key generation")
				.name(MenuId.SECRET_KEY_NEW.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('K'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed K")))
				.build())

			.actionListener(new NewKeyGenerationInternalFrameAction("New key generation")).build()
			.toJMenuItem();
		keyMenu.add(newSecretKeyJMenuItem);
		// Open private key
		JMenuItem openPrivateKeyMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Open private key")
				.name(MenuId.OPEN_PRIVATE_KEY.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('P'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed P")))
				.build())

			.actionListener(new OpenPrivateKeyAction("Open private key", getApplicationFrame()))
			.build().toJMenuItem();
		keyMenu.add(openPrivateKeyMenuItem);
		// Separator
		fileMenu.addSeparator();

		final JMenu obfuscationMenu = MenuItemInfo.builder()

			.menuInfo(
				MenuInfo.builder().text("Obfuscation").name(MenuId.OBFUSCATION.propertiesKey())
					.mnemonic(MenuExtensions.toMnemonic('O')).build())

			.build().toJMenu();
		fileMenu.add(obfuscationMenu);

		// New simple obfuscation
		JMenuItem simpleObfuscationMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Simple obfuscation")
				.name(MenuId.SIMPLE_OBFUSCATION.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('Y'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed Y")))
				.build())

			.actionListener(new NewObfuscationInternalFrameAction("Simple Obfuscation")).build()
			.toJMenuItem();

		obfuscationMenu.add(simpleObfuscationMenuItem);
		// New operated obfuscation
		JMenuItem operatedObfuscationMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Operated obfuscation")
				.name(MenuId.OPERATED_OBFUSCATION.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('N'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed N")))
				.build())


			.actionListener(new NewObfuscationInternalFrameAction("Operated Obfuscation")).build()
			.toJMenuItem();
		obfuscationMenu.add(operatedObfuscationMenuItem);

		// Separator
		fileMenu.addSeparator();
		// Convert der to pem file
		JMenuItem convertMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Convert...").name(MenuId.CONVERT.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('C'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed C")))
				.build())


			.actionListener(
				new NewFileConversionInternalFrameAction("Convert *.der-file to *.pem-file"))
			.build().toJMenuItem();
		convertMenuItem.setEnabled(true);
		fileMenu.add(convertMenuItem);

		// Fullscreen
		JMenuItem toggleFullscreenMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Toggle Fullscreen")
				.name(BaseMenuId.TOGGLE_FULLSCREEN.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('T'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("alt pressed F11")))
				.build())


			.actionListener(new ApplicationToggleFullScreenAction("Fullscreen",
				MysticCryptApplicationFrame.getInstance()))
			.build().toJMenuItem();
		fileMenu.add(toggleFullscreenMenuItem);

		// Console
		JMenuItem consoleMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Console").name(MenuId.CONSOLE.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('L'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("alt pressed L")))
				.build())


			.actionListener(new OpenConsoleFrameAction("Console")).build().toJMenuItem();
		fileMenu.add(consoleMenuItem);

		// Exit
		JMenuItem exitMenuItem = MenuItemInfo.builder()

			.menuInfo(MenuInfo.builder().text("Exit").name(BaseMenuId.EXIT.propertiesKey())
				.mnemonic(MenuExtensions.toMnemonic('E'))
				.keyStrokeInfo(KeyStrokeInfo
					.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("alt pressed F4")))
				.build())


			.actionListener(new ExitApplicationAction("Exit")).build().toJMenuItem();
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
		return Messages.getString("InfoJPanel.license.information.value");
	}

	protected InfoDialog onNewInfoDialog(Frame owner, String title)
	{
		return new InfoDialog(owner, title)
		{

			@Override
			protected InfoPanel newInfoPanel()
			{
				final InfoModelBean infoModelBean = InfoModelBean.builder()
						.labelApplicationName(Messages.getString("InfoJPanel.application.name.key", "Application name:"))
						.applicationName(Messages.getString("InfoJPanel.application.name.value", "mystic-crypt-ui"))
						.labelCopyright(Messages.getString("InfoJPanel.copyright.key", "Copyright(C):"))
						.copyright(Messages.getString("InfoJPanel.copyright.value", "2016 Asterios Raptis"))
						.labelVersion(Messages.getString("InfoJPanel.version.key", "Version:"))
						.version(Messages.getString("InfoJPanel.version.value", "7.4-SNAPSHOT"))
						.licence(Messages.getString("InfoJPanel.license.information.value", "This Software is licensed under the MIT License"))
						.build();
				return new InfoPanel()
				{

					@Override
					protected String newLabelTextApplicationName()
					{
						return infoModelBean.getApplicationName();
					}

					@Override
					protected String newLabelTextCopyright()
					{
						return infoModelBean.getCopyright();
					}

					@Override
					protected String newLabelTextLabelApplicationName()
					{
						return infoModelBean.getLabelApplicationName();
					}

					@Override
					protected String newLabelTextLabelCopyright()
					{
						return infoModelBean.getLabelCopyright();
					}

					@Override
					protected String newLabelTextLabelVersion()
					{
						return infoModelBean.getLabelVersion();
					}

					@Override
					protected String newLabelTextVersion()
					{
						return infoModelBean.getVersion();
					}

					@Override
					protected String newTextWarning()
					{
						return infoModelBean.getLicence();
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
		final StringBuilder license = new StringBuilder();
		try (InputStream is = ClassExtensions.getResourceAsStream("LICENSE.txt"))
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

	public void onEnableByPublic()
	{
		JMenuBar menubar = getMenubar();
		List<MenuElement> allMenuElements = ParentMenuResolver.getAllMenuElements(menubar, true);
		allMenuElements.forEach(menuElement -> {
			String name = menuElement.getComponent().getName();
			if (getEnabledMenuIdsWithEmptyModel().containsKey(name))
			{
				menuElement.getComponent().setEnabled(enabledMenuIdsWithEmptyModel.get(name));
			}
		});

		final Set<String> disabledToolBarMenus = SetFactory.newHashSet(
				MenuId.LOCK_WORKSPACE_TOOL_BAR.propertiesKey(),
				MenuId.SAVE_APPLICATION_FILE_TOOL_BAR.propertiesKey(),
				MenuId.SEARCH_TOOL_BAR.propertiesKey()
		);
		ApplicationToolbar toolBar = (ApplicationToolbar) MysticCryptApplicationFrame.getInstance().getToolBar();
		if(toolBar != null) {
			Set<JButton> allButtonElements = toolBar.getToolbarButtons();
			allButtonElements.forEach(jButton -> {
				jButton.setEnabled(!disabledToolBarMenus.contains(jButton.getName()));
			});
		}
	}
	
	public void onEnableBySignin()
	{
		if (MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn())
		{
			JMenuBar menubar = getMenubar();
			List<MenuElement> allMenuElements = ParentMenuResolver.getAllMenuElements(menubar,
					true);
			allMenuElements.forEach(menuElement -> {
				String name = menuElement.getComponent().getName();
				if (getEnabledMenuIdsWithExistingModel().containsKey(name))
				{
					menuElement.getComponent()
							.setEnabled(enabledMenuIdsWithExistingModel.get(name));
				}
			});
		}
		final Set<String> disabledToolBarMenus = SetFactory.newHashSet(
				MenuId.SAVE_APPLICATION_FILE.propertiesKey(),
				MenuId.SAVE_APPLICATION_FILE_TOOL_BAR.propertiesKey()

		);
		ApplicationToolbar toolBar = (ApplicationToolbar) MysticCryptApplicationFrame.getInstance().getToolBar();
		Set<JButton> allButtonElements = toolBar.getToolbarButtons();
		allButtonElements.forEach(jButton -> {
			jButton.setEnabled(!disabledToolBarMenus.contains(jButton.getName()));
		});
	}

	public Map<String, Boolean> getEnabledMenuIdsWithEmptyModel() {
		if(enabledMenuIdsWithEmptyModel == null) {
			Set<BaseMenuId> disabledBaseMenus = SetFactory.newHashSet(
					BaseMenuId.EDIT,
					BaseMenuId.LOOK_AND_FEEL,
					BaseMenuId.LOOK_AND_FEEL_GTK,
					BaseMenuId.LOOK_AND_FEEL_METAL,
					BaseMenuId.LOOK_AND_FEEL_OCEAN,
					BaseMenuId.LOOK_AND_FEEL_MOTIF,
					BaseMenuId.LOOK_AND_FEEL_NIMBUS,
					BaseMenuId.LOOK_AND_FEEL_SYSTEM,
					BaseMenuId.TOGGLE_FULLSCREEN
			);
			Set<MenuId> disabledMenus = SetFactory.newHashSet(
					MenuId.VERIFY_CHECKSUM,
					MenuId.OPEN_DATABASE,
					MenuId.SAVE_APPLICATION_FILE,
					MenuId.SECRET_KEY,
					MenuId.SECRET_KEY_NEW,
					MenuId.OPEN_PRIVATE_KEY,
					MenuId.OBFUSCATION,
					MenuId.SIMPLE_OBFUSCATION,
					MenuId.OPERATED_OBFUSCATION,
					MenuId.CONVERT,
					MenuId.CONSOLE,
					MenuId.SEARCH
			);
			enabledMenuIdsWithEmptyModel = new LinkedHashMap<>();
			Arrays.stream(BaseMenuId.values())
					.forEach(baseMenuId -> {
						if(disabledBaseMenus.contains(baseMenuId)) {
							enabledMenuIdsWithEmptyModel.put(baseMenuId.propertiesKey(), false);
						} else {
							enabledMenuIdsWithEmptyModel.put(baseMenuId.propertiesKey(), true);
						}
					});
			Arrays.stream(MenuId.values())
					.forEach(menuId -> {
						if(disabledMenus.contains(menuId)) {
							enabledMenuIdsWithEmptyModel.put(menuId.propertiesKey(), false);
						} else {
							enabledMenuIdsWithEmptyModel.put(menuId.propertiesKey(), true);
						}
					});
		}
		return enabledMenuIdsWithEmptyModel;
	}

	public Map<String, Boolean> getEnabledMenuIdsWithExistingModel() {
		if(enabledMenuIdsWithExistingModel == null) {

			Set<MenuId> disabledMenus = SetFactory.newHashSet(
					MenuId.SAVE_APPLICATION_FILE
			);
			final Map<String, Boolean> menuIds = new LinkedHashMap<>();
			Arrays.stream(BaseMenuId.values())
					.forEach(baseMenuId -> menuIds.put(baseMenuId.propertiesKey(), true));
			MenuId[] values = MenuId.values();
			Arrays.stream(values)
					.forEach(menuId -> {
						if(disabledMenus.contains(menuId)) {
							menuIds.put(menuId.propertiesKey(), false);
						} else {
							menuIds.put(menuId.propertiesKey(), true);
						}
					});
			enabledMenuIdsWithExistingModel = menuIds;
		}
		return enabledMenuIdsWithExistingModel;
	}


	@Override
	public void onEvent(EventObject<RenderMode> event) {
		RenderMode renderMode = event.getSource();
		final Set<String> saveToolBarMenus = SetFactory.newHashSet(
				MenuId.SAVE_APPLICATION_FILE_TOOL_BAR.propertiesKey()
		);
		Set<String> saveMenus = SetFactory.newHashSet(
				MenuId.SAVE_APPLICATION_FILE.propertiesKey()
		);

		ApplicationToolbar toolBar = (ApplicationToolbar) MysticCryptApplicationFrame.getInstance().getToolBar();

		Set<JButton> allButtonElements = toolBar.getToolbarButtons();
		allButtonElements.forEach(jButton -> {
			if(saveToolBarMenus.contains(jButton.getName())) {
				jButton.setEnabled(RenderMode.EDITABLE.equals(renderMode));
			}
		});
		JMenuBar menubar = getMenubar();
		List<MenuElement> allMenuElements = ParentMenuResolver.getAllMenuElements(menubar,
				true);
		allMenuElements.forEach(menuElement -> {
			String name = menuElement.getComponent().getName();
			if (saveMenus.contains(name))
			{
				menuElement.getComponent()
						.setEnabled(RenderMode.EDITABLE.equals(renderMode));
			}
		});
	}
}

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

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;

import io.github.astrapi69.collection.set.SetFactory;
import io.github.astrapi69.component.model.enumeration.visibility.RenderMode;
import io.github.astrapi69.design.pattern.observer.event.EventListener;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.design.pattern.observer.event.EventSource;
import io.github.astrapi69.lang.ClassExtensions;
import io.github.astrapi69.mystic.crypt.action.ApplicationToggleFullScreenAction;
import io.github.astrapi69.mystic.crypt.action.ExportKeePassDatabaseAction;
import io.github.astrapi69.mystic.crypt.action.ImportKeePassDatabaseAction;
import io.github.astrapi69.mystic.crypt.action.LockWorkspaceAction;
import io.github.astrapi69.mystic.crypt.action.NewSettingsFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenDatabaseTreeFrameAction;
import io.github.astrapi69.mystic.crypt.action.OpenPrivateKeyAction;
import io.github.astrapi69.mystic.crypt.action.SaveApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.SaveAsApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.SearchApplicationFileAction;
import io.github.astrapi69.mystic.crypt.eventbus.ApplicationEventBus;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.swing.action.ExitApplicationAction;
import io.github.astrapi69.swing.base.BaseDesktopMenu;
import io.github.astrapi69.swing.base.BaseMenuId;
import io.github.astrapi69.swing.dialog.info.InfoDialog;
import io.github.astrapi69.swing.dialog.info.InfoPanel;
import io.github.astrapi69.swing.menu.KeyStrokeExtensions;
import io.github.astrapi69.swing.menu.MenuExtensions;
import io.github.astrapi69.swing.menu.ParentMenuResolver;
import io.github.astrapi69.swing.menu.model.KeyStrokeInfo;
import io.github.astrapi69.swing.menu.model.MenuItemInfo;
import io.github.astrapi69.swing.panel.info.InfoModelBean;
import lombok.NonNull;
import lombok.extern.java.Log;

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
			.text(Messages.getString(BaseMenuId.EDIT.propertiesKey()))
			.mnemonic(MenuExtensions.toMnemonic('E')).name(BaseMenuId.EDIT.propertiesKey()).build()
			.toJMenu();

		// note: "Verify checksum" used to be a built-in edit-menu item - it now ships as the
		// internal checksum plugin (plugins/checksum-plugin) and appears under the "Plugins" menu

		// @formatter:off
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
		final JMenu fileMenu = MenuItemInfo.builder().text("File")
			.name(BaseMenuId.FILE.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('F')).build()
			.toJMenu();
		// Open Database

		JMenuItem openDatabaseMenuItem = MenuItemInfo.builder().text("Open Database")
			.name(MenuId.OPEN_DATABASE.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('D'))
			.keyStrokeInfo(
				KeyStrokeInfo.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed D")))
			.actionListener(new OpenDatabaseTreeFrameAction("Open Database")).build().toJMenuItem();
		fileMenu.add(openDatabaseMenuItem);

		// Search the open database
		JMenuItem searchMenuItem = MenuItemInfo.builder().text("Search...")
			.name(MenuId.SEARCH.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('F'))
			.keyStrokeInfo(
				KeyStrokeInfo.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed F")))
			.actionListener(new SearchApplicationFileAction("Search")).build().toJMenuItem();
		fileMenu.add(searchMenuItem);

		// Save application file
		JMenuItem saveApplicationFileMenuItem = MenuItemInfo.builder().text("Save")
			.name(MenuId.SAVE_APPLICATION_FILE.propertiesKey())
			.mnemonic(MenuExtensions.toMnemonic('S'))
			.keyStrokeInfo(
				KeyStrokeInfo.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed S")))

			.actionListener(new SaveApplicationFileAction("Save")).build().toJMenuItem();
		fileMenu.add(saveApplicationFileMenuItem);

		// Save as a new database file
		JMenuItem saveAsApplicationFileMenuItem = MenuItemInfo.builder().text("Save As...")
			.name(MenuId.SAVE_AS_APPLICATION_FILE.propertiesKey())
			.mnemonic(MenuExtensions.toMnemonic('A'))
			.keyStrokeInfo(KeyStrokeInfo
				.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl shift pressed S")))
			.actionListener(new SaveAsApplicationFileAction("Save As")).build().toJMenuItem();
		fileMenu.add(saveAsApplicationFileMenuItem);

		// Separator
		fileMenu.addSeparator();

		// Import from KeePass
		JMenuItem importKeePassMenuItem = MenuItemInfo.builder().text("Import from KeePass...")
			.name(MenuId.IMPORT_KEEPASS.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('I'))
			.actionListener(new ImportKeePassDatabaseAction("Import from KeePass")).build()
			.toJMenuItem();
		fileMenu.add(importKeePassMenuItem);

		// Export to KeePass
		JMenuItem exportKeePassMenuItem = MenuItemInfo.builder().text("Export to KeePass...")
			.name(MenuId.EXPORT_KEEPASS.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('X'))
			.actionListener(new ExportKeePassDatabaseAction("Export to KeePass")).build()
			.toJMenuItem();
		fileMenu.add(exportKeePassMenuItem);

		// Separator
		fileMenu.addSeparator();

		// Lock workspace (hide content behind a master-password prompt)
		JMenuItem lockWorkspaceMenuItem = MenuItemInfo.builder().text("Lock workspace")
			.name(MenuId.LOCK_WORKSPACE.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('L'))
			.keyStrokeInfo(
				KeyStrokeInfo.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed L")))
			.actionListener(new LockWorkspaceAction("Lock workspace")).build().toJMenuItem();
		fileMenu.add(lockWorkspaceMenuItem);

		// @formatter:off
		// Main secret key menu
		// @formatter:on
		final JMenu keyMenu = MenuItemInfo.builder().text("Secret key")
			.name(MenuId.SECRET_KEY.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('K'))
			.build().toJMenu();
		fileMenu.add(keyMenu);
		// @formatter:off
		// @formatter:on
		// note: "New key generation" used to be a built-in item under this Secret key submenu -
		// it now ships as the internal keygen plugin (plugins/keygen-plugin) and appears under the
		// "Plugins" menu
		// Open private key
		JMenuItem openPrivateKeyMenuItem = MenuItemInfo.builder().text("Open private key")
			.name(MenuId.OPEN_PRIVATE_KEY.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('P'))
			.keyStrokeInfo(
				KeyStrokeInfo.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("ctrl pressed P")))

			.actionListener(new OpenPrivateKeyAction("Open private key", getApplicationFrame()))
			.build().toJMenuItem();
		keyMenu.add(openPrivateKeyMenuItem);
		// note: obfuscation used to be wired here as two built-in menu items - it now ships as the
		// internal obfuscation plugin (plugins/obfuscation-plugin) and appears under the "Plugins"
		// menu instead

		// note: "Convert *.der-file to *.pem-file" used to be a built-in file-menu item - it now
		// ships as the internal conversion plugin (plugins/conversion-plugin) and appears under
		// the "Plugins" menu

		// Fullscreen
		JMenuItem toggleFullscreenMenuItem = MenuItemInfo.builder().text("Toggle Fullscreen")
			.name(BaseMenuId.TOGGLE_FULLSCREEN.propertiesKey())
			.mnemonic(MenuExtensions.toMnemonic('T'))
			.keyStrokeInfo(
				KeyStrokeInfo.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("alt pressed F11")))

			.actionListener(new ApplicationToggleFullScreenAction("Fullscreen",
				MysticCryptApplicationFrame.getInstance()))
			.build().toJMenuItem();
		fileMenu.add(toggleFullscreenMenuItem);

		// note: "Console" used to be a built-in file-menu item - it now ships as the internal
		// console plugin (plugins/console-plugin) and appears under the "Plugins" menu

		// Separator
		fileMenu.addSeparator();

		// note: "Create Certificate" now ships as the internal certificate plugin
		// (plugins/certificate-plugin) and appears under the "Plugins" menu

		// Settings (plugins management + general preferences); always available
		JMenuItem settingsMenuItem = MenuItemInfo.builder().text("Settings...")
			.name(MenuId.SETTINGS.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('G'))
			.actionListener(new NewSettingsFrameAction("Settings")).build().toJMenuItem();
		fileMenu.add(settingsMenuItem);

		// Exit
		JMenuItem exitMenuItem = MenuItemInfo.builder().text("Exit")
			.name(BaseMenuId.EXIT.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('E'))
			.keyStrokeInfo(
				KeyStrokeInfo.toKeyStrokeInfo(KeyStrokeExtensions.getKeyStroke("alt pressed F4")))
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
						.version(Messages.getString("InfoJPanel.version.value", "8.1-SNAPSHOT"))
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

	/**
	 * Rearranges the menu bar into a best-practice layout: the top-level "Look and Feel" menu and
	 * the "View Mode" submenu (the app's only Edit item) are moved under a single new top-level
	 * "View" menu, and the now-empty Edit menu is dropped - leaving File | View | ... | Help
	 */
	public void reorganizeMenus()
	{
		JMenuBar menubar = getMenubar();
		JMenu viewMenu = MenuItemInfo.builder().text("View").name("global.menu.view")
			.mnemonic(MenuExtensions.toMnemonic('V')).build().toJMenu();

		// move the top-level Look and Feel menu under View
		JMenu lookAndFeelMenu = getLookAndFeelMenu();
		if (lookAndFeelMenu != null)
		{
			menubar.remove(lookAndFeelMenu);
			viewMenu.add(lookAndFeelMenu);
		}
		// drop the now-empty Edit menu and put View where Edit was (right after File)
		int insertIndex = menubar.getMenuCount();
		JMenu editMenu = getEditMenu();
		if (editMenu != null)
		{
			int editIndex = menubar.getComponentIndex(editMenu);
			if (editIndex >= 0)
			{
				insertIndex = editIndex;
				menubar.remove(editMenu);
			}
		}
		menubar.add(viewMenu, insertIndex);
		menubar.revalidate();
		menubar.repaint();
	}

	/**
	 * Builds the "Plugins" menu from the given extensions and, if it has at least one item,
	 * appends it to the menu bar
	 *
	 * @param contributions
	 *            the {@link PluginMenuContribution} extensions found by the plugin manager
	 * @return the created "Plugins" {@link JMenu}, not attached to the menu bar if no items were
	 *         contributed
	 */
	public JMenu addPluginsMenu(@NonNull List<PluginMenuContribution> contributions)
	{
		JMenuBar menubar = getMenubar();
		// remove a previously built plugins menu so a refresh replaces it instead of stacking a
		// second "Plugins" menu onto the menu bar
		for (int index = menubar.getMenuCount() - 1; index >= 0; index--)
		{
			JMenu existing = menubar.getMenu(index);
			if (existing != null && MenuId.PLUGINS.propertiesKey().equals(existing.getName()))
			{
				menubar.remove(index);
			}
		}

		JMenu pluginsMenu = MenuItemInfo.builder()
			.text(Messages.getString(MenuId.PLUGINS.propertiesKey(), "Plugins"))
			.name(MenuId.PLUGINS.propertiesKey()).mnemonic(MenuExtensions.toMnemonic('P')).build()
			.toJMenu();

		for (PluginMenuContribution contribution : contributions)
		{
			try
			{
				List<JMenuItem> items = contribution.getMenuItems();
				if (items == null || items.isEmpty())
				{
					continue;
				}
				String menuName = contribution.getMenuName();
				if (menuName != null && !menuName.isBlank())
				{
					// group this plugin's items under its own submenu of the "Plugins" menu
					JMenu pluginSubmenu = new JMenu(menuName);
					pluginSubmenu.setName(menuName);
					items.forEach(pluginSubmenu::add);
					pluginsMenu.add(pluginSubmenu);
				}
				else
				{
					// no submenu name declared: add the items directly to the "Plugins" menu
					items.forEach(pluginsMenu::add);
				}
			}
			catch (RuntimeException runtimeException)
			{
				log.log(Level.WARNING, "Plugin extension " + contribution.getClass().getName()
					+ " failed to provide menu items", runtimeException);
			}
		}
		if (pluginsMenu.getItemCount() > 0)
		{
			// best practice: the Help menu stays last, so insert the Plugins menu just before it
			JMenu helpMenu = getHelpMenu();
			int helpIndex = helpMenu == null ? -1 : menubar.getComponentIndex(helpMenu);
			if (helpIndex >= 0)
			{
				menubar.add(pluginsMenu, helpIndex);
			}
			else
			{
				menubar.add(pluginsMenu);
			}
		}
		menubar.revalidate();
		menubar.repaint();
		return pluginsMenu;
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
			toolBar.getToolbarItems().forEach(toolbarItem -> toolbarItem
				.setEnabled(!disabledToolBarMenus.contains(toolbarItem.getName())));
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
		toolBar.getToolbarItems().forEach(toolbarItem -> toolbarItem
			.setEnabled(!disabledToolBarMenus.contains(toolbarItem.getName())));
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
					MenuId.OPEN_DATABASE,
					MenuId.SAVE_APPLICATION_FILE,
					MenuId.SAVE_AS_APPLICATION_FILE,
					MenuId.SECRET_KEY,
					MenuId.OPEN_PRIVATE_KEY,
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

		toolBar.getToolbarItems().forEach(toolbarItem -> {
			if (saveToolBarMenus.contains(toolbarItem.getName()))
			{
				toolbarItem.setEnabled(RenderMode.EDITABLE.equals(renderMode));
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
